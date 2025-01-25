/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.io.codec;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.core.Registry;

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
		return reg.get().byNameCodec().encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return Codec.INT.decode(ops, input).map(o->o.mapFirst(reg.get()::byId));
		}
		return reg.get().byNameCodec().decode(ops, input);
	}

	@Override
	public String toString() {
		return "Registry[reg=" + reg + "]";
	}

}
