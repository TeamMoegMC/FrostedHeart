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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

import static com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockEntity.isValid;

public class MineWorker implements TownWorker {
    public static final MineWorker INSTANCE=new MineWorker();

    public static final Map<ResourceLocation, Map<Item,  Integer>> BIOME_RESOURCES = new HashMap<>();
    public static final Map<Item, Integer> DEFAULT_RESOURCES = Map.of(Items.COBBLESTONE, 1);
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


    @Override
    public boolean work(Town town, CompoundTag workData) {
        if(!isValid(workData)){
            return false;
        }
        if(town instanceof ITownWithResidents townWithResidents){
            CompoundTag dataTE = workData.getCompound("tileEntity");
            CompoundTag dataTown = workData.getCompound("town");
            if(!dataTown.contains("linkedBasePos")){//检查MineBase是否存在
                return false;
            } else{
                if(town instanceof ITownWithBlocks townWithBlocks){
                    BlockPos linkedBasePos = BlockPos.of(dataTown.getLong("linkedBasePos"));
                    Optional<TownWorkerData> mineBaseData = townWithBlocks.getTownBlock(linkedBasePos);
                    if(mineBaseData.isEmpty()){
                        return false;
                    } else {
                        if(mineBaseData.get().getType() != TownWorkerType.MINE_BASE){
                            return false;
                        }
                    }
                } else {
                    return false;//一般来说这是不可能的，怎么会有城镇没有ITownWithBlocks接口，但是存了一个城镇矿场而且还在工作的？
                }
            }
            double rating = dataTE.getDouble("rating");
            long lastSyncedWorkID = dataTE.getLong("lastSyncedWorkID");
            long latestWorkID = dataTown.getLong("latestWorkID");
            double chunkResourceReservesCost = dataTown.getDouble("chunkResourceReservesCost");
            double chunkResourceReserves = dataTE.getDouble("chunkResourceReserves");
            if(lastSyncedWorkID == latestWorkID){
                chunkResourceReservesCost = 0;
            }
            if(chunkResourceReservesCost > 0){
                chunkResourceReserves = chunkResourceReserves - chunkResourceReservesCost;
                if(chunkResourceReserves <= 0){
                    return false;
                }
            }
            ResourceLocation biomeLocation = ResourceLocation.tryParse(dataTE.getString("biome"));
            List<Resident> residents = workData.getCompound("town").getList("residents", Tag.TAG_STRING)
                    .stream()
                    .map(nbt -> UUID.fromString(nbt.getAsString()))
                    .map(townWithResidents::getResident)
                    .map(optional -> optional.orElse(null))
                    .filter(Objects::nonNull)
                    .toList();

            IActionExecutorHandler executorHandler = townWithResidents.getActionExecutorHandler();
            ITownResourceActionExecutor<TownResourceActions.ItemResourceAction> itemResourceExecutor = executorHandler.getExecutor(TownResourceActions.ItemResourceAction.class);
            Map<Item,  Integer> weights = getWeights(biomeLocation);
            int totalWeight = weights.values().stream().mapToInt(weight -> weight).sum();
            for(Resident resident : residents){
                double score = TownWorkerType.MINE.getResidentScore( resident );
                double finalChunkResourceReserves = chunkResourceReserves;
                List<TownResourceActionResults.ItemResourceActionResult> results = weights.entrySet().stream()
                        .map(entry -> new TownResourceActions.ItemResourceAction
                                (new ItemStack(entry.getKey()), ResourceActionType.ADD, Math.sqrt(finalChunkResourceReserves) * rating * score * entry.getValue() / totalWeight, ResourceActionMode.ATTEMPT))
                        .map(itemResourceExecutor::execute)
                        .map(result -> (TownResourceActionResults.ItemResourceActionResult) result)
                        .toList();
                resident.addStrength(20 / resident.getStrength());
                chunkResourceReservesCost += 0.0005 * chunkResourceReserves * rating * score;//至少2000人*天会挖空？
            }
            WORK_ID.add(1);
            latestWorkID = WORK_ID.longValue();
            dataTown.putLong("latestWorkID", latestWorkID);
            dataTown.putDouble("chunkResourceReservesCost", chunkResourceReservesCost);
            workData.put("town", dataTown);
            return true;
        }
        return false;
    }

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

}