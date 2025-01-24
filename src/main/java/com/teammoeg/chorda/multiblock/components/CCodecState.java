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
