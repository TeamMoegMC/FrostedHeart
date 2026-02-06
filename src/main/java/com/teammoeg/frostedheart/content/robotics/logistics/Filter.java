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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

public class Filter {
	public static final Codec<Filter> CODEC=RecordCodecBuilder.create(t->t.group(
		ItemKey.CODEC.fieldOf("key").forGetter(o->o.key),
		Codec.BOOL.optionalFieldOf("ignoreNBT",false).forGetter(o->o.ignoreNbt),
		Codec.INT.fieldOf("size").forGetter(o->o.size)).apply(t, Filter::new));
	@Getter
	@Setter
	ItemKey key;
	@Getter
	@Setter
	boolean ignoreNbt;
	@Setter
	@Getter
	int size=0;

	public Filter(ItemKey key, boolean ignoreNbt,int size) {
		super();
		this.key = key;
		this.ignoreNbt = ignoreNbt;
		this.size=size;
	}
	public Filter() {
		
	}
	public boolean matches(ItemKey okey) {
		if(ignoreNbt)
			return okey.item==key.item;
		return key.equals(okey);
	}
	public boolean matches(ItemStack okey) {
		if(ignoreNbt)
			return okey.getItem()==key.item;
		return key.isSameItem(okey);
	}
	public ItemStack createDisplayStack() {
		if(key==null)return ItemStack.EMPTY;
		return key.createStackWithSize(size);
	}
	public ItemStack getDisplayItem() {
		if(key==null)return ItemStack.EMPTY;
		return key.getStack();
	}
	@Override
	public int hashCode() {
		return Objects.hash(ignoreNbt, key, size);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Filter other = (Filter) obj;
		return ignoreNbt == other.ignoreNbt && Objects.equals(key, other.key) && size == other.size;
	}
}
