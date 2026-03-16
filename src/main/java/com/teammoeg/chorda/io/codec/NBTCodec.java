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

package com.teammoeg.chorda.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.marshaller.ClassInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * 用于NBTSerializable对象的编解码器。通过NBT序列化/反序列化接口与DFU的编解码系统桥接。
 * <p>
 * A codec for NBTSerializable objects. Bridges the NBT serialization/deserialization interface
 * with the DFU codec system.
 *
 * @param <A> 实现NBTSerializable接口的类型 / the type implementing NBTSerializable
 */
public class NBTCodec<A extends NBTSerializable> implements Codec<A> {
	Class<A> clazz;

	/**
	 * 构造一个NBT编解码器。
	 * <p>
	 * Constructs an NBT codec.
	 *
	 * @param clazz 要序列化的类 / the class to serialize
	 */
	public NBTCodec(Class<A> clazz) {
		super();
		this.clazz = clazz;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		DataResult<MapLike<T>> data=ops.getMap(NbtOps.INSTANCE.convertMap(ops, input.serializeNBT()));
		if(!data.result().isPresent())
			return (DataResult<T>) data;
		return ops.mergeToMap(prefix, data.result().get());
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {

		Tag nbt = ops.convertTo(NbtOps.INSTANCE, input);
		if (nbt instanceof CompoundTag) {
			A inst = ClassInfo.createInstance(clazz);
			inst.deserializeNBT((CompoundTag) nbt);
			return DataResult.success(Pair.of(inst, input));
		}
		return DataResult.error(()->"Not A Compound");

	}



}
