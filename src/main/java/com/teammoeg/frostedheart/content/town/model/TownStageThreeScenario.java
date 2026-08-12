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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Explicit inputs for the stage-3 constant-temperature multi-day loop. */
public record TownStageThreeScenario(
        int schemaVersion,
        int modelStage,
        Metadata metadata,
        Simulation simulation,
        Population population,
        House house,
        Workplaces workplaces,
        List<String> buildingOrder,
        Warehouse warehouse,
        Processing processing,
        Tower tower,
        Terrain terrain,
        Diagnostics diagnostics
) {
    public static final List<String> REQUIRED_BUILDINGS = List.of("house", "mine", "hunt");

    public TownStageThreeScenario {
        buildingOrder = List.copyOf(buildingOrder);
    }

    public static boolean isStageThree(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return integer(root, "modelStage", 1) == 3;
    }

    public static TownStageThreeScenario load(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        int schemaVersion = integer(root, "schemaVersion", 1);
        int modelStage = integer(root, "modelStage", 0);
        if (schemaVersion != 1 || modelStage != 3) {
            throw new IllegalArgumentException(
                    "Stage-3 scenarios require schemaVersion=1 and modelStage=3.");
        }

        JsonObject metadataJson = object(root, "metadata");
        Metadata metadata = new Metadata(
                string(metadataJson, "name", path.getFileName().toString()),
                string(metadataJson, "description", ""));

        JsonObject simulationJson = object(root, "simulation");
        Simulation simulation = new Simulation(
                positiveInteger(simulationJson, "days", 120),
                positiveInteger(simulationJson, "runs", 1_000),
                longValue(simulationJson, "seed", 1L));

        JsonObject populationJson = object(root, "population");
        Population population = new Population(
                positiveInteger(populationJson, "standardAdults", 24),
                bounded(number(populationJson, "initialHealth", 50.0), 0.0, 100.0,
                        "population.initialHealth"),
                bounded(number(populationJson, "initialMental", 50.0), 0.0, 100.0,
                        "population.initialMental"),
                bounded(number(populationJson, "initialStrength", 50.0), 0.0, 100.0,
                        "population.initialStrength"),
                bounded(number(populationJson, "initialIntelligence", 50.0), 0.0, 100.0,
                        "population.initialIntelligence"),
                bounded(number(populationJson, "initialMiningProficiency", 0.0), 0.0, 100.0,
                        "population.initialMiningProficiency"),
                bounded(number(populationJson, "initialHuntingProficiency", 0.0), 0.0, 100.0,
                        "population.initialHuntingProficiency"),
                nonNegativeInteger(populationJson, "initialAgeDays", 30));

        JsonObject houseJson = object(root, "house");
        House house = new House(
                finite(number(houseJson, "temperatureCelsius", 24.0),
                        "house.temperatureCelsius"),
                positiveInteger(houseJson, "areaBlocks", population.standardAdults() * 16),
                positiveInteger(houseJson, "volumeBlocks", population.standardAdults() * 48),
                positiveInteger(houseJson, "bedCount", population.standardAdults()),
                bounded(number(houseJson, "decorationRating", 0.75), 0.0, 1.0,
                        "house.decorationRating"));

        JsonObject workplacesJson = object(root, "workplaces");
        Workplaces workplaces = new Workplaces(
                positiveInteger(workplacesJson, "mineCapacity", population.standardAdults()),
                positiveInteger(workplacesJson, "huntCapacity", population.standardAdults()),
                bounded(number(workplacesJson, "huntRating", 1.0), 0.0, 1.0,
                        "workplaces.huntRating"));

        List<String> buildingOrder = stringList(root, "buildingOrder", REQUIRED_BUILDINGS);
        validateBuildingOrder(buildingOrder);

        JsonObject warehouseJson = object(root, "warehouse");
        Warehouse warehouse = new Warehouse(
                positive(number(warehouseJson, "capacityItems", 100_000.0),
                        "warehouse.capacityItems"),
                inventory(warehouseJson));

        JsonObject processingJson = object(root, "processing");
        Processing processing = new Processing(
                nullableCapacity(processingJson, "coalToCokeItemsPerDay"),
                nullableCapacity(processingJson, "rawMeatItemsPerDay"),
                string(processingJson, "cokeItem", "immersiveengineering:coal_coke"));

        JsonObject towerJson = object(root, "tower");
        String fuel = string(towerJson, "fuel", "coke");
        if (!"coal".equals(fuel) && !"coke".equals(fuel)) {
            throw new IllegalArgumentException("tower.fuel must be 'coal' or 'coke'.");
        }
        Tower tower = new Tower(
                fuel,
                string(towerJson, "fuelItem", "coal".equals(fuel)
                        ? "minecraft:coal" : processing.cokeItem()),
                bounded(number(towerJson, "activeFraction", 1.0), 0.0, 1.0,
                        "tower.activeFraction"),
                booleanValue(towerJson, "overdrive", false),
                finiteNonNegative(number(towerJson, "researchEfficiencyBonus", 0.0),
                        "tower.researchEfficiencyBonus"));

        JsonObject terrainJson = object(root, "terrain");
        double huntMaximum = finiteNonNegative(number(
                terrainJson, "maximumHuntUnits", 1_000_000.0), "terrain.maximumHuntUnits");
        Terrain terrain = new Terrain(
                bounded(number(terrainJson, "initialHuntUnits", huntMaximum), 0.0,
                        huntMaximum, "terrain.initialHuntUnits"),
                huntMaximum,
                finiteNonNegative(number(terrainJson, "huntRecoveryUnitsPerDay", 0.0),
                        "terrain.huntRecoveryUnitsPerDay"));

        JsonObject diagnosticsJson = object(root, "diagnostics");
        Diagnostics diagnostics = new Diagnostics(
                positiveInteger(diagnosticsJson, "frontierMaximumPopulation", 64),
                booleanValue(diagnosticsJson, "compareBuildingOrders", false));

        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population,
                house, workplaces, buildingOrder, warehouse, processing, tower,
                terrain, diagnostics);
    }

    public TownStageThreeScenario withBuildingOrder(List<String> order) {
        validateBuildingOrder(order);
        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population, house,
                workplaces, order, warehouse, processing, tower, terrain, diagnostics);
    }

    private static void validateBuildingOrder(List<String> order) {
        Set<String> values = new HashSet<>(order);
        if (order.size() != REQUIRED_BUILDINGS.size()
                || values.size() != REQUIRED_BUILDINGS.size()
                || !values.containsAll(REQUIRED_BUILDINGS)) {
            throw new IllegalArgumentException(
                    "buildingOrder must contain house, mine, and hunt exactly once.");
        }
    }

    private static List<InventoryItem> inventory(JsonObject warehouse) {
        JsonArray values = warehouse.has("initialInventory")
                ? warehouse.getAsJsonArray("initialInventory") : new JsonArray();
        List<InventoryItem> result = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject item = value.getAsJsonObject();
            result.add(new InventoryItem(
                    requiredString(item, "item"),
                    finiteNonNegative(number(item, "amountItems", 0.0),
                            "warehouse.initialInventory.amountItems")));
        }
        return List.copyOf(result);
    }

    private static List<String> stringList(JsonObject root, String name, List<String> fallback) {
        if (!root.has(name)) return fallback;
        List<String> result = new ArrayList<>();
        for (JsonElement value : root.getAsJsonArray(name)) result.add(value.getAsString());
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

    private static double positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
        return value;
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

    private static double bounded(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in [" + minimum + ", " + maximum + "].");
        }
        return value;
    }

    public record Metadata(String name, String description) {
    }

    public record Simulation(int days, int runs, long seed) {
    }

    public record Population(
            int standardAdults,
            double initialHealth,
            double initialMental,
            double initialStrength,
            double initialIntelligence,
            double initialMiningProficiency,
            double initialHuntingProficiency,
            int initialAgeDays
    ) {
    }

    public record House(
            double temperatureCelsius,
            int areaBlocks,
            int volumeBlocks,
            int bedCount,
            double decorationRating
    ) {
    }

    public record Workplaces(int mineCapacity, int huntCapacity, double huntRating) {
    }

    public record Warehouse(double capacityItems, List<InventoryItem> initialInventory) {
        public Warehouse {
            initialInventory = List.copyOf(initialInventory);
            double total = initialInventory.stream().mapToDouble(InventoryItem::amountItems).sum();
            if (total > capacityItems + 1.0 / 8192.0) {
                throw new IllegalArgumentException("Initial inventory exceeds warehouse capacity.");
            }
        }
    }

    public record InventoryItem(String item, double amountItems) {
    }

    public record Processing(
            double coalToCokeItemsPerDay,
            double rawMeatItemsPerDay,
            String cokeItem
    ) {
        public Processing {
            if (cokeItem == null || cokeItem.isBlank()) {
                throw new IllegalArgumentException("processing.cokeItem is required.");
            }
        }
    }

    public record Tower(
            String fuel,
            String fuelItem,
            double activeFraction,
            boolean overdrive,
            double researchEfficiencyBonus
    ) {
        public Tower {
            if (fuelItem == null || fuelItem.isBlank()) {
                throw new IllegalArgumentException("tower.fuelItem is required.");
            }
        }
    }

    public record Terrain(
            double initialHuntUnits,
            double maximumHuntUnits,
            double huntRecoveryUnitsPerDay
    ) {
    }

    public record Diagnostics(int frontierMaximumPopulation, boolean compareBuildingOrders) {
    }
}
