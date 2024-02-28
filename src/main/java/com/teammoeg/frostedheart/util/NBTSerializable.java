package com.teammoeg.frostedheart.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface NBTSerializable extends INBTSerializable<CompoundNBT> {
	default void save(CompoundNBT nbt,boolean packet) {};
	default void load(CompoundNBT nbt,boolean packet) {};
	@Override
	default CompoundNBT serializeNBT() {
		CompoundNBT nbt=new CompoundNBT();
		save(nbt,false);
		return nbt;
	}
	@Override
	default void deserializeNBT(CompoundNBT nbt) {
		load(nbt,false);
	}
}
