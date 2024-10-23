package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class T1GeneratorState extends MasterGeneratorState {

	/** The last position where the machine it supports is */
	BlockPos lastSupportPos;
	public T1GeneratorState() {
		super();
	}
	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
		if(lastSupportPos!=null)
			nbt.putLong("support",lastSupportPos.asLong());
	}
	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
		lastSupportPos=null;
		if(nbt.contains("support"))
			lastSupportPos=BlockPos.of(nbt.getLong("support"));
	}

}
