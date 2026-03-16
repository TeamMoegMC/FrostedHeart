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

package com.teammoeg.chorda.multiblock.components;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;

/**
 * 基于 Mojang {@link Codec} 的多方块状态实现，将泛型数据对象 {@code T} 作为多方块的持久化状态。
 * 使用两个独立的 Codec 分别处理存档保存（Save）和客户端同步（Sync）的序列化需求，
 * 允许在同步时只发送必要的数据以减少网络流量。
 * <p>
 * A Mojang {@link Codec}-based multiblock state implementation that uses a generic data object {@code T}
 * as the multiblock's persistent state. Uses two separate Codecs to handle serialization needs for
 * save persistence (Save) and client synchronization (Sync) independently, allowing only necessary
 * data to be sent during sync to reduce network traffic.
 *
 * @param <T> 状态数据类型，必须实现 {@link CCodecStateData} / The state data type, must implement {@link CCodecStateData}
 * @see CCodecStateData
 * @see CCodecStateFactory
 */
public class CCodecState<T extends CCodecStateData> implements IMultiblockState {

	/** 用于存档保存的 Codec / Codec used for save persistence */
	final Codec<T> Save;

	/** 用于客户端同步的 Codec / Codec used for client synchronization */
	final Codec<T> Sync;

	/** 当前状态数据 / The current state data */
	T data;

	/**
	 * 使用保存 Codec、同步 Codec 和默认数据对象构造多方块状态。
	 * <p>
	 * Constructs a multiblock state with save Codec, sync Codec, and a default data object.
	 *
	 * @param save 用于存档保存序列化的 Codec / The Codec for save persistence serialization
	 * @param sync 用于客户端同步序列化的 Codec / The Codec for client synchronization serialization
	 * @param def 默认的初始状态数据 / The default initial state data
	 */
	public CCodecState(Codec<T> save, Codec<T> sync, T def) {
		super();
		Save = save;
		Sync = sync;
		data=def;
	}

	/**
	 * 使用保存 Codec 将状态数据编码为 NBT 以进行存档保存。
	 * <p>
	 * Encodes the state data to NBT using the save Codec for save persistence.
	 *
	 * @param nbt 要写入的 NBT 复合标签 / The compound tag to write to
	 */
	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		CodecUtil.encodeNBT(Save, nbt, "data", data);
	}

	/**
	 * 使用保存 Codec 从 NBT 解码状态数据以恢复存档。
	 * <p>
	 * Decodes the state data from NBT using the save Codec to restore from a save.
	 *
	 * @param nbt 要读取的 NBT 复合标签 / The compound tag to read from
	 */
	@Override
	public void readSaveNBT(CompoundTag nbt) {
		data=CodecUtil.decodeNBT(Save, nbt, "data");
	}

	/**
	 * 使用同步 Codec 将状态数据编码为 NBT 以进行客户端同步。
	 * <p>
	 * Encodes the state data to NBT using the sync Codec for client synchronization.
	 *
	 * @param nbt 要写入的 NBT 复合标签 / The compound tag to write to
	 */
	@Override
	public void writeSyncNBT(CompoundTag nbt) {
		CodecUtil.encodeNBT(Sync, nbt, "data", data);
	}

	/**
	 * 使用同步 Codec 从 NBT 解码状态数据以接收客户端同步。
	 * <p>
	 * Decodes the state data from NBT using the sync Codec to receive client synchronization.
	 *
	 * @param nbt 要读取的 NBT 复合标签 / The compound tag to read from
	 */
	@Override
	public void readSyncNBT(CompoundTag nbt) {
		data=CodecUtil.decodeNBT(Sync, nbt, "data");
	}

}
