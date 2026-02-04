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

package com.teammoeg.chorda.util.struct;

import java.util.EnumMap;

import net.minecraft.world.entity.EquipmentSlot;

public class EnumDefaultedMap<K extends Enum<K>,T> {
	private T defaultSlot;
	private EnumMap<K,T> map;
	public EnumDefaultedMap(Class<K> enumClass) {
		map=new EnumMap<>(enumClass);
	}
	
	public void put(K slot,T data) {
		if(slot==null)
			defaultSlot=data;
		else
			map.put(slot, data);
	};
	public T get(K slot) {
		if(slot==null)
			return defaultSlot;
		return map.getOrDefault(slot, defaultSlot);
	}
}
