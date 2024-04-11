package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.function.Function;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class NBTRWMarshaller<T> implements Marshaller {
	final Function<CompoundNBT,T> from;
	final Function<T,CompoundNBT> to;
	public NBTRWMarshaller(Function<CompoundNBT,T> from, Function<T, CompoundNBT> to) {
		super();
		this.from = from;
		this.to = to;
	}

	@Override
	public INBT toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(INBT nbt) {
		return from.apply((CompoundNBT) nbt);
		
	}

}
