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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * 备选编解码器构建器。允许注册多个编解码器，按顺序尝试编码/解码，直到找到匹配的编解码器。
 * <p>
 * Alternative codec builder. Allows registering multiple codecs and tries them in order
 * during encoding/decoding until a matching codec is found.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class AlternativeCodecBuilder<A>{
	/**
	 * 编解码器类型记录，包含类信息、编解码器实例和是否仅用于保存的标志。
	 * <p>
	 * Codec type record containing class info, codec instance, and a save-only flag.
	 *
	 * @param clazz 关联的类 / the associated class
	 * @param codec 编解码器实例 / the codec instance
	 * @param saveOnly 是否仅用于编码（保存） / whether this codec is used for encoding (saving) only
	 * @param <A> 编解码器处理的类型 / the type handled by the codec
	 */
	public static record CodecType<A>(Class<? extends A> clazz,Codec<A> codec,boolean saveOnly) {}
	List<CodecType<A>> codecs=new ArrayList<>();
	Class<? super A> def;
	Supplier<A> fallback;
	/**
	 * 构造一个备选编解码器构建器。
	 * <p>
	 * Constructs an alternative codec builder.
	 *
	 * @param clazz 默认的基类类型 / the default base class type
	 */
	public AlternativeCodecBuilder(Class<? super A> clazz) {
		super();
		def=clazz;
	}
	/**
	 * 添加一个与指定类关联的编解码器。
	 * <p>
	 * Adds a codec associated with the specified class.
	 *
	 * @param clazz 关联的类 / the associated class
	 * @param codec 编解码器 / the codec
	 * @return 此构建器实例 / this builder instance
	 */
	public AlternativeCodecBuilder<A> add(Class<? extends A> clazz,Codec<? extends A> codec) {
		this.codecs.add(new CodecType<>(clazz, (Codec<A>)codec,false));
		return this;
	}
	/**
	 * 添加一个仅用于编码（保存）的编解码器。
	 * <p>
	 * Adds a codec that is only used for encoding (saving), skipped during decoding.
	 *
	 * @param clazz 关联的类 / the associated class
	 * @param codec 编解码器 / the codec
	 * @return 此构建器实例 / this builder instance
	 */
	public AlternativeCodecBuilder<A> addSaveOnly(Class<? extends A> clazz,Codec<? extends A> codec) {
		this.codecs.add(new CodecType<>(clazz, (Codec<A>)codec,true));
		return this;
	}
	/**
	 * 添加一个使用默认基类的编解码器。
	 * <p>
	 * Adds a codec using the default base class.
	 *
	 * @param codec 编解码器 / the codec
	 * @return 此构建器实例 / this builder instance
	 */
	public AlternativeCodecBuilder<A> add(Codec<? extends A> codec) {
		this.codecs.add(new CodecType(def, (Codec<A>)codec,false));
		return this;
	}
	/**
	 * 设置回退值提供器，当没有编解码器匹配时使用。
	 * <p>
	 * Sets a fallback value supplier used when no codec matches.
	 *
	 * @param codec 回退值提供器 / the fallback value supplier
	 * @return 此构建器实例 / this builder instance
	 */
	public AlternativeCodecBuilder<A> fallback(Supplier<? extends A> codec) {
		this.fallback=(Supplier<A>)codec;
		return this;
	}
	/**
	 * 构建最终的编解码器。
	 * <p>
	 * Builds the final codec.
	 *
	 * @return 组合后的备选编解码器 / the combined alternative codec
	 */
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
