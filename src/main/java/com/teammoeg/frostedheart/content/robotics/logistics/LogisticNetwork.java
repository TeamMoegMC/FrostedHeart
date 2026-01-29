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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticHub;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LogisticNetwork {
	private LogisticHub hub;
	// List<LogisticEnvolop> envolops=new ArrayList<>();
	Set<LogisticTaskKey> keys=new HashSet<>();
	LinkedList<LogisticTask> tasks=new LinkedList<>();
	List<LogisticTask> prelist=new ArrayList<>(100);
	List<LogisticTask> working=new ArrayList<>(40);
	Level world;
	BlockPos centerPos;
	int MAX_WORKING_TASKS=20;

	
	public LogisticNetwork(Level world, BlockPos centerPos) {
		super();
		this.world = world;
		this.centerPos = centerPos;
		hub=new LogisticHub(world,centerPos);
	}
	public boolean canAddTask(LogisticTaskKey key) {
		return !keys.contains(key);
	}
	public void addTask(LogisticTaskKey key,LogisticTask task) {
		task.taskKey=key;
		keys.add(key);
		this.prelist.add(task);
	}
	public Level getWorld() {
		return world;
	}

	public void tick() {
		//FHMain.LOGGER.info("hub "+hub);
		hub.tick();
		//FHMain.LOGGER.info("Logistic tasks working:"+working.size()+",queued:"+tasks.size());
		List<LogisticTask> nextCycle=new ArrayList<>(working);
		working.clear();
		for(LogisticTask lt:nextCycle) {
			//FHMain.LOGGER.info("Logistic task working "+lt.ticks+","+lt.toString());
			if(lt.ticks>0) {
				lt.ticks--;
				working.add(lt);
			}else {
				
				LogisticTask nlt=lt.work(this);
				if(nlt!=null) {
					working.add(nlt);
					nlt.taskKey=lt.taskKey;
				}else
					keys.remove(lt.taskKey);
					
			}
		}
		if(working.size()<MAX_WORKING_TASKS) {
			Collections.shuffle(prelist);
			tasks.addAll(prelist);
			prelist.clear();
			//FHMain.LOGGER.info("Logistic task preparing");
			for(int i=0;i<tasks.size();i++) {
				LogisticTask wrapper=tasks.pollFirst();
				if(wrapper==null)break;
				LogisticTask lt=wrapper.prepare(this);
				if(lt!=null) {
					lt.taskKey=wrapper.taskKey;
					//FHMain.LOGGER.info("Logistic task added "+lt.toString());
					working.add(lt);
				}else {
					//tasks.addLast(wrapper);
				}
				if(working.size()>=MAX_WORKING_TASKS)
					break;
			}
		}
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
