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
 * 通用列表抽象接口。为数组和 List 提供统一的访问和操作方式，用于编组器的列表序列化。
 * <p>
 * Generic list abstraction interface. Provides a unified access and manipulation pattern
 * for both arrays and Lists, used for list serialization in marshallers.
 *
 * @param <T> 元素类型 / Element type
 */
public interface IList<T> {
	/**
	 * 获取指定索引处的元素。
	 * <p>
	 * Gets the element at the specified index.
	 *
	 * @param val 索引 / Index
	 * @return 该索引处的元素 / The element at the given index
	 */
	T get(int val);

	/**
	 * 获取列表的大小。
	 * <p>
	 * Gets the size of the list.
	 *
	 * @return 元素数量 / Number of elements
	 */
	int size();

	/**
	 * 向列表末尾添加一个元素。
	 * <p>
	 * Adds an element to the end of the list.
	 *
	 * @param object 要添加的元素 / The element to add
	 */
	void add(Object object);

	/**
	 * 获取底层的列表或数组实例。
	 * <p>
	 * Gets the underlying list or array instance.
	 *
	 * @return 底层容器对象 / The underlying container object
	 */
	Object getInstance();
}
