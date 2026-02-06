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

package com.teammoeg.frostedheart.content.town.mine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MineBaseWorker implements TownWorker<MineBaseState> {
	public static final MineBaseWorker INSTANCE=new MineBaseWorker();
	MineBaseWorker() {
	}

	@Override
	public boolean work(Town town, MineBaseState workData, WorkOrder workOrder) {
    	if(workOrder!=WorkOrder.NORMAL)return false;
    	 if(!workData.status.isValid()){
    	        return false;
    	    }
    	 if(workData.biomePath==null)return false;
        if (town instanceof TeamTown teamTown) {
            double efficiency=0;
            for(UUID u:workData.getResidents()) {
            	Resident resident=teamTown.getResident(u).orElse(null);
            	if(resident==null)continue;
            	double cureff=getResidentEfficiency(resident);
            	if(cureff<=0)continue;
            	efficiency+=cureff;
            	resident.onWork(town,TownWorkerType.MINE_BASE,workData);
            }
            final double picked=teamTown.maypickTerrainResource(TerrainResourceType.ORE, efficiency);
            IActionExecutorHandler itemResourceExecutor=teamTown.getActionExecutorHandler();
            Map<Item, Integer> weights=getWeights(workData.biomePath);
            int totalWeight=weights.values().stream().reduce(0, (a,b)->a+b);
            weights.entrySet().stream().map(entry -> new TownResourceActions.ItemResourceAction
                        (new ItemStack(entry.getKey()), ResourceActionType.ADD, picked * entry.getValue() / totalWeight, ResourceActionMode.ATTEMPT))
                .forEach(itemResourceExecutor::execute)
                ;
            teamTown.pickTerrainResource(TerrainResourceType.ORE, efficiency);

            return true;
        }
        return false;
	}

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
	public double getResidentEfficiency(Resident r) {
		return 0.2f*TownWorkerType.MINE_BASE.getResidentScore(r);
	}
}
