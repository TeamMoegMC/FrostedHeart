package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;

public class LogisticInternalRequestTask implements LogisticTask {
	LogisticSlot to;
	ItemStack filter;
	boolean useNBT;
	public LogisticInternalRequestTask(LogisticSlot to, ItemStack filter, boolean useNBT) {
		super();
		this.to = to;
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
		SlotSet slot=network.findSlotsFor(filter, useNBT);
		if(!slot.isEmpty())
			network.internalTransit(slot.iterator().next(), to, msize);

	}

}
