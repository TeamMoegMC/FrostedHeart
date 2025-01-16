package com.teammoeg.chorda.util.io.codec;

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
