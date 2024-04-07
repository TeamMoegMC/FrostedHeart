package com.teammoeg.frostedheart.util.io.codec;

import java.util.Arrays;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class KeysCodec<A> extends MapCodec<A> {
	String[] keys;
	Codec<A> codec;
	public KeysCodec(Codec<A> codec,String...strings) {
		this.keys=strings;
		this.codec=codec;
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		for(String s:keys) {
			T result=input.get(s);
			if(result!=null) {
				return codec.parse(ops, result);
			}
		}
		return DataResult.error("No any of "+Arrays.toString(keys)+" present.");
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		return prefix.add(keys[0], codec.encodeStart(ops, input));
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.of(ops.createString(keys[0]));
	}

}
