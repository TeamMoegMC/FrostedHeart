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

import net.minecraft.nbt.Tag;

public class BasicMarshaller<R extends Tag,T> implements Marshaller {
	final Function<R,T> from;
	final Function<T,R> to;
	final Class<R> cls;
	final T def;
	public BasicMarshaller(Class<R> nbtcls,Function<R, T> from, Function<T, R> to) {
		this(nbtcls,from,to,null);
	}

	public BasicMarshaller(Class<R> cls,Function<R, T> from, Function<T, R> to, T def) {
		super();
		this.from = from;
		this.to = to;
		this.cls = cls;
		this.def = def;
	}

	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(Tag nbt) {
		if(cls.isInstance(nbt))
			return from.apply((R) nbt);
		return def;
	}

}
