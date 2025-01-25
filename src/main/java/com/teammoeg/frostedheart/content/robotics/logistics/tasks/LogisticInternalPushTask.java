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

package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticSlot;

import net.minecraft.world.level.block.entity.BlockEntity;

public class LogisticInternalPushTask implements LogisticTask {
	BlockEntity tile;
	int slot;
	LogisticSlot from;



	public LogisticInternalPushTask(BlockEntity tile, int slot) {
		super();
		this.tile = tile;
		this.slot = slot;
	}



	@Override
	public void work(LogisticNetwork network, int msize) {
		if(from==null) {
			from=new LogisticSlot(network.getStorage(tile.getBlockPos()),slot);
		}
		network.importTransit(from, msize);

	}

}
