package com.teammoeg.frostedheart.util.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class MapPathCodec<A> implements Codec<A> {
	Codec<A> codec;
	String[] path;

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
