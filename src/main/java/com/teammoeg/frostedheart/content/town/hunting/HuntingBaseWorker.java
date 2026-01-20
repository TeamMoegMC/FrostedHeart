package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
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
