package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.SlotSet;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class LogisticInternalRequestTask implements LogisticTask {
	TileEntity tile;
	int slot;
	ItemStack filter;
	LogisticSlot to;
	boolean useNBT;
	private LogisticInternalRequestTask(TileEntity tile, int slot) {
		super();
		this.tile = tile;
		this.slot = slot;
	}

	public LogisticInternalRequestTask(TileEntity tile, int slot, ItemStack filter, boolean useNBT) {
		this(tile,slot);
		if(!to.getItem().isEmpty()) {
			this.filter = to.getItem();
			this.useNBT=true;
		}else {
			this.filter = filter;
			this.useNBT = useNBT;
		}
	}
	
	@Override
	public void work(LogisticNetwork network, int msize) {
		if(to==null) {
			to=new LogisticSlot(network.getStorage(tile.getPos()),slot);
		}
		SlotSet slot=network.findSlotsFor(filter, useNBT);
		if(!slot.isEmpty())
			network.internalTransit(slot.iterator().next(), to, msize);

	}

}
