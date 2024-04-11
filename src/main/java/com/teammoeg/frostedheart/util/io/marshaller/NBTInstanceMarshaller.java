package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;

public class NBTInstanceMarshaller<T> implements Marshaller {
	final BiConsumer<T,CompoundNBT> from;
	final Function<T,CompoundNBT> to;
	final Class<T> objcls;
	public NBTInstanceMarshaller(Class<T> objcls, BiConsumer<T,CompoundNBT> from, Function<T, CompoundNBT> to) {
		super();
		this.from = from;
		this.to = to;
		this.objcls = objcls;
	}

	@Override
	public INBT toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(INBT nbt) {
		if(!(nbt instanceof CompoundNBT))return null;
		T ret=ClassInfo.createInstance(objcls);
		from.accept(ret, (CompoundNBT) nbt);
		return ret;
		
	}

}
