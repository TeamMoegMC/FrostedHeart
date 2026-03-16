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

import java.util.function.Function;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

/**
 * 列表/数组编组器。通过 {@link IList} 抽象在 NBT ListTag 和 Java 列表/数组之间进行序列化和反序列化。
 * <p>
 * List/array marshaller. Serializes and deserializes between NBT ListTag and Java lists/arrays
 * via the {@link IList} abstraction.
 *
 * @param <T> 元素类型 / Element type
 */
public class ListMarshaller<T> implements Marshaller {
	private final Class<T> elmType;
	private final Function<Object,IList<T>> wrapper;
	private final Function<Integer,IList<T>> factory;


	/**
	 * 构造列表编组器。
	 * <p>
	 * Constructs a list marshaller.
	 *
	 * @param elmType 元素类型 / Element type
	 * @param wrapper 将已有对象包装为 IList 的函数 / Function to wrap an existing object as IList
	 * @param factory 按大小创建新 IList 的工厂函数 / Factory function to create a new IList by size
	 */
	public ListMarshaller(Class<T> elmType, Function<Object, IList<T>> wrapper, Function<Integer, IList<T>> factory) {
		super();
		this.elmType = elmType;
		this.wrapper = wrapper;
		this.factory = factory;
	}

	/** {@inheritDoc} */
	@Override
	public Tag toNBT(Object o) {
		ListTag nbt=new ListTag();
		IList<T> list=wrapper.apply(o);
		for(int i=0;i<list.size();i++) {
			nbt.add(MarshallUtil.serialize(list.get(i)));
		}
		return nbt;
	}

	/** {@inheritDoc} */
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
