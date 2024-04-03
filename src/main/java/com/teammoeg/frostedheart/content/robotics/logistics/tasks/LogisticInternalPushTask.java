package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticSlot;

import net.minecraft.tileentity.TileEntity;

public class LogisticInternalPushTask implements LogisticTask {
	TileEntity tile;
	int slot;
	LogisticSlot from;



	public LogisticInternalPushTask(TileEntity tile, int slot) {
		super();
		this.tile = tile;
		this.slot = slot;
	}



	@Override
	public void work(LogisticNetwork network, int msize) {
		if(from==null) {
			from=new LogisticSlot(network.getStorage(tile.getPos()),slot);
		}
		network.importTransit(from, msize);

	}

}
