/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/** Stage-4 Monte Carlo runner coupling current climate/T1 geometry to stage 3. */
public final class TownStageFourSimulator {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    private TownStageFourSimulator() {
    }

    public static SimulationRun run(
            Path projectRoot,
            Path packRoot,
            Path scenarioPath,
            Path outputOverride,
            Integer runsOverride,
            Long seedOverride
    ) throws IOException {
        TownStageFourScenario scenario = TownStageFourScenario.load(scenarioPath);
        TownStageOneTwoData data = TownStageOneTwoData.load(projectRoot, packRoot);
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        int runs = runsOverride == null ? scenario.town().simulation().runs()
                : requirePositive(runsOverride, "runs");
        long seed = seedOverride == null ? scenario.town().simulation().seed() : seedOverride;
        Path output = outputOverride == null
                ? projectRoot.resolve("build/town-model/stage4")
                .resolve(safeName(scenario.town().metadata().name()))
                : outputOverride;
        Files.createDirectories(output);

        TownStageFourModel.ThermalLayout layout =
                TownStageFourModel.analyzeLayout(scenario, parameters);
        CapacityTheory capacity = capacityTheory(scenario, parameters, layout);
        int huntingCapacity = TownStageFourModel.huntingCapacity(scenario, parameters);
        TownStageThreeScenario townScenario = scenario.town().withWorkplaces(
                new TownStageThreeScenario.Workplaces(
                        scenario.town().workplaces().mineCapacity(),
                        huntingCapacity,
                        scenario.town().workplaces().huntRating()));
        Execution execution = execute(
                scenario, townScenario, data, parameters, layout, capacity, runs, seed);
        Summary summary = new Summary(
                1, 4,
                "current-climate-one-t1-sphere-multiday",
                scenario.town().metadata(), runs, scenario.town().simulation().days(), seed,
                List.of(
                        "Ordinary long-term climate uses current event probabilities, durations, Gaussian perturbations, Hermite interpolation and three-track max-positive plus min-negative combination.",
                        "The opening story blizzard is excluded and climate is burned in before the sampled interval.",
                        "All reference interior voxels are above sea level, so climate block affection is alpha=0.5; scenario altitude contribution is disabled.",
                        "T1 heat uses the current integer sphere, maximum heat value and block-temperature ceiling formula.",
                        "Heat inertia and random T1 level ramp are excluded: served hours use the configured steady T1 level and unserved hours immediately lose heat.",
                        "A partial daily tower service fraction is placed at the start of the following 24-hour interval; exact intra-day outage timing is not represented by stage-3 fuel settlement.",
                        "Building temperature is the spatial mean of current block temperatures over explicit interior voxels. Only the configured morning hour enters town settlement.",
                        "Mine production remains temperature-independent; a cold hunting base skips production, while existing cold-house residents still consume food and receive temperature stress."),
                parameters,
                data.sourceFiles(),
                scenario,
                layout,
                huntingCapacity,
                capacity,
                aggregate(execution.rows()));

        Path summaryPath = output.resolve("summary.json");
        Path runsPath = output.resolve("runs.csv");
        Path dailyPath = output.resolve("daily.csv");
        Path dailyAggregatePath = output.resolve("daily-aggregate.csv");
        Path hourlyPath = output.resolve("hourly.csv");
        Path buildingsPath = output.resolve("buildings.csv");
        Files.writeString(summaryPath, GSON.toJson(summary) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        writeRuns(runsPath, execution.rows());
        writeDaily(dailyPath, execution.firstRunDaily());
        writeDailyAggregate(dailyAggregatePath, execution.dailyAggregate());
        writeHourly(hourlyPath, execution.hourlyTrace());
        writeBuildings(buildingsPath, layout);
        return new SimulationRun(
                output, summaryPath, runsPath, dailyPath, dailyAggregatePath,
                hourlyPath, buildingsPath, summary);
    }

    static Execution execute(
            TownStageFourScenario scenario,
            TownStageThreeScenario townScenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            TownStageFourModel.ThermalLayout layout,
            CapacityTheory capacityTheory,
            int runs,
            long seed
    ) {
        List<RunRow> rows = new ArrayList<>(runs);
        List<DailyTrace> daily = new ArrayList<>();
        List<HourlyTrace> hourlyTrace = new ArrayList<>();
        int days = townScenario.simulation().days();
        DailyAccumulator dailyAggregate = new DailyAccumulator(days, runs);
        for (int run = 0; run < runs; run++) {
            long runSeed = mixedSeed(seed, run);
            SplittableRandom townRandom = new SplittableRandom(runSeed ^ 0xA49E2D8391C34621L);
            TownStageFourModel.ClimateSeries climate = TownStageFourModel.climateSeries(
                    runSeed, scenario.climateBurnInDays(), days, parameters);
            TownStageThreeState state = TownStageThreeState.initial(townScenario);
            double previousTowerService = townScenario.tower().activeFraction() > 0.0
                    && state.amount(townScenario.tower().fuelItem()) >= 1.0
                    ? 1.0 : 0.0;
            int houseWorkableHours = 0;
            int huntingWorkableHours = 0;
            int morningHouseWorkableDays = 0;
            int morningHuntingWorkableDays = 0;
            int towerStarvedHours = 0;
            int climateServiceableHours = 0;
            double minimumClimateTemperature = Double.POSITIVE_INFINITY;
            double minimumHouseTemperature = Double.POSITIVE_INFINITY;
            double minimumHuntingTemperature = Double.POSITIVE_INFINITY;
            TownStageThreeModel.DayResult finalDay = null;
            for (int day = 0; day < days; day++) {
                TownStageFourModel.HourThermalResult morning = null;
                int servedHours = Math.max(0, Math.min(24,
                        (int) Math.round(24.0 * previousTowerService)));
                for (int hour = 0; hour < 24; hour++) {
                    boolean heatActive = hour < servedHours
                            && townScenario.tower().activeFraction() > 0.0;
                    if (!heatActive && townScenario.tower().activeFraction() > 0.0) towerStarvedHours++;
                    TownStageFourModel.HourThermalResult thermal =
                            TownStageFourModel.evaluateHour(
                                    climate.temperature(day, hour), heatActive,
                                    scenario, parameters, layout);
                    minimumClimateTemperature = Math.min(
                            minimumClimateTemperature, thermal.climateTemperatureCelsius());
                    if (thermal.climateTemperatureCelsius()
                            >= capacityTheory.fullyCoveredMinimumClimateCelsius()) {
                        climateServiceableHours++;
                    }
                    double houseTemperature = thermal.building("house").temperatureCelsius();
                    double huntingTemperature = thermal.building("hunt").temperatureCelsius();
                    boolean houseWorkable = houseTemperature >= parameters.housing().minimumTemperatureCelsius()
                            && houseTemperature <= parameters.housing().maximumTemperatureCelsius();
                    boolean huntingWorkable = huntingTemperature
                            >= parameters.hunting().minimumWorkingTemperatureCelsius();
                    if (houseWorkable) houseWorkableHours++;
                    if (huntingWorkable) huntingWorkableHours++;
                    minimumHouseTemperature = Math.min(minimumHouseTemperature, houseTemperature);
                    minimumHuntingTemperature = Math.min(minimumHuntingTemperature, huntingTemperature);
                    if (hour == scenario.morningHour()) morning = thermal;
                    if (run == 0) {
                        hourlyTrace.add(new HourlyTrace(
                                day, hour, thermal.climateTemperatureCelsius(),
                                thermal.naturalBlockTemperatureCelsius(), heatActive,
                                houseTemperature, huntingTemperature,
                                houseWorkable, huntingWorkable));
                    }
                }
                if (morning == null) throw new IllegalStateException("Missing morning thermal snapshot.");
                TownStageThreeModel.DailyEnvironment environment =
                        TownStageFourModel.dailyEnvironment(morning, scenario, parameters);
                if (environment.houseAcceptsNewResidents()) morningHouseWorkableDays++;
                if (environment.huntingWorkable()) morningHuntingWorkableDays++;
                TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                        state, townScenario, data, parameters, townRandom, environment);
                dailyAggregate.accept(run, result);
                previousTowerService = result.towerServiceFraction();
                finalDay = result;
                if (run == 0) {
                    daily.add(new DailyTrace(
                            day, morning.climateTemperatureCelsius(),
                            environment.houseTemperatureCelsius(), environment.huntingRating(),
                            morning.building("hunt").temperatureCelsius(),
                            environment.houseAcceptsNewResidents(), environment.huntingWorkable(),
                            result.population(), result.cumulativeDeaths(), result.assignedMiners(),
                            result.assignedHunters(), result.miningSwe(), result.huntingSwe(),
                            result.foodSatisfaction(), result.foodReserveDays(), result.fuelReserveDays(),
                            result.towerServiceFraction(), result.minimumHealth(), result.minimumMental()));
                }
            }
            if (finalDay == null) throw new IllegalStateException("Stage-4 simulation has no days.");
            int hours = days * 24;
            rows.add(new RunRow(
                    run, runSeed, finalDay.population(), state.deaths(),
                    state.firstFoodShortageDay(), state.firstFuelShortageDay(),
                    minimumHouseTemperature, minimumHuntingTemperature,
                    minimumClimateTemperature, fraction(climateServiceableHours, hours),
                    fraction(houseWorkableHours, hours), fraction(huntingWorkableHours, hours),
                    fraction(morningHouseWorkableDays, days), fraction(morningHuntingWorkableDays, days),
                    towerStarvedHours,
                    finalDay.foodReserveDays(), finalDay.fuelReserveDays(),
                    divide(state.cumulativeCoalRequested(), state.cumulativeRawCoalDemand()),
                    divide(state.cumulativeHuntingFoodPotential(), state.cumulativeFoodDemand()),
                    state.deaths() == 0,
                    state.deaths() == 0
                            && state.firstFoodShortageDay() == null
                            && state.firstFuelShortageDay() == null));
        }
        return new Execution(
                List.copyOf(rows), List.copyOf(daily), dailyAggregate.finish(),
                List.copyOf(hourlyTrace));
    }

    static CapacityTheory capacityTheory(
            TownStageFourScenario scenario,
            TownModelParameters parameters,
            TownStageFourModel.ThermalLayout layout
    ) {
        double dimensionPlusBiome = scenario.location().dimensionTemperatureCelsius()
                + scenario.location().biomeTemperatureCelsius();
        double heat = layout.heatTemperatureCelsius();
        double heatMultiplier = parameters.climate().blockHeatApplicationMultiplier();
        double minimumThreshold = parameters.housing().minimumTemperatureCelsius();
        double alpha = parameters.climate().blockMaximumClimateAffection();
        double fullyCoveredClimateLimit =
                (minimumThreshold - dimensionPlusBiome - heatMultiplier * heat) / alpha;
        List<BuildingThermalLimit> buildingLimits = layout.buildings().stream()
                .map(building -> {
                    double threshold = "house".equals(building.role())
                            ? parameters.housing().minimumTemperatureCelsius()
                            : parameters.hunting().minimumWorkingTemperatureCelsius();
                    double minimumClimate = (threshold - dimensionPlusBiome
                            - heatMultiplier * heat * building.coverageFraction()) / alpha;
                    return new BuildingThermalLimit(
                            building.id(), building.role(), building.coverageFraction(),
                            threshold, minimumClimate);
                }).toList();
        return new CapacityTheory(
                alpha,
                dimensionPlusBiome,
                heat,
                heatMultiplier,
                fullyCoveredClimateLimit,
                layout.centeredThreeHighFloorUpperBoundBlocks(),
                layout.centeredThreeHighHousingUpperBoundResidents(),
                buildingLimits,
                "The climate limit solves dimension + biome + alpha*climate + heatMultiplier*heat*coverage = threshold before the heat ceiling.");
    }

    static AggregateSummary aggregate(List<RunRow> rows) {
        long survived = rows.stream().filter(RunRow::survived).count();
        long noShortage = rows.stream().filter(RunRow::noShortage).count();
        long foodShortage = rows.stream()
                .filter(row -> row.firstFoodShortageDay() != null).count();
        long fuelShortage = rows.stream()
                .filter(row -> row.firstFuelShortageDay() != null).count();
        return new AggregateSummary(
                fraction(survived, rows.size()), wilson95(survived, rows.size()),
                fraction(noShortage, rows.size()), wilson95(noShortage, rows.size()),
                fraction(foodShortage, rows.size()), wilson95(foodShortage, rows.size()),
                fraction(fuelShortage, rows.size()), wilson95(fuelShortage, rows.size()),
                statistics(rows.stream().mapToDouble(RunRow::fuelPotentialCoverage).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::foodPotentialCoverage).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::minimumHouseTemperatureCelsius).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::minimumHuntingTemperatureCelsius).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::minimumClimateTemperatureCelsius).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::climateServiceableHourFraction).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::houseWorkableHourFraction).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::huntingWorkableHourFraction).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::morningHouseWorkableDayFraction).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::morningHuntingWorkableDayFraction).toArray()),
                statistics(rows.stream().mapToDouble(RunRow::towerStarvedHours).toArray()));
    }

    private static void writeRuns(Path path, List<RunRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("run,seed,final_population,deaths,first_food_shortage_day,first_fuel_shortage_day,"
                    + "minimum_house_temperature_c,minimum_hunting_temperature_c,minimum_climate_temperature_c,"
                    + "climate_serviceable_hour_fraction,house_workable_hour_fraction,"
                    + "hunting_workable_hour_fraction,morning_house_workable_day_fraction,"
                    + "morning_hunting_workable_day_fraction,tower_starved_hours,final_food_reserve_days,"
                    + "final_fuel_reserve_days,fuel_potential_coverage,food_potential_coverage,"
                    + "survived,no_shortage\n");
            for (RunRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%d,%s,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%d,%.9f,%.9f,%.9f,%.9f,%s,%s%n",
                        row.run(), row.seed(), row.finalPopulation(), row.deaths(),
                        nullableInteger(row.firstFoodShortageDay()),
                        nullableInteger(row.firstFuelShortageDay()),
                        row.minimumHouseTemperatureCelsius(), row.minimumHuntingTemperatureCelsius(),
                        row.minimumClimateTemperatureCelsius(), row.climateServiceableHourFraction(),
                        row.houseWorkableHourFraction(), row.huntingWorkableHourFraction(),
                        row.morningHouseWorkableDayFraction(), row.morningHuntingWorkableDayFraction(),
                        row.towerStarvedHours(), row.finalFoodReserveDays(), row.finalFuelReserveDays(),
                        row.fuelPotentialCoverage(), row.foodPotentialCoverage(),
                        row.survived(), row.noShortage()));
            }
        }
    }

    private static void writeDaily(Path path, List<DailyTrace> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("day,morning_climate_c,house_temperature_c,hunting_rating,hunting_temperature_c,"
                    + "house_workable,hunting_workable,population,deaths,miners,hunters,mining_swe,hunting_swe,"
                    + "food_satisfaction,food_reserve_days,fuel_reserve_days,tower_service,minimum_health,minimum_mental\n");
            for (DailyTrace row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%.9f,%.9f,%.9f,%.9f,%s,%s,%d,%d,%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                        row.day(), row.morningClimateCelsius(), row.houseTemperatureCelsius(),
                        row.huntingRating(), row.huntingTemperatureCelsius(), row.houseWorkable(),
                        row.huntingWorkable(), row.population(), row.deaths(), row.miners(), row.hunters(),
                        row.miningSwe(), row.huntingSwe(), row.foodSatisfaction(), row.foodReserveDays(),
                        row.fuelReserveDays(), row.towerService(), row.minimumHealth(), row.minimumMental()));
            }
        }
    }

    private static void writeDailyAggregate(
            Path path,
            List<DailyAggregate> rows
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("day,mean_population,mean_deaths,food_reserve_p05,food_reserve_p50,"
                    + "food_reserve_p95,fuel_reserve_p05,fuel_reserve_p50,fuel_reserve_p95\n");
            for (DailyAggregate row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                        row.day(), row.meanPopulation(), row.meanDeaths(),
                        row.foodReserve().p05(), row.foodReserve().p50(), row.foodReserve().p95(),
                        row.fuelReserve().p05(), row.fuelReserve().p50(), row.fuelReserve().p95()));
            }
        }
    }

    private static void writeHourly(Path path, List<HourlyTrace> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("day,hour,climate_c,natural_block_temperature_c,heat_active,house_temperature_c,"
                    + "hunting_temperature_c,house_workable,hunting_workable\n");
            for (HourlyTrace row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%.9f,%.9f,%s,%.9f,%.9f,%s,%s%n",
                        row.day(), row.hour(), row.climateCelsius(), row.naturalTemperatureCelsius(),
                        row.heatActive(), row.houseTemperatureCelsius(), row.huntingTemperatureCelsius(),
                        row.houseWorkable(), row.huntingWorkable()));
            }
        }
    }

    private static void writeBuildings(
            Path path,
            TownStageFourModel.ThermalLayout layout
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("id,role,floor_area_blocks,interior_voxels,covered_voxels,coverage_fraction\n");
            for (TownStageFourModel.BuildingGeometry building : layout.buildings()) {
                writer.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%.9f%n",
                        building.id(), building.role(), building.floorAreaBlocks(),
                        building.voxelCount(), building.coveredVoxelCount(),
                        building.coverageFraction()));
            }
        }
    }

    public static void printSummary(SimulationRun run) {
        Summary summary = run.summary();
        System.out.printf(Locale.ROOT,
                "Stage 4: %s%nRuns: %d, days: %d%nT1 sphere: r=%d, %d lattice voxels%n"
                        + "Reference building utilization: %.3f%nSurvival probability: %.3f%n"
                        + "House workable hours mean: %.3f%nHunting workable hours mean: %.3f%nOutput: %s%n",
                summary.metadata().name(), summary.runs(), summary.days(),
                summary.layout().radiusBlocks(), summary.layout().sphereVoxelCount(),
                summary.layout().fieldUtilizationFraction(),
                summary.aggregate().survivalProbability(),
                summary.aggregate().houseWorkableHourFraction().mean(),
                summary.aggregate().huntingWorkableHourFraction().mean(),
                run.outputDirectory());
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive.");
        return value;
    }

    private static String safeName(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return result.isBlank() ? "scenario" : result;
    }

    private static String nullableInteger(Integer value) {
        return value == null ? "" : value.toString();
    }

    static double divide(double numerator, double denominator) {
        return denominator > 0.0 ? numerator / denominator : 1.0;
    }

    static double fraction(long count, long total) {
        return total > 0L ? (double) count / total : 0.0;
    }

    static long mixedSeed(long seed, int run) {
        long value = seed + 0x9E3779B97F4A7C15L * (run + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    static Distribution statistics(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        double mean = Arrays.stream(sorted).average().orElse(0.0);
        double variance = 0.0;
        for (double value : sorted) variance += (value - mean) * (value - mean);
        variance = sorted.length > 1 ? variance / (sorted.length - 1) : 0.0;
        return new Distribution(
                mean, Math.sqrt(variance), percentile(sorted, 0.05),
                percentile(sorted, 0.50), percentile(sorted, 0.95));
    }

    private static double percentile(double[] sorted, double probability) {
        if (sorted.length == 0) return 0.0;
        double position = probability * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) return sorted[lower];
        double fraction = position - lower;
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
    }

    static Interval wilson95(long events, long trials) {
        if (trials <= 0L) return new Interval(0.0, 0.0);
        double z = 1.959963984540054;
        double probability = (double) events / trials;
        double zSquared = z * z;
        double denominator = 1.0 + zSquared / trials;
        double center = (probability + zSquared / (2.0 * trials)) / denominator;
        double radius = z * Math.sqrt(
                probability * (1.0 - probability) / trials
                        + zSquared / (4.0 * trials * trials)) / denominator;
        return new Interval(
                Math.max(0.0, center - radius),
                Math.min(1.0, center + radius));
    }

    private static final class DailyAccumulator {
        private final double[][] population;
        private final double[][] deaths;
        private final double[][] foodReserve;
        private final double[][] fuelReserve;

        private DailyAccumulator(int days, int runs) {
            population = new double[days][runs];
            deaths = new double[days][runs];
            foodReserve = new double[days][runs];
            fuelReserve = new double[days][runs];
        }

        private void accept(int run, TownStageThreeModel.DayResult result) {
            int day = result.day();
            population[day][run] = result.population();
            deaths[day][run] = result.cumulativeDeaths();
            foodReserve[day][run] = result.foodReserveDays();
            fuelReserve[day][run] = result.fuelReserveDays();
        }

        private List<DailyAggregate> finish() {
            List<DailyAggregate> result = new ArrayList<>(population.length);
            for (int day = 0; day < population.length; day++) {
                result.add(new DailyAggregate(
                        day,
                        Arrays.stream(population[day]).average().orElse(0.0),
                        Arrays.stream(deaths[day]).average().orElse(0.0),
                        statistics(foodReserve[day]),
                        statistics(fuelReserve[day])));
            }
            return List.copyOf(result);
        }
    }

    record Execution(
            List<RunRow> rows,
            List<DailyTrace> firstRunDaily,
            List<DailyAggregate> dailyAggregate,
            List<HourlyTrace> hourlyTrace
    ) {
    }

    public record SimulationRun(
            Path outputDirectory,
            Path summaryPath,
            Path runsPath,
            Path dailyPath,
            Path dailyAggregatePath,
            Path hourlyPath,
            Path buildingsPath,
            Summary summary
    ) {
    }

    public record Summary(
            int schemaVersion,
            int modelStage,
            String model,
            TownStageThreeScenario.Metadata metadata,
            int runs,
            int days,
            long seed,
            List<String> assumptions,
            TownModelParameters parameters,
            java.util.Map<String, String> sources,
            TownStageFourScenario scenario,
            TownStageFourModel.ThermalLayout layout,
            int huntingCapacity,
            CapacityTheory capacityTheory,
            AggregateSummary aggregate
    ) {
        public Summary {
            assumptions = List.copyOf(assumptions);
            sources = java.util.Map.copyOf(sources);
        }
    }

    public record CapacityTheory(
            double climateBlockAffectionAboveSeaLevel,
            double dimensionPlusBiomeCelsius,
            double activeHeatFieldCelsius,
            double heatApplicationMultiplier,
            double fullyCoveredMinimumClimateCelsius,
            long centeredThreeHighFloorUpperBoundBlocks,
            long centeredThreeHighHousingUpperBoundResidents,
            List<BuildingThermalLimit> buildingLimits,
            String definition
    ) {
        public CapacityTheory {
            buildingLimits = List.copyOf(buildingLimits);
        }
    }

    public record BuildingThermalLimit(
            String id,
            String role,
            double coverageFraction,
            double minimumTemperatureCelsius,
            double minimumClimateCelsiusWhenHeatActive
    ) {
    }

    public record AggregateSummary(
            double survivalProbability,
            Interval survivalWilson95,
            double noShortageProbability,
            Interval noShortageWilson95,
            double foodShortageProbability,
            Interval foodShortageWilson95,
            double fuelShortageProbability,
            Interval fuelShortageWilson95,
            Distribution fuelPotentialCoverage,
            Distribution foodPotentialCoverage,
            Distribution minimumHouseTemperatureCelsius,
            Distribution minimumHuntingTemperatureCelsius,
            Distribution minimumClimateTemperatureCelsius,
            Distribution climateServiceableHourFraction,
            Distribution houseWorkableHourFraction,
            Distribution huntingWorkableHourFraction,
            Distribution morningHouseWorkableDayFraction,
            Distribution morningHuntingWorkableDayFraction,
            Distribution towerStarvedHours
    ) {
    }

    public record Distribution(
            double mean,
            double standardDeviation,
            double p05,
            double p50,
            double p95
    ) {
    }

    public record Interval(double lower, double upper) {
    }

    public record RunRow(
            int run,
            long seed,
            int finalPopulation,
            int deaths,
            Integer firstFoodShortageDay,
            Integer firstFuelShortageDay,
            double minimumHouseTemperatureCelsius,
            double minimumHuntingTemperatureCelsius,
            double minimumClimateTemperatureCelsius,
            double climateServiceableHourFraction,
            double houseWorkableHourFraction,
            double huntingWorkableHourFraction,
            double morningHouseWorkableDayFraction,
            double morningHuntingWorkableDayFraction,
            int towerStarvedHours,
            double finalFoodReserveDays,
            double finalFuelReserveDays,
            double fuelPotentialCoverage,
            double foodPotentialCoverage,
            boolean survived,
            boolean noShortage
    ) {
    }


    public record DailyAggregate(
            int day,
            double meanPopulation,
            double meanDeaths,
            Distribution foodReserve,
            Distribution fuelReserve
    ) {
    }

    public record DailyTrace(
            int day,
            double morningClimateCelsius,
            double houseTemperatureCelsius,
            double huntingRating,
            double huntingTemperatureCelsius,
            boolean houseWorkable,
            boolean huntingWorkable,
            int population,
            int deaths,
            int miners,
            int hunters,
            double miningSwe,
            double huntingSwe,
            double foodSatisfaction,
            double foodReserveDays,
            double fuelReserveDays,
            double towerService,
            double minimumHealth,
            double minimumMental
    ) {
    }

    public record HourlyTrace(
            int day,
            int hour,
            double climateCelsius,
            double naturalTemperatureCelsius,
            boolean heatActive,
            double houseTemperatureCelsius,
            double huntingTemperatureCelsius,
            boolean houseWorkable,
            boolean huntingWorkable
    ) {
    }
}
