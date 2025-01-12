package com.teammoeg.frostedheart.util.io.codec;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class CompressDifferMapCodec<A> extends MapCodec<A> {
	MapCodec<A> uncompressed;
	MapCodec<A> compressed;
	public CompressDifferMapCodec(MapCodec<A> uncompressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed=uncompressed.codec().fieldOf("value");
	}


	public CompressDifferMapCodec(MapCodec<A> uncompressed, MapCodec<A> compressed) {
		super();
		this.uncompressed = uncompressed;
		this.compressed = compressed;
	}


	@Override
	public String toString() {
		return "CompressDiff[U " + uncompressed +"C "+ compressed +"]";
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		if(ops.compressMaps()) {
			return compressed.decode(ops, input);
		}
		return uncompressed.decode(ops, input);
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		if(ops.compressMaps()) {
			return compressed.encode(input, ops,prefix);
		}
		return uncompressed.encode(input,ops,prefix);
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		if(ops.compressMaps()) {
			return compressed.keys(ops);
		}
		return uncompressed.keys(ops);
	}
	

}
