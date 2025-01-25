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

package com.teammoeg.chorda.multiblock.components;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;

public class CCodecState<T extends CCodecStateData> implements IMultiblockState {
	final Codec<T> Save;
	final Codec<T> Sync;
	T data;

	public CCodecState(Codec<T> save, Codec<T> sync, T def) {
		super();
		Save = save;
		Sync = sync;
		data=def;
	}

	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		CodecUtil.encodeNBT(Save, nbt, "data", data);
	}

	@Override
	public void readSaveNBT(CompoundTag nbt) {
		data=CodecUtil.decodeNBT(Save, nbt, "data");
	}

	@Override
	public void writeSyncNBT(CompoundTag nbt) {
		CodecUtil.encodeNBT(Sync, nbt, "data", data);
	}

	@Override
	public void readSyncNBT(CompoundTag nbt) {
		data=CodecUtil.decodeNBT(Sync, nbt, "data");
	}

}
