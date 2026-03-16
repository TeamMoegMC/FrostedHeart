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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teammoeg.chorda.ChordaConfig;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/**
 * 调度任务队列，每 tick 限制执行的任务数量以优化性能。
 * 每个维度维护独立的队列，任务以循环方式执行。
 * 当每 tick 任务数不是整数时，使用小数累加器来决定何时额外执行一个任务。
 * <p>
 * Scheduler task queue that limits the number of tasks executed per tick for performance optimization.
 * Each dimension maintains its own independent queue, and tasks are executed in a round-robin fashion.
 * When the tasks-per-tick value is not an integer, a fractional accumulator is used to determine when to execute an extra task.
 */
public class SchedulerQueue {
    static Map<ResourceKey<Level>, SchedulerQueue> queues = new HashMap<>();
    ArrayList<ScheduledData> tasks = new ArrayList<>();

    int lastpos;
    double tasksPerTick = ChordaConfig.SERVER.taskPerTick.get();
    double tasksPerTickCounter = 0;//if taskPerTick is not integer, use this to decide when to execute an extra task

    /**
     * 将方块实体添加到其所在维度的调度队列中。
     * <p>
     * Adds a block entity to the scheduler queue of its dimension.
     *
     * @param te 要添加的方块实体 / the block entity to add
     */
    public static void add(BlockEntity te) {
        queues.computeIfAbsent(te.getLevel().dimension(), e -> new SchedulerQueue())
                .add(te.getBlockPos());

    }

    /**
     * 执行指定世界的调度队列的一次 tick。
     * <p>
     * Ticks the scheduler queue for the specified world.
     *
     * @param serverWorld 服务器世界 / the server world
     */
    public static void tickAll(ServerLevel serverWorld) {
        SchedulerQueue q = queues.get(serverWorld.dimension());
        if (q != null)
            q.tick(serverWorld);
    }

    /**
     * 将指定位置的任务添加到队列中。如果该位置已存在任务，则不重复添加。
     * <p>
     * Adds a task at the specified position to the queue. Does not add duplicates if a task at that position already exists.
     *
     * @param pos 方块位置 / the block position
     */
    public void add(BlockPos pos) {
        for (ScheduledData task : tasks) {
            if (task.pos.equals(pos)) {
                return;
            }
        }
        tasks.add(new ScheduledData(pos));
    }

    /**
     * 从队列中移除指定位置的任务。
     * <p>
     * Removes the task at the specified position from the queue.
     *
     * @param pos 方块位置 / the block position
     */
    public void remove(BlockPos pos) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).pos.equals(pos)) {
                if (i < lastpos) {
                    lastpos--;
                }
                tasks.remove(i);
                return;
            }
        }
    }

    /**
     * 执行一次 tick 的任务调度。根据配置的每 tick 任务数限制，
     * 从上次停止的位置开始循环执行任务，并清理无效任务。
     * <p>
     * Performs one tick of task scheduling. Based on the configured tasks-per-tick limit,
     * executes tasks in round-robin order starting from where the last tick stopped, and cleans up invalid tasks.
     *
     * @param world 服务器世界 / the server world
     */
    public void tick(ServerLevel world) {
        //count count of tasks
        int taskNum = (int) tasksPerTick;
        double fracNum = Mth.frac(tasksPerTick);
        tasksPerTickCounter += fracNum;
        if (tasksPerTickCounter >= 1) {
            taskNum++;
            tasksPerTickCounter -= 1;
        }
        if (tasks.isEmpty()) return;
        //run tasks
        int curpos = lastpos;
        while (taskNum > 0) {
            ScheduledData data = tasks.get(curpos);
            BlockEntity te = CUtils.getExistingTileEntity(world, data.pos);
            if (te instanceof ScheduledTaskTileEntity ste&&ste.isStillValid()) {
                ste.executeTask();
            } else {
                data.forRemoval = true;
            }
            taskNum--;
            curpos++;
            if (curpos >= tasks.size()) {
                curpos = 0;
            }
            if (curpos == lastpos) break;
        }
        lastpos = curpos;
        //remove invalid tasks
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).forRemoval) {
                if (i < lastpos) {
                    lastpos--;
                }
                tasks.remove(i);
                i--;
            }
        }
    }
}
