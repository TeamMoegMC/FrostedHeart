package com.teammoeg.frostedheart.util.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.util.registry.Registry;

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
		return uncompressed.decode(ops, input);
	}

}
