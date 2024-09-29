package com.teammoeg.frostedheart.base.multiblock.components;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;

public class FHCodecState<T extends FHCodecStateData> implements IMultiblockState {
	final Codec<T> Save;
	final Codec<T> Sync;
	T data;

	public FHCodecState(Codec<T> save, Codec<T> sync,T def) {
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
