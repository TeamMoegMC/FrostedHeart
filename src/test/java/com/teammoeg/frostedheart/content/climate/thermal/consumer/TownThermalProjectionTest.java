/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.consumer;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownThermalProjectionTest {
    @Test
    void compressesVoxelsByBaseBrickAndChoosesAStableRepresentative() {
        TownThermalProjection projection = new TownThermalProjection();
        projection.include(new BlockPos(-1, -1, -1));
        projection.include(new BlockPos(-4, -4, -4));
        projection.include(new BlockPos(0, 0, 0));

        assertEquals(3, projection.voxelCount());
        assertEquals(2, projection.groupKeys().length);

        long negativeBrick = BlockPos.asLong(-4, -4, -4);
        assertEquals(2, projection.weight(negativeBrick));
        assertEquals(-4, projection.representativeX(negativeBrick));
        assertEquals(-4, projection.representativeY(negativeBrick));
        assertEquals(-4, projection.representativeZ(negativeBrick));

        TownThermalProjection reverseOrder = new TownThermalProjection();
        reverseOrder.include(new BlockPos(-4, -4, -4));
        reverseOrder.include(new BlockPos(-1, -1, -1));
        assertEquals(projection.representativeX(negativeBrick),
                reverseOrder.representativeX(negativeBrick));
        assertEquals(projection.representativeY(negativeBrick),
                reverseOrder.representativeY(negativeBrick));
        assertEquals(projection.representativeZ(negativeBrick),
                reverseOrder.representativeZ(negativeBrick));
    }
}
