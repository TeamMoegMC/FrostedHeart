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
import net.minecraft.nbt.ListTag;

public class ListMarshaller<T> implements Marshaller {
	private final Class<T> elmType;
	private final Function<Object,IList<T>> wrapper;
	private final Function<Integer,IList<T>> factory;


	public ListMarshaller(Class<T> elmType, Function<Object, IList<T>> wrapper, Function<Integer, IList<T>> factory) {
		super();
		this.elmType = elmType;
		this.wrapper = wrapper;
		this.factory = factory;
	}

	@Override
	public Tag toNBT(Object o) {
		ListTag nbt=new ListTag();
		IList<T> list=wrapper.apply(o);
		for(int i=0;i<list.size();i++) {
			nbt.add(MarshallUtil.serialize(list.get(i)));
		}
		return nbt;
	}

	@Override
	public Object fromNBT(Tag nbt) {
		
		if(nbt instanceof ListTag) {
			IList<T> list=factory.apply(((ListTag)nbt).size());
			for(Tag n:(ListTag)nbt) {
				list.add(MarshallUtil.deserialize(elmType, n));
			}
			return list.getInstance();
		}
		return null;
	}

}
