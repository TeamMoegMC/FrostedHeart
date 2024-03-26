package com.teammoeg.frostedheart.util.io.codec;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class DefaultValueCodec<A> implements Codec<A> {
	Codec<A> original;
	Supplier<A> defVal;

	public DefaultValueCodec(final Codec<A> original) {
		super();
		this.original = original;
		this.defVal=()->null;
	}

	public DefaultValueCodec(final Codec<A> original,final Supplier<A> defVal) {
		super();
		this.original = original;
		this.defVal = defVal;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(input==null)
			return DataResult.success(ops.empty());
		return original.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<Pair<A, T>> actual= original.decode(ops, input);
		if(actual.result().isPresent()) {
			return actual;
		}
		return DataResult.success(Pair.of(defVal.get(), input));
	}

	@Override
	public DefaultValueMapCodec<A> fieldOf(String name) {
		return new DefaultValueMapCodec<A>(name,original,defVal);
	}

}
