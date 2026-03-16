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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * 原始类型转换编解码器。将Number编码为特定的原始类型（int、long、float、double、byte、short），
 * 解码时也将其转换回Number。提供各原始类型的静态实例。
 * <p>
 * Primitive type cast codec. Encodes Number as a specific primitive type (int, long, float, double, byte, short),
 * and converts back to Number during decoding. Provides static instances for each primitive type.
 */
public abstract class PrimitiveCastCodec implements Codec<Number>{
	/** 整型转换编解码器。 / Integer cast codec. */
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
	/** 长整型转换编解码器。 / Long cast codec. */
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
	/** 浮点型转换编解码器。 / Float cast codec. */
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
	/** 双精度浮点型转换编解码器。 / Double cast codec. */
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
	/** 字节型转换编解码器。 / Byte cast codec. */
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
	/** 短整型转换编解码器。 / Short cast codec. */
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
