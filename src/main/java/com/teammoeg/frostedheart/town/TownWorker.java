package com.teammoeg.frostedheart.town;

import net.minecraft.nbt.CompoundNBT;

/**
 * A town processing function.
 */
@FunctionalInterface
public interface TownWorker {
	/**
	 * Work with lower priority;
	 *
	 * @param resource the resource
	 * @param workData workData provided by work type
	 * @return true, if work done successfully
	 */
	default boolean beforeWork(Town resource,CompoundNBT workData) {return true;};
	/**
	 * Work during tick
	 *
	 * @param resource the resource
	 * @param workData workData provided by work type
	 * @return true, if work done successfully
	 */
	boolean work(Town resource,CompoundNBT workData);
	/**
	 * Work with higher priority;
	 *
	 * @param resource the resource
	 * @param workData workData provided by work type
	 * @return true, if work done successfully
	 */
	default boolean afterWork(Town resource,CompoundNBT workData) {return true;};
}
