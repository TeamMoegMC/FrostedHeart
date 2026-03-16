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

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * 多键名MapCodec。解码时按顺序尝试多个键名读取值，编码时使用第一个键名写入。
 * <p>
 * Multi-key MapCodec. Tries multiple key names in order during decoding,
 * and uses the first key name for encoding.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class KeysCodec<A> extends MapCodec<A> {
	String[] keys;
	Codec<A> codec;
	/**
	 * 构造一个多键名MapCodec。
	 * <p>
	 * Constructs a multi-key MapCodec.
	 *
	 * @param codec 值的编解码器 / the codec for the value
	 * @param strings 可用的键名列表 / the list of available key names
	 */
	public KeysCodec(Codec<A> codec,String...strings) {
		this.keys=strings;
		this.codec=codec;
	}
	Function def;
	/**
	 * 构造一个带默认值函数的多键名MapCodec。
	 * <p>
	 * Constructs a multi-key MapCodec with a default value function.
	 *
	 * @param codec 值的编解码器 / the codec for the value
	 * @param def 当所有键都不存在时提供默认值的函数 / the function to provide default value when all keys are absent
	 * @param strings 可用的键名列表 / the list of available key names
	 * @param <T> DynamicOps的类型参数 / the type parameter of DynamicOps
	 */
	public <T> KeysCodec(Codec<A> codec,Function<DynamicOps<T>,T> def,String...strings) {
		this.keys=strings;
		this.codec=codec;
		this.def=def;
	}
	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		for(String s:keys) {
			T result=input.get(s);
			if(result!=null) {
				return codec.parse(ops, result);
			}
		}
		if(def!=null)
			return codec.parse(ops, (T)def.apply(ops));
		return DataResult.error(()->"No any of "+Arrays.toString(keys)+" present.");
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		return prefix.add(keys[0], codec.encodeStart(ops, input));
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.of(ops.createString(keys[0]));
	}

}
