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

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * 根据DynamicOps是否压缩来选择不同MapCodec的实现。与{@link CompressDifferCodec}类似，但用于MapCodec。
 * <p>
 * A MapCodec implementation that selects different MapCodecs based on whether DynamicOps is compressed.
 * Similar to {@link CompressDifferCodec}, but for MapCodec.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class CompressDifferMapCodec<A> extends MapCodec<A> {
	MapCodec<A> uncompressed;
	MapCodec<A> compressed;
	/**
	 * 构造一个压缩差异MapCodec，压缩模式下将值包装在"value"字段中。
	 * <p>
	 * Constructs a compress-differ MapCodec, wrapping the value in a "value" field for compressed mode.
	 *
	 * @param uncompressed 非压缩模式下使用的MapCodec / the MapCodec used in uncompressed mode
	 */
	public CompressDifferMapCodec(MapCodec<A> uncompressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed=uncompressed.codec().fieldOf("value");
	}


	/**
	 * 构造一个压缩差异MapCodec，可分别指定压缩和非压缩编解码器。
	 * <p>
	 * Constructs a compress-differ MapCodec with separately specified compressed and uncompressed codecs.
	 *
	 * @param uncompressed 非压缩模式下使用的MapCodec / the MapCodec used in uncompressed mode
	 * @param compressed 压缩模式下使用的MapCodec / the MapCodec used in compressed mode
	 */
	public CompressDifferMapCodec(MapCodec<A> uncompressed, MapCodec<A> compressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed = compressed;
	}


	@Override
	public String toString() {
		return "CompressDiff[U " + uncompressed +"C "+ compressed +"]";
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		if(ops.compressMaps()) {
			return compressed.decode(ops, input);
		}
		return uncompressed.decode(ops, input);
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		if(ops.compressMaps()) {
			return compressed.encode(input, ops,prefix);
		}
		return uncompressed.encode(input,ops,prefix);
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		if(ops.compressMaps()) {
			return compressed.keys(ops);
		}
		return uncompressed.keys(ops);
	}
	

}
