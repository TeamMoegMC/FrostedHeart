package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.function.Function;

import net.minecraft.nbt.INBT;

public class BasicMarshaller<R extends INBT,T> implements Marshaller {
	final Function<R,T> from;
	final Function<T,R> to;
	final Class<R> cls;
	final T def;
	public BasicMarshaller(Class<R> nbtcls,Function<R, T> from, Function<T, R> to) {
		this(nbtcls,from,to,null);
	}

	public BasicMarshaller(Class<R> cls,Function<R, T> from, Function<T, R> to, T def) {
		super();
		this.from = from;
		this.to = to;
		this.cls = cls;
		this.def = def;
	}

	@Override
	public INBT toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(INBT nbt) {
		if(cls.isInstance(nbt))
			return from.apply((R) nbt);
		return def;
	}

}
