package com.teammoeg.frostedheart.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface NBTSerializable extends INBTSerializable<CompoundNBT> {
	/**
	 * @param nbt data
	 * @param isPacket is to packet
	 */
	void save(CompoundNBT nbt,boolean isPacket) ;
	/**
	 * load 
	 * @param nbt data
	 * @param isPacket is from packet
	 */
	void load(CompoundNBT nbt,boolean isPacket) ;
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
	default CompoundNBT serialize(boolean isPacket) {
		CompoundNBT nbt=new CompoundNBT();
		save(nbt,isPacket);
		return nbt;
	}
	default void deserialize(CompoundNBT nbt,boolean isPacket) {
		load(nbt,isPacket);
	}
}
