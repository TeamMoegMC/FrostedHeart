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
