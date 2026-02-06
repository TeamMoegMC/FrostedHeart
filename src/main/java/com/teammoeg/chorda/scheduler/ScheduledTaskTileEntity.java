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

package com.teammoeg.chorda.scheduler;

/**
 * Interface for TileEntities that can have scheduled tasks.
 */
public interface ScheduledTaskTileEntity {
    /**
     * Execute the task.
     * The task here would be put in a global queue, which may be executed asynchronously.
     * Therefore, the task should be some expensive but not tick-wise critical operations.
     * For example, checking whether the structure of a house is valid.
     */
    void executeTask();

    boolean isStillValid();
}
