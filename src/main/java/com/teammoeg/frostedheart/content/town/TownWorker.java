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

package com.teammoeg.frostedheart.content.town;

import net.minecraft.nbt.CompoundNBT;

/**
 * Lowest level town processing function.
 * <p>
 *      It specifies five stages of work during a tick, and
 *      it is recommended to follow the instructions about
 *      how to use each stage.
 * </p>
 */
@FunctionalInterface
public interface TownWorker {

    /**
     * Work with highest priority
     * It's recommended that this work should add service from block data or constant but not from resources.
     * This work should NOT provide resource or cost resource.
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean firstWork(Town town, CompoundNBT workData) {
        return true;
    }

    /**
     * Work with higher priority;
     * It's recommended that this work should provide service for other work with resource cost.
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean beforeWork(Town town, CompoundNBT workData) {
        return true;
    }

    ;

    /**
     * Work during tick
     * It's recommended that most of the jobs are done during this.
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    boolean work(Town town, CompoundNBT workData);

    ;

    /**
     * Work with lower priority;
     * It's recommended that this job recycles resource.
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean afterWork(Town town, CompoundNBT workData) {
        return true;
    }

    ;

    /**
     * Work with lowest priority
     * It's recommended that this job recycles services
     *
     * @param town the town
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean lastWork(Town town, CompoundNBT workData) {
        return true;
    }

    ;
}
