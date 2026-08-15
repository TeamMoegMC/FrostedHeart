/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownFoodInventoryModelTest {
    @Test
    void foodLevelPrecedesNutritionAndNutritionBreaksSameLevelTies() {
        TownFoodInventoryModel.Consumption consumption = TownFoodInventoryModel.consume(
                7.0,
                List.of(
                        new TownFoodInventoryModel.FoodStack("raw-high-quality", 1, 10, 1, 1000),
                        new TownFoodInventoryModel.FoodStack("cooked-low-quality", 2, 1, 4, 1),
                        new TownFoodInventoryModel.FoodStack("cooked-high-quality", 2, 1, 4, 100)));

        assertEquals("cooked-high-quality", consumption.uses().get(0).item());
        assertEquals("cooked-low-quality", consumption.uses().get(1).item());
        assertEquals(7.0, consumption.consumedFoodUnits(), 1.0e-12);
    }

    @Test
    void lastItemCanBeConsumedFractionally() {
        TownFoodInventoryModel.Consumption consumption = TownFoodInventoryModel.consume(
                6.5,
                List.of(new TownFoodInventoryModel.FoodStack("food", 2, 1, 20.8, 6000)));

        assertEquals(6.5 / 20.8, consumption.uses().get(0).amountItems(), 1.0e-12);
        assertEquals(6000.0 * 6.5 / 20.8, consumption.consumedNutrition(), 1.0e-12);
    }
}
