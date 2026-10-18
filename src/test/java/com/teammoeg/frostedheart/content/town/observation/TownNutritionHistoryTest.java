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
package com.teammoeg.frostedheart.content.town.observation;

import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownNutritionHistoryTest {
    @Test
    void capturesAverageAndLinearlyInterpolatedLowTailPerChannel() {
        TownNutritionHistory history = TownNutritionHistory.capture(List.of(
                new ResidentNutrition(0, 20, 40, 60),
                new ResidentNutrition(50, 50, 50, 50),
                new ResidentNutrition(100, 80, 60, 40)));

        assertTrue(history.available());
        assertEquals(50.0, history.averageFat(), 1.0e-12);
        assertEquals(10.0, history.p10Fat(), 1.0e-12);
        assertEquals(50.0, history.averageCarbohydrate(), 1.0e-12);
        assertEquals(26.0, history.p10Carbohydrate(), 1.0e-12);
        assertEquals(50.0, history.averageProtein(), 1.0e-12);
        assertEquals(42.0, history.p10Protein(), 1.0e-12);
        assertEquals(50.0, history.averageVegetable(), 1.0e-12);
        assertEquals(42.0, history.p10Vegetable(), 1.0e-12);
    }

    @Test
    void emptyTownIsAnAvailableZeroSampleRatherThanLegacyMissingData() {
        TownNutritionHistory history = TownNutritionHistory.capture(List.of());

        assertTrue(history.available());
        assertEquals(0.0, history.averageFat(), 1.0e-12);
        assertEquals(0.0, history.p10Vegetable(), 1.0e-12);
    }
}
