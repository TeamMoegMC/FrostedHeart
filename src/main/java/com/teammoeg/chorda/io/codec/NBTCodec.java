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

package com.teammoeg.chorda.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.marshaller.ClassInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;

public class NBTCodec<A extends NBTSerializable> implements Codec<A> {
	Class<A> clazz;
	
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
