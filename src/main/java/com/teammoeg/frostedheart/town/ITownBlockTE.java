package com.teammoeg.frostedheart.town;

import net.minecraft.nbt.CompoundNBT;

public interface ITownBlockTE {
	boolean isWorkValid();
	TownWorkerType getWorker();
	CompoundNBT getWorkData();
	int getPriority();
}
