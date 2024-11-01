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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHBlocks;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseBlockEntity;
import com.teammoeg.frostedheart.content.town.mine.MineWorker;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseWorker;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;

import static java.lang.Double.NEGATIVE_INFINITY;

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
    HOUSE(FHBlocks.HOUSE, (town, workData) -> {
        double residentNum = workData.getCompound("tileEntity").getList("residents", 10).size();
        double actualCost = town.cost(TownResourceType.PREP_FOOD, residentNum, false);
        return Math.abs(residentNum - actualCost) < 0.001;
    }, 0),
    WAREHOUSE(FHBlocks.WAREHOUSE, new WarehouseWorker(), 0),
    MINE(FHBlocks.MINE, new MineWorker(), 0, true,
            (currentResidentNum, nbt) -> {
        int maxResident = nbt.getCompound("tileEntity").getInt("maxResident");
        double rating = TownWorkerData.getRating(nbt);
        if(currentResidentNum < maxResident) return -currentResidentNum + 1.0 * currentResidentNum / maxResident + 0.4/*the base priority of workerRype*/ + rating;
        return NEGATIVE_INFINITY;
            },
            (resident) -> resident.getTrust() * 0.01),
    MINE_BASE(FHBlocks.MINE_BASE, null, 0),
    HUNTING_CAMP(FHBlocks.HUNTING_CAMP, null, 0),
    HUNTING_BASE(FHBlocks.HUNTING_BASE, new HuntingBaseBlockEntity.HuntingBaseWorker(), -1, true, (currentResidentNum, nbt) -> {
        int maxResident = nbt.getCompound("tileEntity").getInt("maxResident");
        if(currentResidentNum < maxResident) return -currentResidentNum + 1.0 * currentResidentNum / maxResident + 0.5 + TownWorkerData.getRating(nbt);
        return Double.NEGATIVE_INFINITY;
    }, (resident) -> resident.getTrust() * 0.01 * Resident.CalculatingFunction1(resident.getSocial()))
    ;

    public static final Codec<TownWorkerType> CODEC= Codec.STRING.xmap(TownWorkerType::from,TownWorkerType::getKey);

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
     * Whether this worker needs resident to work.
     */
    private final boolean needsResident;

    /**
     * The function used when assigning work for resident.
     * 每有一个居民在此工作，优先级应减少1左右，这样可以使居民尽可能均匀地分配在所有工作方块中
     * 当居民数量大于最大居民数时，应该返回Double.NEGATIVE_INFINITY
     * input1 当前居民数量。
     * input2 完整的workerData，包括town部分和tileEntity部分。详见TownWorkerData。
     * output 分配居民时的优先级
     */
    private BiFunction<Integer, CompoundTag, Double> residentPriorityFunction;

    /**
     * Extra score besides proficiency and health
     */
    private Function<Resident, Double> residentExtraScoreFunction;

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
        this.needsResident = false;
    }

    //if needsResident is true, residentPriorityFunction must be set
    TownWorkerType(Supplier<Block> workerBlock, TownWorker worker, int internalPriority, boolean needsResident, BiFunction<Integer, CompoundTag, Double> residentPriorityFunction, Function<Resident, Double> residentExtraScoreFunction) {
        this.block = workerBlock;
        this.worker = worker;
        this.priority = internalPriority;
        this.needsResident = needsResident;
        this.residentPriorityFunction = residentPriorityFunction;
        this.residentExtraScoreFunction = residentExtraScoreFunction;
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
     * 为居民分配工作时，此优先级高者优先分配居民，以此分数判断居民质量
     */
    public double getResidentExtraScore(Resident resident) {
        return residentExtraScoreFunction.apply(resident);
    }

    /**
     * 为居民分配工作时，此优先级高者优先分配居民
     *
     * @param data TownWorkerData中的workData
     */
    public double getResidentPriority(TownWorkerData data) {
        return this.getResidentPriorityFunction().apply(data.getResidents().size(), data.getWorkData());
    }

    public double getResidentPriority(Integer integer, CompoundTag nbt){
        return this.getResidentPriorityFunction().apply(integer, nbt);
    }


    /**
     * Gets the worker.
     *
     * @return the worker
     */
    public TownWorker getWorker() {
        return worker;
    }

    public boolean needsResident() {
        return needsResident;
    }

    public BiFunction<Integer, CompoundTag, Double> getResidentPriorityFunction() {
        if(this.needsResident){
            return residentPriorityFunction;
        } else{
            FHMain.LOGGER.error("This TownWorkerType don't need resident, but tried get residentPriorityFunction");
            return ((useless1, useless2) -> NEGATIVE_INFINITY);
        }
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
