package com.teammoeg.chorda.util.io.codec;

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class DefaultValueCodec<A> implements Codec<A> {
	Codec<A> original;
	Supplier<A> defVal;

	public DefaultValueCodec(final Codec<A> original) {
		super();
		this.original = original;
		this.defVal=()->null;
	}

	public DefaultValueCodec(final Codec<A> original,final Supplier<A> defVal) {
		super();
		this.original = original;
		this.defVal = defVal;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(input==null)
			return DataResult.success(ops.empty());
		return original.encode(input, ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		DataResult<Pair<A, T>> actual= original.decode(ops, input);
		if(actual.result().isPresent()) {
			return actual;
		}
		return DataResult.success(Pair.of(defVal.get(), input));
	}

	@Override
	public MapCodec<A> fieldOf(String name) {
		return new MapCodec<A> (){
		    @Override
		    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
		        final T value = input.get(name);
		        if (value != null) {
			        final DataResult<A> parsed = original.parse(ops, value);
			        if (parsed.result().isPresent()) {
			            return parsed;
			        }
			    }
		        return DataResult.success(defVal.get());
		    }

		    @Override
		    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, RecordBuilder<T> prefix) {
		    	if (input!=null) {
		        	DataResult<T> result=original.encodeStart(ops, input);
		            prefix=prefix.add(name, result);
		        }
		        return prefix;
		    }

		    @Override
		    public <T> Stream<T> keys(final DynamicOps<T> ops) {
		        return Stream.of(ops.createString(name));
		    }

		    @Override
		    public String toString() {
		        return name + ":" + defVal.get() + " " + original;
		    }
		};
	}

}
