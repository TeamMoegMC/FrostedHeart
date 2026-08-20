/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.health.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodNutritionProfileTest {
    @Test
    void rawRecipeValuesBecomeIndependentPercentChannels() {
        FoodNutritionProfile profile = FoodNutritionProfile.fromRecipeValues(
                8_000, 16_000, 24_000, 40_000);

        assertEquals(20.0f, profile.fat(), 1.0e-6f);
        assertEquals(40.0f, profile.carbohydrate(), 1.0e-6f);
        assertEquals(60.0f, profile.protein(), 1.0e-6f);
        assertEquals(100.0f, profile.vegetable(), 1.0e-6f);
    }

    @Test
    void everyPublicChannelIsClampedAndFinite() {
        FoodNutritionProfile profile = new FoodNutritionProfile(
                -1.0f, 101.0f, Float.NaN, Float.POSITIVE_INFINITY);

        assertEquals(FoodNutritionProfile.ZERO.fat(), profile.fat());
        assertEquals(100.0f, profile.carbohydrate());
        assertEquals(0.0f, profile.protein());
        assertEquals(0.0f, profile.vegetable());
    }

    @Test
    void tooltipChannelSharesUseTheProfileTotal() {
        FoodNutritionProfile profile = new FoodNutritionProfile(20, 40, 60, 80);

        double shareTotal = profile.fat() / profile.total()
                + profile.carbohydrate() / profile.total()
                + profile.protein() / profile.total()
                + profile.vegetable() / profile.total();

        assertEquals(1.0, shareTotal, 1.0e-6);
    }
}
