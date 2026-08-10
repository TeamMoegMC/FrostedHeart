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

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownFoodResourceAmountTest {
    @Test
    void rawBeefUsesHungerAndNominalSaturation() {
        assertEquals(4.8, TownFoodResourceAmount.fromFoodProperties(3, 0.3f), 1.0e-6);
    }

    @Test
    void cookedBeefIsWorthMoreThanRawBeef() {
        assertEquals(20.8, TownFoodResourceAmount.fromFoodProperties(8, 0.8f), 1.0e-6);
    }

    @Test
    void negativeInputsCannotCreateNegativeFood() {
        assertEquals(0.0, TownFoodResourceAmount.fromFoodProperties(-1, -0.5f), 0.0);
    }
}
