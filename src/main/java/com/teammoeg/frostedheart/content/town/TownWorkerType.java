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

import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseTileEntity;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

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
    }, 0),
    MINE_BASE(FHBlocks.mine_base, null, 0),
    HUNTING_CAMP(FHBlocks.hunting_camp, null, 0),
    HUNTING_BASE(FHBlocks.hunting_base, new HuntingBaseTileEntity.HuntingBaseWorker(), 0)
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
