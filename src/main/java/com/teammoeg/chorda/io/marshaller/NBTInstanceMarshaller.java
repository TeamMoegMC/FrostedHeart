package com.teammoeg.chorda.io.marshaller;

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class NBTInstanceMarshaller<T> implements Marshaller {
	final BiConsumer<T,CompoundTag> from;
	final Function<T,CompoundTag> to;
	final Class<T> objcls;
	public NBTInstanceMarshaller(Class<T> objcls, BiConsumer<T,CompoundTag> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
		this.objcls = objcls;
	}

	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(Tag nbt) {
		if(!(nbt instanceof CompoundTag))return null;
		T ret=ClassInfo.createInstance(objcls);
		from.accept(ret, (CompoundTag) nbt);
		return ret;
		
	}

}
