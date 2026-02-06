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

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class NBTInstanceMarshaller<T> implements Marshaller {
	final BiConsumer<T,CompoundTag> from;
	final Function<T,CompoundTag> to;
	final Class<T> objcls;
	public NBTInstanceMarshaller(Class<T> objcls, BiConsumer<T,CompoundTag> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
		this.objcls = objcls;
	}

	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(Tag nbt) {
		if(!(nbt instanceof CompoundTag))return null;
		T ret=ClassInfo.createInstance(objcls);
		from.accept(ret, (CompoundTag) nbt);
		return ret;
		
	}

}
