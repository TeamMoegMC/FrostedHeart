/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.compat.caupona;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NutritionEventsTest {
    @Test
    void cauponaWeightedProfileStaysOnPercentageScale() {
        NutritionEvents.ProfileAccumulator accumulator =
                new NutritionEvents.ProfileAccumulator();
        accumulator.add(new FoodNutritionProfile(20, 40, 60, 0), 8);

        FoodNutritionProfile result = accumulator.toProfile(1.0 / 8.0);

        assertEquals(20.0f, result.fat(), 1.0e-6f);
        assertEquals(40.0f, result.carbohydrate(), 1.0e-6f);
        assertEquals(60.0f, result.protein(), 1.0e-6f);
        assertEquals(0.0f, result.vegetable(), 1.0e-6f);
    }
}
