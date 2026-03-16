/*
 * Copyright (c) 2026 TeamMoeg
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
 * NBT序列化接口，提供对象与NBT之间的双向转换，支持网络数据包模式。
 * <p>
 * Interface for serialization into NBT, providing bidirectional conversion between objects and NBT, with network packet mode support.
 */
public interface NBTSerializable extends INBTSerializable<CompoundTag> {

	/**
	 * 将数据保存到已有的NBT标签中。
	 * <p>
	 * Save data into existing NBT tag.
	 *
	 * @param nbt NBT数据标签 / the NBT data tag
	 * @param isPacket 是否用于网络数据包 / whether this is for a network packet
	 */
	void save(CompoundTag nbt,boolean isPacket) ;
	
	/**
	 * 从已有的NBT标签中加载数据。
	 * <p>
	 * Load data from existing NBT tag.
	 *
	 * @param nbt NBT数据标签 / the NBT data tag
	 * @param isPacket 是否来自网络数据包 / whether this is from a network packet
	 */
	void load(CompoundTag nbt,boolean isPacket) ;
	
	/**
	 * 序列化为新的NBT标签用于存储。
	 * <p>
	 * Serialize to a new NBT tag for storage.
	 *
	 * @return 复合NBT标签 / the compound NBT tag
	 */
	@Override
	default CompoundTag serializeNBT() {
		CompoundTag nbt=new CompoundTag();
		save(nbt,false);
		return nbt;
	}
	
	/**
	 * 从NBT标签反序列化。
	 * <p>
	 * Deserialize from NBT tag.
	 *
	 * @param nbt NBT标签 / the NBT tag
	 */
	@Override
	default void deserializeNBT(CompoundTag nbt) {
		load(nbt,false);
	}
	
	/**
	 * 根据数据包模式序列化为NBT标签。
	 * <p>
	 * Serialize to NBT tag respecting packet mode setting.
	 *
	 * @param isPacket 是否用于网络数据包 / whether this is for a network packet
	 * @return 复合NBT标签 / the compound NBT tag
	 */
	default CompoundTag serialize(boolean isPacket) {
		CompoundTag nbt=new CompoundTag();
		save(nbt,isPacket);
		return nbt;
	}
	
	/**
	 * 根据数据包模式从NBT标签反序列化。
	 * <p>
	 * Deserialize from NBT tag respecting packet mode setting.
	 *
	 * @param nbt NBT标签 / the NBT tag
	 * @param isPacket 是否来自网络数据包 / whether this is from a network packet
	 */
	default void deserialize(CompoundTag nbt,boolean isPacket) {
		load(nbt,isPacket);
	}
}
