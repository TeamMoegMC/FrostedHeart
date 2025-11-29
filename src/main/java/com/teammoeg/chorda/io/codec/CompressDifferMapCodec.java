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

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class CompressDifferMapCodec<A> extends MapCodec<A> {
	MapCodec<A> uncompressed;
	MapCodec<A> compressed;
	public CompressDifferMapCodec(MapCodec<A> uncompressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed=uncompressed.codec().fieldOf("value");
	}


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
