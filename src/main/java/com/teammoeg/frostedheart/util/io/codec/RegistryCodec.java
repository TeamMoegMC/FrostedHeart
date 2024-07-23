package com.teammoeg.frostedheart.util.io.codec;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.util.registry.Registry;

public class RegistryCodec<A> implements Codec<A> {
	Supplier<Registry<A>> reg;
	public RegistryCodec(Supplier<Registry<A>> registry) {
		reg=registry;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		//System.out.println("Encoding id");
		if(ops.compressMaps()) {
			return Codec.INT.encode(reg.get().getId(input), ops, prefix);
		}
		return reg.get().encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return Codec.INT.decode(ops, input).map(o->o.mapFirst(reg.get()::byId));
		}
		return reg.get().decode(ops, input);
	}

	@Override
	public String toString() {
		return "Registry[reg=" + reg + "]";
	}

}
