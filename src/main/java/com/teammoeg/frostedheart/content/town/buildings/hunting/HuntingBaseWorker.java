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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class HuntingBaseWorker implements TownWorker<HuntingBaseState> {
    private HuntingBaseWorker(){
        super();
    }
    public static final HuntingBaseWorker INSTANCE = new HuntingBaseWorker();
	@Override
	public boolean work(Town town, HuntingBaseState workData, WorkOrder workOrder) {
    	if(workOrder!=WorkOrder.NORMAL)return false;
        if (town instanceof TeamTown teamTown) {
            double efficiency=0;
            for(UUID u:workData.getResidents()) {
            	Resident resident=teamTown.getResident(u).orElse(null);
            	if(resident==null)continue;
            	double cureff=getResidentEfficiency(resident);
            	if(cureff<=0)continue;
            	efficiency+=cureff;
            	resident.onWork(town,TownWorkerType.HUNTING_BASE,workData);
            }
            double picked=teamTown.maypickTerrainResource(TerrainResourceType.HUNT, efficiency*2);
            
            TownResourceActionResults.ItemResourceActionResult result = (TownResourceActionResults.ItemResourceActionResult) town
                    .getActionExecutorHandler()
                    .execute(new TownResourceActions.ItemResourceAction(new ItemStack(Items.BEEF), ResourceActionType.ADD, picked, ResourceActionMode.MAXIMIZE));
            teamTown.pickTerrainResource(TerrainResourceType.HUNT, result.modifiedAmount());

            return true;
        }
        return false;
    }
	@Override
	public WorkerState createState() {
		return new HuntingBaseState();
	}
	public double getResidentEfficiency(Resident r) {
		return 0.2f*TownWorkerType.HUNTING_BASE.getResidentScore(r);
	}


}
