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

package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.GridAndAmount;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.IGridElement;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.ItemCountProvider;

import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
@ToString
public class LogisticRequestTask extends LogisticTask {
	public static final MapCodec<LogisticRequestTask> WORKING_CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		LogisticTaskKey.CODEC.fieldOf("key").forGetter(o->o.taskKey),
		Codec.INT.fieldOf("ticks").forGetter(o->o.ticks),
		BlockPos.CODEC.fieldOf("from").forGetter(o->o.origin),
		BlockPos.CODEC.fieldOf("to").forGetter(o->o.targetPos),
		ItemStack.CODEC.fieldOf("stack").forGetter(o->o.stack),
		ItemKey.CODEC.fieldOf("item").forGetter(o->o.key)
		).apply(t, LogisticRequestTask::new));
	/**
	 * original data for a request task
	 * */
	transient Filter filter;
	transient int size;
	
	
	
	/**
	 * position of from and to pos, for rendering purpose
	 * */
	BlockPos origin;
	BlockPos targetPos;

	/**
	 * Carried stack in this task, should drop if task canceled
	 * */
	ItemStack stack;
	/**
	 * ItemKey to put
	 * */
	ItemKey key;
	/**
	 * Grid element to put
	 * */
	transient LazyOptional<IItemHandler> target;
	public LogisticRequestTask(Filter filter, int size,BlockPos targetPos, LazyOptional<IItemHandler> storage) {
		super();
		this.filter = filter;
		this.size = size;
		this.target = storage;
		this.targetPos=targetPos;
	}


	@Override
	public LogisticTask work(LogisticNetwork network) {
		//System.out.println("moving "+stack+" to "+targetPos);
		if(target.isPresent()) {
			stack=ItemHandlerHelper.insertItemStacked(target.resolve().get(), stack, false);
		}	
		if(!stack.isEmpty()) {
			GridAndAmount gaa=network.getHub().findGridForPlace(key, stack);
			IGridElement grid=gaa.grid().resolve().get();
			return new LogisticPushTask(targetPos,grid.getPos(),stack,key,gaa.grid());
		}
	
		return null;
	}
	public ItemKey getActualKey(LogisticNetwork network) {
		if(filter.isIgnoreNbt()) {
			Map<ItemKey, ? extends ItemCountProvider> items=network.getHub().getAllItems();
			for(ItemKey ik:items.keySet()) {
				if(filter.matches(ik)) {
					return ik;
				}
			}
		}
		return filter.getKey();
	}
	@Override
	public LogisticTask prepare(LogisticNetwork network) {
		key=getActualKey(network);
		GridAndAmount gaa=network.getHub().findGridForTake(key);
		if(gaa==null)
			return null;
		IGridElement grid=gaa.grid().resolve().get();
		origin=grid.getPos();
		stack=network.getHub().takeItem(gaa.grid(), key, size);
		//System.out.println("taken "+stack+" to "+targetPos);
		this.ticks=20;
		return this;
	}


	public LogisticRequestTask(LogisticTaskKey taskKey, int ticks, BlockPos origin, BlockPos targetPos, ItemStack stack, ItemKey key) {
		super(taskKey, ticks);
		this.origin = origin;
		this.targetPos = targetPos;
		this.stack = stack;
		this.key = key;
	}

}
