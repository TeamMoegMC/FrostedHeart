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

import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownStageThreeModelTest {
    @Test
    void fixedSeedReplaysIdenticalMultidayStateAndInitialStockIsNotProduction() {
        TownStageThreeScenario scenario = scenario(2, List.of("house", "mine", "hunt"));
        TownStageThreeState first = TownStageThreeState.initial(scenario);
        TownStageThreeState second = TownStageThreeState.initial(scenario);
        SplittableRandom firstRandom = new SplittableRandom(42L);
        SplittableRandom secondRandom = new SplittableRandom(42L);

        assertEquals(0.0, first.cumulativeCoalRequested(), 1.0e-12);
        assertEquals(0.0, first.cumulativeHuntingFoodPotential(), 1.0e-12);
        for (int day = 0; day < 10; day++) {
            assertEquals(
                    TownStageThreeModel.settleDay(
                            first, scenario, data(), TownModelParameters.currentDefaults(), firstRandom),
                    TownStageThreeModel.settleDay(
                            second, scenario, data(), TownModelParameters.currentDefaults(), secondRandom));
        }
        assertEquals(first.inventorySnapshot(), second.inventorySnapshot());
        assertEquals(first.cumulativeCoalRequested(), second.cumulativeCoalRequested(), 1.0e-12);
        assertEquals(first.cumulativeHuntingFoodPotential(),
                second.cumulativeHuntingFoodPotential(), 1.0e-12);
    }

    @Test
    void ineligibleStickyWorkerKeepsSlotAndBlocksReplacement() {
        TownStageThreeScenario scenario = scenario(2, List.of("mine", "hunt", "house"));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        state.residents().get(0).setWorkId(TownStageThreeState.HUNT_ID);
        state.residents().get(0).setHealth(10.0);

        TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                state, scenario, data(), TownModelParameters.currentDefaults(),
                new SplittableRandom(1L));

        assertEquals(1, result.assignedHunters());
        assertEquals(0.0, result.huntingSwe(), 1.0e-12);
        assertEquals(TownStageThreeState.MINE_ID, state.residents().get(1).workId());
    }

    @Test
    void itemResourceFlowClosesAgainstWarehouseDelta() {
        TownStageThreeScenario scenario = scenario(2, List.of("house", "mine", "hunt"));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        double initialItems = state.totalInventoryItems();

        TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                state, scenario, data(), TownModelParameters.currentDefaults(),
                new SplittableRandom(7L));
        double flowDelta = result.resourceFlows().stream()
                .filter(flow -> !"process_ticks".equals(flow.resource()))
                .mapToDouble(flow -> switch (flow.action()) {
                    case "produce" -> flow.modified();
                    case "consume" -> -flow.modified();
                    default -> 0.0;
                })
                .sum();

        assertEquals(initialItems + flowDelta, result.inventoryItems(), 1.0e-9);
    }

    private static TownStageThreeScenario scenario(int population, List<String> order) {
        return new TownStageThreeScenario(
                1, 3,
                new TownStageThreeScenario.Metadata("test", ""),
                new TownStageThreeScenario.Simulation(10, 1, 1L),
                new TownStageThreeScenario.Population(
                        population, 50, 50, 50, 50, 0, 0, 30),
                new TownStageThreeScenario.House(24, 8 * population, 24 * population,
                        population, 0.75),
                new TownStageThreeScenario.Workplaces(population, 1, 1.0),
                order,
                new TownStageThreeScenario.Warehouse(10_000, List.of(
                        new TownStageThreeScenario.InventoryItem("cooked_beef", 10),
                        new TownStageThreeScenario.InventoryItem("coke", 10))),
                new TownStageThreeScenario.Processing(
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, "coke"),
                new TownStageThreeScenario.Tower("coke", "coke", 0.0, false, 0.0),
                new TownStageThreeScenario.Terrain(1_000, 1_000, 0),
                new TownStageThreeScenario.Diagnostics(8, false));
    }

    private static TownStageOneTwoData data() {
        TownFoodProcessingModel.MeatDefinition meat =
                new TownFoodProcessingModel.MeatDefinition(
                        "beef", "cooked_beef", 4.8, 20.8,
                        3_000, 6_000, 1, 2);
        return new TownStageOneTwoData(
                List.of(new TownStageZeroModel.WeightedResource("coal", 1.0)),
                List.of(new TownStageZeroModel.WeightedLootEntry("beef", 1.0, 1, 1)),
                1_600, 3_200,
                List.of(meat),
                Map.of(
                        "beef", new TownStageOneTwoData.FoodDefinition("beef", 1, 4.8, 3_000),
                        "cooked_beef", new TownStageOneTwoData.FoodDefinition(
                                "cooked_beef", 2, 20.8, 6_000)),
                Map.of());
    }
}
