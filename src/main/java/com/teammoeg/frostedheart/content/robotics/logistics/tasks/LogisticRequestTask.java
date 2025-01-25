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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LogisticRequestTask implements LogisticTask {
	ItemStack filter;
	int size;
	boolean fetchNBT;
	BlockEntity storage;

	public LogisticRequestTask(ItemStack filter, int size, boolean fetchNBT, BlockEntity storage) {
		super();
		this.filter = filter;
		this.size = size;
		this.fetchNBT = fetchNBT;
		this.storage = storage;
	}

	public ItemStack fetch(LogisticNetwork network,int msize) {
		ItemStack rets= network.fetchItem(filter, fetchNBT, Math.min(msize, size));
		size-=rets.getCount();
		return rets;
	}

	@Override
	public void work(LogisticNetwork network,int msize) {
		int rets= network.fetchItemInto(filter, network.getStorage(storage.getBlockPos()).getInventory(), fetchNBT, Math.min(msize, size));
		size-=rets;
	}

}
