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

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

public record KeyMapCodec<A, K>(String typeKey,Codec<K> keyCodec, Function<A, K> forType, Function<K, MapCodec<A>> forCodec) implements Codec<A> {


	public KeyMapCodec(Codec<K> keyCodec,Function<A, K> forType, Function<K, MapCodec<A>> forCodec) {
		this("type",keyCodec, forType, forCodec);
	}
	public DataResult<MapCodec<A>> getCodec(K type){
		MapCodec<A> codec = forCodec.apply(type);
		if (codec == null)
			return DataResult.error(() -> "Cannot find valid codec for type " + type);
		return DataResult.success(codec);
	}
	public DataResult<K> getType(A input){
		K type = forType.apply(input);
		if (type == null)
			return DataResult.error(() -> "Cannot find valid codec for " + input);
		return DataResult.success(type);
	}
	
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		K type = forType.apply(input);
		if (type == null)
			return DataResult.error(() -> "Cannot find valid codec for " + input);


		MapCodec<A> codec = forCodec.apply(type);
		if (codec == null)
			return DataResult.error(() -> "Cannot find valid codec for type " + type);
		return codec.encode(input, ops,ops.mapBuilder().add(ops.createString(typeKey),keyCodec.encodeStart(ops, type))).build(prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return ops.get(input, typeKey).flatMap(t->keyCodec.parse(ops, t)).flatMap(this::getCodec).flatMap(t->ops.getMap(input).flatMap(d->t.decode(ops, d).map(o->Pair.of(o, input))));
	}
}
