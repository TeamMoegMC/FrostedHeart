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

package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class MineWorker implements TownWorker<MineState> {
    public static final MineWorker INSTANCE=new MineWorker();

    public static final Map<ResourceLocation, Map<Item,  Integer>> BIOME_RESOURCES = new HashMap<>();
    public static final Map<Item, Integer> DEFAULT_RESOURCES = Map.of(Items.COBBLESTONE, 1);
    private static void loadBiomeResources() {
        for(BiomeMineResourceRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), BiomeMineResourceRecipe.TYPE)){
            ResourceLocation biomeID = recipe.biomeID;
            Map<Item, Integer> weights = recipe.weights;
            BIOME_RESOURCES.put(biomeID, weights);
        }
    }

    public static Map<Item, Integer> getWeights(ResourceLocation biomeID){
        if(BIOME_RESOURCES.isEmpty()){
            loadBiomeResources();
        }
        if(BIOME_RESOURCES.containsKey(biomeID)){
            return BIOME_RESOURCES.get(biomeID);
        }
        return DEFAULT_RESOURCES;
    }

    /**
     * 表示当前chunkResourceReservesCost来源于哪次工作。
     * 用于在Worker和BlockEntity之间传输数据时，检查来自worker的chunkResourceReservesCost是否已经被BlockEntity同步到区块数据。
     * 以及在Worker工作的时候，检查上次工作时产生的chunkResourceReservesCost是否已经被同步。
     * <br>
     * 当worker工作时，chunkResourceReservesCost发生变化，将一个新的workerID与chunkResourceReservesCost一并储存进workData。
     * 随后，当城镇与BlockEntity进行数据交互时，新的workerID会BlockEntity读取和记录。
     * BlockEntity会将记录到的chunkResourceReservesCost存进区块里，然后清空chunkResourceReservesCost，将lastSyncedWorkID更新为最新收到的workID。
     * BlockEntity会将lastSyncedWorkID发送到城镇，城镇会记录该lastSyncedWorkID。
     * <br>
     * 当worker发现lasySyncedWorkID与latestWorkID一致，则说明数据已经同步到区块，就会重置chunkResourceReservesCost为0。
     * 城镇与BlockEntity进行数据交互时，BlockEntity会自己检查该workerID是否与BlockEntity上次一致。如果一致，说明这次数据已经更新到区块，chunkResourceReservesCost保持为0不更新。
     */
    public static final LongAdder WORK_ID = new LongAdder();

    private MineWorker(){}

    /**
     * 执行矿山工作逻辑
     * 
     * @param town 城镇对象，需要实现ITownWithResidents接口以获取居民信息
     * @param workData 工作数据，包含矿场相关NBT数据
     * @return 工作是否成功执行
     */
	@Override
	public boolean work(Town town, MineState workData, WorkOrder workOrder) {
		if(workOrder!=WorkOrder.NORMAL)return false;
        // 检查城镇方块能否工作
        if(!workData.status.isValid()){
            return false;
        }
        if(town instanceof ITownWithResidents townWithResidents){

            return true;
        }
        return false;
    }





}