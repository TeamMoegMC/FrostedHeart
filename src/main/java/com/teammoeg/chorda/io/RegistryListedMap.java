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

public abstract class RegistryListedMap<K,E> extends AbstractList<E> {
	Map<K,E> map;
	int msize;
	public RegistryListedMap(Map<K,E> map,int maxsize) {
		this.map=map;
		msize=maxsize;
	}

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
