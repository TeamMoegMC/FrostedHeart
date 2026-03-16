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

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.chorda.io.CodecUtil;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 双模式编解码器。在压缩模式下使用字节缓冲区序列化（适用于网络传输），在非压缩模式下使用schema编解码器（适用于磁盘存储）。
 * <p>
 * Dual-mode codec. Uses byte buffer serialization in compressed mode (suitable for network transfer),
 * and schema codec in uncompressed mode (suitable for disk storage).
 *
 * @param <A> 实际对象的类型 / the type of the actual object
 * @param <S> schema中间表示的类型 / the type of the schema intermediate representation
 */
public class PacketOrSchemaCodec<A,S> implements Codec<A> {
	Function<A,DataResult<S>> schemaSerialize;
	Function<S,DataResult<A>> schemaDeserialize;
	BiConsumer<A,FriendlyByteBuf> bufferSerialize;
	Function<byte[],A> bufferDeserialize;
	Codec<S> schemaCodec;
	/**
	 * 创建一个双模式编解码器的便捷工厂方法。
	 * <p>
	 * Convenience factory method for creating a dual-mode codec.
	 *
	 * @param schema schema编解码器 / the schema codec
	 * @param schemaSerialize schema序列化函数 / the schema serialization function
	 * @param schemaDeserialize schema反序列化函数 / the schema deserialization function
	 * @param bufferSerialize 缓冲区序列化函数 / the buffer serialization function
	 * @param bufferDeserialize 缓冲区反序列化函数 / the buffer deserialization function
	 * @param <A> 实际对象的类型 / the type of the actual object
	 * @param <S> schema中间表示的类型 / the type of the schema intermediate representation
	 * @return 双模式编解码器实例 / the dual-mode codec instance
	 */
	public static <A,S> PacketOrSchemaCodec<A,S> create(Codec<S> schema, Function<A, S> schemaSerialize, Function<S, A> schemaDeserialize, BiConsumer<A, FriendlyByteBuf> bufferSerialize, Function<FriendlyByteBuf, A> bufferDeserialize) {
		return new PacketOrSchemaCodec<>(schema,
		schemaSerialize.andThen(DataResult::success),
		schemaDeserialize.andThen(DataResult::success),
		bufferSerialize,
		bs->bufferDeserialize.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(bs))));
	}
	/**
	 * 构造一个双模式编解码器。
	 * <p>
	 * Constructs a dual-mode codec.
	 *
	 * @param schema schema编解码器 / the schema codec
	 * @param schemaSerialize schema序列化函数（返回DataResult） / schema serialization function (returns DataResult)
	 * @param schemaDeserialize schema反序列化函数（返回DataResult） / schema deserialization function (returns DataResult)
	 * @param bufferSerialize 缓冲区序列化函数 / the buffer serialization function
	 * @param bufferDeserialize 缓冲区反序列化函数 / the buffer deserialization function
	 */
	public PacketOrSchemaCodec(Codec<S> schema, Function<A, DataResult<S>> schemaSerialize, Function<S, DataResult<A>> schemaDeserialize, BiConsumer<A, FriendlyByteBuf> bufferSerialize, Function<FriendlyByteBuf, A> bufferDeserialize) {
		super();
		this.schemaSerialize = schemaSerialize;
		this.schemaDeserialize = schemaDeserialize;
		this.bufferSerialize = bufferSerialize;
		this.bufferDeserialize = bs->bufferDeserialize.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(bs)));
		schemaCodec=schema;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(ops.compressMaps()) {
			FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
			bufferSerialize.accept(input, buffer);
			byte[] ba=new byte[buffer.writerIndex()];
			buffer.getBytes(0, ba);
			return CodecUtil.BYTE_ARRAY_CODEC.encode(ba, ops, prefix);
		}
		return schemaSerialize.apply(input).flatMap(t->schemaCodec.encode(t, ops, prefix) );
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return CodecUtil.BYTE_ARRAY_CODEC.decode(ops, input).map(t->t.mapFirst(bufferDeserialize));
		}
		DataResult<Pair<S, T>> obj=schemaCodec.decode(ops, input);
		return obj.flatMap(t->schemaDeserialize.apply(t.getFirst()).map(o->Pair.of(o, input)));
	}

	@Override
	public String toString() {
		return "PacketOrSchema["+schemaCodec+"]";
	}

}
