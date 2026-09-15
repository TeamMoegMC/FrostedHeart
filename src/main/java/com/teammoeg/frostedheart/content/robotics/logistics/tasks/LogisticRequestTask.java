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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.GridAndAmount;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.IGridElement;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
@ToString
public class LogisticRequestTask extends LogisticTask {
	public static final MapCodec<LogisticRequestTask> WORKING_CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		LogisticTaskKey.CODEC.fieldOf("key").forGetter(o->o.taskKey),
		Codec.INT.fieldOf("ticks").forGetter(o->o.ticks),
		Codec.INT.fieldOf("maxTicks").forGetter(o->o.ticks),
		BlockPos.CODEC.fieldOf("from").forGetter(o->o.origin),
		BlockPos.CODEC.fieldOf("to").forGetter(o->o.targetPos),
		ItemStack.CODEC.fieldOf("stack").forGetter(o->o.stack),
		Codec.INT.optionalFieldOf("failures",0).forGetter(o->o.failures)
		).apply(t, LogisticRequestTask::new));
	private static final int MAX_DELIVERY_FAILURES=15;
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
	int failures;
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
		if(target==null&&targetPos!=null) {
			target=network.getItemHandler(targetPos);
		}
		if(target!=null&&target.isPresent()) {
			stack=ItemHandlerHelper.insertItemStacked(target.orElse(null), stack, false);
		}	
		if(!stack.isEmpty()) {
			GridAndAmount gaa=network.getHub().findGridForPlace(new ItemKey(stack), stack,targetPos);
			if(gaa==null) {
				target=null;
				ticks=20;
				return ++failures>=MAX_DELIVERY_FAILURES?null:this;
			}
			LogisticPushTask pushTask=new LogisticPushTask(targetPos,gaa.grid().pos(),stack);
			stack=ItemStack.EMPTY;
			return pushTask;
		}
	
		return null;
	}
	@Override
	public LogisticTask prepare(LogisticNetwork network) {
		GridAndAmount gaa=network.getHub().findGridForTake(filter, targetPos);
		if(gaa==null)
			return null;
		origin=gaa.grid().pos();
		LazyOptional<IGridElement> data=network.getHub().getByPos(origin);
		if(data!=null) {
			IGridElement grid=data.orElse(null);
			if(grid!=null) {
				stack=grid.takeItem(gaa.grid().slot(), size);
			}
		}
		if(stack.isEmpty())
			return null;
		this.ticks=20;
		return this;
	}

	@Override
	public ItemStack takeCarriedStack() {
		ItemStack carried=stack;
		stack=ItemStack.EMPTY;
		return carried==null?ItemStack.EMPTY:carried;
	}


	public LogisticRequestTask(LogisticTaskKey taskKey, int ticks, int maxTicks, BlockPos origin, BlockPos targetPos, ItemStack stack) {
		this(taskKey,ticks,maxTicks,origin,targetPos,stack,0);
	}

	public LogisticRequestTask(LogisticTaskKey taskKey, int ticks, int maxTicks, BlockPos origin, BlockPos targetPos, ItemStack stack,int failures) {
		super(taskKey, ticks, maxTicks);
		this.origin = origin;
		this.targetPos = targetPos;
		this.stack = stack;
		this.failures=failures;
	}
	@Override
	public void destroy(Level l) {
		if(targetPos!=null&&origin!=null) {
			Vec3 vor=Vec3.atCenterOf(origin);
			Vec3 vtar=Vec3.atCenterOf(targetPos);
			CUtils.dropItem(l, vor.add(vtar.subtract(vor).scale(1-(ticks*1f/maxTicks))), stack);
		}
		if(targetPos==null&&origin!=null)
			CUtils.dropItem(l, origin, stack);

		if(origin==null&&targetPos!=null)
			CUtils.dropItem(l, targetPos, stack);
	}

}
