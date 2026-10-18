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
