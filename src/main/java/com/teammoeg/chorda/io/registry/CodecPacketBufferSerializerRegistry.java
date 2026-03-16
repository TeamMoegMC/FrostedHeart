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

package com.teammoeg.chorda.io.registry;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.chorda.io.PacketWritable;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 基于Codec的数据包缓冲区序列化器注册表。结合了{@link PacketBufferSerializerRegistry}的数据包序列化能力
 * 和Mojang的{@link Codec}接口，支持通过DynamicOps进行编解码。
 * <p>
 * Codec-based packet buffer serializer registry. Combines the packet serialization capability
 * of {@link PacketBufferSerializerRegistry} with Mojang's {@link Codec} interface, supporting
 * encoding/decoding through DynamicOps.
 *
 * @param <A> 可写入数据包的对象类型 / the packet-writable object type
 */
public class CodecPacketBufferSerializerRegistry<A extends PacketWritable> extends PacketBufferSerializerRegistry<A,Dynamic<?>,Dynamic<?>> implements Codec<A>{

	/**
	 * 构造一个基于Codec的数据包缓冲区序列化器注册表。
	 * <p>
	 * Constructs a codec-based packet buffer serializer registry.
	 */
	public CodecPacketBufferSerializerRegistry() {
	}

	/**
	 * 将对象编码为指定DynamicOps格式的数据。
	 * <p>
	 * Encodes an object into data in the specified DynamicOps format.
	 *
	 * @param input 要编码的对象 / the object to encode
	 * @param ops 动态操作接口 / the dynamic ops interface
	 * @param prefix 编码前缀 / the encoding prefix
	 * @param <T> 序列化格式类型 / the serialization format type
	 * @return 包含编码结果的DataResult / DataResult containing the encoded result
	 */
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return DataResult.success((T)super.write(input,new Dynamic<>(ops,prefix)).getValue());
	}

	/**
	 * 从指定DynamicOps格式的数据解码对象。
	 * <p>
	 * Decodes an object from data in the specified DynamicOps format.
	 *
	 * @param ops 动态操作接口 / the dynamic ops interface
	 * @param input 要解码的输入数据 / the input data to decode
	 * @param <T> 序列化格式类型 / the serialization format type
	 * @return 包含解码对象和剩余输入的DataResult / DataResult containing the decoded object and remaining input
	 */
	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(super.read(new Dynamic<>(ops,input)), input));
	}

	/** {@inheritDoc} */
	@Override
	protected void writeType(Pair<Integer, String> type, Dynamic<?> obj) {
		obj.merge(obj.createString("type"), obj.createString(type.getSecond()));
	}

	/** {@inheritDoc} */
	@Override
	protected String readType(Dynamic<?> obj) {
		return obj.asString().result().orElse(null);
	}

	/**
	 * 注册一个类型，使用Codec进行序列化和数据包读取函数进行网络传输。
	 * <p>
	 * Registers a type using a Codec for serialization and a packet reading function for network transport.
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型标识符字符串 / the type identifier string
	 * @param codec 用于编解码的Codec / the Codec for encoding/decoding
	 * @param packet 从数据包缓冲区读取对象的函数 / function to read the object from a packet buffer
	 */
	public void register(Class<A> cls, String type, Codec<A> codec, Function<FriendlyByteBuf, A> packet) {
		super.register(cls, type,
			t->codec.decode(t).result().map(Pair::getFirst).orElse(null),
			(t,c)->((Dynamic<Object>)c).map(x->codec.encode(t,((Dynamic<Object>)c).getOps(),x).result().orElse(null)),
			packet);
	}




}
