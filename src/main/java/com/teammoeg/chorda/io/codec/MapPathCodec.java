/*
 * Copyright (c) 2026 TeamMoeg
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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * 嵌套路径编解码器。在编码时将值包装在嵌套的Map路径中，解码时按路径深入获取值。
 * <p>
 * Nested path codec. Wraps values in nested Map paths during encoding,
 * and traverses the path to retrieve values during decoding.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class MapPathCodec<A> implements Codec<A> {
	Codec<A> codec;
	String[] path;

	/**
	 * 构造一个嵌套路径编解码器。
	 * <p>
	 * Constructs a nested path codec.
	 *
	 * @param codec 值的编解码器 / the codec for the value
	 * @param path 嵌套的路径键名数组 / the array of nested path key names
	 */
	public MapPathCodec(Codec<A> codec, String... path) {
		super();
		this.codec = codec;
		this.path = path;
	}



	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		DataResult<T> result=codec.encodeStart(ops, input);
		for(int i=path.length-1;i>=0;i--) {
			final String cur=path[i];
			result=result.flatMap(o->ops.mergeToMap(ops.emptyMap(), ops.createString(cur), o));
		}
		return result;
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<T> ret=DataResult.success(input);
		for(int i=0;i<path.length;i++) {
			final String cur=path[i];
			
			ret=ret.flatMap(o->ops.get(o, cur));

		}
		return ret.flatMap(o->codec.decode(ops, o));
	}

}
