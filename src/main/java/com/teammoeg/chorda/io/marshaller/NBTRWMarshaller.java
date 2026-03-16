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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 基于读写函数的NBT编组器。使用提供的函数对在对象和CompoundTag之间进行转换。
 * 与{@link NBTInstanceMarshaller}不同，此编组器直接通过反序列化函数创建对象。
 * <p>
 * Read/Write function-based NBT marshaller. Uses provided functions to convert between
 * objects and CompoundTag. Unlike {@link NBTInstanceMarshaller}, this marshaller creates
 * objects directly through the deserialization function.
 *
 * @param <T> 要编组的对象类型 / the type of object to marshal
 */
public class NBTRWMarshaller<T> implements Marshaller {
	/** 从CompoundTag反序列化对象的函数 / Function to deserialize an object from CompoundTag */
	final Function<CompoundTag,T> from;
	/** 将对象序列化为CompoundTag的函数 / Function to serialize an object to CompoundTag */
	final Function<T,CompoundTag> to;

	/**
	 * 构造一个基于读写函数的NBT编组器。
	 * <p>
	 * Constructs a read/write function-based NBT marshaller.
	 *
	 * @param from 从CompoundTag反序列化对象的函数 / function to deserialize an object from CompoundTag
	 * @param to 将对象序列化为CompoundTag的函数 / function to serialize an object to CompoundTag
	 */
	public NBTRWMarshaller(Function<CompoundTag,T> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
	}

	/** {@inheritDoc} */
	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	/** {@inheritDoc} */
	@Override
	public Object fromNBT(Tag nbt) {
		return from.apply((CompoundTag) nbt);
		
	}

}
