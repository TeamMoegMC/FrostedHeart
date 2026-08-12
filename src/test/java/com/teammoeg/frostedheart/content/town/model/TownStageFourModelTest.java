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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownStageFourModelTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void referenceLayoutUsesExplicitVoxelsAndCurrentT1Sphere() throws Exception {
        TownStageFourScenario scenario = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/baseline/stage4-t1-8-residents.json"));
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageFourModel.ThermalLayout layout =
                TownStageFourModel.analyzeLayout(scenario, parameters);
        assertEquals(16, layout.radiusBlocks());
        assertEquals(10, layout.heatTemperatureCelsius());
        assertEquals(17_077L, layout.sphereVoxelCount());
        assertEquals(384L, layout.building("house").voxelCount());
        assertEquals(1.0, layout.building("house").coverageFraction(), EPSILON);
        assertEquals(1.0, layout.building("hunt").coverageFraction(), EPSILON);
    }

    @Test
    void fixedSeedClimateSeriesIsReproducibleAfterBurnIn() {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        float[] first = TownStageFourModel.climateSeries(9L, 365, 10, parameters)
                .hourlyTemperatureCelsius();
        float[] second = TownStageFourModel.climateSeries(9L, 365, 10, parameters)
                .hourlyTemperatureCelsius();
        assertEquals(first.length, second.length);
        for (int index = 0; index < first.length; index++) {
            assertEquals(first[index], second[index], 0.0);
        }
    }

    @Test
    void fullyCoveredNormalT1LosesWorkabilityBelowMinusTwentyClimate() throws Exception {
        TownStageFourScenario scenario = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/baseline/stage4-t1-8-residents.json"));
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageFourModel.ThermalLayout layout =
                TownStageFourModel.analyzeLayout(scenario, parameters);
        TownStageFourModel.HourThermalResult atLimit = TownStageFourModel.evaluateHour(
                -20.0F, true, scenario, parameters, layout);
        TownStageFourModel.HourThermalResult belowLimit = TownStageFourModel.evaluateHour(
                -20.01F, true, scenario, parameters, layout);
        assertEquals(0.0, atLimit.building("house").temperatureCelsius(), EPSILON);
        assertTrue(belowLimit.building("house").temperatureCelsius() < 0.0);
    }

    @Test
    void populationSweepUsesOneHundredDistinctPairedSeedPopulations() throws Exception {
        TownStageFourScenario scenario = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-population-sweep.json"));
        List<Integer> populations = TownStageFourPopulationSweepSimulator.populationPoints(
                scenario.populationSweep());
        assertEquals(100, populations.size());
        assertEquals(1, populations.get(0));
        assertEquals(200, populations.get(populations.size() - 1));
        assertEquals(100, populations.stream().distinct().count());
        assertTrue(populations.contains(13));
        assertEquals(List.of(1, 8, 11, 12, 13, 14, 16, 24, 48, 200),
                scenario.populationSweep().trajectoryPopulations());
    }

    @Test
    void compactPopulationLayoutSatisfiesCurrentHouseAndHuntingCapacity() throws Exception {
        TownStageFourScenario base = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-population-sweep.json"));
        TownStageOneTwoData data = new TownStageOneTwoData(
                List.of(), List.of(), 1600, 3200, List.of(),
                Map.of("minecraft:cooked_beef",
                        new TownStageOneTwoData.FoodDefinition(
                                "minecraft:cooked_beef", 4, 20.8, 7000.0)),
                Map.of());
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageFourScenario scaled = TownStageFourPopulationSweepSimulator.forPopulation(
                base, 200, data, parameters);

        assertEquals(200, scaled.town().population().standardAdults());
        assertTrue(TownStageThreeModel.houseCapacity(scaled.town(), parameters) >= 200);
        assertTrue(TownStageFourModel.huntingCapacity(scaled, parameters) >= 200);
        assertEquals(437.5, scaled.town().warehouse().initialInventory().stream()
                .filter(item -> "minecraft:cooked_beef".equals(item.item()))
                .findFirst().orElseThrow().amountItems(), EPSILON);
        assertEquals(75.0, scaled.town().warehouse().initialInventory().stream()
                .filter(item -> "immersiveengineering:coal_coke".equals(item.item()))
                .findFirst().orElseThrow().amountItems(), EPSILON);
    }
}
