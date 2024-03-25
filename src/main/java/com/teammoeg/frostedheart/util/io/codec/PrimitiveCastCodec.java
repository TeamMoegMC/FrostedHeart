package com.teammoeg.frostedheart.util.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class PrimitiveCastCodec<A,O> implements Codec<A> {
	Codec<O> codec;


	public PrimitiveCastCodec(Codec<O> codec) {
		super();
		this.codec = codec;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return codec.encode((O)input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return codec.decode(ops, input).map(t->t.mapFirst(o->(A)o));
	}

}
