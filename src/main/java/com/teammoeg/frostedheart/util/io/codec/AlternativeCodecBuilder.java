package com.teammoeg.frostedheart.util.io.codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class AlternativeCodecBuilder<A>{
	public static record CodecType<A>(Class<? extends A> clazz,Codec<A> codec,boolean saveOnly) {}
	List<CodecType<A>> codecs=new ArrayList<>();
	Class<A> def;
	public AlternativeCodecBuilder(Class<A> clazz) {
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
