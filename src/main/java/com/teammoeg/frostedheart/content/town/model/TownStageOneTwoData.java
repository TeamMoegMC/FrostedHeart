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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodResourceAmount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Current FH/TWR data inputs required by the stage-1/2 simulator. */
public record TownStageOneTwoData(
        List<TownStageZeroModel.WeightedResource> mineWeights,
        List<TownStageZeroModel.WeightedLootEntry> huntingLoot,
        int coalRecipeProcessTicks,
        int cokeRecipeProcessTicks,
        List<TownFoodProcessingModel.MeatDefinition> meats,
        Map<String, FoodDefinition> foods,
        Map<String, String> sourceFiles
) {
    public TownStageOneTwoData {
        mineWeights = List.copyOf(mineWeights);
        huntingLoot = List.copyOf(huntingLoot);
        meats = List.copyOf(meats);
        foods = Map.copyOf(foods);
        sourceFiles = Map.copyOf(sourceFiles);
    }

    public static TownStageOneTwoData load(Path projectRoot, Path packRoot) throws IOException {
        Path normalizedProject = projectRoot.toAbsolutePath().normalize();
        Path normalizedPack = packRoot.toAbsolutePath().normalize();
        Path coalRecipe = normalizedProject.resolve(
                "src/main/resources/data/frostedheart/recipes/generator/coal.json");
        Path cokeRecipe = normalizedProject.resolve(
                "src/main/resources/data/frostedheart/recipes/generator/coal_coke.json");
        Path huntingLootPath = normalizedProject.resolve(
                "src/main/resources/data/frostedheart/loot_tables/town/hunting.json");
        Path mineScript = normalizedPack.resolve(
                "kubejs/server_scripts/src/recipes_types/frostedheart/biome_mine.js");
        requireFile(coalRecipe);
        requireFile(cokeRecipe);
        requireFile(huntingLootPath);
        requireFile(mineScript);

        List<TownStageZeroModel.WeightedResource> mineWeights =
                TownStageZeroAudit.parseBiomeMineWeights(
                        Files.readString(mineScript), "the_winter_rescue:fossil_deposits");
        List<TownStageZeroModel.WeightedLootEntry> huntingLoot =
                TownStageZeroAudit.parseHuntingLoot(huntingLootPath);
        for (TownStageZeroModel.WeightedLootEntry entry : huntingLoot) {
            requireIntegral(entry.minimumCount(), "loot minimum count for " + entry.item());
            requireIntegral(entry.maximumCount(), "loot maximum count for " + entry.item());
        }

        TownModelParameters parameters = TownModelParameters.currentDefaults();
        Map<String, Integer> foodLevels = readFoodLevels(normalizedProject);
        Map<String, FoodDefinition> foods = new LinkedHashMap<>();
        List<TownFoodProcessingModel.MeatDefinition> meats = new ArrayList<>();
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("fh.generator.coal", coalRecipe.toString());
        sources.put("fh.generator.coke", cokeRecipe.toString());
        sources.put("fh.hunting.loot", huntingLootPath.toString());
        sources.put("twr.mine.fossil-deposits", mineScript.toString());
        sources.put("fh.model.mining-daily", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/buildings/mine/MiningDailyModel.java").toString());
        sources.put("fh.model.hunting-daily", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/buildings/hunting/HuntingDailyModel.java").toString());
        sources.put("fh.model.food-inventory", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodInventoryModel.java").toString());
        sources.put("fh.model.food-processing", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodProcessingModel.java").toString());
        sources.put("fh.model.stage12-theory", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/model/TownStageOneTwoTheory.java").toString());
        sources.put("fh.model.stage12-simulator", normalizedProject.resolve(
                "src/main/java/com/teammoeg/frostedheart/content/town/model/TownStageOneTwoSimulator.java").toString());

        for (int level = 0; level <= 4; level++) {
            Path tag = foodLevelPath(normalizedProject, level);
            sources.put("fh.food.level." + level, tag.toString());
        }
        for (Map.Entry<String, String> source : sources.entrySet()) {
            if (source.getKey().startsWith("fh.model.")) {
                requireFile(Path.of(source.getValue()));
            }
        }

        for (TownModelParameters.MeatFoodParameters meat : parameters.meatFoods()) {
            Path rawNutritionPath = nutritionPath(normalizedProject, meat.rawItem());
            Path cookedNutritionPath = nutritionPath(normalizedProject, meat.cookedItem());
            requireFile(rawNutritionPath);
            requireFile(cookedNutritionPath);
            double rawNutrition = readNutritionPerItem(rawNutritionPath, meat.rawItem());
            double cookedNutrition = readNutritionPerItem(cookedNutritionPath, meat.cookedItem());
            double rawFood = TownFoodResourceAmount.fromFoodProperties(
                    meat.rawHunger(), meat.rawSaturationModifier());
            double cookedFood = TownFoodResourceAmount.fromFoodProperties(
                    meat.cookedHunger(), meat.cookedSaturationModifier());
            int rawLevel = requireFoodLevel(foodLevels, meat.rawItem());
            int cookedLevel = requireFoodLevel(foodLevels, meat.cookedItem());
            FoodDefinition raw = new FoodDefinition(
                    meat.rawItem(), rawLevel, rawFood, rawNutrition);
            FoodDefinition cooked = new FoodDefinition(
                    meat.cookedItem(), cookedLevel, cookedFood, cookedNutrition);
            foods.put(raw.item(), raw);
            foods.put(cooked.item(), cooked);
            meats.add(new TownFoodProcessingModel.MeatDefinition(
                    raw.item(), cooked.item(),
                    raw.foodUnitsPerItem(), cooked.foodUnitsPerItem(),
                    raw.nutritionPerItem(), cooked.nutritionPerItem(),
                    raw.foodLevel(), cooked.foodLevel()));
            sources.put("fh.nutrition." + meat.rawItem(), rawNutritionPath.toString());
            sources.put("fh.nutrition." + meat.cookedItem(), cookedNutritionPath.toString());
        }

        return new TownStageOneTwoData(
                mineWeights,
                huntingLoot,
                TownStageZeroAudit.parseGeneratorRecipeProcessTicks(coalRecipe),
                TownStageZeroAudit.parseGeneratorRecipeProcessTicks(cokeRecipe),
                meats,
                foods,
                sources);
    }

    private static Map<String, Integer> readFoodLevels(Path projectRoot) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        for (int level = 0; level <= 4; level++) {
            Path path = foodLevelPath(projectRoot, level);
            requireFile(path);
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (JsonElement value : root.getAsJsonArray("values")) {
                String item = value.getAsString();
                Integer previous = result.put(item, level);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            item + " appears in resident food levels " + previous + " and " + level);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static double readNutritionPerItem(Path path, String expectedItem) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        if (!"frostedheart:diet_override".equals(root.get("type").getAsString())) {
            throw new IllegalArgumentException("Unsupported stage-2 nutrition recipe: " + path);
        }
        if (!expectedItem.equals(root.get("item").getAsString())) {
            throw new IllegalArgumentException("Nutrition recipe item mismatch: " + path);
        }
        JsonObject group = root.getAsJsonObject("group");
        double channelSum = 0.0;
        for (String channel : List.of("fat", "carbohydrate", "protein", "vegetable")) {
            channelSum += group.get(channel).getAsDouble();
        }
        return channelSum / 4.0;
    }

    private static Path nutritionPath(Path projectRoot, String item) {
        String[] parts = item.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid item id: " + item);
        return projectRoot.resolve("src/generated/resources/data/frostedheart/recipes/diet_value")
                .resolve(parts[0]).resolve(parts[1] + ".json");
    }

    private static Path foodLevelPath(Path projectRoot, int level) {
        return projectRoot.resolve("src/main/resources/data/frostedheart/tags/items")
                .resolve("town_resource_resident_food_level_" + level + ".json");
    }

    private static int requireFoodLevel(Map<String, Integer> levels, String item) {
        Integer level = levels.get(item);
        if (level == null) throw new IllegalArgumentException("Missing resident food level for " + item);
        return level;
    }

    private static void requireIntegral(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value != Math.rint(value)) {
            throw new IllegalArgumentException(name + " must be a non-negative integer.");
        }
    }

    private static void requireFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Required stage-1/2 input is missing: " + path);
    }

    public record FoodDefinition(
            String item,
            int foodLevel,
            double foodUnitsPerItem,
            double nutritionPerItem
    ) {
    }
}
