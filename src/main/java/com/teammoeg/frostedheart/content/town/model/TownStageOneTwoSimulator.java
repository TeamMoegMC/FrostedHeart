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
import com.teammoeg.frostedheart.content.town.buildings.house.HouseDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/** Runs independent one-day stage-1/2 kernel experiments and diagnostics. */
public final class TownStageOneTwoSimulator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TownStageOneTwoSimulator() {
    }

    public static SimulationRun run(
            Path projectRoot,
            Path packRoot,
            Path scenarioPath,
            Path requestedOutput,
            Integer runsOverride,
            Long seedOverride
    ) throws IOException {
        TownStageOneTwoScenario scenario = TownStageOneTwoScenario.load(scenarioPath);
        TownStageOneTwoData data = TownStageOneTwoData.load(projectRoot, packRoot);
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageZeroModel.StageZeroMetrics algebraicTheory = TownStageZeroModel.analyze(
                parameters,
                data.mineWeights(),
                data.huntingLoot(),
                data.coalRecipeProcessTicks(),
                data.cokeRecipeProcessTicks(),
                scenario.tower().researchEfficiencyBonus());
        int runs = runsOverride == null ? scenario.simulation().runs() : runsOverride;
        if (runs <= 0) throw new IllegalArgumentException("--runs must be positive.");
        long seed = seedOverride == null ? scenario.simulation().seed() : seedOverride;
        Path output = requestedOutput == null
                ? projectRoot.toAbsolutePath().normalize()
                        .resolve("build/reports/town-model/simulations")
                        .resolve(OffsetDateTime.now().format(FILE_TIME))
                : requestedOutput.toAbsolutePath().normalize();
        Files.createDirectories(output);

        double miningSwe = TownStageOneTwoTheory.totalSwe(
                scenario.workers().mining(), parameters.mining().productivity());
        double huntingSwe = TownStageOneTwoTheory.totalSwe(
                scenario.workers().hunting(), parameters.hunting().productivity());
        TownStageOneTwoTheory.MiningTheory mining = TownStageOneTwoTheory.mining(
                miningSwe,
                parameters.mining().baseOutputPerStandardWorkerDay(),
                data.mineWeights(),
                parameters.terrainResources().oreReservePerChunk());
        double coalCoked = Math.min(
                mining.coalItems(), scenario.processing().coalToCokeCapacityPerDay());

        TownStageOneTwoTheory.TowerFuelTheory coalTower = TownStageOneTwoTheory.towerFuel(
                data.coalRecipeProcessTicks(), parameters.generatorT1(),
                scenario.tower().overdrive(), scenario.tower().researchEfficiencyBonus());
        TownStageOneTwoTheory.TowerFuelTheory cokeTower = TownStageOneTwoTheory.towerFuel(
                data.cokeRecipeProcessTicks(), parameters.generatorT1(),
                scenario.tower().overdrive(), scenario.tower().researchEfficiencyBonus());
        TownStageOneTwoTheory.TowerFuelTheory selectedTower = "coal".equals(scenario.tower().fuel())
                ? coalTower : cokeTower;
        double selectedFuelProduced = "coal".equals(scenario.tower().fuel())
                ? mining.coalItems() - coalCoked : coalCoked;

        HuntingDailyModel.RollPlan rollPlan = HuntingDailyModel.planRolls(
                huntingSwe,
                parameters.hunting().expectedLootRollsPerStandardWorkerDay(),
                parameters.hunting().passiveExpectedLootRollsPerBaseDay(),
                scenario.hunting().initialLootRollCarry(),
                parameters.hunting().useFractionalLootRollCarry(),
                scenario.hunting().availableHuntUnits());

        TownStageOneTwoTheory.Moment baselinePerRoll = TownStageOneTwoTheory.lootMoment(
                data.huntingLoot(), data.meats(),
                scenario.processing().rawMeatProcessingCapacityPerDay());
        List<RunRow> rows = new ArrayList<>(runs);
        double[] meatSamples = new double[runs];
        double[] foodSamples = new double[runs];
        for (int index = 0; index < runs; index++) {
            long runSeed = mixedSeed(seed, index);
            RunRow row = runHuntingSample(
                    index, runSeed, rollPlan.executedRolls(),
                    scenario.processing().rawMeatProcessingCapacityPerDay(), data);
            rows.add(row);
            meatSamples[index] = row.rawMeatItems();
            foodSamples[index] = row.foodUnitsProduced();
        }
        SampleStats meatStats = SampleStats.of(meatSamples);
        SampleStats foodStats = SampleStats.of(foodSamples);
        double theoryMeatMean = rollPlan.executedRolls() * baselinePerRoll.meanMeatItemsPerRoll();
        double theoryMeatVariance = rollPlan.executedRolls() * baselinePerRoll.varianceMeatItemsPerRoll();
        double theoryMeatStandardError = Math.sqrt(theoryMeatVariance / runs);
        Double theoryFoodMean = exactFoodMeanForBaseline(
                rollPlan.executedRolls(),
                scenario.processing().rawMeatProcessingCapacityPerDay(),
                baselinePerRoll);
        Double theoryFoodStandardError = theoryFoodMean == null ? null
                : Math.sqrt(rollPlan.executedRolls()
                        * baselinePerRoll.varianceFoodUnitsPerRoll() / runs);

        HouseResult house = evaluateScenarioHouse(parameters, data, scenario.house());
        Summary summary = new Summary(
                1,
                "stage-1-2-independent-day-kernels",
                scenario.metadata(),
                runs,
                seed,
                List.of(
                        "No cross-day inventory, worker eligibility, proficiency, or assignment feedback.",
                        "Hunting production and controlled house settlement are separate experiments; game building order is stage 3.",
                        "Climate, T2 heat networks, and thermal dynamics are excluded."),
                parameters,
                data.sourceFiles(),
                algebraicTheory,
                new WorkerSummary(miningSwe, huntingSwe),
                new MiningSummary(
                        mining,
                        coalCoked,
                        mining.coalItems() - coalCoked,
                        coalCoked,
                        coalTower,
                        cokeTower,
                        scenario.tower().fuel(),
                        selectedTower.theoryItemsPerActiveDay() * scenario.tower().activeFraction(),
                        selectedFuelProduced / Math.max(
                                TownFoodInventoryModel.RESOURCE_EPSILON,
                                selectedTower.theoryItemsPerActiveDay()
                                        * scenario.tower().activeFraction())),
                new HuntingSummary(
                        rollPlan,
                        baselinePerRoll,
                        theoryMeatMean,
                        theoryMeatStandardError,
                        meatStats,
                        withinThreeStandardErrors(meatStats.mean(), theoryMeatMean,
                                theoryMeatStandardError),
                        theoryFoodMean,
                        theoryFoodStandardError,
                        foodStats,
                        theoryFoodMean == null || withinThreeStandardErrors(
                                foodStats.mean(), theoryFoodMean, theoryFoodStandardError)),
                house);

        Path summaryPath = output.resolve("summary.json");
        Path runsPath = output.resolve("runs.csv");
        Path miningSweepPath = output.resolve("mining-t1-sweep.csv");
        Path huntingSweepPath = output.resolve("hunting-processing-sweep.csv");
        Path houseTemperaturePath = output.resolve("house-temperature-sweep.csv");
        Path houseFoodPath = output.resolve("house-food-sweep.csv");
        Files.writeString(summaryPath, GSON.toJson(summary) + System.lineSeparator(), StandardCharsets.UTF_8);
        writeRuns(runsPath, rows);
        writeMiningSweep(miningSweepPath, scenario, parameters, data, coalTower, cokeTower);
        writeHuntingSweep(huntingSweepPath, scenario, parameters, data, runs, seed);
        writeHouseTemperatureSweep(houseTemperaturePath, scenario, parameters);
        writeHouseFoodSweep(houseFoodPath, scenario, parameters);

        return new SimulationRun(
                output, summaryPath, runsPath, miningSweepPath, huntingSweepPath,
                houseTemperaturePath, houseFoodPath, summary);
    }

    private static RunRow runHuntingSample(
            int run,
            long seed,
            int rolls,
            double processingCapacity,
            TownStageOneTwoData data
    ) {
        SplittableRandom random = new SplittableRandom(seed);
        Map<String, Integer> allLoot = new LinkedHashMap<>();
        Map<String, Integer> rawMeat = new LinkedHashMap<>();
        Map<String, TownFoodProcessingModel.MeatDefinition> meatDefinitions = new HashMap<>();
        data.meats().forEach(meat -> meatDefinitions.put(meat.rawItem(), meat));
        for (int roll = 0; roll < rolls; roll++) {
            TownStageOneTwoTheory.LootSample sample =
                    TownStageOneTwoTheory.sampleLoot(data.huntingLoot(), random);
            if (sample.count() <= 0) continue;
            allLoot.merge(sample.item(), sample.count(), Integer::sum);
            if (meatDefinitions.containsKey(sample.item())) {
                rawMeat.merge(sample.item(), sample.count(), Integer::sum);
            }
        }
        TownFoodProcessingModel.ProcessingResult processing =
                TownFoodProcessingModel.process(rawMeat, processingCapacity, data.meats());
        return new RunRow(
                run,
                seed,
                rolls,
                processing.rawInputItems(),
                processing.processedItems(),
                TownFoodProcessingModel.totalFoodUnits(processing, data.meats()),
                TownFoodProcessingModel.totalNutrition(processing, data.meats()),
                allLoot.getOrDefault("minecraft:beef", 0),
                allLoot.getOrDefault("minecraft:porkchop", 0),
                allLoot.getOrDefault("minecraft:chicken", 0),
                allLoot.getOrDefault("minecraft:mutton", 0));
    }

    private static HouseResult evaluateScenarioHouse(
            TownModelParameters parameters,
            TownStageOneTwoData data,
            TownStageOneTwoScenario.House house
    ) {
        List<TownFoodInventoryModel.FoodStack> inventory = new ArrayList<>();
        for (TownStageOneTwoScenario.InventoryItem item : house.foodInventory()) {
            TownStageOneTwoData.FoodDefinition food = data.foods().get(item.item());
            if (food == null) {
                throw new IllegalArgumentException(
                        "Stage 2 currently accepts only audited hunting meat in house inventory: "
                                + item.item());
            }
            inventory.add(new TownFoodInventoryModel.FoodStack(
                    food.item(), food.foodLevel(), item.amountItems(),
                    food.foodUnitsPerItem(), food.nutritionPerItem()));
        }
        double requiredFood = house.residentCount()
                * parameters.housing().foodConsumptionPerResidentDay();
        TownFoodInventoryModel.Consumption consumption =
                TownFoodInventoryModel.consume(requiredFood, inventory);
        HouseDailyModel.SettlementReport settlement = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        house.residentCount(),
                        consumption.consumedFoodUnits(),
                        house.temperatureCelsius(),
                        house.areaBlocks(),
                        house.volumeBlocks(),
                        house.decorationRating()),
                TownStageOneTwoTheory.houseParameters(parameters));
        HouseDailyModel.ResidentEffects effects = HouseDailyModel.calculateResidentEffects(
                house.residentHealth(),
                house.residentMental(),
                settlement.foodSatisfaction(),
                1.0,
                1.0,
                settlement.effectiveTemperature(),
                settlement.temperatureRating(),
                settlement.comfortRating(),
                TownStageOneTwoTheory.residentEffectParameters(parameters));
        int capacity = HouseDailyModel.calculateCapacity(
                settlement.spaceRating(),
                house.areaBlocks(),
                parameters.housing().floorBlocksPerResident(),
                house.bedCount());
        boolean structurallyWorkable = HouseDailyModel.isStructurallyWorkable(
                true,
                house.areaBlocks(),
                house.volumeBlocks(),
                parameters.housing().minimumFloorAreaBlocks(),
                parameters.housing().minimumInteriorVolumeBlocks());
        boolean temperatureValid = house.temperatureCelsius()
                >= parameters.housing().minimumTemperatureCelsius()
                && house.temperatureCelsius() <= parameters.housing().maximumTemperatureCelsius();
        return new HouseResult(
                capacity,
                house.residentCount() > capacity,
                structurallyWorkable,
                structurallyWorkable && temperatureValid,
                structurallyWorkable,
                consumption,
                settlement,
                effects);
    }

    private static void writeRuns(Path path, List<RunRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("run,seed,executed_rolls,raw_meat_items,processed_meat_items,food_units_produced,nutrition_produced,beef,porkchop,chicken,mutton\n");
            for (RunRow row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%.9f,%.9f,%.9f,%.9f,%d,%d,%d,%d%n",
                        row.run(), row.seed(), row.executedRolls(), row.rawMeatItems(),
                        row.processedMeatItems(), row.foodUnitsProduced(), row.nutritionProduced(),
                        row.beef(), row.porkchop(), row.chicken(), row.mutton()));
            }
        }
    }

    private static void writeMiningSweep(
            Path path,
            TownStageOneTwoScenario scenario,
            TownModelParameters parameters,
            TownStageOneTwoData data,
            TownStageOneTwoTheory.TowerFuelTheory coalTower,
            TownStageOneTwoTheory.TowerFuelTheory cokeTower
    ) throws IOException {
        double coalFraction = TownStageOneTwoTheory.itemWeightFraction(
                data.mineWeights(), "minecraft:coal");
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("base_output_items_per_swe_day,fuel,theory_required_mining_swe,simulation_required_mining_swe,coal_items_per_swe_day\n");
            for (double output : scenario.diagnostics().miningBaseOutputPerSweDay()) {
                double coalPerSwe = output * coalFraction;
                writeMiningSweepRow(writer, output, "coal", coalPerSwe, coalTower);
                writeMiningSweepRow(writer, output, "coke", coalPerSwe, cokeTower);
            }
        }
    }

    private static void writeMiningSweepRow(
            BufferedWriter writer,
            double output,
            String fuel,
            double coalPerSwe,
            TownStageOneTwoTheory.TowerFuelTheory tower
    ) throws IOException {
        writer.write(String.format(Locale.ROOT, "%s,%s,%.12f,%.12f,%.12f%n",
                decimal(output), fuel,
                tower.theoryItemsPerActiveDay() / coalPerSwe,
                tower.simulationItemsPerActiveDay() / coalPerSwe,
                coalPerSwe));
    }

    private static void writeHuntingSweep(
            Path path,
            TownStageOneTwoScenario scenario,
            TownModelParameters parameters,
            TownStageOneTwoData data,
            int runs,
            long seed
    ) throws IOException {
        double rollsPerSweDay = parameters.hunting().expectedLootRollsPerStandardWorkerDay();
        if (rollsPerSweDay <= 0.0) throw new IllegalArgumentException("Hunting roll rate must be positive.");
        double unitRollSwe = 1.0 / rollsPerSweDay;
        double foodPerResident = parameters.housing().foodConsumptionPerResidentDay();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("processing_capacity_meat_per_day,theory_food_units_per_hunting_swe_day,simulation_mean_food_units_per_hunting_swe_day,simulation_ci95_food_units,theory_supported_residents_per_hunting_swe,simulation_mean_supported_residents_per_hunting_swe,simulation_ci95_supported_residents\n");
            for (double capacity : scenario.diagnostics().rawMeatProcessingCapacityPerDay()) {
                TownStageOneTwoTheory.Moment moment = TownStageOneTwoTheory.lootMoment(
                        data.huntingLoot(), data.meats(), capacity);
                double[] samples = new double[runs];
                for (int index = 0; index < runs; index++) {
                    RunRow row = runHuntingSample(
                            index,
                            mixedSeed(seed ^ 0x5deece66dL, index),
                            1,
                            capacity,
                            data);
                    samples[index] = row.foodUnitsProduced() / unitRollSwe;
                }
                SampleStats stats = SampleStats.of(samples);
                double theoryFood = moment.meanFoodUnitsPerRoll() / unitRollSwe;
                double ci95Food = 1.96 * stats.standardError();
                writer.write(String.format(Locale.ROOT,
                        "%s,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f%n",
                        decimal(capacity), theoryFood, stats.mean(), ci95Food,
                        theoryFood / foodPerResident,
                        stats.mean() / foodPerResident,
                        ci95Food / foodPerResident));
            }
        }
    }

    private static void writeHouseTemperatureSweep(
            Path path,
            TownStageOneTwoScenario scenario,
            TownModelParameters parameters
    ) throws IOException {
        TownStageOneTwoScenario.House house = scenario.house();
        double requiredFood = house.residentCount()
                * parameters.housing().foodConsumptionPerResidentDay();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("temperature_celsius,safe_minimum_temperature_celsius,"
                    + "safe_maximum_temperature_celsius,full_stress_distance_celsius,"
                    + "temperature_stress_penalty_exponent,theory_health_delta,simulation_health_delta,"
                    + "theory_mental_delta,simulation_mental_delta,food_stress,temperature_stress,"
                    + "health_food_penalty,health_temperature_penalty,health_total_penalty,health_recovery,"
                    + "mental_food_penalty,mental_temperature_penalty,mental_total_penalty,mental_recovery,"
                    + "temperature_rating,comfort_rating\n");
            for (double temperature : scenario.diagnostics().houseTemperatureCelsius()) {
                HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                        new HouseDailyModel.SettlementInput(
                                house.residentCount(), requiredFood, temperature,
                                house.areaBlocks(), house.volumeBlocks(), house.decorationRating()),
                        TownStageOneTwoTheory.houseParameters(parameters));
                HouseDailyModel.ResidentEffects effects = controlledEffects(
                        house, parameters, report);
                writer.write(String.format(Locale.ROOT,
                        "%s,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,"
                                + "%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,"
                                + "%.12f,%.12f%n",
                        decimal(temperature),
                        parameters.housing().minimumTemperatureCelsius(),
                        parameters.housing().maximumTemperatureCelsius(),
                        parameters.housing().temperatureFullStressDistanceCelsius(),
                        parameters.housing().temperatureStressPenaltyExponent(),
                        effects.healthDelta(), effects.healthDelta(),
                        effects.mentalDelta(), effects.mentalDelta(),
                        effects.foodStress(), effects.temperatureStress(),
                        effects.healthFoodPenalty(), effects.healthTemperaturePenalty(),
                        effects.healthPenalty(), effects.healthRecovery(),
                        effects.mentalFoodPenalty(), effects.mentalTemperaturePenalty(),
                        effects.mentalPenalty(), effects.mentalRecovery(),
                        report.temperatureRating(), report.comfortRating()));
            }
        }
    }

    private static void writeHouseFoodSweep(
            Path path,
            TownStageOneTwoScenario scenario,
            TownModelParameters parameters
    ) throws IOException {
        TownStageOneTwoScenario.House house = scenario.house();
        double requiredFood = house.residentCount()
                * parameters.housing().foodConsumptionPerResidentDay();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("food_satisfaction,food_deficit_penalty_exponent,"
                    + "theory_health_delta,simulation_health_delta,"
                    + "theory_mental_delta,simulation_mental_delta,food_stress,temperature_stress,"
                    + "health_food_penalty,health_temperature_penalty,health_total_penalty,health_recovery,"
                    + "mental_food_penalty,mental_temperature_penalty,mental_total_penalty,mental_recovery\n");
            for (double satisfaction : scenario.diagnostics().foodSatisfaction()) {
                double consumed = requiredFood * satisfaction;
                HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                        new HouseDailyModel.SettlementInput(
                                house.residentCount(), consumed,
                                house.temperatureCelsius(), house.areaBlocks(),
                                house.volumeBlocks(), house.decorationRating()),
                        TownStageOneTwoTheory.houseParameters(parameters));
                HouseDailyModel.ResidentEffects effects = controlledEffects(
                        house, parameters, report);
                writer.write(String.format(Locale.ROOT,
                        "%s,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,"
                                + "%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f%n",
                        decimal(satisfaction), parameters.housing().foodDeficitPenaltyExponent(),
                        effects.healthDelta(), effects.healthDelta(),
                        effects.mentalDelta(), effects.mentalDelta(),
                        effects.foodStress(), effects.temperatureStress(),
                        effects.healthFoodPenalty(), effects.healthTemperaturePenalty(),
                        effects.healthPenalty(), effects.healthRecovery(),
                        effects.mentalFoodPenalty(), effects.mentalTemperaturePenalty(),
                        effects.mentalPenalty(), effects.mentalRecovery()));
            }
        }
    }

    private static HouseDailyModel.ResidentEffects controlledEffects(
            TownStageOneTwoScenario.House house,
            TownModelParameters parameters,
            HouseDailyModel.SettlementReport report
    ) {
        return HouseDailyModel.calculateResidentEffects(
                house.residentHealth(), house.residentMental(),
                report.foodSatisfaction(), 1.0, 1.0,
                report.effectiveTemperature(), report.temperatureRating(), report.comfortRating(),
                TownStageOneTwoTheory.residentEffectParameters(parameters));
    }

    private static Double exactFoodMeanForBaseline(
            int rolls,
            double capacity,
            TownStageOneTwoTheory.Moment perRoll
    ) {
        if (rolls == 0) return 0.0;
        if (rolls == 1 || capacity == 0.0 || Double.isInfinite(capacity)) {
            return rolls * perRoll.meanFoodUnitsPerRoll();
        }
        return null;
    }

    private static boolean withinThreeStandardErrors(double sample, double theory, double standardError) {
        if (standardError <= 0.0) return Math.abs(sample - theory) <= 1.0e-12;
        return Math.abs(sample - theory) <= 3.0 * standardError;
    }

    private static long mixedSeed(long base, int index) {
        long value = base + 0x9E3779B97F4A7C15L * (index + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.12g", value);
    }

    public static void printSummary(SimulationRun run) {
        Summary summary = run.summary();
        System.out.println("Town model simulation: stages 1-2 / independent day kernels");
        System.out.printf(Locale.ROOT, "  Mining SWE: %.6f; coal: %.6f item/day%n",
                summary.workers().miningSwe(), summary.mining().theory().coalItems());
        System.out.printf(Locale.ROOT,
                "  Hunting rolls: %d; meat theory/sample: %.6f / %.6f item/day; 3SE=%s%n",
                summary.hunting().rollPlan().executedRolls(),
                summary.hunting().theoryMeatItemsPerRun(),
                summary.hunting().meatSimulation().mean(),
                summary.hunting().meatWithinThreeStandardErrors());
        System.out.printf(Locale.ROOT,
                "  House food satisfaction: %.3f; health delta: %.3f; mental delta: %.3f%n",
                summary.house().settlement().foodSatisfaction(),
                summary.house().residentEffects().healthDelta(),
                summary.house().residentEffects().mentalDelta());
        System.out.println("  Reports: " + run.outputDirectory());
    }

    public record SampleStats(
            int count,
            double mean,
            double standardDeviation,
            double standardError,
            double p5,
            double p50,
            double p95
    ) {
        static SampleStats of(double[] values) {
            if (values.length == 0) throw new IllegalArgumentException("At least one sample is required.");
            double mean = Arrays.stream(values).average().orElseThrow();
            double sumSquares = 0.0;
            for (double value : values) {
                double difference = value - mean;
                sumSquares += difference * difference;
            }
            double standardDeviation = values.length > 1
                    ? Math.sqrt(sumSquares / (values.length - 1)) : 0.0;
            double[] sorted = values.clone();
            Arrays.sort(sorted);
            return new SampleStats(
                    values.length,
                    mean,
                    standardDeviation,
                    standardDeviation / Math.sqrt(values.length),
                    quantile(sorted, 0.05),
                    quantile(sorted, 0.50),
                    quantile(sorted, 0.95));
        }

        private static double quantile(double[] sorted, double probability) {
            if (sorted.length == 1) return sorted[0];
            double index = probability * (sorted.length - 1);
            int lower = (int) Math.floor(index);
            int upper = (int) Math.ceil(index);
            double fraction = index - lower;
            return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
        }
    }

    public record RunRow(
            int run,
            long seed,
            int executedRolls,
            double rawMeatItems,
            double processedMeatItems,
            double foodUnitsProduced,
            double nutritionProduced,
            int beef,
            int porkchop,
            int chicken,
            int mutton
    ) {
    }

    public record WorkerSummary(double miningSwe, double huntingSwe) {
    }

    public record MiningSummary(
            TownStageOneTwoTheory.MiningTheory theory,
            double coalProcessedToCoke,
            double unprocessedCoal,
            double cokeProduced,
            TownStageOneTwoTheory.TowerFuelTheory coalTower,
            TownStageOneTwoTheory.TowerFuelTheory cokeTower,
            String selectedFuel,
            double selectedFuelItemsPerScenarioDay,
            double selectedFuelSelfSupplyRatio
    ) {
    }

    public record HuntingSummary(
            HuntingDailyModel.RollPlan rollPlan,
            TownStageOneTwoTheory.Moment perRollTheory,
            double theoryMeatItemsPerRun,
            double theoryMeatStandardError,
            SampleStats meatSimulation,
            boolean meatWithinThreeStandardErrors,
            Double theoryFoodUnitsPerRun,
            Double theoryFoodStandardError,
            SampleStats foodSimulation,
            boolean foodWithinThreeStandardErrors
    ) {
    }

    public record HouseResult(
            int capacityResidents,
            boolean overCapacity,
            boolean structurallyWorkable,
            boolean allocatableAtCurrentTemperature,
            boolean dailySettlementRuns,
            TownFoodInventoryModel.Consumption foodConsumption,
            HouseDailyModel.SettlementReport settlement,
            HouseDailyModel.ResidentEffects residentEffects
    ) {
    }

    public record Summary(
            int schemaVersion,
            String scope,
            TownStageOneTwoScenario.Metadata scenario,
            int runs,
            long seed,
            List<String> explicitBoundary,
            TownModelParameters parameters,
            Map<String, String> sourceFiles,
            TownStageZeroModel.StageZeroMetrics algebraicTheory,
            WorkerSummary workers,
            MiningSummary mining,
            HuntingSummary hunting,
            HouseResult house
    ) {
    }

    public record SimulationRun(
            Path outputDirectory,
            Path summaryPath,
            Path runsPath,
            Path miningSweepPath,
            Path huntingSweepPath,
            Path houseTemperatureSweepPath,
            Path houseFoodSweepPath,
            Summary summary
    ) {
    }
}
