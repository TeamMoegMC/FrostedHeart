package com.teammoeg.frostedheart.scheduler;

import net.minecraft.util.math.BlockPos;

public class ScheduledData {

	BlockPos pos;
	boolean forRemoval=false;
	public ScheduledData(BlockPos pos) {
		this.pos=pos;
	}
}
