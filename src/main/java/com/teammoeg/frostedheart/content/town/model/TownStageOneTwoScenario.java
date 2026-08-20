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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Inputs for independent one-day stage-1/2 kernel experiments. */
public record TownStageOneTwoScenario(
        int schemaVersion,
        Metadata metadata,
        Simulation simulation,
        Workers workers,
        Hunting hunting,
        Processing processing,
        Tower tower,
        House house,
        Diagnostics diagnostics
) {
    public static TownStageOneTwoScenario load(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        int schemaVersion = integer(root, "schemaVersion", 1);
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported stage-1/2 scenario schema: " + schemaVersion);
        }

        JsonObject metadataJson = object(root, "metadata");
        Metadata metadata = new Metadata(
                string(metadataJson, "name", path.getFileName().toString()),
                string(metadataJson, "description", ""));

        JsonObject simulationJson = object(root, "simulation");
        Simulation simulation = new Simulation(
                positiveInteger(simulationJson, "runs", 10_000),
                longValue(simulationJson, "seed", 1L));

        JsonObject workersJson = object(root, "workers");
        Workers workers = new Workers(
                workerList(workersJson, "mining"),
                workerList(workersJson, "hunting"));

        JsonObject huntingJson = object(root, "hunting");
        Hunting hunting = new Hunting(
                carry(number(huntingJson, "initialLootRollCarry", 0.0)),
                finiteNonNegative(number(huntingJson, "availableHuntUnits", 1_000_000.0),
                        "hunting.availableHuntUnits"));

        JsonObject processingJson = object(root, "processing");
        Processing processing = new Processing(
                nullableCapacity(processingJson, "coalToCokeCapacityPerDay"),
                nullableCapacity(processingJson, "rawMeatProcessingCapacityPerDay"));

        JsonObject towerJson = object(root, "tower");
        String fuel = string(towerJson, "fuel", "coke");
        if (!"coal".equals(fuel) && !"coke".equals(fuel)) {
            throw new IllegalArgumentException("tower.fuel must be 'coal' or 'coke'.");
        }
        Tower tower = new Tower(
                fuel,
                bounded(number(towerJson, "activeFraction", 1.0), 0.0, 1.0,
                        "tower.activeFraction"),
                booleanValue(towerJson, "overdrive", false),
                finiteNonNegative(number(towerJson, "researchEfficiencyBonus", 0.0),
                        "tower.researchEfficiencyBonus"));

        JsonObject houseJson = object(root, "house");
        House house = new House(
                nonNegativeInteger(houseJson, "residentCount", 1),
                bounded(number(houseJson, "residentHealth", 50.0), 0.0, 100.0,
                        "house.residentHealth"),
                bounded(number(houseJson, "residentMental", 50.0), 0.0, 100.0,
                        "house.residentMental"),
                finite(number(houseJson, "temperatureCelsius", 24.0),
                        "house.temperatureCelsius"),
                positiveInteger(houseJson, "areaBlocks", 16),
                positiveInteger(houseJson, "volumeBlocks", 48),
                nonNegativeInteger(houseJson, "bedCount", 4),
                bounded(number(houseJson, "decorationRating", 0.75), 0.0, 1.0,
                        "house.decorationRating"),
                foodInventory(houseJson));

        JsonObject diagnosticsJson = object(root, "diagnostics");
        Diagnostics diagnostics = new Diagnostics(
                doubleList(diagnosticsJson, "miningBaseOutputPerSweDay",
                        List.of(2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)),
                doubleList(diagnosticsJson, "rawMeatProcessingCapacityPerDay",
                        List.of(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0)),
                finiteDoubleList(diagnosticsJson, "houseTemperatureCelsius",
                        List.of(-20.0, -15.0, -10.0, -5.0, 0.0, 5.0, 10.0, 14.0,
                                18.0, 22.0, 24.0, 28.0, 34.0, 40.0, 45.0, 50.0, 55.0, 60.0)),
                doubleList(diagnosticsJson, "foodSatisfaction",
                        List.of(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)));

        return new TownStageOneTwoScenario(
                schemaVersion, metadata, simulation, workers, hunting,
                processing, tower, house, diagnostics);
    }

    private static List<Worker> workerList(JsonObject root, String name) {
        JsonArray values = root.has(name) ? root.getAsJsonArray(name) : new JsonArray();
        List<Worker> workers = new ArrayList<>();
        int index = 0;
        for (JsonElement value : values) {
            JsonObject worker = value.getAsJsonObject();
            workers.add(new Worker(
                    string(worker, "name", name + "-" + index),
                    bounded(number(worker, "health", 50.0), 0.0, 100.0, name + ".health"),
                    bounded(number(worker, "mental", 50.0), 0.0, 100.0, name + ".mental"),
                    bounded(number(worker, "strength", 50.0), 0.0, 100.0, name + ".strength"),
                    bounded(number(worker, "intelligence", 50.0), 0.0, 100.0, name + ".intelligence"),
                    finiteNonNegative(number(worker, "proficiency", 0.0), name + ".proficiency")));
            index++;
        }
        return List.copyOf(workers);
    }

    private static List<InventoryItem> foodInventory(JsonObject house) {
        JsonArray values = house.has("foodInventory")
                ? house.getAsJsonArray("foodInventory") : new JsonArray();
        List<InventoryItem> result = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject item = value.getAsJsonObject();
            result.add(new InventoryItem(
                    requiredString(item, "item"),
                    finiteNonNegative(number(item, "amountItems", 0.0),
                            "house.foodInventory.amountItems")));
        }
        return List.copyOf(result);
    }

    private static List<Double> doubleList(JsonObject root, String name, List<Double> fallback) {
        if (!root.has(name)) return fallback;
        List<Double> result = new ArrayList<>();
        for (JsonElement value : root.getAsJsonArray(name)) {
            result.add(finiteNonNegative(value.getAsDouble(), "diagnostics." + name));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("diagnostics." + name + " cannot be empty.");
        return List.copyOf(result);
    }

    private static List<Double> finiteDoubleList(
            JsonObject root,
            String name,
            List<Double> fallback
    ) {
        if (!root.has(name)) return fallback;
        List<Double> result = new ArrayList<>();
        for (JsonElement value : root.getAsJsonArray(name)) {
            result.add(finite(value.getAsDouble(), "diagnostics." + name));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("diagnostics." + name + " cannot be empty.");
        return List.copyOf(result);
    }

    private static double nullableCapacity(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) return Double.POSITIVE_INFINITY;
        return finiteNonNegative(root.get(name).getAsDouble(), "processing." + name);
    }

    private static JsonObject object(JsonObject root, String name) {
        return root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
    }

    private static String requiredString(JsonObject root, String name) {
        String value = string(root, name, "");
        if (value.isBlank()) throw new IllegalArgumentException(name + " is required.");
        return value;
    }

    private static String string(JsonObject root, String name, String fallback) {
        return root.has(name) ? root.get(name).getAsString() : fallback;
    }

    private static int integer(JsonObject root, String name, int fallback) {
        return root.has(name) ? root.get(name).getAsInt() : fallback;
    }

    private static int positiveInteger(JsonObject root, String name, int fallback) {
        int value = integer(root, name, fallback);
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive.");
        return value;
    }

    private static int nonNegativeInteger(JsonObject root, String name, int fallback) {
        int value = integer(root, name, fallback);
        if (value < 0) throw new IllegalArgumentException(name + " must be non-negative.");
        return value;
    }

    private static long longValue(JsonObject root, String name, long fallback) {
        return root.has(name) ? root.get(name).getAsLong() : fallback;
    }

    private static boolean booleanValue(JsonObject root, String name, boolean fallback) {
        return root.has(name) ? root.get(name).getAsBoolean() : fallback;
    }

    private static double number(JsonObject root, String name, double fallback) {
        return root.has(name) ? root.get(name).getAsDouble() : fallback;
    }

    private static double finite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite.");
        return value;
    }

    private static double finiteNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
        return value;
    }

    private static double carry(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value >= 1.0) {
            throw new IllegalArgumentException("hunting.initialLootRollCarry must be in [0, 1).");
        }
        return value;
    }

    private static double bounded(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "].");
        }
        return value;
    }

    public record Metadata(String name, String description) {
    }

    public record Simulation(int runs, long seed) {
    }

    public record Workers(List<Worker> mining, List<Worker> hunting) {
        public Workers {
            mining = List.copyOf(mining);
            hunting = List.copyOf(hunting);
        }
    }

    public record Worker(
            String name,
            double health,
            double mental,
            double strength,
            double intelligence,
            double proficiency
    ) {
    }

    public record Hunting(double initialLootRollCarry, double availableHuntUnits) {
    }

    public record Processing(
            double coalToCokeCapacityPerDay,
            double rawMeatProcessingCapacityPerDay
    ) {
    }

    public record Tower(
            String fuel,
            double activeFraction,
            boolean overdrive,
            double researchEfficiencyBonus
    ) {
    }

    public record House(
            int residentCount,
            double residentHealth,
            double residentMental,
            double temperatureCelsius,
            int areaBlocks,
            int volumeBlocks,
            int bedCount,
            double decorationRating,
            List<InventoryItem> foodInventory
    ) {
        public House {
            foodInventory = List.copyOf(foodInventory);
        }
    }

    public record InventoryItem(String item, double amountItems) {
    }

    public record Diagnostics(
            List<Double> miningBaseOutputPerSweDay,
            List<Double> rawMeatProcessingCapacityPerDay,
            List<Double> houseTemperatureCelsius,
            List<Double> foodSatisfaction
    ) {
        public Diagnostics {
            miningBaseOutputPerSweDay = List.copyOf(miningBaseOutputPerSweDay);
            rawMeatProcessingCapacityPerDay = List.copyOf(rawMeatProcessingCapacityPerDay);
            houseTemperatureCelsius = List.copyOf(houseTemperatureCelsius);
            foodSatisfaction = List.copyOf(foodSatisfaction);
        }
    }
}
