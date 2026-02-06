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

import com.teammoeg.frostedheart.content.town.worker.NopWorkerState;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Lowest level town processing function.
 * <p>
 *      It specifies five stages of work during a tick, and
 *      it is recommended to follow the instructions about
 *      how to use each stage.
 * </p>
 */
@FunctionalInterface
public interface TownWorker<T extends WorkerState> {

    /**
     * Empty work, don't do anything.
     * Just used to avoid null pointer exception.
     */
    public static TownWorker EMPTY = (town, workData, workOrder)->true;



    /**
     * Work during tick
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    boolean work(Town town, T workData,WorkOrder workOrder);
    
    default WorkerState createState() {
    	return new NopWorkerState();
    };
    default void onRemoved(ServerLevel level,WorkerState state,BlockPos pos) {
    	
    }

}
