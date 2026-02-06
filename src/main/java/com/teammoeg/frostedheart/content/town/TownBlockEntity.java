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

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;

/**
 * A town block's tile entity.
 * <p>
 * Should be implemented by tile entities that are associated with town blocks.
 */
public interface TownBlockEntity<T extends WorkerState> {
    /**
     * Get the priority of the worker.
     *
     * @return the priority
     */
    int getPriority();

    /**
     * Get the worker type.
     *
     * @return the worker type
     */
    TownWorkerType getWorkerType();

    /**
     * Check if the work is valid.
     *
     * @return true if the work is valid, false otherwise
     */
    boolean isWorkValid();

    /**
     * Get the occupied area of the entire structure.
     * Occupied area should be counted when scanning the structure.
     * @return the occupied area of the entire structure.
     */
    OccupiedArea getOccupiedArea();

    /**
     * Set the worker state.
     * see TownBuildingCoreBlockTileEntity.workerState
     */
    void setStatus(TownWorkerStatus state);

    /**
     * Get the worker state.
     */
    TownWorkerStatus getStatus();
}
