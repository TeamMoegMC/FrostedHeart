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

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

/**
 * 基于类型键的分派编解码器。根据"type"字段的值选择不同的MapCodec进行编解码。
 * <p>
 * A dispatch codec based on a type key. Selects different MapCodecs for encoding/decoding
 * based on the value of the "type" field.
 *
 * @param typeKey 类型键的字段名 / the field name of the type key
 * @param keyCodec 类型键值的编解码器 / the codec for type key values
 * @param forType 从对象获取类型键的函数 / the function to get the type key from an object
 * @param forCodec 从类型键获取MapCodec的函数 / the function to get a MapCodec from a type key
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 * @param <K> 类型键的类型 / the type of the type key
 */
public record KeyMapCodec<A, K>(String typeKey,Codec<K> keyCodec, Function<A, K> forType, Function<K, MapCodec<A>> forCodec) implements Codec<A> {


	/**
	 * 使用默认类型键"type"构造分派编解码器。
	 * <p>
	 * Constructs a dispatch codec using the default type key "type".
	 *
	 * @param keyCodec 类型键值的编解码器 / the codec for type key values
	 * @param forType 从对象获取类型键的函数 / the function to get the type key from an object
	 * @param forCodec 从类型键获取MapCodec的函数 / the function to get a MapCodec from a type key
	 */
	public KeyMapCodec(Codec<K> keyCodec,Function<A, K> forType, Function<K, MapCodec<A>> forCodec) {
		this("type",keyCodec, forType, forCodec);
	}
	/**
	 * 根据类型键获取对应的MapCodec。
	 * <p>
	 * Gets the corresponding MapCodec for the given type key.
	 *
	 * @param type 类型键值 / the type key value
	 * @return 包含MapCodec的DataResult / DataResult containing the MapCodec
	 */
	public DataResult<MapCodec<A>> getCodec(K type){
		MapCodec<A> codec = forCodec.apply(type);
		if (codec == null)
			return DataResult.error(() -> "Cannot find valid codec for type " + type);
		return DataResult.success(codec);
	}
	/**
	 * 获取对象的类型键。
	 * <p>
	 * Gets the type key for an object.
	 *
	 * @param input 输入对象 / the input object
	 * @return 包含类型键的DataResult / DataResult containing the type key
	 */
	public DataResult<K> getType(A input){
		K type = forType.apply(input);
		if (type == null)
			return DataResult.error(() -> "Cannot find valid codec for " + input);
		return DataResult.success(type);
	}
	
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		K type = forType.apply(input);
		if (type == null)
			return DataResult.error(() -> "Cannot find valid codec for " + input);


		MapCodec<A> codec = forCodec.apply(type);
		if (codec == null)
			return DataResult.error(() -> "Cannot find valid codec for type " + type);
		return codec.encode(input, ops,ops.mapBuilder().add(ops.createString(typeKey),keyCodec.encodeStart(ops, type))).build(prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return ops.get(input, typeKey).flatMap(t->keyCodec.parse(ops, t)).flatMap(this::getCodec).flatMap(t->ops.getMap(input).flatMap(d->t.decode(ops, d).map(o->Pair.of(o, input))));
	}
}
