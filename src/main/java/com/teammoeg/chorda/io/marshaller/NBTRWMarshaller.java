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

import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class NBTRWMarshaller<T> implements Marshaller {
	final Function<CompoundTag,T> from;
	final Function<T,CompoundTag> to;
	public NBTRWMarshaller(Function<CompoundTag,T> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
	}

	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(Tag nbt) {
		return from.apply((CompoundTag) nbt);
		
	}

}
