/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurroundingTemperatureSimulatorCacheTest {

    @Test
    void zeroTemperatureEmptyShapeCanReuseAirInfo() {
        assertTrue(BlockInfoCachePolicy.canReuseAirInfo(Shapes.empty(), 0f));
    }

    @Test
    void heatedEmptyShapeKeepsItsTemperature() {
        assertFalse(BlockInfoCachePolicy.canReuseAirInfo(Shapes.empty(), 15f));
    }

    @Test
    void fullShapeNeverUsesAirInfo() {
        assertFalse(BlockInfoCachePolicy.canReuseAirInfo(Shapes.block(), 0f));
    }
}
