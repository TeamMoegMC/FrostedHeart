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

package com.teammoeg.chorda.dataholders;

import java.util.Optional;

/**
 * 特殊数据的持有者接口（类似 Forge Capability 的数据存储机制）。
 * 实现此接口的类可以持有和管理多种 {@link SpecialData} 类型的数据组件。
 * <p>
 * Interface for special data holders (a Forge Capability-like data storage mechanism).
 * Classes implementing this interface can hold and manage multiple {@link SpecialData} typed data components.
 *
 * @param <U> 持有者的实际类型 / the actual type of the holder
 */
public interface SpecialDataHolder<U extends SpecialDataHolder<U>> {
	
	/**
	 * 获取或创建数据组件。如果该类型的数据不存在，则自动创建。
	 * <p>
	 * Gets or creates a data component. If data of this type does not exist, it is automatically created.
	 *
	 * @param <T> 数据组件的对象类型 / the data component object type
	 * @param cap 数据组件的类型 / the data component type
	 * @return 数据组件实例 / the data component instance
	 */
	<T extends SpecialData> T getData(SpecialDataType<T> cap);
	
	/**
	 * 获取已存在的数据组件（如果存在）。与 {@link #getData} 不同，此方法不会自动创建数据。
	 * <p>
	 * Gets the data component if it exists. Unlike {@link #getData}, this method does not auto-create the data.
	 *
	 * @param <T> 数据组件的对象类型 / the data component object type
	 * @param cap 数据组件的类型 / the data component type
	 * @return 包含数据组件的 Optional，如果不存在则为空 / an Optional containing the data component, or empty if not present
	 */
	<T extends SpecialData> Optional<T> getOptional(SpecialDataType<T> cap);
}
