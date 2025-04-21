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
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.GridAndAmount;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.IGridElement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class LogisticPushTask extends LogisticTask {
	
	/**
	 * The origin slot and handler, the task takes item from this chest if required
	 * initial task for the task
	 * */
	int fromSlot;
	LazyOptional<IItemHandler> handler;
	/**
	 * position of from and to pos, for rendering purpose
	 * */
	BlockPos origin;
	BlockPos targetPos;

	/**
	 * Carried stack in this task, should drop if task canceled
	 * prepare stage:prepare
	 * use stage:work
	 * */
	ItemStack stack;
	/**
	 * ItemKey to put
	 * prepare stage:prepare
	 * use stage:work
	 * */
	ItemKey key;
	/**
	 * Grid element to put
	 * prepare stage:prepare
	 * use stage:work
	 * */
	LazyOptional<IGridElement> target;

	public LogisticPushTask(BlockPos origin, BlockPos targetPos, ItemStack stack, ItemKey key, LazyOptional<IGridElement> target) {
		super();
		this.origin = origin;
		this.targetPos = targetPos;
		this.stack = stack;
		this.key = key;
		this.target = target;
		this.ticks=20;
	}

	public LogisticPushTask(BlockPos pos,LazyOptional<IItemHandler> handler, int fromSlot) {
		super();
		this.handler = handler;
		this.fromSlot = fromSlot;
		this.origin=pos;
	}

	@Override
	public LogisticTask work(LogisticNetwork network) {
		stack=network.getHub().pushItem(target, key, stack);
		if(!stack.isEmpty()) {
			GridAndAmount gaa=network.getHub().findGridForPlace(key, stack);
			target=gaa.grid();
			origin=targetPos;
			targetPos=target.resolve().get().getPos();
			ticks=20;
			return null;
		}
		return this;
	}

	@Override
	public boolean isStillValid() {
		return handler.isPresent()&&target==null;
	}

	@Override
	public LogisticTask prepare(LogisticNetwork network) {
		if(!handler.isPresent())
			return null;
		IItemHandler itemHandler=handler.resolve().get();
		stack=itemHandler.getStackInSlot(fromSlot);
		key=new ItemKey(stack);
		
		GridAndAmount gaa=network.getHub().findGridForPlace(key, stack);
		if(gaa==null)
			return null;
		target=gaa.grid();
		targetPos=target.resolve().get().getPos();
		stack=itemHandler.extractItem(fromSlot, gaa.amount(), false);
		this.ticks=20;
		return this;
	}
	
}
