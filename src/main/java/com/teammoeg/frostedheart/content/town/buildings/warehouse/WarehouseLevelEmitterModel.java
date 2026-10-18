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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import org.jetbrains.annotations.Nullable;

import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcher;

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
