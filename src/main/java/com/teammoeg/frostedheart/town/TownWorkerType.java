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

package com.teammoeg.frostedheart.town;

import com.teammoeg.frostedheart.FHBlocks;
import net.minecraft.block.Block;

// TODO: Auto-generated Javadoc
/**
 * The Enum TownWorkerType.
 */
public enum TownWorkerType {
	
	/** The dummy. */
	DUMMY(null,null,-1),
	HOUSE(FHBlocks.house, (resource, workData) -> {
		double cost = 1;
		double actualCost = resource.cost(TownResourceType.PREP_FOOD, cost, false);
		return cost == actualCost;
	}, 0);
	
	/** Town block. */
	private Block block;
	
	/** The worker. */
	private TownWorker worker;
	
	/** The priority. */
	private int priority;
	
	/**
	 * Instantiates a new town worker type.
	 *
	 * @param workerBlock the worker block
	 * @param worker the worker
	 * @param internalPriority the internal priority
	 */
	private TownWorkerType(Block workerBlock, TownWorker worker, int internalPriority) {
		this.block = workerBlock;
		this.worker = worker;
		this.priority = internalPriority;
	}
	
	/**
	 * Gets the block.
	 *
	 * @return the block
	 */
	public Block getBlock() {
		return block;
	}
	
	/**
	 * Gets the worker.
	 *
	 * @return the worker
	 */
	public TownWorker getWorker() {
		return worker;
	}
	
	/**
	 * Gets the priority.
	 *
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}
	

}
