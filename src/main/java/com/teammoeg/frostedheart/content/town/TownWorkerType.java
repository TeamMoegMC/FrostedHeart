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

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHBlocks;

import net.minecraft.block.Block;

import static java.lang.Math.abs;

/**
 * The second-lowest level town processing function.
 * <p>
 * Each TownWorkerType associates a block with a TownWorker.
 * <p>
 * For example, a HouseBlock is associated with a TownWorker functions
 * that cost food and add service.
 * <p>
 * Normally, you should add a new TownWorkerType for each new block type.
 */
public enum TownWorkerType {

    /**
     * The dummy.
     */
    DUMMY(null, null, -1),
    HOUSE(FHBlocks.house, (town, workData) -> {
        double residentNum = workData.getList("residents", 10).size();
        double actualCost = town.cost(TownResourceType.PREP_FOOD, residentNum, false);
        return Math.abs(residentNum - actualCost) < 0.01;
    }, 0),
    WAREHOUSE(FHBlocks.warehouse, null, 0),
    MINE(FHBlocks.mine, (town, workDate) -> {
        double add = 1;
        double actualAdd = town.add(TownResourceType.STONE, add, false);/*日后再说*/
        return add == actualAdd;
    }, 0),
    MINE_BASE(FHBlocks.mine_base, null, 0)//日后再说
    ;

    /**
     * Town block.
     */
    private final Supplier<Block> block;

    /**
     * The worker.
     */
    private final TownWorker worker;

    /**
     * The priority.
     */
    private final int priority;

    /**
     * Instantiates a new town worker type.
     *
     * @param workerBlock      the worker block
     * @param worker           the worker
     * @param internalPriority the internal priority
     */
    TownWorkerType(Supplier<Block> workerBlock, TownWorker worker, int internalPriority) {
        this.block = workerBlock;
        this.worker = worker;
        this.priority = internalPriority;
    }

    /**
     * Gets the block.
     *
     * @return the block
     */
    public Block getBlock() {
        return block==null?null:block.get();
    }

    /**
     * Gets the priority.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the worker.
     *
     * @return the worker
     */
    public TownWorker getWorker() {
        return worker;
    }


}
