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
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Paired-seed population sweep for the current stage-4 T1 model. */
public final class TownStageFourPopulationSweepSimulator {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();
    private static final int BUILDING_HEIGHT_BLOCKS = 3;

    private TownStageFourPopulationSweepSimulator() {
    }

    public static SimulationRun run(
            Path projectRoot,
            Path packRoot,
            Path scenarioPath,
            Path outputOverride,
            Integer runsOverride,
            Long seedOverride
    ) throws IOException {
        TownStageFourScenario baseScenario = TownStageFourScenario.load(scenarioPath);
        TownStageFourScenario.PopulationSweep sweep = baseScenario.populationSweep();
        if (sweep == null) {
            throw new IllegalArgumentException(
                    "A stage-4 population sweep requires a populationSweep object.");
        }
        TownStageOneTwoData data = TownStageOneTwoData.load(projectRoot, packRoot);
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        int runs = runsOverride == null ? baseScenario.town().simulation().runs()
                : requirePositive(runsOverride, "runs");
        long seed = seedOverride == null ? baseScenario.town().simulation().seed() : seedOverride;
        Path output = outputOverride == null
                ? projectRoot.resolve("build/town-model/stage4")
                .resolve(safeName(baseScenario.town().metadata().name()) + "-population-sweep")
                : outputOverride;
        Files.createDirectories(output);

        List<Integer> populationPoints = populationPoints(sweep);
        Set<Integer> curvePopulations = Set.copyOf(populationPoints);
        LinkedHashSet<Integer> requested = new LinkedHashSet<>(populationPoints);
        requested.addAll(sweep.trajectoryPopulations());
        List<Integer> simulatedPopulations = requested.stream().sorted().toList();
        Set<Integer> trajectoryPopulations = Set.copyOf(sweep.trajectoryPopulations());

        List<PopulationRow> rows = new ArrayList<>(populationPoints.size());
        List<ReserveTrajectoryRow> trajectories = new ArrayList<>(
                sweep.trajectoryPopulations().size()
                        * baseScenario.town().simulation().days());
        int completed = 0;
        for (int population : simulatedPopulations) {
            TownStageFourScenario scenario = forPopulation(
                    baseScenario, population, data, parameters);
            TownStageFourModel.ThermalLayout layout =
                    TownStageFourModel.analyzeLayout(scenario, parameters);
            TownStageFourSimulator.CapacityTheory capacityTheory =
                    TownStageFourSimulator.capacityTheory(scenario, parameters, layout);
            int huntingCapacity = TownStageFourModel.huntingCapacity(scenario, parameters);
            TownStageThreeScenario townScenario = scenario.town().withWorkplaces(
                    new TownStageThreeScenario.Workplaces(
                            scenario.town().workplaces().mineCapacity(),
                            huntingCapacity,
                            scenario.town().workplaces().huntRating()));
            TownStageFourSimulator.Execution execution = TownStageFourSimulator.execute(
                    scenario, townScenario, data, parameters, layout, capacityTheory,
                    runs, seed);
            TownStageFourSimulator.AggregateSummary aggregate =
                    TownStageFourSimulator.aggregate(execution.rows());
            if (curvePopulations.contains(population)) {
                TownStageThreeTheory.FrontierPoint theory = bestTheoryPoint(
                        TownStageThreeTheory.evaluate(townScenario, data, parameters), population);
                rows.add(populationRow(
                        population, scenario, layout, capacityTheory, huntingCapacity,
                        theory, aggregate));
            }
            if (trajectoryPopulations.contains(population)) {
                for (TownStageFourSimulator.DailyAggregate daily : execution.dailyAggregate()) {
                    trajectories.add(new ReserveTrajectoryRow(
                            population, daily.day(), daily.meanPopulation(), daily.meanDeaths(),
                            daily.foodReserve(), daily.fuelReserve()));
                }
            }
            completed++;
            if (completed == 1 || completed % 5 == 0 || completed == simulatedPopulations.size()) {
                System.out.printf(Locale.ROOT,
                        "Stage-4 population sweep: %d/%d populations complete (P=%d).%n",
                        completed, simulatedPopulations.size(), population);
            }
        }
        rows.sort(Comparator.comparingInt(PopulationRow::population));
        trajectories.sort(Comparator
                .comparingInt(ReserveTrajectoryRow::population)
                .thenComparingInt(ReserveTrajectoryRow::day));

        Summary summary = new Summary(
                1, 4, "current-climate-one-t1-sphere-population-sweep",
                baseScenario.town().metadata(), runs,
                baseScenario.town().simulation().days(), seed,
                Map.of(
                        "fuelPotentialSelfSupplyRatio",
                        "Cumulative coal requested from mining before storage limits divided by cumulative raw-coal-equivalent T1 demand; initial inventory is excluded.",
                        "foodPotentialSelfSupplyRatio",
                        "Cumulative edible food potential from hunting and configured meat processing before storage limits divided by cumulative resident food demand; initial inventory is excluded.",
                        "fuelShortageProbability",
                        "Fraction of runs with at least one day when requested T1 fuel could not be supplied.",
                        "foodShortageProbability",
                        "Fraction of runs with at least one resident-day below full food satisfaction.",
                        "survivalProbability",
                        "Fraction of runs with zero resident deaths over the full simulated interval.",
                        "noShortageProbability",
                        "Fraction of runs with zero deaths, no food shortage day, and no T1 fuel shortage day.",
                        "compactCapacityLayout",
                        "For each population, each three-block-high building uses the smallest balanced integer rectangle whose current code capacity is at least that population; house and hunting interiors are stacked above sea level."),
                parameters, data.sourceFiles(), baseScenario, sweep,
                populationPoints, sweep.trajectoryPopulations(), List.copyOf(rows));

        Path summaryPath = output.resolve("summary.json");
        Path populationPath = output.resolve("population.csv");
        Path reservePath = output.resolve("reserve-trajectories.csv");
        Files.writeString(summaryPath, GSON.toJson(summary) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        writePopulation(populationPath, rows);
        writeTrajectories(reservePath, trajectories);
        return new SimulationRun(
                output, summaryPath, populationPath, reservePath, summary);
    }

    static List<Integer> populationPoints(TownStageFourScenario.PopulationSweep sweep) {
        if (sweep.populationPoints() == 1) return List.of(sweep.minimumPopulation());
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        double span = sweep.maximumPopulation() - sweep.minimumPopulation();
        for (int index = 0; index < sweep.populationPoints(); index++) {
            result.add((int) Math.round(
                    sweep.minimumPopulation()
                            + span * index / (sweep.populationPoints() - 1.0)));
        }
        if (result.size() != sweep.populationPoints()) {
            throw new IllegalStateException("Population interpolation produced duplicate points.");
        }
        return List.copyOf(result);
    }

    static TownStageFourScenario forPopulation(
            TownStageFourScenario base,
            int population,
            TownStageOneTwoData data,
            TownModelParameters parameters
    ) {
        if (population <= 0) throw new IllegalArgumentException("population must be positive.");
        Rectangle house = capacityRectangle(population, true, parameters);
        Rectangle hunt = capacityRectangle(population, false, parameters);
        TownStageThreeScenario source = base.town();
        TownStageThreeScenario.Population originalPopulation = source.population();
        TownStageThreeScenario.Population scaledPopulation = new TownStageThreeScenario.Population(
                population, originalPopulation.initialHealth(), originalPopulation.initialMental(),
                originalPopulation.initialStrength(), originalPopulation.initialIntelligence(),
                originalPopulation.initialMiningProficiency(),
                originalPopulation.initialHuntingProficiency(), originalPopulation.initialAgeDays());
        TownStageThreeScenario.House scaledHouse = new TownStageThreeScenario.House(
                source.house().temperatureCelsius(), house.area(),
                Math.multiplyExact(house.area(), BUILDING_HEIGHT_BLOCKS), population,
                source.house().decorationRating());
        TownStageThreeScenario.Workplaces scaledWorkplaces =
                new TownStageThreeScenario.Workplaces(
                        population, population, source.workplaces().huntRating());
        double foodScale = (double) population / source.population().standardAdults();
        List<TownStageThreeScenario.InventoryItem> inventory = source.warehouse()
                .initialInventory().stream()
                .map(item -> new TownStageThreeScenario.InventoryItem(
                        item.item(), data.foods().containsKey(item.item())
                                ? item.amountItems() * foodScale : item.amountItems()))
                .toList();
        TownStageThreeScenario.Warehouse warehouse = new TownStageThreeScenario.Warehouse(
                Math.max(source.warehouse().capacityItems(),
                        inventory.stream().mapToDouble(
                                TownStageThreeScenario.InventoryItem::amountItems).sum()),
                inventory);
        TownStageThreeScenario town = new TownStageThreeScenario(
                source.schemaVersion(), source.modelStage(),
                new TownStageThreeScenario.Metadata(
                        source.metadata().name() + "-p" + population,
                        "Population-scaled compact-capacity layout derived from "
                                + source.metadata().name() + "."),
                source.simulation(), scaledPopulation, scaledHouse, scaledWorkplaces,
                source.buildingOrder(), warehouse, source.processing(), source.tower(),
                source.terrain(), new TownStageThreeScenario.Diagnostics(
                        Math.max(population, source.diagnostics().frontierMaximumPopulation()),
                        false));
        int houseY = base.building("house").boxes().stream()
                .mapToInt(TownStageFourScenario.VoxelBox::minimumY).min().orElse(64);
        int huntY = Math.max(
                base.building("hunt").boxes().stream()
                        .mapToInt(TownStageFourScenario.VoxelBox::minimumY).min().orElse(67),
                houseY + BUILDING_HEIGHT_BLOCKS);
        List<TownStageFourScenario.ThermalBuilding> buildings = List.of(
                thermalBuilding("house-compact-p" + population, "house", house, houseY),
                thermalBuilding("hunt-compact-p" + population, "hunt", hunt, huntY));
        return new TownStageFourScenario(
                town, base.climateBurnInDays(), base.morningHour(), base.location(),
                base.towerCenter(), buildings, null);
    }

    private static TownStageFourScenario.ThermalBuilding thermalBuilding(
            String id,
            String role,
            Rectangle rectangle,
            int minimumY
    ) {
        TownStageFourScenario.VoxelBox box = new TownStageFourScenario.VoxelBox(
                -Math.floorDiv(rectangle.width(), 2), minimumY,
                -Math.floorDiv(rectangle.depth(), 2),
                rectangle.width(), BUILDING_HEIGHT_BLOCKS, rectangle.depth());
        return new TownStageFourScenario.ThermalBuilding(
                id, role, rectangle.area(), List.of(box));
    }

    private static Rectangle capacityRectangle(
            int population,
            boolean house,
            TownModelParameters parameters
    ) {
        int requiredArea = 1;
        while (capacity(requiredArea, population, house, parameters) < population) {
            requiredArea++;
        }
        int width = Math.max(1, (int) Math.floor(Math.sqrt(requiredArea)));
        int depth = (int) Math.ceil((double) requiredArea / width);
        Rectangle rectangle = new Rectangle(width, depth);
        if (capacity(rectangle.area(), population, house, parameters) < population) {
            throw new IllegalStateException("Balanced rectangle lost required building capacity.");
        }
        return rectangle;
    }

    private static int capacity(
            int area,
            int population,
            boolean house,
            TownModelParameters parameters
    ) {
        TownModelParameters.SpaceRatingParameters space = parameters.buildingScoring().space();
        double rating = TownMathFunctions.calculateSpaceRating(
                Math.multiplyExact(area, BUILDING_HEIGHT_BLOCKS), area,
                space.areaCoefficient(), space.heightLogCoefficient(), space.heightLogOffset(),
                space.responseScale(), space.responseExponent());
        if (house) {
            return HouseDailyModel.calculateCapacity(
                    rating, area, parameters.housing().floorBlocksPerResident(), population);
        }
        return HuntingDailyModel.calculateCapacity(
                rating, area, parameters.hunting().floorBlocksPerWorkerSlot(),
                parameters.hunting().minimumWorkerSlots());
    }

    private static TownStageThreeTheory.FrontierPoint bestTheoryPoint(
            TownStageThreeTheory.TheorySummary theory,
            int population
    ) {
        return theory.frontier().stream()
                .filter(point -> point.population() == population)
                .max(Comparator.comparingDouble(TownStageThreeTheory.FrontierPoint::jointCoverage))
                .orElseThrow();
    }

    private static PopulationRow populationRow(
            int population,
            TownStageFourScenario scenario,
            TownStageFourModel.ThermalLayout layout,
            TownStageFourSimulator.CapacityTheory capacityTheory,
            int huntingCapacity,
            TownStageThreeTheory.FrontierPoint theory,
            TownStageFourSimulator.AggregateSummary aggregate
    ) {
        TownStageFourModel.BuildingGeometry house = layout.building("house");
        TownStageFourModel.BuildingGeometry hunt = layout.building("hunt");
        return new PopulationRow(
                population, scenario.town().house().areaBlocks(),
                scenario.town().house().volumeBlocks(), huntingCapacity,
                house.coverageFraction(), hunt.coverageFraction(),
                layout.fieldUtilizationFraction(),
                capacityTheory.buildingLimits().stream()
                        .filter(limit -> "house".equals(limit.role())).findFirst()
                        .orElseThrow().minimumClimateCelsiusWhenHeatActive(),
                capacityTheory.buildingLimits().stream()
                        .filter(limit -> "hunt".equals(limit.role())).findFirst()
                        .orElseThrow().minimumClimateCelsiusWhenHeatActive(),
                theory.miners(), theory.hunters(), theory.fuelCoverage(), theory.foodCoverage(),
                aggregate.fuelPotentialCoverage(), aggregate.foodPotentialCoverage(),
                aggregate.fuelShortageProbability(), aggregate.fuelShortageWilson95(),
                aggregate.foodShortageProbability(), aggregate.foodShortageWilson95(),
                aggregate.survivalProbability(), aggregate.survivalWilson95(),
                aggregate.noShortageProbability(), aggregate.noShortageWilson95(),
                aggregate.climateServiceableHourFraction(),
                aggregate.houseWorkableHourFraction(),
                aggregate.huntingWorkableHourFraction());
    }

    private static void writePopulation(Path path, List<PopulationRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("population,house_floor_area_blocks,house_volume_blocks,hunting_capacity,"
                    + "house_coverage_fraction,hunting_coverage_fraction,field_utilization_fraction,"
                    + "house_minimum_climate_c,hunting_minimum_climate_c,theory_miners,theory_hunters,"
                    + "theory_fuel_self_supply_ratio,theory_food_self_supply_ratio,"
                    + "fuel_self_supply_mean,fuel_self_supply_p05,fuel_self_supply_p50,fuel_self_supply_p95,"
                    + "food_self_supply_mean,food_self_supply_p05,food_self_supply_p50,food_self_supply_p95,"
                    + "fuel_shortage_probability,fuel_shortage_wilson_lower,fuel_shortage_wilson_upper,"
                    + "food_shortage_probability,food_shortage_wilson_lower,food_shortage_wilson_upper,"
                    + "survival_probability,survival_wilson_lower,survival_wilson_upper,"
                    + "no_shortage_probability,no_shortage_wilson_lower,no_shortage_wilson_upper,"
                    + "climate_serviceable_p05,climate_serviceable_p50,climate_serviceable_p95,"
                    + "house_workable_p05,house_workable_p50,house_workable_p95,"
                    + "hunting_workable_p05,hunting_workable_p50,hunting_workable_p95\n");
            for (PopulationRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%d,%d,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f%n",
                        row.population(), row.houseFloorAreaBlocks(), row.houseVolumeBlocks(),
                        row.huntingCapacity(), row.houseCoverageFraction(),
                        row.huntingCoverageFraction(), row.fieldUtilizationFraction(),
                        row.houseMinimumClimateCelsius(), row.huntingMinimumClimateCelsius(),
                        row.theoryMiners(), row.theoryHunters(),
                        row.theoryFuelSelfSupplyRatio(), row.theoryFoodSelfSupplyRatio(),
                        row.fuelSelfSupplyRatio().mean(), row.fuelSelfSupplyRatio().p05(),
                        row.fuelSelfSupplyRatio().p50(), row.fuelSelfSupplyRatio().p95(),
                        row.foodSelfSupplyRatio().mean(), row.foodSelfSupplyRatio().p05(),
                        row.foodSelfSupplyRatio().p50(), row.foodSelfSupplyRatio().p95(),
                        row.fuelShortageProbability(), row.fuelShortageWilson95().lower(),
                        row.fuelShortageWilson95().upper(), row.foodShortageProbability(),
                        row.foodShortageWilson95().lower(), row.foodShortageWilson95().upper(),
                        row.survivalProbability(), row.survivalWilson95().lower(),
                        row.survivalWilson95().upper(), row.noShortageProbability(),
                        row.noShortageWilson95().lower(), row.noShortageWilson95().upper(),
                        row.climateServiceableHourFraction().p05(),
                        row.climateServiceableHourFraction().p50(),
                        row.climateServiceableHourFraction().p95(),
                        row.houseWorkableHourFraction().p05(),
                        row.houseWorkableHourFraction().p50(),
                        row.houseWorkableHourFraction().p95(),
                        row.huntingWorkableHourFraction().p05(),
                        row.huntingWorkableHourFraction().p50(),
                        row.huntingWorkableHourFraction().p95()));
            }
        }
    }

    private static void writeTrajectories(
            Path path,
            List<ReserveTrajectoryRow> rows
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("population,day,mean_population,mean_deaths,"
                    + "food_reserve_p05,food_reserve_p50,food_reserve_p95,"
                    + "fuel_reserve_p05,fuel_reserve_p50,fuel_reserve_p95\n");
            for (ReserveTrajectoryRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                        row.population(), row.day(), row.meanPopulation(), row.meanDeaths(),
                        row.foodReserve().p05(), row.foodReserve().p50(), row.foodReserve().p95(),
                        row.fuelReserve().p05(), row.fuelReserve().p50(), row.fuelReserve().p95()));
            }
        }
    }

    public static void printSummary(SimulationRun run) {
        Summary summary = run.summary();
        System.out.printf(Locale.ROOT,
                "Stage 4 population sweep: %s%nPopulations: %d points, %d..%d%n"
                        + "Runs per population: %d, days: %d%nOutput: %s%n",
                summary.metadata().name(), summary.populationPoints().size(),
                summary.populationSweep().minimumPopulation(),
                summary.populationSweep().maximumPopulation(),
                summary.runsPerPopulation(), summary.days(), run.outputDirectory());
    }

    private static String safeName(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return result.isBlank() ? "scenario" : result;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive.");
        return value;
    }

    private record Rectangle(int width, int depth) {
        private int area() {
            return Math.multiplyExact(width, depth);
        }
    }

    public record SimulationRun(
            Path outputDirectory,
            Path summaryPath,
            Path populationPath,
            Path reserveTrajectoriesPath,
            Summary summary
    ) {
    }

    public record Summary(
            int schemaVersion,
            int modelStage,
            String model,
            TownStageThreeScenario.Metadata metadata,
            int runsPerPopulation,
            int days,
            long seed,
            Map<String, String> metricDefinitions,
            TownModelParameters parameters,
            Map<String, String> sources,
            TownStageFourScenario baseScenario,
            TownStageFourScenario.PopulationSweep populationSweep,
            List<Integer> populationPoints,
            List<Integer> trajectoryPopulations,
            List<PopulationRow> results
    ) {
        public Summary {
            metricDefinitions = Map.copyOf(metricDefinitions);
            sources = Map.copyOf(sources);
            populationPoints = List.copyOf(populationPoints);
            trajectoryPopulations = List.copyOf(trajectoryPopulations);
            results = List.copyOf(results);
        }
    }

    public record PopulationRow(
            int population,
            int houseFloorAreaBlocks,
            int houseVolumeBlocks,
            int huntingCapacity,
            double houseCoverageFraction,
            double huntingCoverageFraction,
            double fieldUtilizationFraction,
            double houseMinimumClimateCelsius,
            double huntingMinimumClimateCelsius,
            int theoryMiners,
            int theoryHunters,
            double theoryFuelSelfSupplyRatio,
            double theoryFoodSelfSupplyRatio,
            TownStageFourSimulator.Distribution fuelSelfSupplyRatio,
            TownStageFourSimulator.Distribution foodSelfSupplyRatio,
            double fuelShortageProbability,
            TownStageFourSimulator.Interval fuelShortageWilson95,
            double foodShortageProbability,
            TownStageFourSimulator.Interval foodShortageWilson95,
            double survivalProbability,
            TownStageFourSimulator.Interval survivalWilson95,
            double noShortageProbability,
            TownStageFourSimulator.Interval noShortageWilson95,
            TownStageFourSimulator.Distribution climateServiceableHourFraction,
            TownStageFourSimulator.Distribution houseWorkableHourFraction,
            TownStageFourSimulator.Distribution huntingWorkableHourFraction
    ) {
    }

    public record ReserveTrajectoryRow(
            int population,
            int day,
            double meanPopulation,
            double meanDeaths,
            TownStageFourSimulator.Distribution foodReserve,
            TownStageFourSimulator.Distribution fuelReserve
    ) {
    }
}
