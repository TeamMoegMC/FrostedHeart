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
	default boolean beforeWork(ITownResource resource,CompoundNBT workData) {return true;};
	/**
	 * Work during tick
	 *
	 * @param resource the resource
	 * @param workData workData provided by work type
	 * @return true, if work done successfully
	 */
	boolean work(ITownResource resource,CompoundNBT workData);
	/**
	 * Work with higher priority;
	 *
	 * @param resource the resource
	 * @param workData workData provided by work type
	 * @return true, if work done successfully
	 */
	default boolean afterWork(ITownResource resource,CompoundNBT workData) {return true;};
}
