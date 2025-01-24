package com.teammoeg.chorda.io;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public record CodecWithFactory<A>(Codec<A> codec, Supplier<A> factory) implements Codec<A>{

	public A getInstance() {
		return factory.get();
	}
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return codec.encode( input,ops, prefix);
	}


	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(codec.parse(ops, input).result().orElseGet(factory),input));
	}


}
