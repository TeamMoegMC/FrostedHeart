package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class NBTRWMarshaller<T> implements Marshaller {
	final Function<CompoundTag,T> from;
	final Function<T,CompoundTag> to;
	public NBTRWMarshaller(Function<CompoundTag,T> from, Function<T, CompoundTag> to) {
		super();
		this.from = from;
		this.to = to;
	}

	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	@Override
	public Object fromNBT(Tag nbt) {
		return from.apply((CompoundTag) nbt);
		
	}

}
