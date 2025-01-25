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

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class KeysCodec<A> extends MapCodec<A> {
	String[] keys;
	Codec<A> codec;
	public KeysCodec(Codec<A> codec,String...strings) {
		this.keys=strings;
		this.codec=codec;
	}
	Function def;
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
