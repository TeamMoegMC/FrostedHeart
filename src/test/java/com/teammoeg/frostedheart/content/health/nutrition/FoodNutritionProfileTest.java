/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
