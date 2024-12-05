package com.teammoeg.frostedheart.util.io.codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class AlternativeCodecBuilder<A>{
	List<Pair<Class<? extends A>,Codec<A>>> codecs=new ArrayList<>();
	Class<A> def;
	public AlternativeCodecBuilder(Class<A> clazz) {
		super();
		def=clazz;
	}
	public AlternativeCodecBuilder<A> add(Class<? extends A> clazz,Codec<? extends A> codec) {
		this.codecs.add(Pair.of(clazz, (Codec<A>)codec));
		return this;
	}
	public AlternativeCodecBuilder<A> add(Codec<? extends A> codec) {
		this.codecs.add(Pair.of(def, (Codec<A>)codec));
		return this;
	}
	public Codec<A> build() {
		return new Codec<A>() {

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				for(Pair<Class<? extends A>, Codec<A>> codec:codecs) {
					if(codec.getFirst().isInstance(input)) {
						DataResult<T> result=codec.getSecond().encode(input, ops, prefix);
						if(result.result().isPresent())
							return result;
					}
				}
				return DataResult.error(()->"No matching encodec present for "+input);
			}
		
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				for(Pair<Class<? extends A>, Codec<A>> codec:codecs) {
					DataResult<Pair<A, T>> result=codec.getSecond().decode(ops, input);
					
					if(result.result().isPresent())
						return result;
					//System.out.println("getClass "+codec.getFirst()+" Result "+result);
				}
				return DataResult.error(()->"No matching decodec present for "+input);
			}
			@Override
			public String toString() {
				StringBuilder sb=new StringBuilder("AlternativeCodec[");
				for(Pair<Class<? extends A>, Codec<A>> cod:codecs) {
					sb.append(cod.getFirst().getSimpleName());
					sb.append("-");
					sb.append(cod.getSecond());
					
				}
				sb.append("]");
				return sb.toString();
			}
		};
	}

}
