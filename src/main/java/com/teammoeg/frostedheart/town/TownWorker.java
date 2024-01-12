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

package com.teammoeg.frostedheart.town;

import net.minecraft.nbt.CompoundNBT;

/**
 * A town processing function.
 */
@FunctionalInterface
public interface TownWorker {
    /**
     * Work with highest priority
     * It's recommended that this work should add service from block data or constant but not from resources.
     * This work should NOT provide resource or cost resource.
     *
     * @param resource the resource
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean firstWork(Town resource, CompoundNBT workData) {
        return true;
    }

    ;

    /**
     * Work with higher priority;
     * It's recommended that this work should provide service for other work with resource cost.
     *
     * @param resource the resource
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean beforeWork(Town resource, CompoundNBT workData) {
        return true;
    }

    ;

    /**
     * Work during tick
     * It's recommended that most of jobs are done during this.
     *
     * @param resource the resource
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    boolean work(Town resource, CompoundNBT workData);

    /**
     * Work with lower priority;
     * It's recommended that this job recycles resource.
     *
     * @param resource the resource
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean afterWork(Town resource, CompoundNBT workData) {
        return true;
    }

    ;

    /**
     * Work with lowest priority
     * It's recommended that this job recycles services
     *
     * @param resource the resource
     * @param workData workData provided by work type
     * @return true, if work done successfully
     */
    default boolean lastWork(Town resource, CompoundNBT workData) {
        return true;
    }

    ;
}
