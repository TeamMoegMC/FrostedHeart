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

public abstract class PrimitiveCastCodec implements Codec<Number>{
	public static final Codec<Number> INT=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.INT.encode(input.intValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.INT.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	public static final Codec<Number> LONG=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.LONG.encode(input.longValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.LONG.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	public static final Codec<Number> FLOAT=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.FLOAT.encode(input.floatValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.FLOAT.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	public static final Codec<Number> DOUBLE=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.DOUBLE.encode(input.doubleValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.DOUBLE.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	public static final Codec<Number> BYTE=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.BYTE.encode(input.byteValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.BYTE.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	public static final Codec<Number> SHORT=new PrimitiveCastCodec() {
		@Override
		public <T> DataResult<T> encode(Number input, DynamicOps<T> ops, T prefix) {
			return Codec.SHORT.encode(input.shortValue(), ops, prefix);
		}
		@Override
		public <T> DataResult<Pair<Number, T>> decode(DynamicOps<T> ops, T input) {
			return Codec.SHORT.decode(ops,input).map(t->t.mapFirst(o->(Number)o));
		}
	};
	private PrimitiveCastCodec() {}
}
