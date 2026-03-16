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

package com.teammoeg.chorda.io.marshaller;

import net.minecraft.nbt.Tag;

/**
 * 编组器接口。定义对象与NBT标签之间的双向转换契约。
 * <p>
 * Marshaller interface. Defines the bidirectional conversion contract between objects and NBT tags.
 */
public interface Marshaller {
	/**
	 * 将对象序列化为NBT标签。
	 * <p>
	 * Serializes an object to an NBT tag.
	 *
	 * @param o 要序列化的对象 / the object to serialize
	 * @return 序列化后的NBT标签 / the serialized NBT tag
	 */
	Tag toNBT(Object o);

	/**
	 * 从NBT标签反序列化为对象。
	 * <p>
	 * Deserializes an object from an NBT tag.
	 *
	 * @param nbt 要反序列化的NBT标签 / the NBT tag to deserialize from
	 * @return 反序列化后的对象 / the deserialized object
	 */
	Object fromNBT(Tag nbt);
}
