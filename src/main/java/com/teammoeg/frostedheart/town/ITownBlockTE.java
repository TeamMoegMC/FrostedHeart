package com.teammoeg.frostedheart.town;

import net.minecraft.nbt.CompoundNBT;

public interface ITownBlockTE {
	boolean isWorkValid();
	TownWorkerType getWorker();
	CompoundNBT getWorkData();
	void setWorkData(CompoundNBT data);
	int getPriority();
}
