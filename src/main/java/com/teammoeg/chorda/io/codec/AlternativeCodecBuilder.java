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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class AlternativeCodecBuilder<A>{
	public static record CodecType<A>(Class<? extends A> clazz,Codec<A> codec,boolean saveOnly) {}
	List<CodecType<A>> codecs=new ArrayList<>();
	Class<? super A> def;
	Supplier<A> fallback;
	public AlternativeCodecBuilder(Class<? super A> clazz) {
		super();
		def=clazz;
	}
	public AlternativeCodecBuilder<A> add(Class<? extends A> clazz,Codec<? extends A> codec) {
		this.codecs.add(new CodecType<>(clazz, (Codec<A>)codec,false));
		return this;
	}
	public AlternativeCodecBuilder<A> addSaveOnly(Class<? extends A> clazz,Codec<? extends A> codec) {
		this.codecs.add(new CodecType<>(clazz, (Codec<A>)codec,true));
		return this;
	}
	public AlternativeCodecBuilder<A> add(Codec<? extends A> codec) {
		this.codecs.add(new CodecType(def, (Codec<A>)codec,false));
		return this;
	}
	public AlternativeCodecBuilder<A> fallback(Supplier<? extends A> codec) {
		this.fallback=(Supplier<A>)codec;
		return this;
	}
	public Codec<A> build() {
		return new Codec<A>() {

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				for(CodecType<A> codec:codecs) {
					if(codec.clazz().isInstance(input)) {
						DataResult<T> result=codec.codec().encode(input, ops, prefix);
						if(result.result().isPresent())
							return result;
					}
				}
				if(fallback!=null)
					return DataResult.success(ops.empty());
				return DataResult.error(()->"No matching encodec present for "+input);
			}
		
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				for(CodecType<A> codec:codecs) {
					if(codec.saveOnly())
						continue;
					DataResult<Pair<A, T>> result=codec.codec().decode(ops, input);
					
					if(result.result().isPresent())
						return result;
					//System.out.println("getClass "+codec.getFirst()+" Result "+result);
				}
				if(fallback!=null)
					return DataResult.success(Pair.of(fallback.get(), input));
				return DataResult.error(()->"No matching decodec present for "+input);
			}
			@Override
			public String toString() {
				StringBuilder sb=new StringBuilder("AlternativeCodec[");
				for(CodecType<A> cod:codecs) {
					sb.append(cod.clazz().getSimpleName());
					sb.append("-");
					sb.append(cod.codec());
					
				}
				sb.append("]");
				return sb.toString();
			}
		};
	}

}
