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

package com.teammoeg.chorda.util.struct;

import java.util.function.Supplier;

/**
 * 可变的Supplier实现，允许在运行时更改其提供的值。
 * 适用于需要延迟设置或动态更改供应值的场景。
 * <p>
 * Mutable Supplier implementation allowing the supplied value to be changed at runtime.
 * Suitable for scenarios requiring deferred setting or dynamic value changes.
 *
 * @param <T> 提供值的类型 / the type of value supplied
 */
public class MutableSupplier<T> implements Supplier<T> {
	T obj;

	/**
	 * 构造一个空的可变供应器。
	 * <p>
	 * Construct an empty mutable supplier.
	 */
	public MutableSupplier() {
	}

	/**
	 * 获取当前供应值。
	 * <p>
	 * Get the current supplied value.
	 *
	 * @return 当前值，可能为null / the current value, may be null
	 */
	@Override
	public T get() {
		return obj;
	}
	/**
	 * 设置供应值。
	 * <p>
	 * Set the supplied value.
	 *
	 * @param obj 要设置的值 / the value to set
	 */
	public void set(T obj) {
		this.obj=obj;
	}

}
