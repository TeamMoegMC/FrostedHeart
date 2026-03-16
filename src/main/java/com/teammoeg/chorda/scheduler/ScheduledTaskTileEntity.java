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
 * 可拥有调度任务的方块实体接口。实现此接口的方块实体可以注册到 {@link SchedulerQueue}，
 * 以在全局队列中按限定速率执行任务。
 * <p>
 * Interface for block entities that can have scheduled tasks. Block entities implementing this interface
 * can be registered to the {@link SchedulerQueue} for rate-limited task execution in a global queue.
 */
public interface ScheduledTaskTileEntity {
    /**
     * 执行调度任务。任务会被放入全局队列，可能异步执行。
     * 因此任务应该是开销较大但不需要每 tick 执行的操作，
     * 例如检查建筑结构是否有效。
     * <p>
     * Executes the scheduled task. The task is placed in a global queue and may be executed asynchronously.
     * Therefore, the task should be an expensive but non-tick-critical operation,
     * such as checking whether a building structure is valid.
     */
    void executeTask();

    /**
     * 检查此方块实体是否仍然有效。如果返回 false，任务将从调度队列中移除。
     * <p>
     * Checks whether this block entity is still valid. If false is returned, the task will be removed from the scheduler queue.
     *
     * @return 如果方块实体仍然有效则返回 true / true if the block entity is still valid
     */
    boolean isStillValid();
}
