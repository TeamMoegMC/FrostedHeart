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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class CompressDifferCodec<A> implements Codec<A> {
	Codec<A> uncompressed;
	Codec<A> compressed;
	public CompressDifferCodec(Codec<A> uncompressed, Codec<A> compressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed = compressed;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(ops.compressMaps()) {
			return compressed.encode(input, ops, prefix);
		}
		return uncompressed.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return compressed.decode(ops, input);
		}
		DataResult<Pair<A, T>> res= uncompressed.decode(ops, input);
		return res;
	}

	@Override
	public String toString() {
		return "CompressDiff[U " + uncompressed + " C " + compressed + "]";
	}
	

}
