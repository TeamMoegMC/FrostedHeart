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

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.CUtils;
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
@ToString
public class LogisticPushTask extends LogisticTask {
	public static final MapCodec<LogisticPushTask> WORKING_CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		LogisticTaskKey.CODEC.fieldOf("key").forGetter(o->o.taskKey),
		Codec.INT.fieldOf("ticks").forGetter(o->o.ticks),
		Codec.INT.fieldOf("maxTicks").forGetter(o->o.ticks),
		BlockPos.CODEC.fieldOf("from").forGetter(o->o.origin),
		BlockPos.CODEC.optionalFieldOf("to").forGetter(o->Optional.ofNullable(o.targetPos)),
		ItemStack.CODEC.fieldOf("stack").forGetter(o->o.stack),
		Codec.INT.optionalFieldOf("failures",0).forGetter(o->o.failures)
		).apply(t, LogisticPushTask::new));
	private static final int MAX_DELIVERY_FAILURES=15;
	/**
	 * The origin slot and handler, the task takes item from this chest if required
	 * initial task for the task
	 * */
	transient int fromSlot;
	transient LazyOptional<IItemHandler> handler;
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
	int failures;
	transient ItemKey key;
	public LogisticPushTask(LogisticTaskKey taskKey, int ticks, int maxTicks, BlockPos origin, Optional<BlockPos> targetPos, ItemStack stack) {
		this(taskKey,ticks,maxTicks,origin,targetPos,stack,0);
	}

	public LogisticPushTask(LogisticTaskKey taskKey, int ticks, int maxTicks, BlockPos origin, Optional<BlockPos> targetPos, ItemStack stack,int failures) {
		super(taskKey, ticks,maxTicks);
		this.origin = origin;
		this.targetPos = targetPos.orElse(null);
		this.stack = stack;
		this.failures=failures;
	}
	
	public LogisticPushTask(BlockPos origin, BlockPos targetPos, ItemStack stack) {
		super();
		this.origin = origin;
		this.targetPos = targetPos;
		this.stack = stack;
		this.ticks=this.maxTicks=20;
	}

	public LogisticPushTask(BlockPos pos,LazyOptional<IItemHandler> handler, int fromSlot) {
		super();
		this.handler = handler;
		this.fromSlot = fromSlot;
		this.origin=pos;
	}

	@Override
	public LogisticTask work(LogisticNetwork network) {
		if(stack==null||stack.isEmpty())
			return null;
		if(targetPos!=null) {
			LazyOptional<IGridElement> target=network.getHub().getByPos(targetPos);
			if(target!=null) {
				IGridElement grid=target.orElse(null);
				stack=grid.pushItem(stack);
			}
		}
		if(!stack.isEmpty()) {
			if(key==null)
				key=new ItemKey(stack);
			GridAndAmount gaa=network.getHub().findGridForPlace(key, stack, targetPos);
			if(gaa==null) {
				targetPos=null;
				ticks=20;
				return ++failures>=MAX_DELIVERY_FAILURES?null:this;
			}
			if(targetPos!=null)
				origin=targetPos;
			ticks=20;
			targetPos=gaa.grid().pos();
			failures=0;
			return this;
		}
		return null;
	}
	@Override
	public LogisticTask prepare(LogisticNetwork network) {
		if(handler==null||!handler.isPresent())
			return null;
		IItemHandler itemHandler=handler.orElse(null);
		if(itemHandler==null)
			return null;
			
		stack=itemHandler.getStackInSlot(fromSlot);
		if(stack.isEmpty())
			return null;
		key=new ItemKey(stack);
		
		GridAndAmount gaa=network.getHub().findGridForPlace(key, stack, origin);
		if(gaa==null)
			return null;
		targetPos=gaa.grid().pos();
		stack=itemHandler.extractItem(fromSlot, gaa.amount(), false);
		this.ticks=20;
		return this;
	}

	@Override
	public ItemStack takeCarriedStack() {
		ItemStack carried=stack;
		stack=ItemStack.EMPTY;
		return carried==null?ItemStack.EMPTY:carried;
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
