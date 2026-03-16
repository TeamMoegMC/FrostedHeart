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

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * 基于反射的MapCodec实现。利用{@link ClassInfo}通过反射自动完成对象与序列化格式之间的编解码，
 * 内部通过NBT作为中间格式进行DynamicOps的转换。
 * <p>
 * Reflection-based MapCodec implementation. Uses {@link ClassInfo} to automatically encode/decode
 * objects via reflection, converting through NBT as an intermediate format for DynamicOps operations.
 *
 * @param <A> 要编解码的对象类型 / the type of object to encode/decode
 */
public class ReflectionCodec<A> extends MapCodec<A> {
	/** 类信息，包含反射元数据 / Class information containing reflection metadata */
	ClassInfo info;

	/**
	 * 构造一个反射编解码器。
	 * <p>
	 * Constructs a reflection codec.
	 *
	 * @param info 要编解码的目标类 / the target class to encode/decode
	 */
	public ReflectionCodec(Class<A> info) {
		this.info=ClassInfo.valueOf(info);
	}
	/**
	 * 将序列化数据解码为对象。先将输入转换为NBT中间格式，再通过ClassInfo反序列化。
	 * <p>
	 * Decodes serialized data into an object. Converts input to NBT intermediate format first,
	 * then deserializes via ClassInfo.
	 *
	 * @param ops 动态操作接口 / the dynamic ops interface
	 * @param input 类映射形式的输入数据 / the input data in map-like form
	 * @param <T> 序列化格式类型 / the serialization format type
	 * @return 包含解码对象的DataResult / DataResult containing the decoded object
	 */
	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		CompoundTag nbt=new CompoundTag();
		input.entries().forEach(o->nbt.put(NbtOps.INSTANCE.getStringValue(ops.convertTo(NbtOps.INSTANCE, o.getFirst())).result().orElse(""), ops.convertTo(NbtOps.INSTANCE, o.getSecond())));
		return DataResult.success((A)info.fromNBT(nbt));
	}
	/**
	 * 将对象编码为序列化数据。先通过ClassInfo将对象转换为NBT，再转换为目标格式。
	 * <p>
	 * Encodes an object into serialized data. Converts the object to NBT via ClassInfo first,
	 * then converts to the target format.
	 *
	 * @param input 要编码的对象 / the object to encode
	 * @param ops 动态操作接口 / the dynamic ops interface
	 * @param prefix 记录构建器前缀 / the record builder prefix
	 * @param <T> 序列化格式类型 / the serialization format type
	 * @return 包含编码数据的RecordBuilder / RecordBuilder containing the encoded data
	 */
	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		CompoundTag nbt=(CompoundTag) info.toNBT(input);
		for(String in:nbt.getAllKeys())
			prefix.add(in, NbtOps.INSTANCE.convertTo(ops, nbt.get(in)));
		return prefix;
	}
	/**
	 * 返回此编解码器处理的所有字段键的流。
	 * <p>
	 * Returns a stream of all field keys handled by this codec.
	 *
	 * @param ops 动态操作接口 / the dynamic ops interface
	 * @param <T> 序列化格式类型 / the serialization format type
	 * @return 字段键的流 / a stream of field keys
	 */
	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return info.getFields().map(o->o.name).map(ops::createString);
	}
}
