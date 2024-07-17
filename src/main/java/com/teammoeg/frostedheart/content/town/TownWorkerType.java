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

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHBlocks;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseTileEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

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
    HOUSE(FHBlocks.house, (town, workData) -> {
        double residentNum = workData.getList("residents", 10).size();
        double actualCost = town.cost(TownResourceType.PREP_FOOD, residentNum, false);
        return Math.abs(residentNum - actualCost) < 0.001;
    }, 0),
    WAREHOUSE(FHBlocks.warehouse, null, 0),
    MINE(FHBlocks.mine, (town, workData) -> {
        double rating = workData.getDouble("rating");
        ListNBT list = workData.getList("resources", Constants.NBT.TAG_COMPOUND);
        EnumMap<TownResourceType, Double> resources = new EnumMap<>(TownResourceType.class);
        list.forEach(nbt -> {
            CompoundNBT nbt_1 = (CompoundNBT) nbt;
            String key = nbt_1.getString("type");
            double amount = nbt_1.getDouble("amount");
            resources.put(TownResourceType.from(key), amount);
        });
        Random random = new Random();
        int add = rating > random.nextFloat() * 10 ? 1 : 0;
        double randomDouble = random.nextDouble();
        double counter = 0;
        for(Map.Entry<TownResourceType, Double> entry : resources.entrySet()){
            counter += entry.getValue();
            if(counter >= randomDouble){
                double actualAdd = town.add(entry.getKey(), add, false);
                return Math.abs(add - actualAdd) < 0.001;
            }
        }
        return true;
    }, 0, true,
            (currentResidentNum, workerData) -> {
        if(currentResidentNum < workerData.getInt("maxResident")) return -currentResidentNum + 1.0 * currentResidentNum / workerData.getInt("maxResident") + 0.4/*the base priority of workerRype*/ + workerData.getDouble("rating");
        return NEGATIVE_INFINITY;
            },
            (resident) -> resident.getTrust() * 0.01),
    MINE_BASE(FHBlocks.mine_base, null, 0),
    HUNTING_CAMP(FHBlocks.hunting_camp, null, 0),
    HUNTING_BASE(FHBlocks.hunting_base, new HuntingBaseTileEntity.HuntingBaseWorker(), -1, true, (currentResidentNum, workerData) -> {
        if(currentResidentNum < workerData.getInt("maxResident")) return -currentResidentNum + 1.0 * currentResidentNum / workerData.getInt("maxResidnet" + 0.5 + workerData.getDouble("rating"));
        return Double.NEGATIVE_INFINITY;
    }, (resident) -> resident.getTrust() * 0.01 * Resident.CalculatingFunction1(resident.getSocial()))
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
     * Whether this worker needs resident to work.
     */
    private final boolean needsResident;

    /**
     * The function used when assigning work for resident.
     * input number : current resident number
     * CompoundNBT: worker data
     * output number: priority
     * 每有一个居民在此工作，优先级应减少1左右，这样可以使居民尽可能均匀地分配在所有工作方块中
     * 当居民数量大于最大居民数时，应该返回Double.NEGATIVE_INFINITY
     */
    private BiFunction<Integer, CompoundNBT, Double> residentPriorityFunction;

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
        this(workerBlock, worker, internalPriority, false);
    }

    //若needsResident==false，可使用上方的方法，若needsResident==true，则应使用下方的方法
    @Deprecated
    TownWorkerType(Supplier<Block> workerBlock, TownWorker worker, int internalPriority, boolean needsResident) {
        this.block = workerBlock;
        this.worker = worker;
        this.priority = internalPriority;
        this.needsResident = needsResident;
    }

    //if needsResident is true, residentPriorityFunction must be set
    TownWorkerType(Supplier<Block> workerBlock, TownWorker worker, int internalPriority, boolean needsResident, BiFunction<Integer, CompoundNBT, Double> residentPriorityFunction, Function<Resident, Double> residentExtraScoreFunction) {
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
    public double getResidentPriority(Integer currentResident, CompoundNBT data) {
        return this.residentPriorityFunction.apply(currentResident, data);
    }

    public double getResidentPriority(Integer currentResident, TownWorkerData data) {
        return this.getResidentPriority(currentResident, data.getWorkData());
    }
    public double getResidentPriority(Collection<?> currentResidents, TownWorkerData data) {
        return this.getResidentPriority(currentResidents.size(), data.getWorkData());
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

    public BiFunction<Integer, CompoundNBT, Double> getResidentPriorityFunction() {
        if(this.needsResident){
            return residentPriorityFunction;
        } else{
            FHMain.LOGGER.error("This TownWorkerType don't need resident, but tried get residentPriorityFunction");
            return ((a, b) -> NEGATIVE_INFINITY);
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
