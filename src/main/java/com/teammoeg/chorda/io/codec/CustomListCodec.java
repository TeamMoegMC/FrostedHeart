package com.teammoeg.chorda.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public class CustomListCodec<A,L extends Collection<A>> implements Codec<L> {
    private final Codec<A> elementCodec;
    private final Supplier<L> factory;
    public CustomListCodec(final Codec<A> elementCodec,Supplier<L> factory) {
        this.elementCodec = elementCodec;
        this.factory=factory;
    }

    @Override
    public <T> DataResult<T> encode(final L input, final DynamicOps<T> ops, final T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();
        for (final A a : input) {
            builder.add(elementCodec.encodeStart(ops, a));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<L, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getList(input).flatMap(t->{
        	L obj=factory.get();
        	StringBuilder sb=new StringBuilder();
        	t.accept(o->{
        		DataResult<Pair<A,T>> a=elementCodec.decode(ops,input);
        		Pair<A,T> at=a.getOrThrow(true, sb::append);
        		obj.add(at.getFirst());
        	});
        	if(sb.length()!=0)
        		return DataResult.success(Pair.of(obj, input));
        	return DataResult.error(sb::toString,Pair.of(obj, input));
        });
    }


    @Override
	public int hashCode() {
		return Objects.hash(elementCodec, factory);
	}

    @Override
    public String toString() {
        return "CustomListCodec[" + elementCodec + ']';
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CustomListCodec other = (CustomListCodec) obj;
		return Objects.equals(elementCodec, other.elementCodec) && Objects.equals(factory, other.factory);
	}
}
