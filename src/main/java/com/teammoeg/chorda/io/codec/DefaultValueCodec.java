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

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * 带默认值的编解码器包装器。当解码失败时返回默认值，当编码null值时输出空。
 * <p>
 * A codec wrapper with default values. Returns the default value when decoding fails,
 * and outputs empty when encoding null values.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class DefaultValueCodec<A> implements Codec<A> {
	Codec<A> original;
	Supplier<A> defVal;

	/**
	 * 构造一个默认值编解码器，默认值为null。
	 * <p>
	 * Constructs a default value codec with null as the default.
	 *
	 * @param original 原始编解码器 / the original codec
	 */
	public DefaultValueCodec(final Codec<A> original) {
		super();
		this.original = original;
		this.defVal=()->null;
	}

	/**
	 * 构造一个带指定默认值提供器的编解码器。
	 * <p>
	 * Constructs a default value codec with a specified default value supplier.
	 *
	 * @param original 原始编解码器 / the original codec
	 * @param defVal 默认值提供器 / the default value supplier
	 */
	public DefaultValueCodec(final Codec<A> original,final Supplier<A> defVal) {
		super();
		this.original = original;
		this.defVal = defVal;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(input==null)
			return DataResult.success(ops.empty());
		return original.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<Pair<A, T>> actual= original.decode(ops, input);
		if(actual.result().isPresent()) {
			return actual;
		}
		return DataResult.success(Pair.of(defVal.get(), input));
	}

	@Override
	public MapCodec<A> fieldOf(String name) {
		return new MapCodec<A> (){
		    @Override
		    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
		        final T value = input.get(name);
		        if (value != null) {
			        final DataResult<A> parsed = original.parse(ops, value);
			        if (parsed.result().isPresent()) {
			            return parsed;
			        }
			    }
		        return DataResult.success(defVal.get());
		    }

		    @Override
		    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, RecordBuilder<T> prefix) {
		    	if (input!=null) {
		        	DataResult<T> result=original.encodeStart(ops, input);
		            prefix=prefix.add(name, result);
		        }
		        return prefix;
		    }

		    @Override
		    public <T> Stream<T> keys(final DynamicOps<T> ops) {
		        return Stream.of(ops.createString(name));
		    }

		    @Override
		    public String toString() {
		        return name + ":" + defVal.get() + " " + original;
		    }
		};
	}

}
