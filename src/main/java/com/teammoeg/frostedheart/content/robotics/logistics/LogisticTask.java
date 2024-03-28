package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;

public class LogisticTask {
	ItemStack filter;
	int size;
	boolean fetchNBT;
	public LogisticTask() {

	}
	public ItemStack fetch(LogisticNetwork network,int cnt) {
		return network.fetchItem(network.getWorld(), filter, fetchNBT, cnt);
	}
}
