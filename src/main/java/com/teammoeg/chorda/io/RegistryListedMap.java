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

package com.teammoeg.chorda.io;

import java.util.AbstractList;
import java.util.Map;

/**
 * 基于注册表的Map列表视图，将Map包装为AbstractList以便按索引访问注册表条目。
 * <p>
 * A registry-backed map list view that wraps a Map as an AbstractList for index-based access to registry entries.
 *
 * @param <K> 键类型 / the key type
 * @param <E> 值类型 / the element type
 */
public abstract class RegistryListedMap<K,E> extends AbstractList<E> {
	Map<K,E> map;
	int msize;
	/**
	 * 构造一个新的注册表列表Map。
	 * <p>
	 * Constructs a new registry listed map.
	 *
	 * @param map 底层Map存储 / the underlying map storage
	 * @param maxsize 列表的最大大小 / the maximum size of the list
	 */
	public RegistryListedMap(Map<K,E> map,int maxsize) {
		this.map=map;
		msize=maxsize;
	}

	/**
	 * 根据整数索引获取对应的键。子类需实现此映射逻辑。
	 * <p>
	 * Gets the key corresponding to the given integer index. Subclasses must implement this mapping logic.
	 *
	 * @param id 整数索引 / the integer index
	 * @return 对应的键 / the corresponding key
	 */
	public abstract K getKey(int id);
	@Override
	public E get(int index) {
		return map.get(getKey(index));
	}
	@Override
	public int size() {
		return msize;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsValue(o);
	}

	@Override
	public boolean remove(Object o) {
		return map.values().remove(o);
	}
	@Override
	public void clear() {
		map.clear();
	}
	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

}
