package com.teammoeg.chorda.io;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;


/**
 * Interface for serialization into NBT
 */
public interface NBTSerializable extends INBTSerializable<CompoundTag> {
	
	/**
	 * Save data into existing nbt
	 *
	 * @param nbt data
	 * @param isPacket is to packet
	 */
	void save(CompoundTag nbt,boolean isPacket) ;
	
	/**
	 * load from existing nbt
	 *
	 * @param nbt data
	 * @param isPacket is from packet
	 */
	void load(CompoundTag nbt,boolean isPacket) ;
	
	/**
	 * Serialize to new NBT for storage.
	 *
	 * @return the compound NBT
	 */
	@Override
	default CompoundTag serializeNBT() {
		CompoundTag nbt=new CompoundTag();
		save(nbt,false);
		return nbt;
	}
	
	/**
	 * Deserialize from NBT.
	 *
	 * @param nbt the nbt
	 */
	@Override
	default void deserializeNBT(CompoundTag nbt) {
		load(nbt,false);
	}
	
	/**
	 * Serialize to nbt respects side setting.
	 *
	 * @param isPacket is to packet
	 * @return the compound NBT
	 */
	default CompoundTag serialize(boolean isPacket) {
		CompoundTag nbt=new CompoundTag();
		save(nbt,isPacket);
		return nbt;
	}
	
	/**
	 * Deserialize nbt respects side setting.
	 *
	 * @param nbt the nbt
	 * @param isPacket is from packet
	 */
	default void deserialize(CompoundTag nbt,boolean isPacket) {
		load(nbt,isPacket);
	}
}
