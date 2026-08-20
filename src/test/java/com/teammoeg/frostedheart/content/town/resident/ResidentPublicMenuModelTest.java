/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentPublicMenuModelTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void nutritionPointsUseHungerButNotSaturation() {
        ResidentNutrition.NutritionIntake beef = ResidentPublicMenuModel.nutritionPoints(
                new FoodNutritionProfile(0, 0, 60, 0), 8);
        ResidentNutrition.NutritionIntake potato = ResidentPublicMenuModel.nutritionPoints(
                new FoodNutritionProfile(0, 40, 0, 20), 5);

        assertEquals(480.0, beef.protein(), EPSILON);
        assertEquals(200.0, potato.carbohydrate(), EPSILON);
        assertEquals(100.0, potato.vegetable(), EPSILON);
    }

    @Test
    void highestAvailableFoodTierIsAnAbsoluteConstraint() {
        List<ResidentPublicMenuModel.Recipient> residents = List.of(
                new ResidentPublicMenuModel.Recipient(
                        new ResidentNutrition(0, 0, 0, 0), 1.0));
        ResidentPublicMenuModel.Candidate<String> usefulLowTier = candidate(
                "protein", 3, new ResidentNutrition.NutritionIntake(0, 0, 200, 0));
        ResidentPublicMenuModel.Candidate<String> emptyHighTier = candidate(
                "empty", 4, ResidentNutrition.NutritionIntake.ZERO);

        ResidentPublicMenuModel.Plan<String> result = ResidentPublicMenuModel.plan(
                20, 8, List.of(usefulLowTier, emptyHighTier), residents,
                ResidentPublicMenuModel.Parameters.DEFAULT);

        assertEquals(2.0, result.itemAmounts().getOrDefault("empty", 0.0), EPSILON);
        assertEquals(0.0, result.itemAmounts().getOrDefault("protein", 0.0), EPSILON);
    }

    @Test
    void menuTargetsAggregateDeficitAndIsDistributedByShare() {
        List<ResidentPublicMenuModel.Recipient> residents = List.of(
                new ResidentPublicMenuModel.Recipient(
                        new ResidentNutrition(70, 0, 0, 70), 0.75),
                new ResidentPublicMenuModel.Recipient(
                        new ResidentNutrition(70, 70, 70, 0), 0.25));
        ResidentPublicMenuModel.Candidate<String> protein = candidate(
                "protein", 4, new ResidentNutrition.NutritionIntake(0, 0, 200, 0));
        ResidentPublicMenuModel.Candidate<String> vegetable = candidate(
                "vegetable", 4, new ResidentNutrition.NutritionIntake(0, 0, 0, 200));

        ResidentPublicMenuModel.Plan<String> result = ResidentPublicMenuModel.plan(
                20, 8, List.of(protein, vegetable), residents,
                ResidentPublicMenuModel.Parameters.DEFAULT);

        assertTrue(result.itemAmounts().getOrDefault("protein", 0.0)
                > result.itemAmounts().getOrDefault("vegetable", 0.0));
        ResidentNutrition first = residents.get(0).nutrition().withMeal(
                result.nutrition().scale(0.75), 200, 2, 2);
        ResidentNutrition second = residents.get(1).nutrition().withMeal(
                result.nutrition().scale(0.25), 200, 2, 2);
        assertTrue(first.protein() > 0.0);
        assertTrue(second.protein() > 70.0);
    }

    @Test
    void priorityGroupsReserveHighTierFoodBeforeLaterGroups() {
        ResidentPublicMenuModel.Recipient recipient = new ResidentPublicMenuModel.Recipient(
                new ResidentNutrition(0, 0, 0, 0), 1.0);
        List<ResidentPublicMenuModel.Group<String>> groups = List.of(
                new ResidentPublicMenuModel.Group<>("high-house", 10.0, List.of(recipient)),
                new ResidentPublicMenuModel.Group<>("low-house", 10.0, List.of(recipient)));
        List<ResidentPublicMenuModel.Candidate<String>> foods = List.of(
                new ResidentPublicMenuModel.Candidate<>(
                        "premium", "premium", 4, 1.0, 10.0,
                        ResidentNutrition.NutritionIntake.ZERO),
                new ResidentPublicMenuModel.Candidate<>(
                        "basic", "basic", 3, 1.0, 10.0,
                        ResidentNutrition.NutritionIntake.ZERO));

        List<ResidentPublicMenuModel.GroupPlan<String, String>> result =
                ResidentPublicMenuModel.planInPriorityOrder(
                        groups, 8, foods, ResidentPublicMenuModel.Parameters.DEFAULT);

        assertEquals(List.of("high-house", "low-house"),
                result.stream().map(ResidentPublicMenuModel.GroupPlan::key).toList());
        assertEquals(Map.of("premium", 1.0), result.get(0).plan().itemAmounts());
        assertEquals(Map.of("basic", 1.0), result.get(1).plan().itemAmounts());
    }

    private static ResidentPublicMenuModel.Candidate<String> candidate(
            String key,
            int tier,
            ResidentNutrition.NutritionIntake nutrition
    ) {
        return new ResidentPublicMenuModel.Candidate<>(
                key, key, tier, 2.0, 10.0, nutrition);
    }
}
