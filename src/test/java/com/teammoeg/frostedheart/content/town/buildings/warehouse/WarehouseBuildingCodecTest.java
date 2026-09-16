/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

class WarehouseBuildingCodecTest {
    @Test
    void codecPersistsOnlyWarehouseStructureAndCapacityState() {
        BlockPos corePos = new BlockPos(12, 64, -8);
        WarehouseBuilding source = new WarehouseBuilding(
                corePos, true,Optional.of(Set.of(new OccupiedCell(new BlockPos(4, 64, -2),2),
                    	new OccupiedCell(new BlockPos(5, 64, -2),2)))
                , true,
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
