/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcher;
import org.jetbrains.annotations.Nullable;

/** Pure state rules shared by the warehouse level emitter and its tests. */
final class WarehouseLevelEmitterModel {
    private WarehouseLevelEmitterModel() {
    }

    static boolean shouldEmit(WarehouseRedstoneMode mode, long stock, int threshold) {
        return switch (mode) {
            case HIGH_SIGNAL -> stock >= threshold;
            case LOW_SIGNAL -> stock < threshold;
            case IGNORE -> false;
        };
    }

    static StateChange compareState(
            long previousStock,
            boolean previousOutput,
            long stock,
            boolean output
    ) {
        return new StateChange(
                stock != previousStock || output != previousOutput,
                output != previousOutput);
    }

    static void configureWatcher(
            IWarehouseStockWatcher watcher,
            @Nullable SimpleItemKey filter
    ) {
        if (watcher == null) {
            return;
        }
        watcher.reset();
        if (filter != null) {
            watcher.addWatch(filter);
        }
    }

    static int stockForMenu(long stock) {
        return (int) Math.max(0L, Math.min(stock, Integer.MAX_VALUE));
    }

    record StateChange(boolean changed, boolean outputChanged) {
    }
}
