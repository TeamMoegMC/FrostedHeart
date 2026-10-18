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

package com.teammoeg.frostedheart.content.town.transport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

class WarehouseTopologySnapshotTest {
    @Test
    void snapshotCopiesEntriesAndSortsByCoreCoordinates() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(8, 64, 0);
        List<WarehouseTopologyEntry> source = new ArrayList<>(List.of(
                new WarehouseTopologyEntry(mutable, 200.0),
                new WarehouseTopologyEntry(new BlockPos(-4, 70, 2), 100.0)));

        WarehouseTopologySnapshot snapshot = WarehouseTopologySnapshot.of(
                Level.OVERWORLD, source);
        mutable.set(100, 100, 100);
        source.clear();

        assertEquals(List.of(new BlockPos(-4, 70, 2), new BlockPos(8, 64, 0)),
                snapshot.entries().stream().map(WarehouseTopologyEntry::corePos).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.entries().add(new WarehouseTopologyEntry(BlockPos.ZERO, 1.0)));
        assertTrue(snapshot.isUsable());
    }

    @Test
    void unavailableDimensionAndEmptyEntriesRemainDistinctFacts() {
        WarehouseTopologySnapshot missingDimension = WarehouseTopologySnapshot.of(
                null, List.of(new WarehouseTopologyEntry(BlockPos.ZERO, 1.0)));
        WarehouseTopologySnapshot emptyTown = WarehouseTopologySnapshot.of(
                Level.OVERWORLD, List.of());

        assertFalse(missingDimension.isUsable());
        assertFalse(emptyTown.isUsable());
        assertFalse(missingDimension.equals(emptyTown));
    }
}
