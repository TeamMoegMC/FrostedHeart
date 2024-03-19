package com.teammoeg.frostedheart.util.io.codec;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.teammoeg.frostedheart.util.IterateUtils;
import com.teammoeg.frostedheart.util.utility.MutablePair;

public class JointCodec<P1,P2,A> extends MapCodec<A> {
	public static class DynamicJointCodec<A> extends MapCodec<A> {
		List<MapCodec<Object>> codecs;
		Function<Object[],A> func;
		List<Function<A,Object>> toPs;
		
		public DynamicJointCodec(List<MapCodec<Object>> codecs, Function<Object[], A> func, List<Function<A, Object>> toPs) {
			super();
			this.codecs = codecs;
			this.func = func;
			this.toPs = toPs;
		}

		@Override
		public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
			Object[] objs=new Object[codecs.size()];
			for(int i=0;i<objs.length;i++) {
				DataResult<?> p1=codecs.get(i).decode(ops, input);
				if(!p1.result().isPresent())
					return DataResult.error(p1.error().map(t->t.message()).orElse("Value "+i+" is not present."));
				objs[i]=p1.result().orElse(null);
			}
			return DataResult.success(func.apply(objs));
		}

		@Override
		public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			for(MutablePair<Function<A, Object>, MapCodec<Object>> codec:IterateUtils.joinAnd(toPs, codecs)) {
				prefix=codec.getSecond().encode(codec.getFirst().apply(input), ops, prefix);
			}
			return prefix;
		}

		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return codecs.stream().flatMap(t->t.keys(ops));
		}
	}
	MapCodec<P1> codec1;
	MapCodec<P2> codec2;
	BiFunction<P1,P2,A> func;
	Function<A,P1> toP1;
	Function<A,P2> toP2;
	
	public JointCodec(MapCodec<P1> codec1, MapCodec<P2> codec2, BiFunction<P1, P2, A> func, Function<A, P1> toP1, Function<A, P2> toP2) {
		super();
		this.codec1 = codec1;
		this.codec2 = codec2;
		this.func = func;
		this.toP1 = toP1;
		this.toP2 = toP2;
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		DataResult<P1> p1=codec1.decode(ops, input);
		if(!p1.result().isPresent())
			return DataResult.error(p1.error().map(t->t.message()).orElse("Value 1 is not present."));
		DataResult<P2> p2=codec2.decode(ops, input);
		if(!p2.result().isPresent())
			return DataResult.error(p2.error().map(t->t.message()).orElse("Value 2 is not present."));
		return DataResult.success(func.apply(p1.result().get(), p2.result().get()));
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		return codec2.encode(toP2.apply(input), ops, codec1.encode(toP1.apply(input), ops, prefix));
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.concat(codec1.keys(ops), codec2.keys(ops));
	}
}
