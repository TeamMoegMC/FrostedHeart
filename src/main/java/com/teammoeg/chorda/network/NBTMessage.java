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

package com.teammoeg.chorda.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 基于 NBT 的网络消息抽象基类。
 * 将整个消息的数据存储为一个 {@link CompoundTag}，自动处理序列化和反序列化。
 * 子类只需关注消息的处理逻辑和 NBT 数据的构建。
 * <p>
 * Abstract base class for NBT-based network messages.
 * Stores the entire message data as a {@link CompoundTag}, with automatic
 * serialization and deserialization. Subclasses only need to focus on
 * message handling logic and NBT data construction.
 */
public abstract class NBTMessage implements CMessage {
	/** 消息携带的 NBT 复合标签数据 / the NBT compound tag data carried by this message */
	private CompoundTag tag;

	/**
	 * 从字节缓冲区反序列化构造 NBT 消息。
	 * <p>
	 * Constructs an NBT message by deserializing from the byte buffer.
	 *
	 * @param buffer 源字节缓冲区 / the source byte buffer
	 */
	public NBTMessage(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}
	/**
	 * 使用指定的 NBT 复合标签构造消息。
	 * <p>
	 * Constructs a message with the specified NBT compound tag.
	 *
	 * @param tag NBT 复合标签数据 / the NBT compound tag data
	 */
	public NBTMessage(CompoundTag tag) {
		super();
		this.tag = tag;
	}

	/** {@inheritDoc} */
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeNbt(tag);
	}

	/**
	 * 获取此消息携带的 NBT 复合标签。
	 * <p>
	 * Gets the NBT compound tag carried by this message.
	 *
	 * @return NBT 复合标签 / the NBT compound tag
	 */
	public CompoundTag getTag() {
		return tag;
	}

	/**
	 * 设置此消息携带的 NBT 复合标签。
	 * <p>
	 * Sets the NBT compound tag carried by this message.
	 *
	 * @param tag NBT 复合标签 / the NBT compound tag
	 */
	public void setTag(CompoundTag tag) {
		this.tag = tag;
	}

}
