/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorFuelModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads current FH/TWR stage-0 sources and writes traceable algebra reports. */
public final class TownStageZeroAudit {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String SCOPE = "stage-0-t1-algebra";

    private TownStageZeroAudit() {
    }

    public static AuditRun run(Path projectRoot, Path packRoot, Path requestedOutput) throws IOException {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        Path normalizedPackRoot = packRoot.toAbsolutePath().normalize();
        Path output = requestedOutput == null
                ? normalizedProjectRoot.resolve("build/reports/town-model/audit")
                        .resolve(OffsetDateTime.now().format(FILE_TIME))
                : requestedOutput.toAbsolutePath().normalize();

        InputPaths paths = InputPaths.resolve(normalizedProjectRoot, normalizedPackRoot);
        paths.requireAll();

        TownModelParameters parameters = TownModelParameters.currentDefaults();
        int coalRecipeTicks = parseGeneratorRecipeProcessTicks(paths.generatorCoal());
        int cokeRecipeTicks = parseGeneratorRecipeProcessTicks(paths.generatorCoke());
        List<TownStageZeroModel.WeightedLootEntry> huntingLoot = parseHuntingLoot(paths.huntingLoot());
        List<TownStageZeroModel.WeightedResource> mineWeights = parseBiomeMineWeights(
                Files.readString(paths.biomeMineScript()), "the_winter_rescue:fossil_deposits");
        double efficiencyLevelOne = parseResearchStatBonus(paths.generatorEfficiencyOne(), "generator_effi");
        double efficiencyLevelTwo = parseResearchStatBonus(paths.generatorEfficiencyTwo(), "generator_effi");

        TownStageZeroModel.StageZeroMetrics metrics = TownStageZeroModel.analyze(
                parameters,
                mineWeights,
                huntingLoot,
                coalRecipeTicks,
                cokeRecipeTicks,
                0.0);
        TownStageZeroModel.StageZeroMetrics efficiencyOneMetrics = TownStageZeroModel.analyze(
                parameters,
                mineWeights,
                huntingLoot,
                coalRecipeTicks,
                cokeRecipeTicks,
                efficiencyLevelOne);
        double cumulativeEfficiencyBonus = efficiencyLevelOne + efficiencyLevelTwo;
        TownStageZeroModel.StageZeroMetrics efficiencyTwoMetrics = TownStageZeroModel.analyze(
                parameters,
                mineWeights,
                huntingLoot,
                coalRecipeTicks,
                cokeRecipeTicks,
                cumulativeEfficiencyBonus);

        List<SourceFile> sourceFiles = sourceFiles(paths);
        List<ParameterValue> parameterValues = parameterValues(
                parameters,
                paths,
                mineWeights,
                huntingLoot,
                coalRecipeTicks,
                cokeRecipeTicks,
                efficiencyLevelOne,
                efficiencyLevelTwo);
        String snapshotHash = snapshotHash(sourceFiles, parameterValues);
        String generatedAt = OffsetDateTime.now().toString();
        SourceSnapshot snapshot = new SourceSnapshot(
                1, SCOPE, generatedAt, snapshotHash, sourceFiles, parameterValues);
        List<AuditIssue> issues = List.of();
        AuditReport report = new AuditReport(
                1,
                SCOPE,
                generatedAt,
                snapshotHash,
                auditMetrics(metrics, efficiencyOneMetrics, efficiencyTwoMetrics),
                issues,
                List.of("T2 generator heat network", "multi-day simulation", "climate and thermal inertia"));

        Files.createDirectories(output);
        Path snapshotPath = output.resolve("source-snapshot.json");
        Path reportPath = output.resolve("audit-report.json");
        writeJson(snapshotPath, snapshot);
        writeJson(reportPath, report);
        return new AuditRun(output, snapshotPath, reportPath, snapshot, report, metrics);
    }

    static int parseGeneratorRecipeProcessTicks(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return root.get("time").getAsInt();
    }

    static List<TownStageZeroModel.WeightedLootEntry> parseHuntingLoot(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        JsonArray pools = root.getAsJsonArray("pools");
        if (pools == null || pools.size() != 1) {
            throw new IllegalArgumentException("Stage 0 expects exactly one hunting loot pool: " + path);
        }
        JsonObject pool = pools.get(0).getAsJsonObject();
        if (!pool.get("rolls").isJsonPrimitive() || pool.get("rolls").getAsDouble() != 1.0) {
            throw new IllegalArgumentException("Stage 0 expects one roll per hunting pool: " + path);
        }

        List<TownStageZeroModel.WeightedLootEntry> result = new ArrayList<>();
        for (JsonElement element : pool.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (!"minecraft:item".equals(entry.get("type").getAsString())) {
                throw new IllegalArgumentException("Unsupported hunting loot entry type in " + path);
            }
            double minimum = 1.0;
            double maximum = 1.0;
            if (entry.has("functions")) {
                for (JsonElement functionElement : entry.getAsJsonArray("functions")) {
                    JsonObject function = functionElement.getAsJsonObject();
                    if (!"minecraft:set_count".equals(function.get("function").getAsString())) continue;
                    JsonElement count = function.get("count");
                    if (count.isJsonPrimitive()) {
                        minimum = count.getAsDouble();
                        maximum = minimum;
                    } else {
                        JsonObject range = count.getAsJsonObject();
                        minimum = range.get("min").getAsDouble();
                        maximum = range.get("max").getAsDouble();
                    }
                }
            }
            result.add(new TownStageZeroModel.WeightedLootEntry(
                    entry.get("name").getAsString(),
                    entry.has("weight") ? entry.get("weight").getAsDouble() : 1.0,
                    minimum,
                    maximum));
        }
        return List.copyOf(result);
    }

    static List<TownStageZeroModel.WeightedResource> parseBiomeMineWeights(
            String script,
            String biomeId
    ) {
        Pattern recipePattern = Pattern.compile(
                "biomeMineResourceRecipe\\(\\s*['\"]" + Pattern.quote(biomeId)
                        + "['\"]\\s*,\\s*\\{(.*?)\\}\\s*\\)",
                Pattern.DOTALL);
        Matcher recipeMatcher = recipePattern.matcher(script);
        if (!recipeMatcher.find()) {
            throw new IllegalArgumentException("Missing biome mine recipe: " + biomeId);
        }
        Pattern entryPattern = Pattern.compile(
                "['\"]([^'\"]+)['\"]\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
        Matcher entryMatcher = entryPattern.matcher(recipeMatcher.group(1));
        List<TownStageZeroModel.WeightedResource> result = new ArrayList<>();
        while (entryMatcher.find()) {
            result.add(new TownStageZeroModel.WeightedResource(
                    entryMatcher.group(1), Double.parseDouble(entryMatcher.group(2))));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Biome mine recipe has no weights: " + biomeId);
        }
        return List.copyOf(result);
    }

    static double parseResearchStatBonus(Path path, String variable) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        double result = 0.0;
        for (JsonElement element : root.getAsJsonArray("effects")) {
            JsonObject effect = element.getAsJsonObject();
            if (!"stats".equals(effect.get("type").getAsString())) continue;
            if (!variable.equals(effect.get("vars").getAsString())) continue;
            double value = effect.get("val").getAsDouble();
            result += effect.has("percent") && effect.get("percent").getAsBoolean()
                    ? value / 100.0
                    : value;
        }
        return result;
    }

    private static List<SourceFile> sourceFiles(InputPaths paths) throws IOException {
        List<IdentifiedPath> inputs = List.of(
                new IdentifiedPath("fh:town-model-parameters", paths.townModelParameters()),
                new IdentifiedPath("fh:town-math-functions", paths.townMathFunctions()),
                new IdentifiedPath("fh:house-daily-model", paths.houseDailyModel()),
                new IdentifiedPath("fh:resident-daily-model", paths.residentDailyModel()),
                new IdentifiedPath("fh:town-food-resource-amount", paths.townFoodResourceAmount()),
                new IdentifiedPath("fh:generator-fuel-model", paths.generatorFuelModel()),
                new IdentifiedPath("fh:generator-heat-field-model", paths.generatorHeatFieldModel()),
                new IdentifiedPath("fh:generator-data", paths.generatorData()),
                new IdentifiedPath("fh:climate-common-events", paths.climateCommonEvents()),
                new IdentifiedPath("fh:town-config", paths.fhConfig()),
                new IdentifiedPath("fh:generator-coal-recipe", paths.generatorCoal()),
                new IdentifiedPath("fh:generator-coke-recipe", paths.generatorCoke()),
                new IdentifiedPath("fh:hunting-loot", paths.huntingLoot()),
                new IdentifiedPath("twr:biome-mine-script", paths.biomeMineScript()),
                new IdentifiedPath("twr:generator-efficiency-1", paths.generatorEfficiencyOne()),
                new IdentifiedPath("twr:generator-efficiency-2", paths.generatorEfficiencyTwo()));
        List<SourceFile> result = new ArrayList<>();
        for (IdentifiedPath input : inputs) {
            result.add(new SourceFile(
                    input.id(),
                    input.path().toAbsolutePath().normalize().toString(),
                    sha256(Files.readAllBytes(input.path()))));
        }
        return List.copyOf(result);
    }

    private static List<ParameterValue> parameterValues(
            TownModelParameters parameters,
            InputPaths paths,
            List<TownStageZeroModel.WeightedResource> mineWeights,
            List<TownStageZeroModel.WeightedLootEntry> huntingLoot,
            int coalRecipeTicks,
            int cokeRecipeTicks,
            double efficiencyLevelOne,
            double efficiencyLevelTwo
    ) {
        List<ParameterValue> values = new ArrayList<>();
        TownModelParameters.MiningParameters mining = parameters.mining();
        TownModelParameters.HuntingParameters hunting = parameters.hunting();
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        addMiningParameters(values, mining, paths.townModelParameters());
        addProductivity(values, "mining.productivity", mining.productivity(), paths.townModelParameters());
        addHuntingParameters(values, hunting, paths.townModelParameters());
        addProductivity(values, "hunting.productivity", hunting.productivity(), paths.townModelParameters());
        addHousingParameters(values, parameters.housing(), paths.townModelParameters());
        addResidentParameters(values, parameters.residents(), paths.townModelParameters());
        addBuildingScoringParameters(values, parameters.buildingScoring(), paths.townModelParameters());
        addTerrainResourceParameters(values, parameters.terrainResources(), paths.townModelParameters());
        add(values, "generatorT1.baseFuelDurationMultiplier", generator.baseFuelDurationMultiplier(),
                "dimensionless", "shared-default", paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_BASE_FUEL_DURATION_MULTIPLIER -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.baseFuelDurationMultiplier");
        add(values, "generatorT1.baseProcessTicksPerGameTick", generator.baseProcessTicksPerGameTick(),
                "process-tick/game-tick", "shared-default", paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_BASE_PROCESS_TICKS_PER_GAME_TICK -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.baseProcessTicksPerGameTick");
        add(values, "generatorT1.overdriveExtraProcessTicksPerGameTick",
                generator.overdriveExtraProcessTicksPerGameTick(), "process-tick/game-tick",
                "shared-default", paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_OVERDRIVE_EXTRA_PROCESS_TICKS_PER_GAME_TICK -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.overdriveExtraProcessTicksPerGameTick");
        add(values, "generatorT1.townBatchGameTicks", generator.townBatchGameTicks(),
                "game-tick/batch", "shared-default", paths.townModelParameters(),
                "TownModelParameters.Defaults.TOWN_UPDATE_INTERVAL_GAME_TICKS -> "
                        + "FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks");
        add(values, "generatorT1.gameTicksPerDay", generator.gameTicksPerDay(),
                "game-tick/day", "minecraft-unit", paths.townModelParameters(),
                "TownModelParameters.GameUnits.GAME_TICKS_PER_DAY");
        add(values, "generatorT1.coalRecipeProcessTicks", coalRecipeTicks,
                "process-tick/item", "fh-recipe", paths.generatorCoal(), "time");
        add(values, "generatorT1.cokeRecipeProcessTicks", cokeRecipeTicks,
                "process-tick/item", "fh-recipe", paths.generatorCoke(), "time");
        add(values, "research.generatorEfficiency.level1Bonus", efficiencyLevelOne,
                "dimensionless", "twr-research", paths.generatorEfficiencyOne(),
                "effects[vars=generator_effi]");
        add(values, "research.generatorEfficiency.level2Bonus", efficiencyLevelTwo,
                "dimensionless", "twr-research", paths.generatorEfficiencyTwo(),
                "effects[vars=generator_effi]");
        add(values, "research.generatorEfficiency.cumulativeThroughLevel2",
                efficiencyLevelOne + efficiencyLevelTwo, "dimensionless", "derived",
                paths.generatorEfficiencyTwo(), "level1Bonus + level2Bonus");
        add(values, "generatorT1.baseRadiusBlocks", generator.baseRadiusBlocks(), "block",
                "shared-default", paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_BASE_RADIUS_BLOCKS -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.baseRadiusBlocks");
        add(values, "generatorT1.additionalRadiusPerLevelBlocks",
                generator.additionalRadiusPerLevelBlocks(), "block/level", "shared-default",
                paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_ADDITIONAL_RADIUS_PER_LEVEL_BLOCKS -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.additionalRadiusPerLevelBlocks");
        add(values, "generatorT1.temperaturePerLevelCelsius", generator.temperaturePerLevelCelsius(),
                "celsius/level", "shared-default",
                paths.townModelParameters(),
                "TownModelParameters.Defaults.GENERATOR_T1_TEMPERATURE_PER_LEVEL_CELSIUS -> "
                        + "FHConfig.SERVER.TOWN.GENERATOR_T1.temperaturePerLevelCelsius");

        for (TownModelParameters.MeatFoodParameters meat : parameters.meatFoods()) {
            String prefix = "food." + meat.rawItem();
            add(values, prefix + ".rawHunger", meat.rawHunger(), "hunger/item",
                    "minecraft-1.20.1-default", paths.townModelParameters(), "MeatFoodParameters");
            add(values, prefix + ".rawSaturationModifier", meat.rawSaturationModifier(),
                    "dimensionless", "minecraft-1.20.1-default", paths.townModelParameters(),
                    "MeatFoodParameters");
            add(values, prefix + ".cookedHunger", meat.cookedHunger(), "hunger/item",
                    "minecraft-1.20.1-default", paths.townModelParameters(), "MeatFoodParameters");
            add(values, prefix + ".cookedSaturationModifier", meat.cookedSaturationModifier(),
                    "dimensionless", "minecraft-1.20.1-default", paths.townModelParameters(),
                    "MeatFoodParameters");
        }
        for (TownStageZeroModel.WeightedResource entry : mineWeights) {
            add(values, "mine.fossilDeposits.weight." + entry.item(), entry.weight(),
                    "relative-weight", "twr-kubejs", paths.biomeMineScript(),
                    "biomeMineResourceRecipe(the_winter_rescue:fossil_deposits)");
        }
        for (TownStageZeroModel.WeightedLootEntry entry : huntingLoot) {
            String prefix = "hunting.loot." + entry.item();
            add(values, prefix + ".weight", entry.weight(), "relative-weight", "fh-loot-table",
                    paths.huntingLoot(), "pools[0].entries");
            add(values, prefix + ".minimumCount", entry.minimumCount(), "item/roll",
                    "fh-loot-table", paths.huntingLoot(), "minecraft:set_count.min");
            add(values, prefix + ".maximumCount", entry.maximumCount(), "item/roll",
                    "fh-loot-table", paths.huntingLoot(), "minecraft:set_count.max");
        }
        return List.copyOf(values);
    }

    private static void addProductivity(
            List<ParameterValue> values,
            String prefix,
            TownModelParameters.ResidentProductivityParameters productivity,
            Path source
    ) {
        Map<String, Double> fields = new LinkedHashMap<>();
        fields.put("healthWeight", productivity.healthWeight());
        fields.put("mentalWeight", productivity.mentalWeight());
        fields.put("strengthWeight", productivity.strengthWeight());
        fields.put("intelligenceWeight", productivity.intelligenceWeight());
        fields.put("productivityAtAttributeZero", productivity.productivityAtAttributeZero());
        fields.put("productivityAtAttributeHundred", productivity.productivityAtAttributeHundred());
        fields.put("maximumProficiency", productivity.maximumProficiency());
        fields.put("bonusAtMaximumProficiency", productivity.bonusAtMaximumProficiency());
        fields.put("minimumProductivity", productivity.minimumProductivity());
        fields.put("maximumProductivity", productivity.maximumProductivity());
        String configSection = prefix.startsWith("mining.") ? "MINING" : "HUNTING";
        fields.forEach((name, value) -> {
            String unit;
            if (name.endsWith("Weight")) {
                unit = "relative-weight";
            } else if ("maximumProficiency".equals(name)) {
                unit = "proficiency-point";
            } else {
                unit = "SWE/worker";
            }
            add(values, prefix + "." + name, value, unit, "shared-default", source,
                    "TownModelParameters.Defaults." + configSection + "_"
                            + productivityDefaultSuffix(name) + " -> FHConfig.SERVER.TOWN."
                            + configSection + "." + productivityConfigField(name));
        });
    }

    private static void addMiningParameters(
            List<ParameterValue> values,
            TownModelParameters.MiningParameters mining,
            Path source
    ) {
        addShared(values, "mining.baseOutputPerStandardWorkerDay", mining.baseOutputPerStandardWorkerDay(),
                "item/SWE/day", source, "MINING_BASE_OUTPUT_PER_SWE_DAY", "MINING.baseOutputPerStandardWorkerDay");
        addShared(values, "mining.floorBlocksPerWorkerSlot", mining.floorBlocksPerWorkerSlot(),
                "block2/worker", source, "MINING_FLOOR_BLOCKS_PER_WORKER_SLOT", "MINING.floorBlocksPerWorkerSlot");
        addShared(values, "mining.minimumWorkerSlots", mining.minimumWorkerSlots(),
                "worker", source, "MINING_MINIMUM_WORKER_SLOTS", "MINING.minimumWorkerSlots");
        addShared(values, "mining.connectionRadiusBlocks", mining.connectionRadiusBlocks(),
                "block", source, "MINING_CONNECTION_RADIUS_BLOCKS", "MINING.connectionRadiusBlocks");
        addShared(values, "mining.assignmentBasePriority", mining.assignmentBasePriority(),
                "priority", source, "MINING_ASSIGNMENT_BASE_PRIORITY", "MINING.assignmentBasePriority");
        addShared(values, "mining.assignmentPenaltyPerWorker", mining.assignmentPenaltyPerWorker(),
                "priority/worker", source, "MINING_ASSIGNMENT_PENALTY_PER_WORKER", "MINING.assignmentPenaltyPerWorker");
        addShared(values, "mining.assignmentFillRatioBonus", mining.assignmentFillRatioBonus(),
                "priority", source, "MINING_ASSIGNMENT_FILL_RATIO_BONUS", "MINING.assignmentFillRatioBonus");
    }

    private static void addHuntingParameters(
            List<ParameterValue> values,
            TownModelParameters.HuntingParameters hunting,
            Path source
    ) {
        addShared(values, "hunting.expectedLootRollsPerStandardWorkerDay",
                hunting.expectedLootRollsPerStandardWorkerDay(), "roll/SWE/day", source,
                "HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY", "HUNTING.expectedLootRollsPerStandardWorkerDay");
        addShared(values, "hunting.passiveExpectedLootRollsPerBaseDay",
                hunting.passiveExpectedLootRollsPerBaseDay(), "roll/base/day", source,
                "HUNTING_PASSIVE_EXPECTED_LOOT_ROLLS_PER_BASE_DAY", "HUNTING.passiveExpectedLootRollsPerBaseDay");
        addShared(values, "hunting.useFractionalLootRollCarry", hunting.useFractionalLootRollCarry(),
                "boolean", source, "HUNTING_USE_FRACTIONAL_LOOT_ROLL_CARRY", "HUNTING.useFractionalLootRollCarry");
        addShared(values, "hunting.floorBlocksPerWorkerSlot", hunting.floorBlocksPerWorkerSlot(),
                "block2/worker", source, "HUNTING_FLOOR_BLOCKS_PER_WORKER_SLOT", "HUNTING.floorBlocksPerWorkerSlot");
        addShared(values, "hunting.minimumWorkerSlots", hunting.minimumWorkerSlots(),
                "worker", source, "HUNTING_MINIMUM_WORKER_SLOTS", "HUNTING.minimumWorkerSlots");
        addShared(values, "hunting.minimumFloorAreaBlocks", hunting.minimumFloorAreaBlocks(),
                "block2", source, "HUNTING_MINIMUM_FLOOR_AREA_BLOCKS", "HUNTING.minimumFloorAreaBlocks");
        addShared(values, "hunting.minimumInteriorVolumeBlocks", hunting.minimumInteriorVolumeBlocks(),
                "block3", source, "HUNTING_MINIMUM_INTERIOR_VOLUME_BLOCKS", "HUNTING.minimumInteriorVolumeBlocks");
        addShared(values, "hunting.minimumWorkingTemperatureCelsius", hunting.minimumWorkingTemperatureCelsius(),
                "celsius", source, "HUNTING_MINIMUM_WORKING_TEMPERATURE_CELSIUS", "HUNTING.minimumWorkingTemperatureCelsius");
        addShared(values, "hunting.spaceRatingWeight", hunting.spaceRatingWeight(),
                "relative-weight", source, "HUNTING_SPACE_RATING_WEIGHT", "HUNTING.spaceRatingWeight");
        addShared(values, "hunting.temperatureRatingWeight", hunting.temperatureRatingWeight(),
                "relative-weight", source, "HUNTING_TEMPERATURE_RATING_WEIGHT", "HUNTING.temperatureRatingWeight");
        addShared(values, "hunting.assignmentBasePriority", hunting.assignmentBasePriority(),
                "priority", source, "HUNTING_ASSIGNMENT_BASE_PRIORITY", "HUNTING.assignmentBasePriority");
        addShared(values, "hunting.assignmentPenaltyPerWorker", hunting.assignmentPenaltyPerWorker(),
                "priority/worker", source, "HUNTING_ASSIGNMENT_PENALTY_PER_WORKER", "HUNTING.assignmentPenaltyPerWorker");
        addShared(values, "hunting.assignmentFillRatioBonus", hunting.assignmentFillRatioBonus(),
                "priority", source, "HUNTING_ASSIGNMENT_FILL_RATIO_BONUS", "HUNTING.assignmentFillRatioBonus");
        addShared(values, "hunting.assignmentRatingMultiplier", hunting.assignmentRatingMultiplier(),
                "priority/rating", source, "HUNTING_ASSIGNMENT_RATING_MULTIPLIER", "HUNTING.assignmentRatingMultiplier");
    }

    private static void addHousingParameters(
            List<ParameterValue> values,
            TownModelParameters.HousingParameters housing,
            Path source
    ) {
        addShared(values, "housing.foodConsumptionPerResidentDay", housing.foodConsumptionPerResidentDay(),
                "food-unit/resident/day", source, "HOUSING_FOOD_PER_RESIDENT_DAY", "HOUSING.foodConsumptionPerResidentDay");
        addShared(values, "housing.nutritionReferencePerFoodUnit", housing.nutritionReferencePerFoodUnit(),
                "nutrition/food-unit", source, "HOUSING_NUTRITION_REFERENCE_PER_FOOD_UNIT", "HOUSING.nutritionReferencePerFoodUnit");
        addShared(values, "housing.minimumNutritionRecoveryMultiplier", housing.minimumNutritionRecoveryMultiplier(),
                "dimensionless", source, "HOUSING_MINIMUM_NUTRITION_RECOVERY_MULTIPLIER", "HOUSING.minimumNutritionRecoveryMultiplier");
        addShared(values, "housing.healthLossAtZeroFoodPerResidentDay", housing.healthLossAtZeroFoodPerResidentDay(),
                "health/resident/day", source, "HOUSING_HEALTH_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY", "HOUSING.healthLossAtZeroFoodPerResidentDay");
        addShared(values, "housing.mentalLossAtZeroFoodPerResidentDay", housing.mentalLossAtZeroFoodPerResidentDay(),
                "mental/resident/day", source, "HOUSING_MENTAL_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY", "HOUSING.mentalLossAtZeroFoodPerResidentDay");
        addShared(values, "housing.maximumHealthRecoveryPerResidentDay", housing.maximumHealthRecoveryPerResidentDay(),
                "health/resident/day", source, "HOUSING_MAXIMUM_HEALTH_RECOVERY_PER_RESIDENT_DAY", "HOUSING.maximumHealthRecoveryPerResidentDay");
        addShared(values, "housing.maximumMentalRecoveryPerResidentDay", housing.maximumMentalRecoveryPerResidentDay(),
                "mental/resident/day", source, "HOUSING_MAXIMUM_MENTAL_RECOVERY_PER_RESIDENT_DAY", "HOUSING.maximumMentalRecoveryPerResidentDay");
        addShared(values, "housing.minimumFloorAreaBlocks", housing.minimumFloorAreaBlocks(),
                "block2", source, "HOUSING_MINIMUM_FLOOR_AREA_BLOCKS", "HOUSING.minimumFloorAreaBlocks");
        addShared(values, "housing.minimumInteriorVolumeBlocks", housing.minimumInteriorVolumeBlocks(),
                "block3", source, "HOUSING_MINIMUM_INTERIOR_VOLUME_BLOCKS", "HOUSING.minimumInteriorVolumeBlocks");
        addShared(values, "housing.minimumTemperatureCelsius", housing.minimumTemperatureCelsius(),
                "celsius", source, "HOUSING_MINIMUM_TEMPERATURE_CELSIUS", "HOUSING.minimumTemperatureCelsius");
        addShared(values, "housing.maximumTemperatureCelsius", housing.maximumTemperatureCelsius(),
                "celsius", source, "HOUSING_MAXIMUM_TEMPERATURE_CELSIUS", "HOUSING.maximumTemperatureCelsius");
        addShared(values, "housing.floorBlocksPerResident", housing.floorBlocksPerResident(),
                "block2/resident", source, "HOUSING_FLOOR_BLOCKS_PER_RESIDENT", "HOUSING.floorBlocksPerResident");
        addShared(values, "housing.temperatureComfortWeight", housing.temperatureComfortWeight(),
                "relative-weight", source, "HOUSING_TEMPERATURE_COMFORT_WEIGHT", "HOUSING.temperatureComfortWeight");
        addShared(values, "housing.spaceComfortWeight", housing.spaceComfortWeight(),
                "relative-weight", source, "HOUSING_SPACE_COMFORT_WEIGHT", "HOUSING.spaceComfortWeight");
        addShared(values, "housing.decorationComfortWeight", housing.decorationComfortWeight(),
                "relative-weight", source, "HOUSING_DECORATION_COMFORT_WEIGHT", "HOUSING.decorationComfortWeight");
        TownModelParameters.DecorationRatingParameters decoration = housing.decorationRating();
        addShared(values, "housing.decorationRating.countLogOffset", decoration.countLogOffset(),
                "item", source, "DECORATION_COUNT_LOG_OFFSET", "HOUSING.decorationCountLogOffset");
        addShared(values, "housing.decorationRating.countLogMultiplier", decoration.countLogMultiplier(),
                "score", source, "DECORATION_COUNT_LOG_MULTIPLIER", "HOUSING.decorationCountLogMultiplier");
        addShared(values, "housing.decorationRating.typeBaseScore", decoration.typeBaseScore(),
                "score/type", source, "DECORATION_TYPE_BASE_SCORE", "HOUSING.decorationTypeBaseScore");
        addShared(values, "housing.decorationRating.baseDemand", decoration.baseDemand(),
                "score", source, "DECORATION_BASE_DEMAND", "HOUSING.decorationBaseDemand");
        addShared(values, "housing.decorationRating.floorBlocksPerDemand", decoration.floorBlocksPerDemand(),
                "block2/score", source, "DECORATION_FLOOR_BLOCKS_PER_DEMAND", "HOUSING.decorationFloorBlocksPerDemand");
    }

    private static void addResidentParameters(
            List<ParameterValue> values,
            TownModelParameters.ResidentParameters residents,
            Path source
    ) {
        addShared(values, "residents.homelessHealthLossPerDay", residents.homelessHealthLossPerDay(),
                "health/resident/day", source, "RESIDENT_HOMELESS_HEALTH_LOSS_PER_DAY", "RESIDENT_RULES.homelessHealthLossPerDay");
        addShared(values, "residents.removalHealthThreshold", residents.removalHealthThreshold(),
                "health", source, "RESIDENT_REMOVAL_HEALTH_THRESHOLD", "RESIDENT_RULES.removalHealthThreshold");
        addShared(values, "residents.removalMentalThreshold", residents.removalMentalThreshold(),
                "mental", source, "RESIDENT_REMOVAL_MENTAL_THRESHOLD", "RESIDENT_RULES.removalMentalThreshold");
        addShared(values, "residents.minimumWorkingAge", residents.minimumWorkingAge(),
                "age-group", source, "RESIDENT_MINIMUM_WORKING_AGE", "RESIDENT_RULES.minimumWorkingAge");
        addShared(values, "residents.minimumWorkingHealthExclusive", residents.minimumWorkingHealthExclusive(),
                "health", source, "RESIDENT_MINIMUM_WORKING_HEALTH_EXCLUSIVE", "RESIDENT_RULES.minimumWorkingHealthExclusive");
        addShared(values, "residents.minimumWorkingMentalExclusive", residents.minimumWorkingMentalExclusive(),
                "mental", source, "RESIDENT_MINIMUM_WORKING_MENTAL_EXCLUSIVE", "RESIDENT_RULES.minimumWorkingMentalExclusive");
        addShared(values, "residents.workRequiresHousing", residents.workRequiresHousing(),
                "boolean", source, "RESIDENT_WORK_REQUIRES_HOUSING", "RESIDENT_RULES.workRequiresHousing");
        addShared(values, "residents.maximumWorkProficiency", residents.maximumWorkProficiency(),
                "proficiency-point", source, "RESIDENT_MAXIMUM_WORK_PROFICIENCY", "RESIDENT_PROGRESSION.maximumWorkProficiency");
        addShared(values, "residents.proficiencyGrowthAtZeroPerWorkday", residents.proficiencyGrowthAtZeroPerWorkday(),
                "proficiency-point/workday", source, "RESIDENT_PROFICIENCY_GROWTH_AT_ZERO_PER_WORKDAY", "RESIDENT_PROGRESSION.proficiencyGrowthAtZeroPerWorkday");
        addShared(values, "residents.minimumProficiencyGrowthPerWorkday", residents.minimumProficiencyGrowthPerWorkday(),
                "proficiency-point/workday", source, "RESIDENT_MINIMUM_PROFICIENCY_GROWTH_PER_WORKDAY", "RESIDENT_PROGRESSION.minimumProficiencyGrowthPerWorkday");
        TownModelParameters.ResidentAgingParameters aging = residents.aging();
        addShared(values, "residents.aging.infantToChildDays", aging.infantToChildDays(),
                "day", source, "RESIDENT_INFANT_TO_CHILD_DAYS", "RESIDENT_AGING.infantToChildDays");
        addShared(values, "residents.aging.childToAdultDays", aging.childToAdultDays(),
                "day", source, "RESIDENT_CHILD_TO_ADULT_DAYS", "RESIDENT_AGING.childToAdultDays");
        addShared(values, "residents.aging.infantStrengthGainPerDay", aging.infantStrengthGainPerDay(),
                "strength/day", source, "RESIDENT_INFANT_STRENGTH_GAIN_PER_DAY", "RESIDENT_AGING.infantStrengthGainPerDay");
        addShared(values, "residents.aging.infantIntelligenceGainPerDay", aging.infantIntelligenceGainPerDay(),
                "intelligence/day", source, "RESIDENT_INFANT_INTELLIGENCE_GAIN_PER_DAY", "RESIDENT_AGING.infantIntelligenceGainPerDay");
        addShared(values, "residents.aging.infantAttributeCap", aging.infantAttributeCap(),
                "attribute", source, "RESIDENT_INFANT_ATTRIBUTE_CAP", "RESIDENT_AGING.infantAttributeCap");
        addShared(values, "residents.aging.childStrengthGainPerDay", aging.childStrengthGainPerDay(),
                "strength/day", source, "RESIDENT_CHILD_STRENGTH_GAIN_PER_DAY", "RESIDENT_AGING.childStrengthGainPerDay");
        addShared(values, "residents.aging.childIntelligenceGainPerDay", aging.childIntelligenceGainPerDay(),
                "intelligence/day", source, "RESIDENT_CHILD_INTELLIGENCE_GAIN_PER_DAY", "RESIDENT_AGING.childIntelligenceGainPerDay");
        addShared(values, "residents.aging.childStrengthCap", aging.childStrengthCap(),
                "strength", source, "RESIDENT_CHILD_STRENGTH_CAP", "RESIDENT_AGING.childStrengthCap");
        addShared(values, "residents.aging.childIntelligenceCap", aging.childIntelligenceCap(),
                "intelligence", source, "RESIDENT_CHILD_INTELLIGENCE_CAP", "RESIDENT_AGING.childIntelligenceCap");
        addShared(values, "residents.aging.adultStrengthGainPerDay", aging.adultStrengthGainPerDay(),
                "strength/day", source, "RESIDENT_ADULT_STRENGTH_GAIN_PER_DAY", "RESIDENT_AGING.adultStrengthGainPerDay");
        addShared(values, "residents.aging.adultIntelligenceGainPerDay", aging.adultIntelligenceGainPerDay(),
                "intelligence/day", source, "RESIDENT_ADULT_INTELLIGENCE_GAIN_PER_DAY", "RESIDENT_AGING.adultIntelligenceGainPerDay");
        addShared(values, "residents.aging.adultAttributeCap", aging.adultAttributeCap(),
                "attribute", source, "RESIDENT_ADULT_ATTRIBUTE_CAP", "RESIDENT_AGING.adultAttributeCap");
        addShared(values, "residents.aging.elderStrengthDecayPerDay", aging.elderStrengthDecayPerDay(),
                "strength/day", source, "RESIDENT_ELDER_STRENGTH_DECAY_PER_DAY", "RESIDENT_AGING.elderStrengthDecayPerDay");
        addShared(values, "residents.aging.elderStrengthFloor", aging.elderStrengthFloor(),
                "strength", source, "RESIDENT_ELDER_STRENGTH_FLOOR", "RESIDENT_AGING.elderStrengthFloor");
    }

    private static void addBuildingScoringParameters(
            List<ParameterValue> values,
            TownModelParameters.BuildingScoringParameters scoring,
            Path source
    ) {
        TownModelParameters.TemperatureRatingParameters temperature = scoring.temperature();
        addShared(values, "buildingScoring.temperature.comfortableTemperatureCelsius", temperature.comfortableTemperatureCelsius(),
                "celsius", source, "BUILDING_COMFORTABLE_TEMPERATURE_CELSIUS", "BUILDING_SCORING.comfortableTemperatureCelsius");
        addShared(values, "buildingScoring.temperature.minimumRating", temperature.minimumRating(),
                "rating", source, "BUILDING_MINIMUM_TEMPERATURE_RATING", "BUILDING_SCORING.minimumTemperatureRating");
        addShared(values, "buildingScoring.temperature.sigmoidSlopePerCelsius", temperature.sigmoidSlopePerCelsius(),
                "1/celsius", source, "BUILDING_TEMPERATURE_RATING_SLOPE", "BUILDING_SCORING.temperatureRatingSlope");
        addShared(values, "buildingScoring.temperature.halfPointTemperatureDifferenceCelsius", temperature.halfPointTemperatureDifferenceCelsius(),
                "celsius", source, "BUILDING_TEMPERATURE_RATING_HALF_POINT_DIFFERENCE_CELSIUS", "BUILDING_SCORING.temperatureRatingHalfPointDifferenceCelsius");
        TownModelParameters.SpaceRatingParameters space = scoring.space();
        addShared(values, "buildingScoring.space.areaCoefficient", space.areaCoefficient(),
                "score/block2", source, "BUILDING_SPACE_AREA_COEFFICIENT", "BUILDING_SCORING.spaceAreaCoefficient");
        addShared(values, "buildingScoring.space.heightLogCoefficient", space.heightLogCoefficient(),
                "score/block2", source, "BUILDING_SPACE_HEIGHT_LOG_COEFFICIENT", "BUILDING_SCORING.spaceHeightLogCoefficient");
        addShared(values, "buildingScoring.space.heightLogOffset", space.heightLogOffset(),
                "block", source, "BUILDING_SPACE_HEIGHT_LOG_OFFSET", "BUILDING_SCORING.spaceHeightLogOffset");
        addShared(values, "buildingScoring.space.responseScale", space.responseScale(),
                "1/score", source, "BUILDING_SPACE_RESPONSE_SCALE", "BUILDING_SCORING.spaceResponseScale");
        addShared(values, "buildingScoring.space.responseExponent", space.responseExponent(),
                "dimensionless", source, "BUILDING_SPACE_RESPONSE_EXPONENT", "BUILDING_SCORING.spaceResponseExponent");
    }

    private static void addTerrainResourceParameters(
            List<ParameterValue> values,
            TownModelParameters.TerrainResourceParameters resources,
            Path source
    ) {
        addShared(values, "terrainResources.oreReservePerChunk", resources.oreReservePerChunk(),
                "ore/chunk", source, "ORE_RESERVE_PER_CHUNK", "RESOURCE.oreReservePerChunk");
        addShared(values, "terrainResources.oreRecoveryPerChunkDay", resources.oreRecoveryPerChunkDay(),
                "ore/chunk/day", source, "ORE_RECOVERY_PER_CHUNK_DAY", "RESOURCE.oreRecoveryPerChunkDay");
        addShared(values, "terrainResources.huntReservePerSquareBlock", resources.huntReservePerSquareBlock(),
                "hunt/block2", source, "HUNT_RESERVE_PER_SQUARE_BLOCK", "RESOURCE.huntReservePerSquareBlock");
        addShared(values, "terrainResources.huntRecoveryPerSquareBlockDay", resources.huntRecoveryPerSquareBlockDay(),
                "hunt/block2/day", source, "HUNT_RECOVERY_PER_SQUARE_BLOCK_DAY", "RESOURCE.huntRecoveryPerSquareBlockDay");
    }

    private static void addShared(
            List<ParameterValue> values,
            String name,
            Object value,
            String unit,
            Path source,
            String defaultConstant,
            String configPath
    ) {
        add(values, name, value, unit, "shared-default", source,
                "TownModelParameters.Defaults." + defaultConstant
                        + " -> FHConfig.SERVER.TOWN." + configPath);
    }

    private static String productivityDefaultSuffix(String fieldName) {
        return switch (fieldName) {
            case "healthWeight" -> "HEALTH_WEIGHT";
            case "mentalWeight" -> "MENTAL_WEIGHT";
            case "strengthWeight" -> "STRENGTH_WEIGHT";
            case "intelligenceWeight" -> "INTELLIGENCE_WEIGHT";
            case "productivityAtAttributeZero" -> "PRODUCTIVITY_AT_ATTRIBUTE_ZERO";
            case "productivityAtAttributeHundred" -> "PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED";
            case "maximumProficiency" -> "MAXIMUM_PROFICIENCY";
            case "bonusAtMaximumProficiency" -> "BONUS_AT_MAXIMUM_PROFICIENCY";
            case "minimumProductivity" -> "MINIMUM_PRODUCTIVITY";
            case "maximumProductivity" -> "MAXIMUM_PRODUCTIVITY";
            default -> throw new IllegalArgumentException("Unknown productivity field: " + fieldName);
        };
    }

    private static String productivityConfigField(String fieldName) {
        return switch (fieldName) {
            case "minimumProductivity" -> "minimumResidentProductivity";
            case "maximumProductivity" -> "maximumResidentProductivity";
            default -> fieldName;
        };
    }

    private static void add(
            List<ParameterValue> values,
            String name,
            Object value,
            String unit,
            String sourceType,
            Path sourcePath,
            String sourceSymbol
    ) {
        values.add(new ParameterValue(
                name, value, unit, sourceType,
                sourcePath.toAbsolutePath().normalize().toString(), sourceSymbol));
    }

    private static List<AuditMetric> auditMetrics(
            TownStageZeroModel.StageZeroMetrics value,
            TownStageZeroModel.StageZeroMetrics efficiencyOne,
            TownStageZeroModel.StageZeroMetrics efficiencyTwo
    ) {
        List<AuditMetric> result = new ArrayList<>(List.of(
                metric("miningStandardWorkerSwe", value.miningStandardWorkerSwe(), "SWE/worker",
                        "linearResidentProductivity([50,50,50,50], proficiency=0)",
                        "mining.productivity.*"),
                metric("huntingStandardWorkerSwe", value.huntingStandardWorkerSwe(), "SWE/worker",
                        "linearResidentProductivity([50,50,50,50], proficiency=0)",
                        "hunting.productivity.*"),
                metric("coalPerMiningSweDay", value.coalPerMiningSweDay(), "coal/SWE/day",
                        "mining output per SWE-day * coal weight / total fossil-deposit weight",
                        "mining.baseOutputPerStandardWorkerDay", "mine.fossilDeposits.weight.*"),
                metric("meatPerHuntingSweDay", value.meatPerHuntingSweDay(), "meat/SWE/day",
                        "rolls per SWE-day * weighted expected raw-meat count per roll",
                        "hunting.expectedLootRollsPerStandardWorkerDay", "hunting.loot.*"),
                metric("rawFoodUnitsPerHuntingSweDay", value.rawFoodUnitsPerHuntingSweDay(),
                        "food-unit/SWE/day", "rolls per SWE-day * weighted H*(1+2*m) for raw meat",
                        "hunting.expectedLootRollsPerStandardWorkerDay", "hunting.loot.*", "food.*"),
                metric("cookedFoodUnitsPerHuntingSweDay", value.cookedFoodUnitsPerHuntingSweDay(),
                        "food-unit/SWE/day", "rolls per SWE-day * weighted H*(1+2*m) for cooked meat",
                        "hunting.expectedLootRollsPerStandardWorkerDay", "hunting.loot.*", "food.*"),
                metric("foodUnitsPerResidentDay", value.foodUnitsPerResidentDay(),
                        "food-unit/resident/day", "FHConfig housing default",
                        "housing.foodConsumptionPerResidentDay"),
                metric("rawDietHuntingSwePerResident", value.rawDietHuntingSwePerResident(),
                        "hunting-SWE/resident", "food per resident-day / raw food per hunting SWE-day",
                        "housing.foodConsumptionPerResidentDay", "hunting.loot.*", "food.*"),
                metric("cookedDietHuntingSwePerResident", value.cookedDietHuntingSwePerResident(),
                        "hunting-SWE/resident", "food per resident-day / cooked food per hunting SWE-day",
                        "housing.foodConsumptionPerResidentDay", "hunting.loot.*", "food.*"),
                metric("idealTowerCoalPerActiveDay", value.idealTowerCoalPerActiveDay(),
                        "coal/active-day", "24000 / decimalFloor(recipe ticks * duration multiplier)",
                        "generatorT1.coalRecipeProcessTicks", "generatorT1.baseFuelDurationMultiplier"),
                metric("idealTowerCokePerActiveDay", value.idealTowerCokePerActiveDay(),
                        "coke/active-day", "24000 / decimalFloor(recipe ticks * duration multiplier)",
                        "generatorT1.cokeRecipeProcessTicks", "generatorT1.baseFuelDurationMultiplier"),
                metric("idealTowerMiningSweUsingCoal", value.idealTowerMiningSweUsingCoal(),
                        "mining-SWE", "ideal coal per active-day / coal per mining SWE-day",
                        "generatorT1.coalRecipeProcessTicks", "mine.fossilDeposits.weight.*"),
                metric("idealTowerMiningSweUsingCoke", value.idealTowerMiningSweUsingCoke(),
                        "mining-SWE", "ideal coke per active-day / coal per mining SWE-day; 1 coal -> 1 coke",
                        "generatorT1.cokeRecipeProcessTicks", "mine.fossilDeposits.weight.*"),
                metric("currentTownBatchTowerCoalPerActiveDay",
                        value.currentTownBatchTowerCoalPerActiveDay(), "coal/active-day",
                        "20-game-tick batches with remaining process ticks carried across fuel items",
                        "generatorT1.coalRecipeProcessTicks", "generatorT1.townBatchGameTicks"),
                metric("currentTownBatchTowerCokePerActiveDay",
                        value.currentTownBatchTowerCokePerActiveDay(), "coke/active-day",
                        "20-game-tick batches with remaining process ticks carried across fuel items",
                        "generatorT1.cokeRecipeProcessTicks", "generatorT1.townBatchGameTicks"),
                metric("currentTownBatchMiningSweUsingCoal",
                        value.currentTownBatchMiningSweUsingCoal(), "mining-SWE",
                        "current batched coal per active-day / coal per mining SWE-day",
                        "generatorT1.coalRecipeProcessTicks", "mine.fossilDeposits.weight.*"),
                metric("currentTownBatchMiningSweUsingCoke",
                        value.currentTownBatchMiningSweUsingCoke(), "mining-SWE",
                        "current batched coke per active-day / coal per mining SWE-day; 1 coal -> 1 coke",
                        "generatorT1.cokeRecipeProcessTicks", "mine.fossilDeposits.weight.*"),
                metric("t1NormalRadiusBlocks", value.t1NormalRadiusBlocks(), "block",
                        "generator radius at range level 1", "generatorT1.baseRadiusBlocks"),
                metric("t1NormalHeatFieldCelsius", value.t1NormalHeatFieldCelsius(), "celsius",
                        "generator heat field at temperature level 1",
                        "generatorT1.temperaturePerLevelCelsius")));
        addEfficiencyMetrics(result, "generatorEfficiencyLevel1", efficiencyOne,
                "research.generatorEfficiency.level1Bonus");
        addEfficiencyMetrics(result, "generatorEfficiencyLevel2", efficiencyTwo,
                "research.generatorEfficiency.cumulativeThroughLevel2");
        return List.copyOf(result);
    }

    private static void addEfficiencyMetrics(
            List<AuditMetric> result,
            String prefix,
            TownStageZeroModel.StageZeroMetrics metrics,
            String researchInput
    ) {
        result.add(metric(prefix + "EffectiveCoalProcessTicks",
                metrics.effectiveCoalProcessTicks(), "process-tick/item",
                "decimal floor of recipe ticks * (base multiplier + research bonus)",
                "generatorT1.coalRecipeProcessTicks", "generatorT1.baseFuelDurationMultiplier",
                researchInput));
        result.add(metric(prefix + "EffectiveCokeProcessTicks",
                metrics.effectiveCokeProcessTicks(), "process-tick/item",
                "decimal floor of recipe ticks * (base multiplier + research bonus)",
                "generatorT1.cokeRecipeProcessTicks", "generatorT1.baseFuelDurationMultiplier",
                researchInput));
        result.add(metric(prefix + "IdealTowerCoalPerActiveDay",
                metrics.idealTowerCoalPerActiveDay(), "coal/active-day",
                "24000 / current effective process ticks",
                "generatorT1.coalRecipeProcessTicks", researchInput));
        result.add(metric(prefix + "IdealTowerCokePerActiveDay",
                metrics.idealTowerCokePerActiveDay(), "coke/active-day",
                "24000 / current effective process ticks",
                "generatorT1.cokeRecipeProcessTicks", researchInput));
        result.add(metric(prefix + "CurrentTownBatchTowerCoalPerActiveDay",
                metrics.currentTownBatchTowerCoalPerActiveDay(), "coal/active-day",
                "current effective process ticks with carry-preserving 20-tick batching",
                "generatorT1.coalRecipeProcessTicks", "generatorT1.townBatchGameTicks",
                researchInput));
        result.add(metric(prefix + "CurrentTownBatchTowerCokePerActiveDay",
                metrics.currentTownBatchTowerCokePerActiveDay(), "coke/active-day",
                "current effective process ticks with carry-preserving 20-tick batching",
                "generatorT1.cokeRecipeProcessTicks", "generatorT1.townBatchGameTicks",
                researchInput));
    }

    private static AuditMetric metric(
            String name,
            double value,
            String unit,
            String formula,
            String... inputs
    ) {
        return new AuditMetric(name, value, unit, formula, List.of(inputs));
    }

    private static String snapshotHash(
            List<SourceFile> sources,
            List<ParameterValue> parameters
    ) {
        StringBuilder canonical = new StringBuilder();
        sources.stream().sorted(Comparator.comparing(SourceFile::id))
                .forEach(source -> canonical.append(source.id()).append('=')
                        .append(source.sha256()).append('\n'));
        parameters.stream().sorted(Comparator.comparing(ParameterValue::name))
                .forEach(parameter -> canonical.append(parameter.name()).append('=')
                        .append(parameter.value()).append('|').append(parameter.unit()).append('\n'));
        return sha256(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.writeString(path, GSON.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public static void printSummary(AuditRun run) {
        System.out.println("Town model audit: stage 0 / T1");
        System.out.println("Snapshot: " + run.snapshot().snapshotHash());
        for (AuditMetric metric : run.report().metrics()) {
            System.out.printf(Locale.ROOT, "  %-44s %14.7f  %s%n",
                    metric.name(), metric.value(), metric.unit());
        }
        System.out.println("Reports: " + run.outputDirectory());
        System.out.println("Note: currentTownBatch metrics use the same carry-preserving fuel balance "
                + "as GeneratorData and therefore match the ideal long-run recipe-duration rates.");
    }

    private record IdentifiedPath(String id, Path path) {
    }

    private record InputPaths(
            Path townModelParameters,
            Path townMathFunctions,
            Path houseDailyModel,
            Path residentDailyModel,
            Path townFoodResourceAmount,
            Path generatorFuelModel,
            Path generatorHeatFieldModel,
            Path generatorData,
            Path climateCommonEvents,
            Path fhConfig,
            Path generatorCoal,
            Path generatorCoke,
            Path huntingLoot,
            Path biomeMineScript,
            Path generatorEfficiencyOne,
            Path generatorEfficiencyTwo
    ) {
        static InputPaths resolve(Path projectRoot, Path packRoot) {
            return new InputPaths(
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/town/model/TownModelParameters.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/town/TownMathFunctions.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseDailyModel.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentDailyModel.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodResourceAmount.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorFuelModel.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorHeatFieldModel.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorData.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/content/climate/event/ClimateCommonEvents.java"),
                    projectRoot.resolve("src/main/java/com/teammoeg/frostedheart/infrastructure/config/FHConfig.java"),
                    projectRoot.resolve("src/main/resources/data/frostedheart/recipes/generator/coal.json"),
                    projectRoot.resolve("src/main/resources/data/frostedheart/recipes/generator/coal_coke.json"),
                    projectRoot.resolve("src/main/resources/data/frostedheart/loot_tables/town/hunting.json"),
                    packRoot.resolve("kubejs/server_scripts/src/recipes_types/frostedheart/biome_mine.js"),
                    packRoot.resolve("config/fhresearches/generator_efficiency_1.json"),
                    packRoot.resolve("config/fhresearches/generator_efficiency_2.json"));
        }

        void requireAll() throws IOException {
            for (Path path : List.of(
                    townModelParameters, townMathFunctions, houseDailyModel, residentDailyModel,
                    townFoodResourceAmount,
                    generatorFuelModel, generatorHeatFieldModel, generatorData,
                    climateCommonEvents, fhConfig, generatorCoal,
                    generatorCoke, huntingLoot, biomeMineScript,
                    generatorEfficiencyOne, generatorEfficiencyTwo)) {
                if (!Files.isRegularFile(path)) {
                    throw new IOException("Required stage-0 audit input is missing: " + path);
                }
            }
        }
    }

    public record SourceFile(String id, String path, String sha256) {
    }

    public record ParameterValue(
            String name,
            Object value,
            String unit,
            String sourceType,
            String sourcePath,
            String sourceSymbol
    ) {
    }

    public record SourceSnapshot(
            int schemaVersion,
            String scope,
            String generatedAt,
            String snapshotHash,
            List<SourceFile> sourceFiles,
            List<ParameterValue> parameters
    ) {
    }

    public record AuditMetric(
            String name,
            double value,
            String unit,
            String formula,
            List<String> inputs
    ) {
    }

    public record AuditIssue(
            String severity,
            String code,
            String description,
            Map<String, Double> measurements
    ) {
    }

    public record AuditReport(
            int schemaVersion,
            String scope,
            String generatedAt,
            String sourceSnapshotHash,
            List<AuditMetric> metrics,
            List<AuditIssue> issues,
            List<String> explicitlyExcluded
    ) {
    }

    public record AuditRun(
            Path outputDirectory,
            Path sourceSnapshotPath,
            Path auditReportPath,
            SourceSnapshot snapshot,
            AuditReport report,
            TownStageZeroModel.StageZeroMetrics metrics
    ) {
    }
}
