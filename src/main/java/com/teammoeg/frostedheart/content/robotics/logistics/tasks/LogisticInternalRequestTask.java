package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.SlotSet;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LogisticInternalRequestTask implements LogisticTask {
	BlockEntity tile;
	int slot;
	ItemStack filter;
	LogisticSlot to;
	boolean useNBT;
	private LogisticInternalRequestTask(BlockEntity tile, int slot) {
		super();
		this.tile = tile;
		this.slot = slot;
	}

	public LogisticInternalRequestTask(BlockEntity tile, int slot, ItemStack filter, boolean useNBT) {
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
			to=new LogisticSlot(network.getStorage(tile.getBlockPos()),slot);
		}
		SlotSet slot=network.findSlotsFor(filter, useNBT);
		if(!slot.isEmpty())
			network.internalTransit(slot.iterator().next(), to, msize);

	}

}
