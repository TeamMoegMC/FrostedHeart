/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.base.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHConfig;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class SchedulerQueue {
    static Map<ResourceKey<Level>, SchedulerQueue> queues = new HashMap<>();
    ArrayList<ScheduledData> tasks = new ArrayList<>();

    int lastpos;
    double tasksPerTick = FHConfig.SERVER.taskPerTick.get();
    double tasksPerTickCounter = 0;//if taskPerTick is not integer, use this to decide when to execute an extra task

    public static void add(BlockEntity te) {
        queues.computeIfAbsent(te.getLevel().dimension(), e -> new SchedulerQueue())
                .add(te.getBlockPos());

    }

    public static void tickAll(ServerLevel serverWorld) {
        SchedulerQueue q = queues.get(serverWorld.dimension());
        if (q != null)
            q.tick(serverWorld);
    }

    public void add(BlockPos pos) {
        for (ScheduledData task : tasks) {
            if (task.pos.equals(pos)) {
                return;
            }
        }
        tasks.add(new ScheduledData(pos));
    }

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
            BlockEntity te = Utils.getExistingTileEntity(world, data.pos);
            if ((te instanceof ScheduledTaskTileEntity)) {
                ((ScheduledTaskTileEntity) te).executeTask();
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
