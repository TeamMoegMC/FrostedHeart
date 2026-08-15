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

/** Stage-4 additions: random climate, one T1 sphere, and explicit interior voxels. */
public record TownStageFourScenario(
        TownStageThreeScenario town,
        int climateBurnInDays,
        int morningHour,
        Location location,
        Position towerCenter,
        List<ThermalBuilding> thermalBuildings,
        PopulationSweep populationSweep,
        TensionExperiment tensionExperiment
) {
    public TownStageFourScenario {
        thermalBuildings = List.copyOf(thermalBuildings);
        validate(town, location, thermalBuildings);
    }

    public static TownStageFourScenario load(Path path) throws IOException {
        TownStageThreeScenario town = TownStageThreeScenario.loadForStage(path, 4);
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        JsonObject simulation = object(root, "simulation");
        int burnInDays = positiveInteger(simulation, "climateBurnInDays", 365);
        int morningHour = boundedInteger(simulation, "morningHour", 1, 0, 23);

        JsonObject locationJson = object(root, "location");
        Location location = new Location(
                finite(number(locationJson, "dimensionTemperatureCelsius", -10.0),
                        "location.dimensionTemperatureCelsius"),
                finite(number(locationJson, "biomeTemperatureCelsius", 0.0),
                        "location.biomeTemperatureCelsius"),
                booleanValue(locationJson, "ignoreAltitudeTemperature", true));

        JsonObject layout = object(root, "thermalLayout");
        Position towerCenter = position(layout.get("towerCenter"), "thermalLayout.towerCenter");
        JsonArray buildingsJson = layout.has("buildings")
                ? layout.getAsJsonArray("buildings") : new JsonArray();
        List<ThermalBuilding> buildings = new ArrayList<>();
        for (JsonElement value : buildingsJson) {
            JsonObject building = value.getAsJsonObject();
            String id = requiredString(building, "id");
            String role = requiredString(building, "role");
            int floorArea = positiveInteger(building, "floorAreaBlocks", 0);
            JsonArray boxesJson = building.has("boxes")
                    ? building.getAsJsonArray("boxes") : new JsonArray();
            List<VoxelBox> boxes = new ArrayList<>();
            for (JsonElement boxValue : boxesJson) {
                JsonObject box = boxValue.getAsJsonObject();
                Position minimum = position(box.get("min"), id + ".boxes.min");
                Position size = position(box.get("size"), id + ".boxes.size");
                boxes.add(new VoxelBox(minimum.x(), minimum.y(), minimum.z(),
                        positive(size.x(), id + ".boxes.size[0]"),
                        positive(size.y(), id + ".boxes.size[1]"),
                        positive(size.z(), id + ".boxes.size[2]")));
            }
            buildings.add(new ThermalBuilding(id, role, floorArea, boxes));
        }

        PopulationSweep populationSweep = null;
        if (root.has("populationSweep")) {
            JsonObject sweep = root.getAsJsonObject("populationSweep");
            int minimumPopulation = positiveInteger(sweep, "minimumPopulation", 1);
            int maximumPopulation = positiveInteger(sweep, "maximumPopulation", 200);
            int populationPoints = positiveInteger(sweep, "populationPoints", 20);
            JsonArray populationValuesJson = sweep.has("populationValues")
                    ? sweep.getAsJsonArray("populationValues") : new JsonArray();
            List<Integer> populationValues = new ArrayList<>();
            for (JsonElement value : populationValuesJson) populationValues.add(value.getAsInt());
            JsonArray trajectoryJson = sweep.has("trajectoryPopulations")
                    ? sweep.getAsJsonArray("trajectoryPopulations") : new JsonArray();
            List<Integer> trajectoryPopulations = new ArrayList<>();
            for (JsonElement value : trajectoryJson) trajectoryPopulations.add(value.getAsInt());
            int timelinePopulation = positiveInteger(sweep, "timelinePopulation", 24);
            populationSweep = new PopulationSweep(
                    minimumPopulation, maximumPopulation, populationPoints,
                    populationValues,
                    trajectoryPopulations.isEmpty()
                            ? List.of(1, 8, 11, 12, 13, 14, 16, 24, 48, 200)
                            : trajectoryPopulations,
                    timelinePopulation);
        }
        TensionExperiment tensionExperiment = null;
        if (root.has("tensionExperiment")) {
            JsonObject tension = root.getAsJsonObject("tensionExperiment");
            tensionExperiment = new TensionExperiment(
                    nonNegativeInteger(tension, "townBurnInDays", 120),
                    positive(number(tension, "foodReserveCapDays", 14.0),
                            "tensionExperiment.foodReserveCapDays"),
                    positive(number(tension, "fuelReserveCapNormalDays", 21.0),
                            "tensionExperiment.fuelReserveCapNormalDays"),
                    positiveIntegerList(tension, "mineCapacities", List.of(5, 6, 7, 8)),
                    positiveIntegerList(tension, "huntCapacities", List.of(3, 4, 6, 8)),
                    positiveInteger(tension, "detailedMineCapacity", 8),
                    positiveInteger(tension, "detailedHuntCapacity", 4),
                    positiveInteger(tension, "visibleForecastHours", 120),
                    positiveInteger(tension, "overdriveActionWindowHours", 24),
                    positiveInteger(tension, "forecastSampleHours", 3),
                    boundedInteger(tension, "forecastTriggerLevel", -2, -4, -1));
        }
        if (populationSweep != null && tensionExperiment != null) {
            throw new IllegalArgumentException(
                    "populationSweep and tensionExperiment are mutually exclusive.");
        }
        return new TownStageFourScenario(
                town, burnInDays, morningHour, location, towerCenter, buildings,
                populationSweep, tensionExperiment);
    }

    public ThermalBuilding building(String role) {
        return thermalBuildings.stream()
                .filter(value -> role.equals(value.role()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing thermal building role: " + role));
    }

    private static void validate(
            TownStageThreeScenario town,
            Location location,
            List<ThermalBuilding> buildings
    ) {
        if (!location.ignoreAltitudeTemperature()) {
            throw new IllegalArgumentException(
                    "Stage 4 currently requires ignoreAltitudeTemperature=true.");
        }
        Set<String> ids = new HashSet<>();
        Set<String> roles = new HashSet<>();
        List<VoxelBox> allBoxes = new ArrayList<>();
        for (ThermalBuilding building : buildings) {
            if (!ids.add(building.id())) {
                throw new IllegalArgumentException("Duplicate thermal building id: " + building.id());
            }
            if (!("house".equals(building.role()) || "hunt".equals(building.role()))) {
                throw new IllegalArgumentException("Thermal role must be house or hunt: " + building.role());
            }
            if (!roles.add(building.role())) {
                throw new IllegalArgumentException("Stage 4 requires one aggregate building per role: " + building.role());
            }
            if (building.boxes().isEmpty()) {
                throw new IllegalArgumentException("Thermal building has no interior boxes: " + building.id());
            }
            for (VoxelBox box : building.boxes()) {
                if (box.minimumY() <= 63) {
                    throw new IllegalArgumentException(
                            "Stage-4 reference buildings must be entirely above sea level (y > 63).");
                }
                for (VoxelBox previous : allBoxes) {
                    if (box.overlaps(previous)) {
                        throw new IllegalArgumentException(
                                "Thermal interior boxes overlap; one voxel cannot belong to two buildings.");
                    }
                }
                allBoxes.add(box);
            }
        }
        if (!roles.equals(Set.of("house", "hunt"))) {
            throw new IllegalArgumentException("Stage 4 requires exactly one house and one hunt thermal building.");
        }
        ThermalBuilding house = buildings.stream()
                .filter(value -> "house".equals(value.role())).findFirst().orElseThrow();
        if (house.voxelCount() != town.house().volumeBlocks()) {
            throw new IllegalArgumentException(
                    "House thermal voxel count must equal house.volumeBlocks: "
                            + house.voxelCount() + " != " + town.house().volumeBlocks());
        }
        if (house.floorAreaBlocks() != town.house().areaBlocks()) {
            throw new IllegalArgumentException(
                    "House floorAreaBlocks must equal house.areaBlocks.");
        }
    }

    private static JsonObject object(JsonObject root, String name) {
        return root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
    }

    private static Position position(JsonElement value, String name) {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != 3) {
            throw new IllegalArgumentException(name + " must be a three-integer array [x,y,z].");
        }
        JsonArray values = value.getAsJsonArray();
        return new Position(values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
    }

    private static String requiredString(JsonObject root, String name) {
        String value = root.has(name) ? root.get(name).getAsString() : "";
        if (value.isBlank()) throw new IllegalArgumentException(name + " is required.");
        return value;
    }

    private static int positiveInteger(JsonObject root, String name, int fallback) {
        int value = root.has(name) ? root.get(name).getAsInt() : fallback;
        return positive(value, name);
    }

    private static int nonNegativeInteger(JsonObject root, String name, int fallback) {
        int value = root.has(name) ? root.get(name).getAsInt() : fallback;
        if (value < 0) throw new IllegalArgumentException(name + " must be non-negative.");
        return value;
    }

    private static List<Integer> positiveIntegerList(
            JsonObject root,
            String name,
            List<Integer> fallback
    ) {
        if (!root.has(name)) return fallback;
        List<Integer> values = new ArrayList<>();
        Set<Integer> unique = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray(name)) {
            int value = positive(element.getAsInt(), "tensionExperiment." + name);
            if (!unique.add(value)) {
                throw new IllegalArgumentException(
                        "Duplicate tensionExperiment." + name + " value: " + value);
            }
            values.add(value);
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("tensionExperiment." + name + " cannot be empty.");
        }
        return List.copyOf(values);
    }

    private static int boundedInteger(JsonObject root, String name, int fallback, int lower, int upper) {
        int value = root.has(name) ? root.get(name).getAsInt() : fallback;
        if (value < lower || value > upper) {
            throw new IllegalArgumentException(name + " must be in [" + lower + "," + upper + "].");
        }
        return value;
    }

    private static int positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive.");
        return value;
    }

    private static double positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
        return value;
    }

    private static double number(JsonObject root, String name, double fallback) {
        return root.has(name) ? root.get(name).getAsDouble() : fallback;
    }

    private static double finite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite.");
        return value;
    }

    private static boolean booleanValue(JsonObject root, String name, boolean fallback) {
        return root.has(name) ? root.get(name).getAsBoolean() : fallback;
    }

    public record Location(
            double dimensionTemperatureCelsius,
            double biomeTemperatureCelsius,
            boolean ignoreAltitudeTemperature
    ) {
    }

    public record Position(int x, int y, int z) {
    }

    public record VoxelBox(
            int minimumX,
            int minimumY,
            int minimumZ,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        public long voxelCount() {
            return Math.multiplyExact(Math.multiplyExact((long) sizeX, sizeY), sizeZ);
        }

        public boolean overlaps(VoxelBox other) {
            return minimumX < other.minimumX + other.sizeX
                    && other.minimumX < minimumX + sizeX
                    && minimumY < other.minimumY + other.sizeY
                    && other.minimumY < minimumY + sizeY
                    && minimumZ < other.minimumZ + other.sizeZ
                    && other.minimumZ < minimumZ + sizeZ;
        }
    }

    public record ThermalBuilding(
            String id,
            String role,
            int floorAreaBlocks,
            List<VoxelBox> boxes
    ) {
        public ThermalBuilding {
            boxes = List.copyOf(boxes);
        }

        public long voxelCount() {
            return boxes.stream().mapToLong(VoxelBox::voxelCount).sum();
        }
    }

    /** Optional request for a paired-seed population sweep of the same stage-4 scenario. */
    public record PopulationSweep(
            int minimumPopulation,
            int maximumPopulation,
            int populationPoints,
            List<Integer> populationValues,
            List<Integer> trajectoryPopulations,
            int timelinePopulation
    ) {
        public PopulationSweep {
            populationValues = List.copyOf(populationValues);
            trajectoryPopulations = List.copyOf(trajectoryPopulations);
            if (minimumPopulation <= 0 || maximumPopulation < minimumPopulation) {
                throw new IllegalArgumentException(
                        "populationSweep requires 0 < minimumPopulation <= maximumPopulation.");
            }
            int availablePopulations = maximumPopulation - minimumPopulation + 1;
            if (populationPoints <= 0 || populationPoints > availablePopulations) {
                throw new IllegalArgumentException(
                        "populationSweep.populationPoints must be in [1, "
                                + availablePopulations + "].");
            }
            Set<Integer> populationValueSet = new HashSet<>();
            for (int population : populationValues) {
                if (population < minimumPopulation || population > maximumPopulation) {
                    throw new IllegalArgumentException(
                            "Every explicit population value must lie inside the sweep range.");
                }
                if (!populationValueSet.add(population)) {
                    throw new IllegalArgumentException(
                            "Duplicate explicit population value: " + population);
                }
            }
            if (!populationValues.isEmpty() && populationValues.size() != populationPoints) {
                throw new IllegalArgumentException(
                        "populationValues size must equal populationPoints when provided.");
            }
            Set<Integer> unique = new HashSet<>();
            for (int population : trajectoryPopulations) {
                if (population < minimumPopulation || population > maximumPopulation) {
                    throw new IllegalArgumentException(
                            "Every trajectory population must lie inside the sweep range.");
                }
                if (!unique.add(population)) {
                    throw new IllegalArgumentException(
                            "Duplicate trajectory population: " + population);
                }
            }
            if (timelinePopulation < minimumPopulation || timelinePopulation > maximumPopulation) {
                throw new IllegalArgumentException(
                        "populationSweep.timelinePopulation must lie inside the sweep range.");
            }
        }
    }

    /** Optional fixed-population experiment for operational reserve tension. */
    public record TensionExperiment(
            int townBurnInDays,
            double foodReserveCapDays,
            double fuelReserveCapNormalDays,
            List<Integer> mineCapacities,
            List<Integer> huntCapacities,
            int detailedMineCapacity,
            int detailedHuntCapacity,
            int visibleForecastHours,
            int overdriveActionWindowHours,
            int forecastSampleHours,
            int forecastTriggerLevel
    ) {
        public TensionExperiment {
            mineCapacities = List.copyOf(mineCapacities);
            huntCapacities = List.copyOf(huntCapacities);
            if (!mineCapacities.contains(detailedMineCapacity)) {
                throw new IllegalArgumentException(
                        "detailedMineCapacity must be included in mineCapacities.");
            }
            if (!huntCapacities.contains(detailedHuntCapacity)) {
                throw new IllegalArgumentException(
                        "detailedHuntCapacity must be included in huntCapacities.");
            }
            if (overdriveActionWindowHours > visibleForecastHours) {
                throw new IllegalArgumentException(
                        "overdriveActionWindowHours cannot exceed visibleForecastHours.");
            }
        }
    }
}
