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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticHub;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticPushTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class LogisticNetwork {
	private LogisticHub hub;
	Set<LogisticTaskKey> keys=new HashSet<>();
	LinkedList<LogisticTask> tasks=new LinkedList<>();
	List<LogisticTask> working=new ArrayList<>(40);
	Level world;
	BlockPos centerPos;
	private final Runnable markDirty;
	private int hubRevalidationTicks;
	private boolean closed;
	static final int MAX_WORKING_TASKS=20;
	private static final int HUB_REVALIDATION_INTERVAL=200;
	private static final TypedCodecRegistry<LogisticTask> idr=new TypedCodecRegistry<>();
	static {
		idr.register(LogisticPushTask.class, "push", LogisticPushTask.WORKING_CODEC);
		idr.register(LogisticRequestTask.class, "pull", LogisticRequestTask.WORKING_CODEC);
	}
	public static final Codec<LogisticTask> TASK_CODEC=idr.codec();
	
	public LogisticNetwork(Level world, BlockPos centerPos) {
		this(world,centerPos,()->{});
	}
	public LogisticNetwork(Level world, BlockPos centerPos,Runnable markDirty) {
		this.world = world;
		this.centerPos = centerPos;
		this.markDirty = markDirty;
		hub=new LogisticHub(world,centerPos);
	}
	public boolean canAddTask(LogisticTaskKey key) {
		return !keys.contains(key);
	}
	public void addTask(LogisticTaskKey key,LogisticTask task) {
		if(closed||keys.contains(key))
			return;
		task.taskKey=key;
		keys.add(key);
		this.tasks.addLast(task);
	}
	public Level getWorld() {
		return world;
	}
	public void save(CompoundTag nbt) {
		nbt.put("tasks", CodecUtil.toNBTList(working, TASK_CODEC));
	}
	public void load(CompoundTag nbt) {
		tasks.clear();
		working.clear();
		keys.clear();
		working.addAll(CodecUtil.fromNBTList(nbt.getList("tasks", Tag.TAG_COMPOUND), TASK_CODEC));
		for(LogisticTask task:working) {
			if(task.taskKey!=null)
				keys.add(task.taskKey);
		}
	}
	public void tick() {
		if(closed)
			return;
		hub.tick();
		if(++hubRevalidationTicks>=HUB_REVALIDATION_INTERVAL) {
			hubRevalidationTicks=0;
			hub.revalidate();
		}
		List<LogisticTask> nextCycle=new ArrayList<>(working);
		working.clear();
		for(LogisticTask lt:nextCycle) {
			if(lt.ticks>0) {
				lt.ticks--;
				working.add(lt);
			}else {
				try {
					LogisticTask nlt=lt.work(this);
					if(nlt!=null) {
						working.add(nlt);
						nlt.taskKey=lt.taskKey;
					}else {
						recoverCarriedItem(lt);
						keys.remove(lt.taskKey);
					}
					markDirty.run();
				}catch(RuntimeException ex) {
					FHMain.LOGGER.error("Canceling failed logistic task {}",lt,ex);
					recoverCarriedItem(lt);
					keys.remove(lt.taskKey);
					markDirty.run();
				}
			}
		}
		while(working.size()<MAX_WORKING_TASKS&&!tasks.isEmpty()) {
			LogisticTask wrapper=tasks.pollFirst();
			try {
				LogisticTask lt=wrapper.prepare(this);
				if(lt!=null) {
					lt.taskKey=wrapper.taskKey;
					working.add(lt);
					markDirty.run();
				}else {
					keys.remove(wrapper.taskKey);
				}
			}catch(RuntimeException ex) {
				FHMain.LOGGER.error("Canceling failed logistic task {}",wrapper,ex);
				recoverCarriedItem(wrapper);
				keys.remove(wrapper.taskKey);
				markDirty.run();
			}
		}
	}

	public void cancelTasksAt(BlockPos pos) {
		tasks.removeIf(task->removeQueuedTaskAt(task,pos));
		List<LogisticTask> retained=new ArrayList<>(working.size());
		for(LogisticTask task:working) {
			if(task.taskKey!=null&&task.taskKey.pos().equals(pos)) {
				recoverCarriedItem(task);
				keys.remove(task.taskKey);
			}else
				retained.add(task);
		}
		if(retained.size()!=working.size()) {
			working.clear();
			working.addAll(retained);
			markDirty.run();
		}
	}

	private boolean removeQueuedTaskAt(LogisticTask task,BlockPos pos) {
		if(task.taskKey!=null&&task.taskKey.pos().equals(pos)) {
			keys.remove(task.taskKey);
			return true;
		}
		return false;
	}

	public void shutdown() {
		if(closed)
			return;
		closed=true;
		for(LogisticTask task:working)
			recoverCarriedItem(task);
		working.clear();
		tasks.clear();
		keys.clear();
		markDirty.run();
	}

	private void recoverCarriedItem(LogisticTask task) {
		var carried=task.takeCarriedStack();
		if(carried.isEmpty())
			return;
		var remainder=carried;
		try {
			remainder=hub.pushItem(carried,true);
		}catch(RuntimeException ex) {
			FHMain.LOGGER.error("Failed to return item carried by logistic task {}",task,ex);
		}
		if(!remainder.isEmpty()&&world!=null&&!world.isClientSide)
			Containers.dropItemStack(world,centerPos.getX()+0.5,centerPos.getY()+0.5,centerPos.getZ()+0.5,remainder);
	}

	public LazyOptional<IItemHandler> getItemHandler(BlockPos pos) {
		if(world==null||!world.hasChunkAt(pos))
			return LazyOptional.empty();
		BlockEntity blockEntity=world.getBlockEntity(pos);
		if(blockEntity==null)
			return LazyOptional.empty();
		return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
	}
	public void setWorld(Level world) {
		this.world = world;
	}

	public BlockPos getCenterPos() {
		return centerPos;
	}

	public void setCenterPos(BlockPos centerPos) {
		this.centerPos = centerPos;
	}

	public LogisticHub getHub() {
		return hub;
	}

	public void setHub(LogisticHub hub) {
		this.hub = hub;
	}
}
