/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.chunkheat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphericalHeatFieldModelTest {
    @Test
    void boundaryIsIncludedAndNextBlockIsExcluded() {
        assertTrue(SphericalHeatFieldModel.contains(0, 0, 0, 16, 16, 0, 0));
        assertTrue(SphericalHeatFieldModel.contains(0, 0, 0, 16, 0, -16, 0));
        assertFalse(SphericalHeatFieldModel.contains(0, 0, 0, 16, 16, 1, 0));
        assertFalse(SphericalHeatFieldModel.contains(0, 0, 0, 16, 17, 0, 0));
    }

    @Test
    void t1DefaultIntegerCapacityBoundsAreLocked() {
        assertEquals(17_077L, SphericalHeatFieldModel.latticeVolume(16));
        assertEquals(793L, SphericalHeatFieldModel.centeredFootprintUpperBound(16, 3));
    }
}
