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
 */

package com.teammoeg.frostedheart.content.town.resource;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownFoodNutritionModelTest {
    @Test
    void qualityIsNutritionPerFoodResourceUnit() {
        assertEquals(300.0,
                TownFoodNutritionModel.calculateNutritionPerFoodUnit(6_000.0, 20.0),
                1.0e-9);
        assertEquals(150.0,
                TownFoodNutritionModel.calculateNutritionPerFoodUnit(3_000.0, 20.0),
                1.0e-9);
    }

    @Test
    void invalidInputsHaveZeroQuality() {
        assertEquals(0.0,
                TownFoodNutritionModel.calculateNutritionPerFoodUnit(6_000.0, 0.0),
                0.0);
        assertEquals(0.0,
                TownFoodNutritionModel.calculateNutritionPerFoodUnit(Double.NaN, 20.0),
                0.0);
    }

    @Test
    void higherQualityWinsAndStableKeyBreaksTies() {
        List<TownFoodNutritionModel.FoodCandidate> candidates = new ArrayList<>(List.of(
                new TownFoodNutritionModel.FoodCandidate(null, 100.0, "z"),
                new TownFoodNutritionModel.FoodCandidate(null, 200.0, "b"),
                new TownFoodNutritionModel.FoodCandidate(null, 200.0, "a")
        ));

        candidates.sort(TownFoodNutritionModel.HIGHEST_NUTRITION_FIRST);

        assertEquals(List.of("a", "b", "z"),
                candidates.stream()
                        .map(TownFoodNutritionModel.FoodCandidate::stableKey)
                        .toList());
    }
}
