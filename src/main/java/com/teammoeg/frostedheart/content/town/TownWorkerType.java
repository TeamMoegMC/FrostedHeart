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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.house.HouseWorker;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseResidentHandler;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseWorker;
import com.teammoeg.frostedheart.content.town.mine.MineResidentHandler;
import com.teammoeg.frostedheart.content.town.mine.MineWorker;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseWorker;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

import lombok.Getter;
import net.minecraft.world.level.block.Block;

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
    HOUSE(FHBlocks.HOUSE::get, HouseWorker.INSTANCE, 0),
    WAREHOUSE(FHBlocks.WAREHOUSE::get, new WarehouseWorker(), 0),
    //MINE(FHBlocks.MINE::get, MineWorker.INSTANCE, 0, MineResidentHandler.INSTANCE),
    MINE_BASE(FHBlocks.MINE_BASE::get, TownWorker.EMPTY, 0),
    //HUNTING_CAMP(FHBlocks.HUNTING_CAMP::get, TownWorker.EMPTY, 0),
    HUNTING_BASE(FHBlocks.HUNTING_BASE::get, HuntingBaseWorker.INSTANCE, -1,  HuntingBaseResidentHandler.INSTANCE);

    public static final Codec<TownWorkerType> CODEC= Codec.STRING.xmap(TownWorkerType::from,TownWorkerType::getKey);

    /**
     * Town block.
     */
    private final Supplier<Block> block;

    /**
     * The worker.
     * -- GETTER --
     *  Gets the worker.
     */
    @Getter
    private final TownWorker worker;

    /**
     * The priority.
     * -- GETTER --
     *  Gets the priority.
     */
    @Getter
    private final int priority;

    /**
     * The resident handler.
     * <br>
     * 包括工作方块分配居民的优先级函数，以及对于某个工作方块居民的评分函数
     */
    private final WorkerResidentHandler residentHandler;

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
        this.residentHandler = WorkerResidentHandler.DUMMY;
    }

    //if needsResident is true, residentPriorityFunction must be set
    TownWorkerType(Supplier<Block> workerBlock, TownWorker worker, int internalPriority, WorkerResidentHandler residentHandler) {
        this.block = workerBlock;
        this.worker = worker;
        this.priority = internalPriority;
        this.residentHandler = residentHandler;
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
     * 为居民分配工作时，此优先级高者优先分配居民
     *
     * @param data TownWorkerData中的workData
     */
    public double getResidentPriority(TownWorkerData data) {
        return this.getResidentHandler().getResidentPriority(data);
    }

    public double getResidentPriority(int residentNum, TownWorkerData data){
        return this.getResidentHandler().getResidentPriority(data, residentNum);
    }

    public double getResidentScore(Resident resident) {
        return this.getResidentHandler().getResidentScore(resident);
    }

    /**
     * 这种工作是否需要居民
     * 特别的，对于房屋，它会返回false，因为尽管房屋也有居民居住，但它并不算一个工作，也不参与工作分配。
     */
    public boolean needsResident() {
        return residentHandler != WorkerResidentHandler.DUMMY;
    }

    public WorkerResidentHandler getResidentHandler() {
        if(residentHandler==WorkerResidentHandler.DUMMY){
            throw new IllegalStateException("TownWorkerType " + this.name() + " does not have a resident handler!");
        }
        return residentHandler;
    }


    /**
     * Gets key
     */
    public String getKey() {
        return name().toLowerCase();
    }

    public static TownWorkerType from(String key) {
        return valueOf(key.toUpperCase());
    }

}
