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

package com.teammoeg.chorda.io.registry;

import java.util.HashMap;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

public class TypeRegistry<T> {

	protected Map<Class<? extends T>, Pair<Integer, String>> typeInfo = new HashMap<>();

	public TypeRegistry() {
		super();
	}

	public int idOf(T obj) {
	    Pair<Integer, String> info = typeInfo.get(obj.getClass());
	    if (info == null)
	        return -1;
	    return info.getFirst();
	}
	public String typeOf(Class<?> cls) {
		return typeInfo.get(cls).getSecond();
	}
	public Pair<Integer, String> fullTypeOf(Class<?> cls) {
		return typeInfo.get(cls);
	}
	public void register(Class<? extends T> cls, String type) {
	    int id = typeInfo.size();
	    
	    typeInfo.put(cls, Pair.of(id, type));
	}

}