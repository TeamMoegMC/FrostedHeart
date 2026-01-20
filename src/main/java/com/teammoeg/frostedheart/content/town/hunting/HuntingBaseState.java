package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

public class HuntingBaseState extends WorkerState {
	// get max resident
	@Getter
	int maxResidents;
	public HuntingBaseState() {
	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
	}

}
