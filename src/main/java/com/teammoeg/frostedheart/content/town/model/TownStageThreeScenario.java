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
import java.util.LinkedHashMap;
import java.util.Map;
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
        Staffing staffing,
        List<String> buildingOrder,
        Warehouse warehouse,
        Processing processing,
        Tower tower,
        Terrain terrain,
        Diagnostics diagnostics
) {
    public static final List<String> REQUIRED_BUILDINGS = List.of("house", "mine", "hunt");
    public static final List<String> WORK_BUILDINGS = List.of("mine", "hunt");

    public TownStageThreeScenario {
        buildingOrder = List.copyOf(buildingOrder);
        staffing = staffing == null ? Staffing.automatic() : staffing;
    }

    public static boolean isStageThree(Path path) throws IOException {
        return modelStage(path) == 3;
    }

    public static int modelStage(Path path) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        return integer(root, "modelStage", 1);
    }

    public static TownStageThreeScenario load(Path path) throws IOException {
        return loadForStage(path, 3);
    }

    static TownStageThreeScenario loadForStage(Path path, int requiredStage) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        int schemaVersion = integer(root, "schemaVersion", 1);
        int modelStage = integer(root, "modelStage", 0);
        if (schemaVersion != 1 || modelStage != requiredStage) {
            throw new IllegalArgumentException(
                    "Stage-" + requiredStage
                            + " scenarios require schemaVersion=1 and modelStage="
                            + requiredStage + ".");
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
                populationJson.has("initialResidents")
                        ? positiveInteger(populationJson, "initialResidents", 24)
                        : positiveInteger(populationJson, "standardAdults", 24),
                PopulationInitialization.parse(string(
                        populationJson, "initialization", "fixed")),
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
                positiveInteger(houseJson, "areaBlocks", population.initialResidents() * 16),
                positiveInteger(houseJson, "volumeBlocks", population.initialResidents() * 48),
                positiveInteger(houseJson, "bedCount", population.initialResidents()),
                bounded(number(houseJson, "decorationRating", 0.75), 0.0, 1.0,
                        "house.decorationRating"));

        JsonObject workplacesJson = object(root, "workplaces");
        Workplaces workplaces = new Workplaces(
                positiveInteger(workplacesJson, "mineCapacity", population.initialResidents()),
                positiveInteger(workplacesJson, "huntCapacity", population.initialResidents()),
                bounded(number(workplacesJson, "huntRating", 1.0), 0.0, 1.0,
                        "workplaces.huntRating"));

        JsonObject staffingJson = object(root, "staffing");
        List<String> staffingQueue = stringList(
                staffingJson, "queue", WORK_BUILDINGS);
        validateStaffingQueue(staffingQueue);
        JsonObject targetJson = object(staffingJson, "targets");
        Map<String, Integer> staffingTargets = new LinkedHashMap<>();
        for (String building : WORK_BUILDINGS) {
            staffingTargets.put(building,
                    nonNegativeInteger(targetJson, building, 0));
        }
        Staffing staffing = new Staffing(staffingQueue, staffingTargets);

        List<String> buildingOrder = stringList(root, "buildingOrder", REQUIRED_BUILDINGS);
        validateBuildingOrder(buildingOrder);

        JsonObject warehouseJson = object(root, "warehouse");
        Warehouse warehouse = new Warehouse(
                positive(number(warehouseJson, "capacityItems", 100_000.0),
                        "warehouse.capacityItems"),
                inventory(warehouseJson),
                simulationFoods(warehouseJson),
                inventory(warehouseJson, "dailySupplies",
                        "warehouse.dailySupplies.amountItems"));

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
                house, workplaces, staffing, buildingOrder, warehouse, processing, tower,
                terrain, diagnostics);
    }

    public TownStageThreeScenario withBuildingOrder(List<String> order) {
        validateBuildingOrder(order);
        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population, house,
                workplaces, staffing, order, warehouse, processing, tower, terrain, diagnostics);
    }

    public TownStageThreeScenario withWorkplaces(Workplaces value) {
        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population, house,
                value, staffing, buildingOrder, warehouse, processing, tower, terrain, diagnostics);
    }

    public TownStageThreeScenario withTower(Tower value) {
        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population, house,
                workplaces, staffing, buildingOrder, warehouse, processing, value, terrain, diagnostics);
    }

    public TownStageThreeScenario withStaffing(Staffing value) {
        return new TownStageThreeScenario(
                schemaVersion, modelStage, metadata, simulation, population, house,
                workplaces, value, buildingOrder, warehouse, processing, tower,
                terrain, diagnostics);
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

    private static void validateStaffingQueue(List<String> order) {
        Set<String> values = new HashSet<>(order);
        if (order.size() != WORK_BUILDINGS.size()
                || values.size() != WORK_BUILDINGS.size()
                || !values.containsAll(WORK_BUILDINGS)) {
            throw new IllegalArgumentException(
                    "staffing.queue must contain mine and hunt exactly once.");
        }
    }

    private static List<InventoryItem> inventory(JsonObject warehouse) {
        return inventory(warehouse, "initialInventory",
                "warehouse.initialInventory.amountItems");
    }

    private static List<InventoryItem> inventory(
            JsonObject warehouse,
            String member,
            String amountName
    ) {
        JsonArray values = warehouse.has(member)
                ? warehouse.getAsJsonArray(member) : new JsonArray();
        List<InventoryItem> result = new ArrayList<>();
        for (JsonElement value : values) {
            JsonObject item = value.getAsJsonObject();
            result.add(new InventoryItem(
                    requiredString(item, "item"),
                    finiteNonNegative(number(item, "amountItems", 0.0),
                            amountName)));
        }
        return List.copyOf(result);
    }

    private static List<SimulationFood> simulationFoods(JsonObject warehouse) {
        JsonArray values = warehouse.has("simulationFoods")
                ? warehouse.getAsJsonArray("simulationFoods") : new JsonArray();
        List<SimulationFood> result = new ArrayList<>();
        Set<String> items = new HashSet<>();
        for (JsonElement value : values) {
            JsonObject food = value.getAsJsonObject();
            String item = requiredString(food, "item");
            if (!items.add(item)) {
                throw new IllegalArgumentException("Duplicate warehouse.simulationFoods item: " + item);
            }
            result.add(new SimulationFood(
                    item,
                    positiveInteger(food, "hunger", 0),
                    finiteNonNegative(number(food, "saturationModifier", 0.0),
                            "warehouse.simulationFoods.saturationModifier")));
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
            int initialResidents,
            PopulationInitialization initialization,
            double initialHealth,
            double initialMental,
            double initialStrength,
            double initialIntelligence,
            double initialMiningProficiency,
            double initialHuntingProficiency,
            int initialAgeDays
    ) {
    }

    public enum PopulationInitialization {
        FIXED,
        GAME_GENERATED;

        private static PopulationInitialization parse(String value) {
            return switch (value) {
                case "fixed" -> FIXED;
                case "gameGenerated" -> GAME_GENERATED;
                default -> throw new IllegalArgumentException(
                        "population.initialization must be 'fixed' or 'gameGenerated'.");
            };
        }
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

    public record Staffing(List<String> queue, Map<String, Integer> targets) {
        public Staffing {
            queue = List.copyOf(queue);
            validateStaffingQueue(queue);
            Map<String, Integer> normalized = new LinkedHashMap<>();
            for (String building : WORK_BUILDINGS) {
                normalized.put(building, Math.max(0, targets.getOrDefault(building, 0)));
            }
            targets = Map.copyOf(normalized);
        }

        public static Staffing automatic() {
            return new Staffing(WORK_BUILDINGS, Map.of());
        }

        public int target(String building) {
            return targets.getOrDefault(building, 0);
        }
    }

    public record Warehouse(
            double capacityItems,
            List<InventoryItem> initialInventory,
            List<SimulationFood> simulationFoods,
            List<InventoryItem> dailySupplies
    ) {
        public Warehouse {
            initialInventory = List.copyOf(initialInventory);
            simulationFoods = List.copyOf(simulationFoods);
            dailySupplies = List.copyOf(dailySupplies);
            double total = initialInventory.stream().mapToDouble(InventoryItem::amountItems).sum();
            if (total > capacityItems + 1.0 / 8192.0) {
                throw new IllegalArgumentException("Initial inventory exceeds warehouse capacity.");
            }
        }

        public Warehouse(double capacityItems, List<InventoryItem> initialInventory) {
            this(capacityItems, initialInventory, List.of(), List.of());
        }

        public Warehouse(
                double capacityItems,
                List<InventoryItem> initialInventory,
                List<SimulationFood> simulationFoods
        ) {
            this(capacityItems, initialInventory, simulationFoods, List.of());
        }
    }

    public record InventoryItem(String item, double amountItems) {
    }

    public record SimulationFood(String item, int hunger, double saturationModifier) {
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

        public Tower withOverdrive(boolean value) {
            return new Tower(
                    fuel, fuelItem, activeFraction, value, researchEfficiencyBonus);
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
