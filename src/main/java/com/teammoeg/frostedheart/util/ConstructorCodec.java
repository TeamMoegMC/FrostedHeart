package com.teammoeg.frostedheart.util;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class ConstructorCodec<A> implements Codec<A>{
	Codec<A> codec;
	Supplier<A> constructor;


	public ConstructorCodec(Codec<A> codec, Supplier<A> constructor) {
		super();
		this.codec = codec;
		this.constructor = constructor;
	}

	public A getInstance() {
		return constructor.get();
	}
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return codec.encode( input,ops, prefix);
	}


	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(codec.parse(ops, input).result().orElseGet(()->constructor.get()),input));
	}


}
