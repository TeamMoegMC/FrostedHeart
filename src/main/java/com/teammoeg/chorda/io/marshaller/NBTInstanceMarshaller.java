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

/**
 * 基于实例的NBT编组器。通过反射创建对象实例，然后使用提供的函数进行NBT读写操作。
 * 与{@link NBTRWMarshaller}不同，此编组器先创建空实例再填充数据。
 * <p>
 * Instance-based NBT marshaller. Creates object instances via reflection, then uses provided
 * functions for NBT read/write operations. Unlike {@link NBTRWMarshaller}, this marshaller
 * creates an empty instance first and then populates it with data.
 *
 * @param <T> 要编组的对象类型 / the type of object to marshal
 */
public class NBTInstanceMarshaller<T> implements Marshaller {
	/** 从CompoundTag读取数据到已有实例的函数 / Function to read data from CompoundTag into an existing instance */
	final BiConsumer<T,CompoundTag> from;
	/** 将实例序列化为CompoundTag的函数 / Function to serialize an instance to CompoundTag */
	final Function<T,CompoundTag> to;
	/** 目标对象的类 / The class of the target object */
	final Class<T> objcls;

	/**
	 * 构造一个基于实例的NBT编组器。
	 * <p>
	 * Constructs an instance-based NBT marshaller.
	 *
	 * @param objcls 目标对象的类，用于反射创建实例 / the class of the target object, used for reflective instantiation
	 * @param from 从CompoundTag读取数据到实例的消费者 / consumer that reads data from CompoundTag into an instance
	 * @param to 将实例转换为CompoundTag的函数 / function that converts an instance to CompoundTag
	 */
	public NBTInstanceMarshaller(Class<T> objcls, BiConsumer<T,CompoundTag> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
		this.objcls = objcls;
	}

	/** {@inheritDoc} */
	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	/**
	 * 从NBT标签反序列化对象。如果标签不是CompoundTag则返回null。
	 * 通过反射创建新实例并用NBT数据填充。
	 * <p>
	 * Deserializes an object from an NBT tag. Returns null if the tag is not a CompoundTag.
	 * Creates a new instance via reflection and populates it with NBT data.
	 *
	 * @param nbt 要反序列化的NBT标签 / the NBT tag to deserialize from
	 * @return 反序列化后的对象，如果标签类型不匹配则返回null / the deserialized object, or null if the tag type does not match
	 */
	@Override
	public Object fromNBT(Tag nbt) {
		if(!(nbt instanceof CompoundTag))return null;
		T ret=ClassInfo.createInstance(objcls);
		from.accept(ret, (CompoundTag) nbt);
		return ret;
		
	}

}
