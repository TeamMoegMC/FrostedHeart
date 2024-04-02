package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticPushTask implements LogisticTask {
	ItemStackHandler handler;
	int fromSlot;
	
	public LogisticPushTask(ItemStackHandler handler, int fromSlot) {
		super();
		this.handler = handler;
		this.fromSlot = fromSlot;
	}

	@Override
	public void work(LogisticNetwork network, int msize) {
		ItemStack extracted=handler.extractItem(fromSlot, msize, false);
		extracted=network.receiveItem(extracted);
		handler.insertItem(fromSlot, extracted, false);
	}

}
