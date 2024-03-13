package com.teammoeg.frostedheart.util.io;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

public class NullableCodec<A> implements Codec<A> {
	Codec<A> original;
	Supplier<A> defVal;

	public NullableCodec(final Codec<A> original) {
		super();
		this.original = original;
		this.defVal=()->null;
	}

	public NullableCodec(final Codec<A> original,final Supplier<A> defVal) {
		super();
		this.original = original;
		this.defVal = defVal;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(input==null)
			return DataResult.success(prefix);
		return original.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		System.out.println(input);
		DataResult<Pair<A, T>> actual= original.decode(ops, input);
		if(actual.result().isPresent()) {
			return actual;
		}
		return DataResult.success(Pair.of(defVal.get(), input));
	}

	@Override
	public MapCodec<A> fieldOf(String name) {
		return new DefaultValueCodec<A>(name,original,defVal);
	}

}
