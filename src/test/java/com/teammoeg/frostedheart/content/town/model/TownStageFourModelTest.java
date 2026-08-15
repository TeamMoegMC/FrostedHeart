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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    void populationSweepUsesTwentyExplicitPairedSeedPopulations() throws Exception {
        TownStageFourScenario scenario = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-population-sweep.json"));
        List<Integer> populations = TownStageFourPopulationSweepSimulator.populationPoints(
                scenario.populationSweep());
        assertEquals(20, populations.size());
        assertEquals(1, populations.get(0));
        assertEquals(200, populations.get(populations.size() - 1));
        assertEquals(20, populations.stream().distinct().count());
        assertTrue(populations.contains(13));
        assertEquals(List.of(1, 8, 11, 12, 13, 14, 16, 24, 48, 200),
                scenario.populationSweep().trajectoryPopulations());
        assertEquals(24, scenario.populationSweep().timelinePopulation());
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

        assertEquals(200, scaled.town().population().initialResidents());
        assertTrue(TownStageThreeModel.houseCapacity(scaled.town(), parameters) >= 200);
        assertTrue(TownStageFourModel.huntingCapacity(scaled, parameters) >= 200);
        assertEquals(437.5, scaled.town().warehouse().initialInventory().stream()
                .filter(item -> "minecraft:cooked_beef".equals(item.item()))
                .findFirst().orElseThrow().amountItems(), EPSILON);
        assertEquals(75.0, scaled.town().warehouse().initialInventory().stream()
                .filter(item -> "immersiveengineering:coal_coke".equals(item.item()))
                .findFirst().orElseThrow().amountItems(), EPSILON);
    }

    @Test
    void stageFourPopulationUsesSeededGameplayResidentGeneration() throws Exception {
        TownStageFourScenario base = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-population-sweep.json"));
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageThreeState first = TownStageThreeState.initial(
                base.town(), parameters, new java.util.SplittableRandom(41L));
        TownStageThreeState replay = TownStageThreeState.initial(
                base.town(), parameters, new java.util.SplittableRandom(41L));

        assertEquals(first.residents().get(0).age(), replay.residents().get(0).age());
        assertEquals(first.residents().get(0).strength(), replay.residents().get(0).strength(), 0.0);
        assertNotEquals(first.residents().get(0).strength(), first.residents().get(1).strength());
    }

    @Test
    void tensionScenarioIsFixedToTwentyFourResidentsAndExplicitControls() throws Exception {
        TownStageFourScenario scenario = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-24-tension.json"));
        TownStageFourScenario.TensionExperiment experiment = scenario.tensionExperiment();

        assertEquals(24, scenario.town().population().initialResidents());
        assertEquals(120, experiment.townBurnInDays());
        assertEquals(14.0, experiment.foodReserveCapDays(), EPSILON);
        assertEquals(21.0, experiment.fuelReserveCapNormalDays(), EPSILON);
        assertEquals(List.of(5, 6, 7, 8), experiment.mineCapacities());
        assertEquals(List.of(3, 4, 6, 8), experiment.huntCapacities());
        assertEquals(-2, experiment.forecastTriggerLevel());
    }

    @Test
    void forecastTriggerUsesCurrentStrongBottomPlusSensitivityBoundary() {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        assertEquals(2.0, parameters.climate().forecastSensitivityCelsius(), EPSILON);
        assertTrue(TownStageFourTensionModel.atOrBelowForecastLevel(
                -18.01F, -2, parameters.climate()));
        assertTrue(!TownStageFourTensionModel.atOrBelowForecastLevel(
                -18.0F, -2, parameters.climate()));
    }

    @Test
    void tensionLayoutUsesCurrentCompactCapacityAndFiniteOperatingBuffers() throws Exception {
        TownStageFourScenario base = TownStageFourScenario.load(Path.of(
                "Scripts/town_scenarios/experiments/stage4-t1-24-tension.json"));
        TownStageOneTwoData data = new TownStageOneTwoData(
                List.of(), List.of(), 1600, 3200, List.of(),
                Map.of("minecraft:cooked_beef",
                        new TownStageOneTwoData.FoodDefinition(
                                "minecraft:cooked_beef", 4, 20.8, 7000.0)),
                Map.of());
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownStageFourScenario scenario = TownStageFourTensionModel.forCapacities(
                base, 8, 4, data, parameters);
        TownStageThreeState state = TownStageThreeState.initial(
                scenario.town(), parameters, new java.util.SplittableRandom(17L));
        state.add("minecraft:cooked_beef", 10.0,
                com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode.MAXIMIZE);
        state.add("immersiveengineering:coal_coke", 10.0,
                com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode.MAXIMIZE);

        TownStageFourTensionModel.trimOperationalReserves(
                state, scenario.town(), data, parameters, 14.0, 21.0);

        assertEquals(24, TownStageThreeModel.houseCapacity(scenario.town(), parameters));
        assertEquals(4, TownStageFourModel.huntingCapacity(scenario, parameters));
        assertEquals(14.0, TownStageThreeModel.foodReserveDays(
                state, data, parameters, 24), 1.0e-8);
        assertEquals(21.0, TownStageThreeModel.fuelReserveDays(
                state, scenario.town(), data, parameters), 1.0e-8);
    }
}
