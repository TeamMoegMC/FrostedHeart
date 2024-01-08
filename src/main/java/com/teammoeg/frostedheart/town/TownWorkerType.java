package com.teammoeg.frostedheart.town;

import net.minecraft.block.Block;

// TODO: Auto-generated Javadoc
/**
 * The Enum TownWorkerType.
 */
public enum TownWorkerType {
	
	/** The dummy. */
	DUMMY(null,null,-1);
	
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
