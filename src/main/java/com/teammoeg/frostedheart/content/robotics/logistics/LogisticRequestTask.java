package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;

public class LogisticRequestTask implements LogisticTask {
	ItemStack filter;
	int size;
	boolean fetchNBT;
	ILogisticsStorage storage;

	public LogisticRequestTask(ItemStack filter, int size, boolean fetchNBT, ILogisticsStorage storage) {
		super();
		this.filter = filter;
		this.size = size;
		this.fetchNBT = fetchNBT;
		this.storage = storage;
	}

	public ItemStack fetch(LogisticNetwork network,int msize) {
		ItemStack rets= network.fetchItem(filter, fetchNBT, Math.min(msize, size));
		size-=rets.getCount();
		return rets;
	}

	@Override
	public void work(LogisticNetwork network,int msize) {
		int rets= network.fetchItemInto(filter, storage.getInventory(), fetchNBT, Math.min(msize, size));
		size-=rets;
	}

}
