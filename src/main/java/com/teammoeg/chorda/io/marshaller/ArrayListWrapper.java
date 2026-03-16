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

package com.teammoeg.chorda.io.marshaller;

/**
 * 数组的 {@link IList} 包装器。将原生数组适配为 IList 接口，支持按索引访问和顺序添加元素。
 * <p>
 * Array wrapper implementing {@link IList}. Adapts a native array to the IList interface,
 * supporting indexed access and sequential element addition.
 *
 * @param <T> 元素类型 / Element type
 */
public class ArrayListWrapper<T> implements IList<T> {
	T[] array;
	int cridx=0;
	/**
	 * 使用类型安全的数组构造包装器。
	 * <p>
	 * Constructs a wrapper with a type-safe array.
	 *
	 * @param array 要包装的数组 / The array to wrap
	 */
	public ArrayListWrapper(T[] array) {
		super();
		this.array = array;
	}
	/**
	 * 使用未检查类型转换的 Object 参数构造包装器。
	 * <p>
	 * Constructs a wrapper from a raw Object with an unchecked cast.
	 *
	 * @param array 要包装的数组对象 / The array object to wrap
	 */
	public ArrayListWrapper(Object array) {
		super();
		this.array = (T[]) array;
	}

	/** {@inheritDoc} */
	@Override
	public T get(int val) {
		return array[val];
	}

	/** {@inheritDoc} */
	@Override
	public int size() {
		return array.length;
	}

	/** {@inheritDoc} */
	@Override
	public void add(Object object) {
		array[cridx++]=(T) object;
	}

	/** {@inheritDoc} */
	@Override
	public Object getInstance() {
		return array;
	}

}
