package com.teammoeg.frostedheart.util.io;

import net.minecraft.nbt.INBT;

public interface Marshaller {
	INBT toNBT(Object o);
	Object fromNBT(INBT nbt);
}
