package com.teammoeg.frostedheart.util.io;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;


/**
 * Interface for serialization into NBT
 */
public interface NBTSerializable extends INBTSerializable<CompoundNBT> {
	
	/**
	 * Save data into existing nbt
	 *
	 * @param nbt data
	 * @param isPacket is to packet
	 */
	void save(CompoundNBT nbt,boolean isPacket) ;
	
	/**
	 * load from existing nbt
	 *
	 * @param nbt data
	 * @param isPacket is from packet
	 */
	void load(CompoundNBT nbt,boolean isPacket) ;
	
	/**
	 * Serialize to new NBT for storage.
	 *
	 * @return the compound NBT
	 */
	@Override
	default CompoundNBT serializeNBT() {
		CompoundNBT nbt=new CompoundNBT();
		save(nbt,false);
		return nbt;
	}
	
	/**
	 * Deserialize from NBT.
	 *
	 * @param nbt the nbt
	 */
	@Override
	default void deserializeNBT(CompoundNBT nbt) {
		load(nbt,false);
	}
	
	/**
	 * Serialize to nbt respects side setting.
	 *
	 * @param isPacket is to packet
	 * @return the compound NBT
	 */
	default CompoundNBT serialize(boolean isPacket) {
		CompoundNBT nbt=new CompoundNBT();
		save(nbt,isPacket);
		return nbt;
	}
	
	/**
	 * Deserialize nbt respects side setting.
	 *
	 * @param nbt the nbt
	 * @param isPacket is from packet
	 */
	default void deserialize(CompoundNBT nbt,boolean isPacket) {
		load(nbt,isPacket);
	}
}
