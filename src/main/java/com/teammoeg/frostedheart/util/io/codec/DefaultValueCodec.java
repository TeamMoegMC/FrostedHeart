package com.teammoeg.frostedheart.util.io.codec;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class DefaultValueCodec<A> extends MapCodec<A> {
    private final String name;
    private final Codec<A> elementCodec;
    private final Supplier<A> defVal;
    public DefaultValueCodec(final String name, final Codec<A> elementCodec, final Supplier<A> defaultValue) {
        this.name = name;
        this.elementCodec = elementCodec;
        this.defVal = defaultValue;
    }

    @Override
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final T value = input.get(name);
        if (value != null) {
	        final DataResult<A> parsed = elementCodec.parse(ops, value);
	        if (parsed.result().isPresent()) {
	            return parsed;
	        }
	    }
        return DataResult.success(defVal.get());
    }

    @Override
    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, RecordBuilder<T> prefix) {
        System.out.println(name);
    	if (input!=null) {
        	DataResult<T> result=elementCodec.encodeStart(ops, input);
            prefix=prefix.add(name, result);
        }
        return prefix;
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return Stream.of(ops.createString(name));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultValueCodec<?> that = (DefaultValueCodec<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(elementCodec, that.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, elementCodec);
    }

    @Override
    public String toString() {
        return name + ":" + defVal.get() + " " + elementCodec;
    }
}
