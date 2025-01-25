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

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class ReflectionCodec<A> extends MapCodec<A> {
	ClassInfo info;
	public ReflectionCodec(Class<A> info) {
		this.info=ClassInfo.valueOf(info);
	}
	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		CompoundTag nbt=new CompoundTag();
		input.entries().forEach(o->nbt.put(NbtOps.INSTANCE.getStringValue(ops.convertTo(NbtOps.INSTANCE, o.getFirst())).result().orElse(""), ops.convertTo(NbtOps.INSTANCE, o.getSecond())));
		return DataResult.success((A)info.fromNBT(nbt));
	}
	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		CompoundTag nbt=(CompoundTag) info.toNBT(input);
		for(String in:nbt.getAllKeys())
			prefix.add(in, NbtOps.INSTANCE.convertTo(ops, nbt.get(in)));
		return prefix;
	}
	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return info.getFields().map(o->o.name).map(ops::createString);
	}
}
