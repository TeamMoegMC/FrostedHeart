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

/**
 * 类型注册表。维护类到类型ID和名称之间的双向映射关系，
 * 为序列化系统提供类型识别支持。
 * <p>
 * Type registry. Maintains bidirectional mappings between classes and
 * type IDs/names, providing type identification support for the serialization system.
 *
 * @param <T> 注册的基类型 / the base type being registered
 */
public class TypeRegistry<T> {

	/** 类 -> (ID, 名称)对的映射 / Mapping from class to (ID, name) pair */
	protected Map<Class<? extends T>, Pair<Integer, String>> typeInfo = new HashMap<>();

	/**
	 * 构造一个空的类型注册表。
	 * <p>
	 * Constructs an empty type registry.
	 */
	public TypeRegistry() {
		super();
	}

	/**
	 * 获取对象的类型ID。
	 * <p>
	 * Gets the type ID of an object.
	 *
	 * @param obj 要查询的对象 / the object to look up
	 * @return 类型ID，如果未注册则返回-1 / the type ID, or -1 if not registered
	 */
	public int idOf(T obj) {
	    Pair<Integer, String> info = typeInfo.get(obj.getClass());
	    if (info == null)
	        return -1;
	    return info.getFirst();
	}
	/**
	 * 获取类的类型名称。
	 * <p>
	 * Gets the type name of a class.
	 *
	 * @param cls 要查询的类 / the class to look up
	 * @return 类型名称字符串 / the type name string
	 */
	public String typeOf(Class<?> cls) {
		return typeInfo.get(cls).getSecond();
	}
	/**
	 * 获取类的完整类型信息，包括ID和名称。
	 * <p>
	 * Gets the full type information for a class, including both ID and name.
	 *
	 * @param cls 要查询的类 / the class to look up
	 * @return 包含类型ID和名称的Pair / a Pair containing the type ID and name
	 */
	public Pair<Integer, String> fullTypeOf(Class<?> cls) {
		return typeInfo.get(cls);
	}
	/**
	 * 注册一个新的类型。自动分配递增的ID。
	 * <p>
	 * Registers a new type. Automatically assigns an incrementing ID.
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型名称 / the type name
	 */
	public void register(Class<? extends T> cls, String type) {
	    int id = typeInfo.size();
	    
	    typeInfo.put(cls, Pair.of(id, type));
	}

}