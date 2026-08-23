/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseBuildingCodecTest {
    @Test
    void codecPersistsOnlyWarehouseStructureAndCapacityState() {
        BlockPos corePos = new BlockPos(12, 64, -8);
        WarehouseBuilding source = new WarehouseBuilding(
                corePos, true, OccupiedVolume.EMPTY, true,
                false, 12_345.5, 48, 144, 3);

        CompoundTag encoded = (CompoundTag) WarehouseBuilding.CODEC
                .encodeStart(NbtOps.INSTANCE, source).result().orElseThrow();
        WarehouseBuilding decoded = WarehouseBuilding.CODEC
                .parse(NbtOps.INSTANCE, encoded).result().orElseThrow();

        assertFalse(encoded.contains("interfaces"));
        assertFalse(encoded.contains("emitters"));
        assertEquals(corePos, decoded.getPos());
        assertTrue(decoded.isStructureValid());
        assertTrue(decoded.isInitialized());
        assertEquals(12_345.5, decoded.getCapacity());
        assertEquals(48, decoded.getArea());
        assertEquals(144, decoded.getVolume());
        assertEquals(3, decoded.getDecorationAmount());
    }
}
