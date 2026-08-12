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

/** Monte Carlo runner and CSV/JSON report writer for stage 3. */
public final class TownStageThreeSimulator {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    private TownStageThreeSimulator() {
    }

    public static SimulationRun run(
            Path projectRoot,
            Path packRoot,
            Path scenarioPath,
            Path outputOverride,
            Integer runsOverride,
            Long seedOverride
    ) throws IOException {
        TownStageThreeScenario scenario = TownStageThreeScenario.load(scenarioPath);
        TownStageOneTwoData data = TownStageOneTwoData.load(projectRoot, packRoot);
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        int runs = runsOverride == null ? scenario.simulation().runs() : requirePositive(runsOverride, "runs");
        long seed = seedOverride == null ? scenario.simulation().seed() : seedOverride;
        Path output = outputOverride == null
                ? projectRoot.resolve("build/town-model/stage3")
                .resolve(safeName(scenario.metadata().name()))
                : outputOverride;
        Files.createDirectories(output);

        TownStageThreeTheory.TheorySummary theory =
                TownStageThreeTheory.evaluate(scenario, data, parameters);
        Execution execution = execute(scenario, data, parameters, runs, seed, true);
        AggregateSummary aggregate = aggregate(execution.rows());
        TheoryComparison comparison = compareTheory(execution, theory);
        Summary summary = new Summary(
                1,
                3,
                "constant-temperature-t1-multiday-current-order",
                scenario.metadata(),
                runs,
                scenario.simulation().days(),
                seed,
                List.of(
                        "Temperature is fixed by the scenario; climate and all T2 behavior are excluded.",
                        "Coal-to-coke and raw-to-cooked-meat are capacity limits, not simulated machines.",
                        "Tower fuel uses exact T1 integer item loading and 20-tick batches; ash removal and transport are ideal.",
                        "Work assignments are sticky and only vacancies are filled, matching current TeamTownData behavior.",
                        "Initial inventory is excluded from structural production-coverage numerators."),
                parameters,
                data.sourceFiles(),
                scenario,
                theory,
                comparison,
                aggregate);

        Path summaryPath = output.resolve("summary.json");
        Path runsPath = output.resolve("runs.csv");
        Path dailyPath = output.resolve("daily.csv");
        Path resourcesPath = output.resolve("resources.csv");
        Path frontierPath = output.resolve("frontier.csv");
        Path orderPath = output.resolve("order-comparison.csv");
        Files.writeString(summaryPath, GSON.toJson(summary) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        writeRuns(runsPath, execution.rows());
        writeDaily(dailyPath, execution.daily());
        writeResources(resourcesPath, execution.trace());
        writeFrontier(frontierPath, theory.frontier());
        if (scenario.diagnostics().compareBuildingOrders()) {
            writeOrderComparison(orderPath, scenario, data, parameters, seed);
        } else {
            Files.deleteIfExists(orderPath);
            orderPath = null;
        }
        return new SimulationRun(
                output, summaryPath, runsPath, dailyPath, resourcesPath,
                frontierPath, orderPath, summary);
    }

    private static Execution execute(
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            int runs,
            long seed,
            boolean captureTrace
    ) {
        int days = scenario.simulation().days();
        DailyAccumulator daily = new DailyAccumulator(days, runs);
        List<RunRow> rows = new ArrayList<>(runs);
        List<TownStageThreeModel.DayResult> trace = new ArrayList<>();
        for (int run = 0; run < runs; run++) {
            long runSeed = mixedSeed(seed, run);
            SplittableRandom random = new SplittableRandom(runSeed);
            TownStageThreeState state = TownStageThreeState.initial(scenario);
            double minimumHealth = Double.POSITIVE_INFINITY;
            double minimumMental = Double.POSITIVE_INFINITY;
            TownStageThreeModel.DayResult finalDay = null;
            for (int day = 0; day < days; day++) {
                TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                        state, scenario, data, parameters, random);
                daily.accept(run, result);
                minimumHealth = Math.min(minimumHealth, result.minimumHealth());
                minimumMental = Math.min(minimumMental, result.minimumMental());
                finalDay = result;
                if (captureTrace && run == 0) trace.add(result);
            }
            if (finalDay == null) throw new IllegalStateException("Stage-3 simulation has no days.");
            double fuelPotentialCoverage = divide(
                    state.cumulativeCoalRequested(), state.cumulativeRawCoalDemand());
            double fuelAcceptedCoverage = divide(
                    state.cumulativeCoalAccepted(), state.cumulativeRawCoalDemand());
            double foodPotentialCoverage = divide(
                    state.cumulativeHuntingFoodPotential(), state.cumulativeFoodDemand());
            double foodAcceptedCoverage = divide(
                    state.cumulativeHuntingFoodAccepted(), state.cumulativeFoodDemand());
            rows.add(new RunRow(
                    run, runSeed, finalDay.population(), state.deaths(),
                    state.firstFoodShortageDay(), state.firstFuelShortageDay(),
                    finalDay.foodReserveDays(), finalDay.fuelReserveDays(),
                    fuelPotentialCoverage, fuelAcceptedCoverage,
                    foodPotentialCoverage, foodAcceptedCoverage,
                    minimumHealth, minimumMental,
                    state.cumulativeOreRequested(), state.cumulativeOreAccepted(),
                    state.cumulativeCoalRequested(), state.cumulativeCoalAccepted(),
                    state.cumulativeHuntingFoodPotential(),
                    state.cumulativeHuntingFoodAccepted(),
                    state.cumulativeFoodDemand(), state.cumulativeRawCoalDemand(),
                    state.cumulativeRejectedItems(), state.cumulativeMiningSweDays(),
                    state.cumulativeHuntingSweDays(),
                    state.deaths() == 0,
                    state.deaths() == 0
                            && state.firstFoodShortageDay() == null
                            && state.firstFuelShortageDay() == null));
        }
        return new Execution(List.copyOf(rows), daily.finish(), List.copyOf(trace));
    }

    private static AggregateSummary aggregate(List<RunRow> rows) {
        double[] fuelPotential = rows.stream().mapToDouble(RunRow::fuelPotentialCoverage).toArray();
        double[] fuelAccepted = rows.stream().mapToDouble(RunRow::fuelAcceptedCoverage).toArray();
        double[] foodPotential = rows.stream().mapToDouble(RunRow::foodPotentialCoverage).toArray();
        double[] foodAccepted = rows.stream().mapToDouble(RunRow::foodAcceptedCoverage).toArray();
        double[] finalFoodReserve = rows.stream().mapToDouble(RunRow::finalFoodReserveDays).toArray();
        double[] finalFuelReserve = rows.stream().mapToDouble(RunRow::finalFuelReserveDays).toArray();
        long survived = rows.stream().filter(RunRow::survived).count();
        long noShortage = rows.stream().filter(RunRow::noShortage).count();
        long foodShortage = rows.stream().filter(
                row -> row.firstFoodShortageDay() != null).count();
        long fuelShortage = rows.stream().filter(
                row -> row.firstFuelShortageDay() != null).count();
        return new AggregateSummary(
                fraction(survived, rows.size()), wilson95(survived, rows.size()),
                fraction(noShortage, rows.size()), wilson95(noShortage, rows.size()),
                fraction(foodShortage, rows.size()), wilson95(foodShortage, rows.size()),
                fraction(fuelShortage, rows.size()), wilson95(fuelShortage, rows.size()),
                statistics(fuelPotential), statistics(fuelAccepted),
                statistics(foodPotential), statistics(foodAccepted),
                statistics(finalFoodReserve), statistics(finalFuelReserve));
    }

    private static TheoryComparison compareTheory(
            Execution execution,
            TownStageThreeTheory.TheorySummary theory
    ) {
        double totalMiningSwe = execution.rows().stream()
                .mapToDouble(RunRow::cumulativeMiningSweDays).sum();
        double totalHuntingSwe = execution.rows().stream()
                .mapToDouble(RunRow::cumulativeHuntingSweDays).sum();
        Double simulatedCoalPerSwe = totalMiningSwe > 0.0
                ? execution.rows().stream().mapToDouble(RunRow::cumulativeCoalRequested).sum()
                / totalMiningSwe : null;
        Double simulatedFoodPerSwe = totalHuntingSwe > 0.0
                ? execution.rows().stream().mapToDouble(RunRow::cumulativeHuntingFoodPotential).sum()
                / totalHuntingSwe : null;
        return new TheoryComparison(
                theory.coalItemsPerMiningSweDay(), simulatedCoalPerSwe,
                simulatedCoalPerSwe == null ? null
                        : relativeError(simulatedCoalPerSwe, theory.coalItemsPerMiningSweDay()),
                theory.cookedFoodUnitsPerHuntingSweDay(), simulatedFoodPerSwe,
                simulatedFoodPerSwe == null ? null
                        : relativeError(simulatedFoodPerSwe, theory.cookedFoodUnitsPerHuntingSweDay()));
    }

    private static void writeRuns(Path path, List<RunRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("run,seed,final_population,deaths,first_food_shortage_day,first_fuel_shortage_day,"
                    + "final_food_reserve_days,final_fuel_reserve_days,fuel_potential_coverage,"
                    + "fuel_accepted_coverage,food_potential_coverage,food_accepted_coverage,"
                    + "minimum_health,minimum_mental,cumulative_ore_requested,cumulative_ore_accepted,"
                    + "cumulative_coal_requested,cumulative_coal_accepted,cumulative_hunting_food_potential,"
                    + "cumulative_hunting_food_accepted,cumulative_food_demand,cumulative_raw_coal_demand,"
                    + "cumulative_rejected_items,cumulative_mining_swe_days,cumulative_hunting_swe_days,"
                    + "survived,no_shortage\n");
            for (RunRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%d,%s,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%s,%s%n",
                        row.run(), row.seed(), row.finalPopulation(), row.deaths(),
                        nullableInteger(row.firstFoodShortageDay()),
                        nullableInteger(row.firstFuelShortageDay()),
                        row.finalFoodReserveDays(), row.finalFuelReserveDays(),
                        row.fuelPotentialCoverage(), row.fuelAcceptedCoverage(),
                        row.foodPotentialCoverage(), row.foodAcceptedCoverage(),
                        row.minimumHealth(), row.minimumMental(),
                        row.cumulativeOreRequested(), row.cumulativeOreAccepted(),
                        row.cumulativeCoalRequested(), row.cumulativeCoalAccepted(),
                        row.cumulativeHuntingFoodPotential(), row.cumulativeHuntingFoodAccepted(),
                        row.cumulativeFoodDemand(), row.cumulativeRawCoalDemand(),
                        row.cumulativeRejectedItems(), row.cumulativeMiningSweDays(),
                        row.cumulativeHuntingSweDays(), row.survived(), row.noShortage()));
            }
        }
    }

    private static void writeDaily(Path path, List<DailyAggregate> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("day,mean_population,mean_deaths,mean_miners,mean_hunters,mean_mining_swe,"
                    + "mean_hunting_swe,mean_food_satisfaction,food_reserve_p05,food_reserve_p50,"
                    + "food_reserve_p95,fuel_reserve_p05,fuel_reserve_p50,fuel_reserve_p95,"
                    + "mean_tower_service,mean_minimum_health,mean_minimum_mental,mean_coal_accepted,"
                    + "mean_hunting_food_produced\n");
            for (DailyAggregate row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                        row.day(), row.meanPopulation(), row.meanDeaths(), row.meanMiners(),
                        row.meanHunters(), row.meanMiningSwe(), row.meanHuntingSwe(),
                        row.meanFoodSatisfaction(), row.foodReserve().p05(), row.foodReserve().p50(),
                        row.foodReserve().p95(), row.fuelReserve().p05(), row.fuelReserve().p50(),
                        row.fuelReserve().p95(), row.meanTowerService(), row.meanMinimumHealth(),
                        row.meanMinimumMental(), row.meanCoalAccepted(),
                        row.meanHuntingFoodProduced()));
            }
        }
    }

    private static void writeResources(
            Path path,
            List<TownStageThreeModel.DayResult> trace
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("day,source,action,resource,requested,modified,rejected\n");
            for (TownStageThreeModel.DayResult day : trace) {
                for (TownStageThreeModel.ResourceFlow flow : day.resourceFlows()) {
                    writer.write(String.format(Locale.ROOT, "%d,%s,%s,%s,%.9f,%.9f,%.9f%n",
                            flow.day(), flow.source(), flow.action(), flow.resource(),
                            flow.requested(), flow.modified(), flow.rejected()));
                }
            }
        }
    }

    private static void writeFrontier(
            Path path,
            List<TownStageThreeTheory.FrontierPoint> rows
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("population,miners,hunters,fuel_coverage,food_coverage,joint_coverage,feasible\n");
            for (TownStageThreeTheory.FrontierPoint row : rows) {
                writer.write(String.format(Locale.ROOT, "%d,%d,%d,%.9f,%.9f,%.9f,%s%n",
                        row.population(), row.miners(), row.hunters(), row.fuelCoverage(),
                        row.foodCoverage(), row.jointCoverage(), row.feasible()));
            }
        }
    }

    private static void writeOrderComparison(
            Path path,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            long seed
    ) throws IOException {
        List<List<String>> orders = permutations(TownStageThreeScenario.REQUIRED_BUILDINGS);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("building_order,deaths,first_food_shortage_day,first_fuel_shortage_day,"
                    + "fuel_potential_coverage,food_potential_coverage,minimum_health,minimum_mental\n");
            for (List<String> order : orders) {
                Execution execution = execute(
                        scenario.withBuildingOrder(order), data, parameters, 1, seed, false);
                RunRow row = execution.rows().get(0);
                writer.write(String.format(Locale.ROOT, "%s,%d,%s,%s,%.9f,%.9f,%.9f,%.9f%n",
                        String.join(">", order), row.deaths(),
                        nullableInteger(row.firstFoodShortageDay()),
                        nullableInteger(row.firstFuelShortageDay()),
                        row.fuelPotentialCoverage(), row.foodPotentialCoverage(),
                        row.minimumHealth(), row.minimumMental()));
            }
        }
    }

    private static List<List<String>> permutations(List<String> values) {
        List<List<String>> result = new ArrayList<>();
        for (String first : values) {
            for (String second : values) {
                if (second.equals(first)) continue;
                for (String third : values) {
                    if (!third.equals(first) && !third.equals(second)) {
                        result.add(List.of(first, second, third));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    public static void printSummary(SimulationRun run) {
        Summary summary = run.summary();
        System.out.printf(Locale.ROOT,
                "Stage 3: %s%nRuns: %d, days: %d%nSurvival probability: %.3f%n"
                        + "No-shortage probability: %.3f%nFuel potential coverage mean: %.3f%n"
                        + "Food potential coverage mean: %.3f%nOutput: %s%n",
                summary.metadata().name(), summary.runs(), summary.days(),
                summary.aggregate().survivalProbability(),
                summary.aggregate().noShortageProbability(),
                summary.aggregate().fuelPotentialCoverage().mean(),
                summary.aggregate().foodPotentialCoverage().mean(),
                run.outputDirectory());
    }

    private static double divide(double numerator, double denominator) {
        return denominator > 0.0 ? numerator / denominator : 1.0;
    }

    private static double relativeError(double actual, double expected) {
        return expected != 0.0 ? (actual - expected) / expected : 0.0;
    }

    private static double fraction(long count, long total) {
        return total > 0L ? (double) count / total : 0.0;
    }

    private static Interval wilson95(long events, long trials) {
        if (trials <= 0L) return new Interval(0.0, 0.0);
        double z = 1.959963984540054;
        double probability = (double) events / trials;
        double zSquared = z * z;
        double denominator = 1.0 + zSquared / trials;
        double center = (probability + zSquared / (2.0 * trials)) / denominator;
        double radius = z * Math.sqrt(
                probability * (1.0 - probability) / trials
                        + zSquared / (4.0 * trials * trials)) / denominator;
        double lower = Math.max(0.0, center - radius);
        double upper = Math.min(1.0, center + radius);
        if (lower < 1.0e-15) lower = 0.0;
        if (1.0 - upper < 1.0e-15) upper = 1.0;
        return new Interval(lower, upper);
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

    private static long mixedSeed(long seed, int run) {
        long value = seed + 0x9E3779B97F4A7C15L * (run + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static Distribution statistics(double[] values) {
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

    private static final class DailyAccumulator {
        private final int runs;
        private final double[][] population;
        private final double[][] deaths;
        private final double[][] miners;
        private final double[][] hunters;
        private final double[][] miningSwe;
        private final double[][] huntingSwe;
        private final double[][] foodSatisfaction;
        private final double[][] foodReserve;
        private final double[][] fuelReserve;
        private final double[][] towerService;
        private final double[][] minimumHealth;
        private final double[][] minimumMental;
        private final double[][] coalAccepted;
        private final double[][] huntingFoodProduced;

        private DailyAccumulator(int days, int runs) {
            this.runs = runs;
            population = matrix(days, runs);
            deaths = matrix(days, runs);
            miners = matrix(days, runs);
            hunters = matrix(days, runs);
            miningSwe = matrix(days, runs);
            huntingSwe = matrix(days, runs);
            foodSatisfaction = matrix(days, runs);
            foodReserve = matrix(days, runs);
            fuelReserve = matrix(days, runs);
            towerService = matrix(days, runs);
            minimumHealth = matrix(days, runs);
            minimumMental = matrix(days, runs);
            coalAccepted = matrix(days, runs);
            huntingFoodProduced = matrix(days, runs);
        }

        private void accept(int run, TownStageThreeModel.DayResult result) {
            int day = result.day();
            population[day][run] = result.population();
            deaths[day][run] = result.cumulativeDeaths();
            miners[day][run] = result.assignedMiners();
            hunters[day][run] = result.assignedHunters();
            miningSwe[day][run] = result.miningSwe();
            huntingSwe[day][run] = result.huntingSwe();
            foodSatisfaction[day][run] = result.foodSatisfaction();
            foodReserve[day][run] = result.foodReserveDays();
            fuelReserve[day][run] = result.fuelReserveDays();
            towerService[day][run] = result.towerServiceFraction();
            minimumHealth[day][run] = result.minimumHealth();
            minimumMental[day][run] = result.minimumMental();
            coalAccepted[day][run] = result.coalAccepted();
            huntingFoodProduced[day][run] = result.huntingFoodAccepted();
        }

        private List<DailyAggregate> finish() {
            List<DailyAggregate> result = new ArrayList<>(population.length);
            for (int day = 0; day < population.length; day++) {
                result.add(new DailyAggregate(
                        day,
                        mean(population[day]), mean(deaths[day]),
                        mean(miners[day]), mean(hunters[day]),
                        mean(miningSwe[day]), mean(huntingSwe[day]),
                        mean(foodSatisfaction[day]),
                        statistics(foodReserve[day]), statistics(fuelReserve[day]),
                        mean(towerService[day]), mean(minimumHealth[day]),
                        mean(minimumMental[day]), mean(coalAccepted[day]),
                        mean(huntingFoodProduced[day])));
            }
            return List.copyOf(result);
        }

        private static double[][] matrix(int days, int runs) {
            return new double[days][runs];
        }

        private static double mean(double[] values) {
            return Arrays.stream(values).average().orElse(0.0);
        }
    }

    private record Execution(
            List<RunRow> rows,
            List<DailyAggregate> daily,
            List<TownStageThreeModel.DayResult> trace
    ) {
    }

    public record SimulationRun(
            Path outputDirectory,
            Path summaryPath,
            Path runsPath,
            Path dailyPath,
            Path resourcesPath,
            Path frontierPath,
            Path orderComparisonPath,
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
            TownStageThreeScenario scenario,
            TownStageThreeTheory.TheorySummary theory,
            TheoryComparison theoryComparison,
            AggregateSummary aggregate
    ) {
        public Summary {
            assumptions = List.copyOf(assumptions);
            sources = java.util.Map.copyOf(sources);
        }
    }

    public record TheoryComparison(
            double theoryCoalItemsPerMiningSweDay,
            Double simulationCoalItemsPerMiningSweDay,
            Double coalRelativeError,
            double theoryCookedFoodUnitsPerHuntingSweDay,
            Double simulationCookedFoodUnitsPerHuntingSweDay,
            Double foodRelativeError
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
            Distribution fuelAcceptedCoverage,
            Distribution foodPotentialCoverage,
            Distribution foodAcceptedCoverage,
            Distribution finalFoodReserveDays,
            Distribution finalFuelReserveDays
    ) {
    }

    public record Interval(double lower, double upper) {
    }

    public record Distribution(double mean, double standardDeviation, double p05, double p50, double p95) {
    }

    public record DailyAggregate(
            int day,
            double meanPopulation,
            double meanDeaths,
            double meanMiners,
            double meanHunters,
            double meanMiningSwe,
            double meanHuntingSwe,
            double meanFoodSatisfaction,
            Distribution foodReserve,
            Distribution fuelReserve,
            double meanTowerService,
            double meanMinimumHealth,
            double meanMinimumMental,
            double meanCoalAccepted,
            double meanHuntingFoodProduced
    ) {
    }

    public record RunRow(
            int run,
            long seed,
            int finalPopulation,
            int deaths,
            Integer firstFoodShortageDay,
            Integer firstFuelShortageDay,
            double finalFoodReserveDays,
            double finalFuelReserveDays,
            double fuelPotentialCoverage,
            double fuelAcceptedCoverage,
            double foodPotentialCoverage,
            double foodAcceptedCoverage,
            double minimumHealth,
            double minimumMental,
            double cumulativeOreRequested,
            double cumulativeOreAccepted,
            double cumulativeCoalRequested,
            double cumulativeCoalAccepted,
            double cumulativeHuntingFoodPotential,
            double cumulativeHuntingFoodAccepted,
            double cumulativeFoodDemand,
            double cumulativeRawCoalDemand,
            double cumulativeRejectedItems,
            double cumulativeMiningSweDays,
            double cumulativeHuntingSweDays,
            boolean survived,
            boolean noShortage
    ) {
    }
}
