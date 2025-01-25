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

package com.teammoeg.frostedheart.content.robotics.logistics;

import com.teammoeg.frostedheart.content.robotics.logistics.workers.ILogisticsStorage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.ItemHandlerHelper;

public class LogisticEnvolop {
	ItemStack stack;
	BlockPos pos;
	ILogisticsStorage storage;
	int ticks;
	public LogisticEnvolop(ItemStack stack, BlockPos pos, ILogisticsStorage storage,int maxtime) {
		super();
		this.stack = stack;
		this.pos = pos;
		this.storage = storage;
		this.ticks=maxtime;
	}
	public boolean tick() {
		ticks--;
		if(ticks>0) {
			return false;
		}else {
			complete();
			return true;
		}
	}
	public void complete() {
		ItemHandlerHelper.insertItem(storage.getInventory(), stack, false);
	}

}
