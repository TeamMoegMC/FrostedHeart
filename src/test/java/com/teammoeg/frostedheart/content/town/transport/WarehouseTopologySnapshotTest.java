/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
