package com.teammoeg.frostedheart.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface NBTSerializable extends INBTSerializable<CompoundNBT> {
	/**
	 * @param nbt data
	 * @param isPacket is to packet
	 */
	default void save(CompoundNBT nbt,boolean isPacket) {};
	/**
	 * load 
	 * @param nbt data
	 * @param isPacket is from packet
	 */
	default void load(CompoundNBT nbt,boolean isPacket) {};
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
