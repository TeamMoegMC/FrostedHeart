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

import java.util.EnumMap;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * 带默认值的枚举映射，当查询的键不存在时返回默认值。
 * 支持null键作为默认值的键。
 * <p>
 * Enum map with a default value that is returned when the queried key does not exist.
 * Supports null key as the key for the default value.
 *
 * @param <K> 枚举键类型 / the enum key type
 * @param <T> 值类型 / the value type
 */
public class EnumDefaultedMap<K extends Enum<K>,T> {
	private T defaultSlot;
	private EnumMap<K,T> map;
	/**
	 * 构造带默认值的枚举映射。
	 * <p>
	 * Construct an enum defaulted map.
	 *
	 * @param enumClass 枚举键的类对象 / the class object of the enum key type
	 */
	public EnumDefaultedMap(Class<K> enumClass) {
		map=new EnumMap<>(enumClass);
	}
	
	/**
	 * 存入键值对。如果键为null则设置为默认值。
	 * <p>
	 * Put a key-value pair. If key is null, sets it as the default value.
	 *
	 * @param slot 枚举键，null表示默认值 / the enum key, null for default
	 * @param data 值 / the value
	 */
	public void put(K slot,T data) {
		if(slot==null)
			defaultSlot=data;
		else
			map.put(slot, data);
	};
	/**
	 * 获取键对应的值，不存在时返回默认值。
	 * <p>
	 * Get the value for the given key, returning the default value if not present.
	 *
	 * @param slot 枚举键 / the enum key
	 * @return 对应的值或默认值 / the corresponding value or default
	 */
	public T get(K slot) {
		if(slot==null)
			return defaultSlot;
		return map.getOrDefault(slot, defaultSlot);
	}
}
