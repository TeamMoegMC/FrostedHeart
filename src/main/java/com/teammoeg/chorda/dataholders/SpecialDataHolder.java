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

package com.teammoeg.chorda.dataholders;

import java.util.Optional;

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
	<T extends SpecialData> T getData(SpecialDataType<T> cap);
	
	/**
	 * Get data if exists.
	 *
	 * @param <T> the data component object type
	 * @param cap the data component type
	 * @return the data component
	 */
	<T extends SpecialData> Optional<T> getOptional(SpecialDataType<T> cap);
}
