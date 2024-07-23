package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticSlot;

import net.minecraft.world.level.block.entity.BlockEntity;

public class LogisticInternalPushTask implements LogisticTask {
	BlockEntity tile;
	int slot;
	LogisticSlot from;



	public LogisticInternalPushTask(BlockEntity tile, int slot) {
		super();
		this.tile = tile;
		this.slot = slot;
	}



	@Override
	public void work(LogisticNetwork network, int msize) {
		if(from==null) {
			from=new LogisticSlot(network.getStorage(tile.getBlockPos()),slot);
		}
		network.importTransit(from, msize);

	}

}
