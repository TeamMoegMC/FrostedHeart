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

import net.minecraft.core.BlockPos;

/**
 * 调度任务的数据容器，存储方块实体的位置和移除标记。
 * 用于 {@link SchedulerQueue} 中跟踪待执行的调度任务。
 * <p>
 * Data container for a scheduled task, storing the block entity position and removal flag.
 * Used in {@link SchedulerQueue} to track pending scheduled tasks.
 */
public class ScheduledData {

    /** 关联的方块实体位置。 / The position of the associated block entity. */
    BlockPos pos;
    /** 是否标记为待移除。 / Whether this entry is marked for removal. */
    boolean forRemoval = false;

    /**
     * 构造一个新的调度数据实例。
     * <p>
     * Constructs a new scheduled data instance.
     *
     * @param pos 方块实体的位置 / the block entity position
     */
    public ScheduledData(BlockPos pos) {
        this.pos = pos;
    }
}
