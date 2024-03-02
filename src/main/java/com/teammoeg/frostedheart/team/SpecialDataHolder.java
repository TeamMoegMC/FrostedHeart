package com.teammoeg.frostedheart.team;

import java.util.Optional;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

/**
 * The Holder for special data(frostedheart implemented forge capability-like storage)
 *
 * @param <U> the actual type
 */
public interface SpecialDataHolder<U extends SpecialDataHolder<U>> {
	
	/**
	 * Get or create data component
	 *
	 * @param <T> the data component object type
	 * @param cap the data component type
	 * @return the data component
	 */
	<T extends NBTSerializable> T getData(SpecialDataType<T,U> cap);
	
	/**
	 * Get data if exists.
	 *
	 * @param <T> the data component object type
	 * @param cap the data component type
	 * @return the data component
	 */
	<T extends NBTSerializable> Optional<T> getOptional(SpecialDataType<T,U> cap);
}
