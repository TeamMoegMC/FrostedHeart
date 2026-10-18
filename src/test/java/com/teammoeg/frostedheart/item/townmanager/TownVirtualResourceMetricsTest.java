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

package com.teammoeg.frostedheart.item.townmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownVirtualResourceMetricsTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void reportsAvailableCapacityWithoutThrottling() {
        TownVirtualResourceMetrics.CapacityBreakdown result =
                TownVirtualResourceMetrics.capacity(100.0, 30.0);

        assertEquals(100.0, result.total(), EPSILON);
        assertEquals(30.0, result.used(), EPSILON);
        assertEquals(70.0, result.available(), EPSILON);
        assertEquals(0.0, result.shortfall(), EPSILON);
        assertEquals(0.3, result.utilizationFraction(), EPSILON);
        assertEquals(1.0, result.effectiveRateScale(), EPSILON);
        assertFalse(result.overcommitted());
    }

    @Test
    void reportsShortfallAndProportionalEffectiveRate() {
        TownVirtualResourceMetrics.CapacityBreakdown result =
                TownVirtualResourceMetrics.capacity(64.0, 96.0);

        assertEquals(0.0, result.available(), EPSILON);
        assertEquals(32.0, result.shortfall(), EPSILON);
        assertEquals(1.5, result.utilizationFraction(), EPSILON);
        assertEquals(2.0 / 3.0, result.effectiveRateScale(), EPSILON);
        assertTrue(result.overcommitted());
    }

    @Test
    void sanitizesInvalidAndNegativeInputs() {
        TownVirtualResourceMetrics.CapacityBreakdown result =
                TownVirtualResourceMetrics.capacity(Double.NaN, Double.POSITIVE_INFINITY);
        TownVirtualResourceMetrics.CapacityBreakdown negative =
                TownVirtualResourceMetrics.capacity(-10.0, -5.0);

        assertEquals(0.0, result.total(), EPSILON);
        assertEquals(0.0, result.used(), EPSILON);
        assertEquals(0.0, result.available(), EPSILON);
        assertEquals(1.0, result.effectiveRateScale(), EPSILON);
        assertEquals(result, negative);
    }
}
