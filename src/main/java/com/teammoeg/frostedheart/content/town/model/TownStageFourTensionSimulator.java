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
import com.teammoeg.frostedheart.content.town.observation.TownObservationModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/** Paired-seed 24-resident steady-state tension experiment for stage 4. */
public final class TownStageFourTensionSimulator {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();
    private static final double EPSILON = 1.0e-9;

    private TownStageFourTensionSimulator() {
    }

    public static SimulationRun run(
            Path projectRoot,
            Path packRoot,
            Path scenarioPath,
            Path outputOverride,
            Integer runsOverride,
            Long seedOverride
    ) throws IOException {
        TownStageFourScenario base = TownStageFourScenario.load(scenarioPath);
        TownStageFourScenario.TensionExperiment experiment = base.tensionExperiment();
        if (experiment == null) {
            throw new IllegalArgumentException(
                    "A stage-4 tension run requires a tensionExperiment object.");
        }
        if (base.town().population().initialResidents() != 24) {
            throw new IllegalArgumentException(
                    "The first steady-state tension experiment is fixed at 24 residents.");
        }
        TownStageOneTwoData data = TownStageOneTwoData.load(projectRoot, packRoot);
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        int runs = runsOverride == null ? base.town().simulation().runs()
                : requirePositive(runsOverride, "runs");
        long seed = seedOverride == null ? base.town().simulation().seed() : seedOverride;
        Path output = outputOverride == null
                ? projectRoot.resolve("build/town-model/stage4")
                .resolve(safeName(base.town().metadata().name()) + "-tension")
                : outputOverride;
        Files.createDirectories(output);

        int totalDays = Math.addExact(
                experiment.townBurnInDays(), base.town().simulation().days());
        TownStageFourModel.ClimateSeries[] climates = new TownStageFourModel.ClimateSeries[runs];
        long[] runSeeds = new long[runs];
        for (int run = 0; run < runs; run++) {
            runSeeds[run] = TownStageFourSimulator.mixedSeed(seed, run);
            climates[run] = TownStageFourModel.climateSeries(
                    runSeeds[run], base.climateBurnInDays(), totalDays, parameters);
        }

        List<CapacityRow> capacityRows = new ArrayList<>();
        List<RunRow> detailedRunRows = new ArrayList<>();
        List<DailyTrace> detailedDaily = new ArrayList<>();
        List<RasterEvent> detailedEvents = new ArrayList<>();
        int totalLayouts = experiment.mineCapacities().size()
                * experiment.huntCapacities().size();
        int completedLayouts = 0;
        for (int mineCapacity : experiment.mineCapacities()) {
            for (int requestedHuntCapacity : experiment.huntCapacities()) {
                TownStageFourScenario scenario = TownStageFourTensionModel.forCapacities(
                        base, mineCapacity, requestedHuntCapacity, data, parameters);
                int actualHuntCapacity = scenario.town().workplaces().huntCapacity();
                TownStageFourModel.ThermalLayout normalLayout =
                        TownStageFourModel.analyzeLayout(scenario, parameters, false);
                TownStageFourModel.ThermalLayout overdriveLayout =
                        TownStageFourModel.analyzeLayout(scenario, parameters, true);
                boolean detailed = mineCapacity == experiment.detailedMineCapacity()
                        && requestedHuntCapacity == experiment.detailedHuntCapacity();
                for (Strategy strategy : Strategy.values()) {
                    List<RunRow> rows = new ArrayList<>(runs);
                    for (int run = 0; run < runs; run++) {
                        TrialResult result = executeTrial(
                                scenario, experiment, data, parameters,
                                normalLayout, overdriveLayout, strategy, run,
                                runSeeds[run], climates[run], detailed);
                        rows.add(result.row());
                        if (detailed) {
                            detailedRunRows.add(result.row());
                            detailedDaily.addAll(result.daily());
                            detailedEvents.addAll(result.events());
                        }
                    }
                    capacityRows.add(aggregate(
                            mineCapacity, requestedHuntCapacity, actualHuntCapacity,
                            normalLayout, strategy, rows));
                }
                completedLayouts++;
                if (completedLayouts == 1 || completedLayouts % 4 == 0
                        || completedLayouts == totalLayouts) {
                    System.out.printf(Locale.ROOT,
                            "Stage-4 tension experiment: %d/%d layouts complete (mine=%d, hunt=%d).%n",
                            completedLayouts, totalLayouts, mineCapacity, actualHuntCapacity);
                }
            }
        }

        Summary summary = new Summary(
                1, 4, "current-stage4-24-resident-steady-state-tension",
                base.town().metadata(), runs, base.town().simulation().days(), seed,
                Map.ofEntries(
                        Map.entry("burnInSurvivalProbability",
                                "Fraction of paired trials retaining all 24 residents after town and climate have advanced together for townBurnInDays."),
                        Map.entry("fullSurvivalProbability",
                                "Fraction of trials with no resident exit during either burn-in or the measured interval."),
                        Map.entry("measurementSurvivalAmongBurnInSurvivors",
                                "Among towns still at 24 residents after burn-in, the fraction with no exit in the measured interval."),
                        Map.entry("dangerZoneFraction",
                                "Fraction of measured days when food or current-mode T1 fuel reserve is at least 3 but below 7 days."),
                        Map.entry("warningLeadDays",
                                "Days from the first player-visible severe forecast, reserve below 7 days, unsafe house, stopped hunting, or exit-risk signal to the first measured resident exit."),
                        Map.entry("meanRecoveryDays",
                                "Mean current observer episode duration from a below-3-day/critical state until both reserves are at least 7 days and services and exit risk recover."),
                        Map.entry("operationalReserveCap",
                                "A scenario-level transmitter exports only food and coal/coke above the configured caps after each town tick; it does not change gameplay production or consumption formulas."),
                        Map.entry("forecastStrategy",
                                "Reads the existing cold-bottom plus forecast-sensitivity category at 3-hour samples and toggles current T1 overdrive only when the configured severe level lies within the 24-hour action window.")),
                parameters, data.sourceFiles(), base, experiment, List.copyOf(capacityRows));

        Path summaryPath = output.resolve("summary.json");
        Path capacityPath = output.resolve("capacity-grid.csv");
        Path runsPath = output.resolve("detailed-runs.csv");
        Path dailyPath = output.resolve("player-timeline-trials.csv");
        Path rasterPath = output.resolve("event-raster.csv");
        Files.writeString(summaryPath, GSON.toJson(summary) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        writeCapacityGrid(capacityPath, capacityRows);
        writeRuns(runsPath, detailedRunRows);
        writeDaily(dailyPath, detailedDaily);
        writeEvents(rasterPath, detailedEvents);
        return new SimulationRun(
                output, summaryPath, capacityPath, runsPath, dailyPath, rasterPath, summary);
    }

    private static TrialResult executeTrial(
            TownStageFourScenario scenario,
            TownStageFourScenario.TensionExperiment experiment,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            TownStageFourModel.ThermalLayout normalLayout,
            TownStageFourModel.ThermalLayout overdriveLayout,
            Strategy strategy,
            int run,
            long runSeed,
            TownStageFourModel.ClimateSeries climate,
            boolean capture
    ) {
        TownStageThreeScenario normalTown = scenario.town().withTower(
                scenario.town().tower().withOverdrive(false));
        TownStageThreeState state = TownStageThreeState.initial(
                normalTown, parameters,
                new SplittableRandom(runSeed ^ 0x6A09E667F3BCC909L));
        SplittableRandom townRandom = new SplittableRandom(runSeed ^ 0xA49E2D8391C34621L);
        int initialPopulation = state.residents().size();
        int measuredDays = normalTown.simulation().days();
        int totalDays = experiment.townBurnInDays() + measuredDays;
        double previousTowerService = state.amount(normalTown.tower().fuelItem()) >= 1.0
                ? 1.0 : 0.0;
        TownStageFourObserver observer = null;
        int populationAtMeasurementStart = 0;
        int measuredFoodShortageDays = 0;
        int measuredFuelShortageDays = 0;
        int dangerDays = 0;
        int overdriveDays = 0;
        long measuredLoadedFuel = 0L;
        double measuredExportedFood = 0.0;
        double measuredExportedFuel = 0.0;
        Integer firstWarningDay = null;
        Integer firstExitDay = null;
        boolean previousForecast = false;
        boolean previousOverdrive = false;
        boolean previousFoodCaution = false;
        boolean previousFuelCaution = false;
        boolean softInstability = false;
        List<DailyTrace> daily = new ArrayList<>();
        List<RasterEvent> events = new ArrayList<>();
        double fullyCoveredMinimumClimate = TownStageFourSimulator.capacityTheory(
                scenario, parameters, normalLayout).fullyCoveredMinimumClimateCelsius();

        for (int globalDay = 0; globalDay < totalDays; globalDay++) {
            int measuredDay = globalDay - experiment.townBurnInDays();
            boolean measuring = measuredDay >= 0;
            if (measuredDay == 0) {
                populationAtMeasurementStart = state.residents().size();
                observer = new TownStageFourObserver(
                        measuredDays, state, normalTown, data, parameters);
            }
            boolean forecastSevere = TownStageFourTensionModel.forecastTriggersOverdrive(
                    climate, globalDay, scenario.morningHour(), experiment, parameters);
            boolean overdrive = strategy == Strategy.FORECAST && forecastSevere;
            TownStageThreeScenario dailyTown = normalTown.withTower(
                    normalTown.tower().withOverdrive(overdrive));
            TownStageFourModel.ThermalLayout dailyLayout = overdrive
                    ? overdriveLayout : normalLayout;
            int servedHours = Math.max(0, Math.min(24,
                    (int) Math.round(24.0 * previousTowerService)));
            boolean heatActiveAtMorning = scenario.morningHour() < servedHours
                    && dailyTown.tower().activeFraction() > 0.0;
            TownStageFourModel.HourThermalResult morning = TownStageFourModel.evaluateHour(
                    climate.temperature(globalDay, scenario.morningHour()),
                    heatActiveAtMorning, scenario, parameters, dailyLayout);
            TownStageThreeModel.DailyEnvironment environment =
                    TownStageFourModel.dailyEnvironment(morning, scenario, parameters);
            List<TownObservationModel.ResidentStatus> beforeSettlement = measuring
                    ? TownStageFourObserver.copyStatuses(state) : List.of();
            int populationBefore = state.residents().size();
            TownStageThreeModel.DayResult rawResult = TownStageThreeModel.settleDay(
                    state, dailyTown, data, parameters, townRandom, environment);
            TownStageFourTensionModel.ExportResult exported =
                    TownStageFourTensionModel.trimOperationalReserves(
                            state, normalTown, data, parameters,
                            experiment.foodReserveCapDays(),
                            experiment.fuelReserveCapNormalDays());
            double foodReserve = TownStageThreeModel.foodReserveDays(
                    state, data, parameters, state.residents().size());
            double fuelReserve = TownStageThreeModel.fuelReserveDays(
                    state, dailyTown, data, parameters);
            TownStageThreeModel.DayResult result = withReserves(rawResult, foodReserve, fuelReserve);
            previousTowerService = result.towerServiceFraction();

            if (!measuring) continue;
            if (observer == null) throw new IllegalStateException("Missing measurement observer.");
            observer.observeDay(
                    measuredDay, beforeSettlement, state, result, environment,
                    morning.climateTemperatureCelsius()
                            < fullyCoveredMinimumClimate);
            TownStageFourObserver.DailyObservation observation = observer.latestObservation();
            int exitsToday = Math.max(0, populationBefore - state.residents().size());
            boolean foodShortage = result.foodSatisfaction() < 1.0 - EPSILON;
            boolean fuelShortage = result.towerServiceFraction() < 1.0 - EPSILON;
            boolean foodCaution = result.foodReserveDays()
                    < TownStageFourTensionModel.DANGER_RESERVE_MAXIMUM_DAYS;
            boolean fuelCaution = result.fuelReserveDays()
                    < TownStageFourTensionModel.DANGER_RESERVE_MAXIMUM_DAYS;
            boolean danger = TownStageFourTensionModel.inDangerZone(result.foodReserveDays())
                    || TownStageFourTensionModel.inDangerZone(result.fuelReserveDays());
            boolean warning = forecastSevere || foodCaution || fuelCaution
                    || !environment.houseAcceptsNewResidents() || !environment.huntingWorkable()
                    || observation.exitRiskCount() > 0;
            if (warning && firstWarningDay == null) firstWarningDay = measuredDay;
            if (exitsToday > 0 && firstExitDay == null) firstExitDay = measuredDay;
            if (foodShortage) measuredFoodShortageDays++;
            if (fuelShortage) measuredFuelShortageDays++;
            if (danger) dangerDays++;
            if (overdrive) overdriveDays++;
            measuredLoadedFuel += result.loadedFuelItems();
            measuredExportedFood += exported.foodUnits();
            measuredExportedFuel += exported.fuelItems();
            softInstability |= danger || foodShortage || fuelShortage
                    || !environment.houseAcceptsNewResidents() || !environment.huntingWorkable()
                    || observation.exitRiskCount() > 0;

            if (capture) {
                addCrossing(events, strategy, run, runSeed, measuredDay,
                        forecastSevere, previousForecast,
                        "FORECAST_SEVERE", "warning", 1);
                addTwoWayCrossing(events, strategy, run, runSeed, measuredDay,
                        overdrive, previousOverdrive, "OVERDRIVE_ON", "OVERDRIVE_OFF", 1);
                addCrossing(events, strategy, run, runSeed, measuredDay,
                        foodCaution, previousFoodCaution, "FOOD_RESERVE_BELOW_7_DAYS", "warning", 1);
                addCrossing(events, strategy, run, runSeed, measuredDay,
                        fuelCaution, previousFuelCaution, "FUEL_RESERVE_BELOW_7_DAYS", "warning", 1);
                daily.add(new DailyTrace(
                        strategy.label, run, runSeed, measuredDay,
                        morning.climateTemperatureCelsius(), forecastSevere, overdrive,
                        environment.houseTemperatureCelsius(),
                        morning.building("hunt").temperatureCelsius(),
                        result.towerServiceFraction(), result.population(),
                        result.cumulativeDeaths(), result.assignedMiners(), result.assignedHunters(),
                        result.miningSwe(), result.huntingSwe(), result.foodSatisfaction(),
                        result.foodReserveDays(), result.fuelReserveDays(),
                        observation.averageHealth(), observation.p10Health(),
                        observation.averageMental(), observation.p10Mental(),
                        observation.unableToWorkCount(), observation.exitRiskCount(),
                        exitsToday));
            }
            previousForecast = forecastSevere;
            previousOverdrive = overdrive;
            previousFoodCaution = foodCaution;
            previousFuelCaution = fuelCaution;
        }
        if (observer == null) throw new IllegalStateException("No measured interval.");
        TownStageFourObserver.RunMetrics metrics = observer.finish();
        if (capture) {
            for (TownSignalEvent event : observer.events()) {
                events.add(new RasterEvent(
                        strategy.label, run, runSeed, Math.toIntExact(event.day()),
                        event.type().name(), event.severity().name().toLowerCase(Locale.ROOT),
                        event.affectedCount()));
            }
        }
        int measuredExits = Math.max(
                0, populationAtMeasurementStart - state.residents().size());
        int warningLead = firstExitDay == null || firstWarningDay == null
                ? -1 : Math.max(0, firstExitDay - firstWarningDay);
        RunRow row = new RunRow(
                strategy.label, run, runSeed,
                scenario.town().workplaces().mineCapacity(),
                scenario.town().workplaces().huntCapacity(),
                populationAtMeasurementStart == initialPopulation,
                state.residents().size() == initialPopulation,
                measuredExits == 0,
                populationAtMeasurementStart, state.residents().size(), measuredExits,
                measuredFoodShortageDays, measuredFuelShortageDays,
                (double) dangerDays / measuredDays, softInstability,
                warningLead, overdriveDays, measuredLoadedFuel,
                measuredExportedFood, measuredExportedFuel, metrics);
        return new TrialResult(row, List.copyOf(daily), List.copyOf(events));
    }

    private static TownStageThreeModel.DayResult withReserves(
            TownStageThreeModel.DayResult value,
            double foodReserveDays,
            double fuelReserveDays
    ) {
        return new TownStageThreeModel.DayResult(
                value.day(), value.population(), value.cumulativeDeaths(),
                value.assignedMiners(), value.assignedHunters(), value.miningSwe(),
                value.huntingSwe(), value.foodRequired(), value.foodConsumed(),
                value.foodSatisfaction(), foodReserveDays, fuelReserveDays,
                value.towerServiceFraction(), value.loadedFuelItems(), value.oreRequested(),
                value.coalAccepted(), value.huntingRolls(), value.huntingFoodPotential(),
                value.huntingFoodAccepted(), value.coalProcessed(), value.meatProcessed(),
                value.inventoryItems(), value.capacityLeft(), value.minimumHealth(),
                value.minimumMental(), value.meanHealth(), value.meanMental(),
                value.huntUnits(), value.exhaustedOreChunks(), value.enteredOreChunks(),
                value.resourceFlows());
    }

    private static CapacityRow aggregate(
            int requestedMineCapacity,
            int requestedHuntCapacity,
            int actualHuntCapacity,
            TownStageFourModel.ThermalLayout layout,
            Strategy strategy,
            List<RunRow> rows
    ) {
        long burnInSurvivors = rows.stream().filter(RunRow::burnInSurvived).count();
        long fullSurvivors = rows.stream().filter(RunRow::fullSurvived).count();
        long measuredSurvivors = rows.stream()
                .filter(RunRow::burnInSurvived).filter(RunRow::measuredSurvived).count();
        long foodShortage = rows.stream().filter(value -> value.foodShortageDays() > 0).count();
        long fuelShortage = rows.stream().filter(value -> value.fuelShortageDays() > 0).count();
        long soft = rows.stream().filter(RunRow::softInstability).count();
        List<Double> warningLead = rows.stream().mapToInt(RunRow::warningLeadDays)
                .filter(value -> value >= 0).mapToDouble(value -> value).boxed().toList();
        List<Double> recovery = rows.stream()
                .filter(value -> value.observationMetrics().recoveredEpisodeCount() > 0)
                .map(value -> value.observationMetrics().meanRecoveryDays()).toList();
        long exitRuns = rows.stream().filter(value -> value.measuredExits() > 0).count();
        long warnedExitRuns = rows.stream()
                .filter(value -> value.measuredExits() > 0 && value.warningLeadDays() >= 0).count();
        return new CapacityRow(
                requestedMineCapacity, requestedHuntCapacity, actualHuntCapacity,
                layout.building("house").coverageFraction(),
                layout.building("hunt").coverageFraction(), strategy.label,
                probability(burnInSurvivors, rows.size()),
                probability(fullSurvivors, rows.size()),
                probability(measuredSurvivors, burnInSurvivors),
                probability(foodShortage, rows.size()), probability(fuelShortage, rows.size()),
                probability(soft, rows.size()),
                distribution(rows.stream().map(RunRow::dangerZoneFraction).toList()),
                probability(warnedExitRuns, exitRuns), distribution(warningLead),
                distribution(recovery),
                distribution(rows.stream().map(value -> (double) value.overdriveDays()).toList()),
                distribution(rows.stream().map(value -> (double) value.loadedFuelItems()).toList()),
                distribution(rows.stream().map(RunRow::exportedFoodUnits).toList()),
                distribution(rows.stream().map(RunRow::exportedFuelItems).toList()));
    }

    private static void addCrossing(
            List<RasterEvent> events,
            Strategy strategy,
            int run,
            long seed,
            int day,
            boolean current,
            boolean previous,
            String type,
            String severity,
            int affected
    ) {
        if (current && !previous) {
            events.add(new RasterEvent(
                    strategy.label, run, seed, day, type, severity, affected));
        }
    }

    private static void addTwoWayCrossing(
            List<RasterEvent> events,
            Strategy strategy,
            int run,
            long seed,
            int day,
            boolean current,
            boolean previous,
            String entered,
            String recovered,
            int affected
    ) {
        if (current == previous) return;
        events.add(new RasterEvent(
                strategy.label, run, seed, day, current ? entered : recovered,
                current ? "information" : "information", affected));
    }

    private static Distribution distribution(List<Double> values) {
        if (values.isEmpty()) return new Distribution(0.0, 0.0, 0.0, 0.0, 0.0, 0);
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        double mean = Arrays.stream(sorted).average().orElse(0.0);
        return new Distribution(
                mean, percentile(sorted, 0.05), percentile(sorted, 0.50),
                percentile(sorted, 0.95), sorted[sorted.length - 1], sorted.length);
    }

    private static double percentile(double[] sorted, double probability) {
        double position = probability * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) return sorted[lower];
        double fraction = position - lower;
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
    }

    private static double probability(long events, long trials) {
        return trials > 0L ? (double) events / trials : 0.0;
    }

    private static void writeCapacityGrid(Path path, List<CapacityRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("requested_mine_capacity,requested_hunt_capacity,actual_hunt_capacity,"
                    + "house_coverage_fraction,hunt_coverage_fraction,strategy,"
                    + "burn_in_survival_probability,full_survival_probability,"
                    + "measurement_survival_probability_among_burn_in_survivors,"
                    + "food_shortage_probability,fuel_shortage_probability,soft_instability_probability,"
                    + "danger_zone_fraction_mean,danger_zone_fraction_p05,danger_zone_fraction_p50,danger_zone_fraction_p95,"
                    + "prior_warning_probability_among_exit_runs,warning_lead_days_p05,warning_lead_days_p50,warning_lead_days_p95,"
                    + "recovery_days_p05,recovery_days_p50,recovery_days_p95,"
                    + "overdrive_days_mean,loaded_fuel_items_mean,loaded_fuel_items_p50,"
                    + "exported_food_units_mean,exported_fuel_items_mean\n");
            for (CapacityRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%.9f,%.9f,%s,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f%n",
                        row.requestedMineCapacity(), row.requestedHuntCapacity(),
                        row.actualHuntCapacity(), row.houseCoverageFraction(),
                        row.huntCoverageFraction(), row.strategy(),
                        row.burnInSurvivalProbability(), row.fullSurvivalProbability(),
                        row.measurementSurvivalAmongBurnInSurvivors(),
                        row.foodShortageProbability(), row.fuelShortageProbability(),
                        row.softInstabilityProbability(), row.dangerZoneFraction().mean(),
                        row.dangerZoneFraction().p05(), row.dangerZoneFraction().p50(),
                        row.dangerZoneFraction().p95(), row.priorWarningProbabilityAmongExitRuns(),
                        row.warningLeadDays().p05(), row.warningLeadDays().p50(),
                        row.warningLeadDays().p95(), row.recoveryDays().p05(),
                        row.recoveryDays().p50(), row.recoveryDays().p95(),
                        row.overdriveDays().mean(), row.loadedFuelItems().mean(),
                        row.loadedFuelItems().p50(), row.exportedFoodUnits().mean(),
                        row.exportedFuelItems().mean()));
            }
        }
    }

    private static void writeRuns(Path path, List<RunRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("strategy,trial,seed,mine_capacity,hunt_capacity,burn_in_survived,"
                    + "full_survived,measured_survived,population_at_measurement_start,final_population,"
                    + "measured_exits,food_shortage_days,fuel_shortage_days,danger_zone_fraction,"
                    + "soft_instability,warning_lead_days,overdrive_days,loaded_fuel_items,"
                    + "exported_food_units,exported_fuel_items,crisis_episode_count,recovered_episode_count,"
                    + "mean_recovery_days,unrecovered_episode\n");
            for (RunRow row : rows) {
                TownStageFourObserver.RunMetrics metrics = row.observationMetrics();
                writer.write(String.format(Locale.ROOT,
                        "%s,%d,%d,%d,%d,%s,%s,%s,%d,%d,%d,%d,%d,%.9f,%s,%d,%d,%d,"
                                + "%.9f,%.9f,%d,%d,%.9f,%s%n",
                        row.strategy(), row.run(), row.seed(), row.mineCapacity(), row.huntCapacity(),
                        row.burnInSurvived(), row.fullSurvived(), row.measuredSurvived(),
                        row.populationAtMeasurementStart(), row.finalPopulation(), row.measuredExits(),
                        row.foodShortageDays(), row.fuelShortageDays(), row.dangerZoneFraction(),
                        row.softInstability(), row.warningLeadDays(), row.overdriveDays(),
                        row.loadedFuelItems(), row.exportedFoodUnits(), row.exportedFuelItems(),
                        metrics.crisisEpisodeCount(), metrics.recoveredEpisodeCount(),
                        metrics.meanRecoveryDays(), metrics.unrecoveredEpisode()));
            }
        }
    }

    private static void writeDaily(Path path, List<DailyTrace> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("strategy,trial,seed,day,morning_climate_c,forecast_severe,overdrive,"
                    + "house_temperature_c,hunting_temperature_c,tower_service,population,cumulative_exits,"
                    + "miners,hunters,mining_swe,hunting_swe,food_satisfaction,food_reserve_days,"
                    + "fuel_reserve_days,average_health,p10_health,average_mental,p10_mental,"
                    + "unable_to_work_count,exit_risk_count,resident_exits\n");
            for (DailyTrace row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%s,%d,%d,%d,%.9f,%s,%s,%.9f,%.9f,%.9f,%d,%d,%d,%d,"
                                + "%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%d,%d,%d%n",
                        row.strategy(), row.run(), row.seed(), row.day(), row.morningClimateCelsius(),
                        row.forecastSevere(), row.overdrive(), row.houseTemperatureCelsius(),
                        row.huntingTemperatureCelsius(), row.towerService(), row.population(),
                        row.cumulativeExits(), row.miners(), row.hunters(), row.miningSwe(),
                        row.huntingSwe(), row.foodSatisfaction(), row.foodReserveDays(),
                        row.fuelReserveDays(), row.averageHealth(), row.p10Health(),
                        row.averageMental(), row.p10Mental(), row.unableToWorkCount(),
                        row.exitRiskCount(), row.residentExits()));
            }
        }
    }

    private static void writeEvents(Path path, List<RasterEvent> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("strategy,trial,seed,day,type,severity,affected_count\n");
            for (RasterEvent row : rows) {
                writer.write(String.format(Locale.ROOT, "%s,%d,%d,%d,%s,%s,%d%n",
                        row.strategy(), row.run(), row.seed(), row.day(), row.type(),
                        row.severity(), row.affectedCount()));
            }
        }
    }

    public static void printSummary(SimulationRun run) {
        Summary summary = run.summary();
        System.out.printf(Locale.ROOT,
                "Stage 4 tension: %s%n24 residents, %d layouts, 2 strategies, %d paired runs%n"
                        + "Town burn-in: %d days; measured: %d days%nOutput: %s%n",
                summary.metadata().name(), summary.results().size() / 2, summary.runs(),
                summary.experiment().townBurnInDays(), summary.days(), run.outputDirectory());
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive.");
        return value;
    }

    private static String safeName(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return result.isBlank() ? "scenario" : result;
    }

    private enum Strategy {
        FIXED("fixed"),
        FORECAST("forecast");

        private final String label;

        Strategy(String label) {
            this.label = label;
        }
    }

    private record TrialResult(
            RunRow row,
            List<DailyTrace> daily,
            List<RasterEvent> events
    ) {
    }

    public record SimulationRun(
            Path outputDirectory,
            Path summaryPath,
            Path capacityGridPath,
            Path detailedRunsPath,
            Path playerTimelineTrialsPath,
            Path eventRasterPath,
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
            Map<String, String> metricDefinitions,
            TownModelParameters parameters,
            Map<String, String> sources,
            TownStageFourScenario baseScenario,
            TownStageFourScenario.TensionExperiment experiment,
            List<CapacityRow> results
    ) {
        public Summary {
            metricDefinitions = Map.copyOf(metricDefinitions);
            sources = Map.copyOf(sources);
            results = List.copyOf(results);
        }
    }

    public record CapacityRow(
            int requestedMineCapacity,
            int requestedHuntCapacity,
            int actualHuntCapacity,
            double houseCoverageFraction,
            double huntCoverageFraction,
            String strategy,
            double burnInSurvivalProbability,
            double fullSurvivalProbability,
            double measurementSurvivalAmongBurnInSurvivors,
            double foodShortageProbability,
            double fuelShortageProbability,
            double softInstabilityProbability,
            Distribution dangerZoneFraction,
            double priorWarningProbabilityAmongExitRuns,
            Distribution warningLeadDays,
            Distribution recoveryDays,
            Distribution overdriveDays,
            Distribution loadedFuelItems,
            Distribution exportedFoodUnits,
            Distribution exportedFuelItems
    ) {
    }

    public record Distribution(
            double mean,
            double p05,
            double p50,
            double p95,
            double maximum,
            int sampleCount
    ) {
    }

    public record RunRow(
            String strategy,
            int run,
            long seed,
            int mineCapacity,
            int huntCapacity,
            boolean burnInSurvived,
            boolean fullSurvived,
            boolean measuredSurvived,
            int populationAtMeasurementStart,
            int finalPopulation,
            int measuredExits,
            int foodShortageDays,
            int fuelShortageDays,
            double dangerZoneFraction,
            boolean softInstability,
            int warningLeadDays,
            int overdriveDays,
            long loadedFuelItems,
            double exportedFoodUnits,
            double exportedFuelItems,
            TownStageFourObserver.RunMetrics observationMetrics
    ) {
    }

    public record DailyTrace(
            String strategy,
            int run,
            long seed,
            int day,
            double morningClimateCelsius,
            boolean forecastSevere,
            boolean overdrive,
            double houseTemperatureCelsius,
            double huntingTemperatureCelsius,
            double towerService,
            int population,
            int cumulativeExits,
            int miners,
            int hunters,
            double miningSwe,
            double huntingSwe,
            double foodSatisfaction,
            double foodReserveDays,
            double fuelReserveDays,
            double averageHealth,
            double p10Health,
            double averageMental,
            double p10Mental,
            int unableToWorkCount,
            int exitRiskCount,
            int residentExits
    ) {
    }

    public record RasterEvent(
            String strategy,
            int run,
            long seed,
            int day,
            String type,
            String severity,
            int affectedCount
    ) {
    }
}
