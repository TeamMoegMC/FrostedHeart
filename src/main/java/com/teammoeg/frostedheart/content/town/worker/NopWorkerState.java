package com.teammoeg.frostedheart.content.town.worker;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

public class NopWorkerState extends WorkerState {
	@Getter
	CompoundTag data;
	public NopWorkerState() {

	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		for(String i:data.getAllKeys()) {
			tag.put(i, data.get(i));
		}
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		data=tag.copy();
	}

}
