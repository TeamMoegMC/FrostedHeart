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

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.house.HouseState;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

public class WarehouseWorker implements TownWorker<WareHouseState> {
	@Override
	public boolean work(Town town, WareHouseState workData, WorkOrder workOrder) {
		if(workOrder==WorkOrder.FIRST) {
			double capacity = workData.capacity;
	        //town.getResourceManager().addIfHaveCapacity(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity);
	        TownResourceActions.VirtualResourceAttributeAction action = new TownResourceActions.VirtualResourceAttributeAction(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity, ResourceActionType.ADD, ResourceActionMode.ATTEMPT);
	        town.getActionExecutorHandler().execute(action);
	        return true;
		}
		return false;
	}
	@Override
	public WorkerState createState() {
		return new WareHouseState();
	}

}
