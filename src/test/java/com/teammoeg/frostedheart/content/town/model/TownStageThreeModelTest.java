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
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void ineligibleWorkerReleasesSlotAndIsReplaced() {
        TownStageThreeScenario scenario = scenario(2, List.of("mine", "hunt", "house"));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        state.residents().get(0).setWorkId(TownStageThreeState.HUNT_ID);
        state.residents().get(0).setHealth(10.0);

        TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                state, scenario, data(), TownModelParameters.currentDefaults(),
                new SplittableRandom(1L));

        assertEquals(0, result.assignedHunters());
        assertEquals(0.0, result.huntingSwe(), 1.0e-12);
        assertEquals(TownStageThreeState.MINE_ID, state.residents().get(1).workId());
        assertEquals(null, state.residents().get(0).workId());
    }

    @Test
    void guaranteedTargetShortfallUsesTheSharedDailyPlanner() {
        TownStageThreeScenario scenario = scenario(2, List.of("house", "mine", "hunt"))
                .withWorkplaces(new TownStageThreeScenario.Workplaces(2, 2, 1.0))
                .withStaffing(new TownStageThreeScenario.Staffing(
                        List.of("hunt", "mine"), Map.of("hunt", 2, "mine", 0)));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        state.residents().get(0).setHealth(10.0);

        TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                state, scenario, data(), TownModelParameters.currentDefaults(),
                new SplittableRandom(3L));

        assertEquals(2, result.staffingTargetWorkers());
        assertEquals(1, result.staffingTargetCovered());
        assertEquals(1, result.staffingTargetShortfall());
        assertEquals(1, result.unableToWorkResidents());
        assertEquals(1, result.assignedHunters());
        assertEquals(0, result.assignedMiners());
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

    @Test
    void stageThreeConsumesFourChannelNutritionInsteadOfScalarQuality() {
        TownStageThreeScenario scenario = scenario(1, List.of("house", "mine", "hunt"));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        TownStageOneTwoData base = data();
        TownStageOneTwoData proteinOnly = new TownStageOneTwoData(
                base.mineWeights(), base.huntingLoot(), base.coalRecipeProcessTicks(),
                base.cokeRecipeProcessTicks(), base.meats(),
                Map.of("cooked_beef", new TownStageOneTwoData.FoodDefinition(
                        "cooked_beef", 2, 20.8,
                        new ResidentNutrition.NutritionIntake(0, 0, 480, 0))),
                base.sourceFiles());

        TownStageThreeModel.settleDay(
                state, scenario, proteinOnly, TownModelParameters.currentDefaults(),
                new SplittableRandom(9L));

        ResidentNutrition result = state.residents().get(0).nutrition();
        assertEquals(69.0, result.fat(), 1.0e-12);
        assertEquals(69.0, result.carbohydrate(), 1.0e-12);
        assertEquals(69.0, result.vegetable(), 1.0e-12);
        assertEquals(73.0, result.protein(), 1.0e-9);
    }

    @Test
    void sameDayHuntingAndProcessingFeedEveningHousing() {
        TownStageThreeScenario source = scenario(1, List.of("house", "mine", "hunt"));
        TownStageThreeScenario scenario = new TownStageThreeScenario(
                source.schemaVersion(), source.modelStage(), source.metadata(), source.simulation(),
                source.population(), source.house(), source.workplaces(),
                new TownStageThreeScenario.Staffing(
                        List.of("hunt", "mine"), Map.of("hunt", 1, "mine", 0)),
                source.buildingOrder(),
                new TownStageThreeScenario.Warehouse(10_000, List.of(
                        new TownStageThreeScenario.InventoryItem("coke", 10))),
                source.processing(), source.tower(), source.terrain(), source.diagnostics());
        TownStageOneTwoData base = data();
        TownStageOneTwoData processingOnlyFood = new TownStageOneTwoData(
                base.mineWeights(), base.huntingLoot(), base.coalRecipeProcessTicks(),
                base.cokeRecipeProcessTicks(), base.meats(),
                Map.of(
                        "beef", new TownStageOneTwoData.FoodDefinition(
                                "beef", 1, 0, ResidentNutrition.NutritionIntake.ZERO),
                        "cooked_beef", new TownStageOneTwoData.FoodDefinition(
                                "cooked_beef", 2, 20.8,
                                new ResidentNutrition.NutritionIntake(0, 0, 480, 0))),
                base.sourceFiles());
        TownStageThreeState state = TownStageThreeState.initial(scenario);

        TownStageThreeModel.DayResult result = TownStageThreeModel.settleDay(
                state, scenario, processingOnlyFood, TownModelParameters.currentDefaults(),
                new SplittableRandom(12L));

        assertEquals(1, result.huntingRolls());
        assertEquals(1.0, result.meatProcessed(), 1.0e-12);
        assertTrue(result.foodConsumed() > 0.0);
        assertTrue(state.residents().get(0).nutrition().protein() > 60.0);
    }

    @Test
    void conditionUtilityStopsOnceProjectedMealFillsTheChannelGap() {
        TownStageThreeScenario source = scenario(1, List.of("house", "mine", "hunt"));
        TownStageThreeScenario scenario = new TownStageThreeScenario(
                source.schemaVersion(), source.modelStage(), source.metadata(), source.simulation(),
                source.population(), source.house(), source.workplaces(), source.staffing(),
                source.buildingOrder(),
                new TownStageThreeScenario.Warehouse(10_000, List.of(
                        new TownStageThreeScenario.InventoryItem("fatty_meat", 10),
                        new TownStageThreeScenario.InventoryItem("starchy_vegetable", 10),
                        new TownStageThreeScenario.InventoryItem("coke", 10))),
                source.processing(), source.tower(), source.terrain(), source.diagnostics());
        TownStageOneTwoData mixedFood = new TownStageOneTwoData(
                List.of(), List.of(), 1_600, 3_200, List.of(),
                Map.of(
                        "fatty_meat", new TownStageOneTwoData.FoodDefinition(
                                "fatty_meat", 2, 20.8,
                                new ResidentNutrition.NutritionIntake(200, 0, 400, 0)),
                        "starchy_vegetable", new TownStageOneTwoData.FoodDefinition(
                                "starchy_vegetable", 2, 11.0,
                                new ResidentNutrition.NutritionIntake(0, 400, 0, 200))),
                Map.of());
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        TownModelParameters parameters = TownModelParameters.currentDefaults()
                .withNutritionTuning(200.0, 1.0, 2.0);

        TownStageThreeModel.settleDay(
                state, scenario, mixedFood, parameters, new SplittableRandom(21L));

        ResidentNutrition nutrition = state.residents().get(0).nutrition();
        assertTrue(nutrition.fat() > 69.0);
        assertTrue(nutrition.carbohydrate() > 69.0);
        assertTrue(nutrition.protein() > 69.0);
        assertTrue(nutrition.vegetable() > 69.0);
    }

    @Test
    void ageTransitionDayUsesThePreviousAgeGrowthParameters() {
        TownStageThreeScenario scenario = scenario(1, List.of("house", "mine", "hunt"));
        TownStageThreeState state = TownStageThreeState.initial(scenario);
        TownStageThreeState.ResidentState resident = state.residents().get(0);
        resident.setAge(1);
        resident.setAgeDays(59);
        resident.setStrength(50.0);

        TownStageThreeModel.settleDay(
                state, scenario, data(), TownModelParameters.currentDefaults(),
                new SplittableRandom(31L));

        assertEquals(2, resident.age());
        assertEquals(60, resident.ageDays());
        assertTrue(resident.strength() > 50.0,
                "The child-to-adult transition day must still settle child growth.");
    }

    private static TownStageThreeScenario scenario(int population, List<String> order) {
        return new TownStageThreeScenario(
                1, 3,
                new TownStageThreeScenario.Metadata("test", ""),
                new TownStageThreeScenario.Simulation(10, 1, 1L),
                new TownStageThreeScenario.Population(
                        population, TownStageThreeScenario.PopulationInitialization.FIXED,
                        50, 50, 50, 50, 0, 0, 30),
                new TownStageThreeScenario.House(24, 16 * population, 48 * population,
                        population, 0.75),
                new TownStageThreeScenario.Workplaces(population, 1, 1.0),
                TownStageThreeScenario.Staffing.automatic(),
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
                        "beef", new TownStageOneTwoData.FoodDefinition(
                                "beef", 1, 4.8,
                                new ResidentNutrition.NutritionIntake(0, 0, 120, 0)),
                        "cooked_beef", new TownStageOneTwoData.FoodDefinition(
                                "cooked_beef", 2, 20.8,
                                new ResidentNutrition.NutritionIntake(0, 0, 480, 0))),
                Map.of());
    }
}
