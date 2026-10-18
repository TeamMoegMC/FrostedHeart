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

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RadiantEquivalentTemperatureTest {
    @Test
    void preservesTheFrozenZeroAndOneHundredFluxContract() {
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(0.0D));
        assertEquals(13.333333333333334D,
                RadiantEquivalentTemperature.deltaC(100.0D));
        assertEquals(33.333333333333336D,
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(20.0D, 100.0D));
    }

    @Test
    void invalidInputsRemainFiniteAndContributeNoRadiantHeat() {
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(-1.0D));
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(Double.NaN));
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(Double.POSITIVE_INFINITY));
        assertTrue(Double.isFinite(
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(
                        Double.NaN, Double.POSITIVE_INFINITY)));
    }
}
