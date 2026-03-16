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

import java.util.Optional;

import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 可空 NBT 网络消息抽象基类，是 {@link NBTMessage} 的变体。
 * 与 {@link NBTMessage} 不同，此类允许 NBT 标签为 {@code null}，
 * 序列化时使用 {@link Optional} 包装来正确处理空值情况。
 * <p>
 * Abstract base class for nullable NBT network messages, a variant of {@link NBTMessage}.
 * Unlike {@link NBTMessage}, this class allows the NBT tag to be {@code null},
 * using {@link Optional} wrapping during serialization to properly handle null values.
 */
public abstract class NullableNBTMessage extends NBTMessage {

	/**
	 * 从字节缓冲区反序列化构造可空 NBT 消息。
	 * 使用 {@link SerializeUtil#readOptional} 读取可能为空的 NBT 标签。
	 * <p>
	 * Constructs a nullable NBT message by deserializing from the byte buffer.
	 * Uses {@link SerializeUtil#readOptional} to read a potentially absent NBT tag.
	 *
	 * @param buffer 源字节缓冲区 / the source byte buffer
	 */
	public NullableNBTMessage(FriendlyByteBuf buffer) {
		this(SerializeUtil.readOptional(buffer, FriendlyByteBuf::readNbt));
	}

	/**
	 * 使用可选的 NBT 复合标签构造消息。若 {@link Optional} 为空，则标签为 {@code null}。
	 * <p>
	 * Constructs a message with an optional NBT compound tag.
	 * If the {@link Optional} is empty, the tag will be {@code null}.
	 *
	 * @param tag 可选的 NBT 复合标签 / the optional NBT compound tag
	 */
	public NullableNBTMessage(Optional<CompoundTag> tag) {
		super(tag.orElse(null));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 重写父类的编码方法，使用 {@link SerializeUtil#writeOptional2} 将可能为空的 NBT 标签写入缓冲区。
	 * <p>
	 * Overrides the parent encoding method, using {@link SerializeUtil#writeOptional2}
	 * to write the potentially null NBT tag to the buffer.
	 */
	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeOptional2(buffer, this.getTag(),FriendlyByteBuf::writeNbt);
	}

}
