/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
