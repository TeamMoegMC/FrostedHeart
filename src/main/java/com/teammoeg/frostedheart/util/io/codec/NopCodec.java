package com.teammoeg.frostedheart.util.io.codec;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class NopCodec<A> implements Codec<A> {
	Supplier<A> generator;
	public NopCodec(A value) {
		super();
		this.generator =()-> value;
	}
	public NopCodec(Supplier<A> generator) {
		super();
		this.generator = generator;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return DataResult.success(ops.empty());
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(generator.get(), input));
	}
	
}
