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

package com.teammoeg.chorda.io.marshaller;

import java.util.ArrayList;
import java.util.List;

public class ListListWrapper<T> implements IList<T> {
	List<T> list;
	public ListListWrapper(List<T> list) {
		super();
		this.list = list;
	}
	public ListListWrapper(int size) {
		super();
		this.list = new ArrayList<>(size);
	}
	@Override
	public T get(int val) {
		return list.get(val);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public void add(Object object) {
		list.add((T) object);
	}

	@Override
	public Object getInstance() {
		return list;
	}

}
