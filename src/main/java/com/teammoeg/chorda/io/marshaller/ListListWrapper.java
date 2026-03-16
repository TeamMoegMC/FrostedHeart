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

import java.util.ArrayList;
import java.util.List;

/**
 * List 的 {@link IList} 包装器。将 {@link List} 适配为 IList 接口，使列表可被编组器统一处理。
 * <p>
 * List wrapper implementing {@link IList}. Adapts a {@link List} to the IList interface,
 * enabling unified handling by marshallers.
 *
 * @param <T> 元素类型 / Element type
 */
public class ListListWrapper<T> implements IList<T> {
	List<T> list;
	/**
	 * 使用已有的 List 构造包装器。
	 * <p>
	 * Constructs a wrapper with an existing List.
	 *
	 * @param list 要包装的列表 / The list to wrap
	 */
	public ListListWrapper(List<T> list) {
		super();
		this.list = list;
	}
	/**
	 * 以指定初始容量创建新的 ArrayList 并包装。
	 * <p>
	 * Creates and wraps a new ArrayList with the specified initial capacity.
	 *
	 * @param size 初始容量 / Initial capacity
	 */
	public ListListWrapper(int size) {
		super();
		this.list = new ArrayList<>(size);
	}
	/** {@inheritDoc} */
	@Override
	public T get(int val) {
		return list.get(val);
	}

	/** {@inheritDoc} */
	@Override
	public int size() {
		return list.size();
	}

	/** {@inheritDoc} */
	@Override
	public void add(Object object) {
		list.add((T) object);
	}

	/** {@inheritDoc} */
	@Override
	public Object getInstance() {
		return list;
	}

}
