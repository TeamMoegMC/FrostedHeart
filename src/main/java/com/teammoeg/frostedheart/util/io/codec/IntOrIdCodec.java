package com.teammoeg.frostedheart.util.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.util.registry.Registry;

public class IntOrIdCodec<A> implements Codec<A> {
	Registry<A> reg;
	public IntOrIdCodec(Registry<A> registry) {
		reg=registry;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(ops.compressMaps()) {
			return Codec.INT.encode(reg.getId(input), ops, prefix);
		}
		return reg.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return Codec.INT.decode(ops, input).map(o->o.mapFirst(reg::getByValue));
		}
		return reg.decode(ops, input);
	}

}
