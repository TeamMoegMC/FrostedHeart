/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcher;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseLevelEmitterModelTest {
    @Test
    void thresholdModesAreComplementaryAndUnavailableStateForcesOutputOff() {
        assertFalse(WarehouseLevelEmitterModel.shouldEmit(
                WarehouseRedstoneMode.HIGH_SIGNAL, 9, 10));
        assertTrue(WarehouseLevelEmitterModel.shouldEmit(
                WarehouseRedstoneMode.HIGH_SIGNAL, 10, 10));
        assertTrue(WarehouseLevelEmitterModel.shouldEmit(
                WarehouseRedstoneMode.LOW_SIGNAL, 9, 10));
        assertFalse(WarehouseLevelEmitterModel.shouldEmit(
                WarehouseRedstoneMode.LOW_SIGNAL, 10, 10));
        assertFalse(WarehouseLevelEmitterModel.shouldEmit(
                WarehouseRedstoneMode.IGNORE, Long.MAX_VALUE, 1));

        WarehouseLevelEmitterModel.StateChange unavailable =
                WarehouseLevelEmitterModel.compareState(50, true, 0, false);
        assertTrue(unavailable.changed());
        assertTrue(unavailable.outputChanged());
    }

    @Test
    void neighborNotificationIsRequiredOnlyForANetOutputChange() {
        WarehouseLevelEmitterModel.StateChange unchanged =
                WarehouseLevelEmitterModel.compareState(10, true, 10, true);
        WarehouseLevelEmitterModel.StateChange stockOnly =
                WarehouseLevelEmitterModel.compareState(10, true, 11, true);
        WarehouseLevelEmitterModel.StateChange output =
                WarehouseLevelEmitterModel.compareState(10, false, 10, true);

        assertFalse(unchanged.changed());
        assertFalse(unchanged.outputChanged());
        assertTrue(stockOnly.changed());
        assertFalse(stockOnly.outputChanged());
        assertTrue(output.changed());
        assertTrue(output.outputChanged());
    }

    @Test
    void watcherConfigurationResetsOnceAndAddsOnlyTheExactFilter() {
        CountingWatcher watcher = new CountingWatcher();
        SimpleItemKey filter = new SimpleItemKey(Items.COBBLESTONE, null);

        WarehouseLevelEmitterModel.configureWatcher(watcher, filter);
        assertEquals(1, watcher.resetCount);
        assertEquals(List.of(filter), watcher.added);

        WarehouseLevelEmitterModel.configureWatcher(watcher, null);
        assertEquals(2, watcher.resetCount);
        assertEquals(List.of(filter), watcher.added);
    }

    @Test
    void menuStockSaturatesBeforeTheLongToIntConversion() {
        assertEquals(0, WarehouseLevelEmitterModel.stockForMenu(-1));
        assertEquals(20, WarehouseLevelEmitterModel.stockForMenu(20));
        assertEquals(Integer.MAX_VALUE,
                WarehouseLevelEmitterModel.stockForMenu(Long.MAX_VALUE));
    }

    private static final class CountingWatcher implements IWarehouseStockWatcher {
        private int resetCount;
        private final List<SimpleItemKey> added = new ArrayList<>();

        @Override
        public void reset() {
            resetCount++;
        }

        @Override
        public void addWatch(SimpleItemKey item) {
            added.add(item);
        }

        @Override
        public void removeWatch(SimpleItemKey item) {
        }

        @Override
        public void setWatchAll(boolean watchAll) {
        }

        @Override
        public boolean isWatchAll() {
            return false;
        }
    }
}
