package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemHandlerHelper;

public class LogisticEnvolop {
	ItemStack stack;
	BlockPos pos;
	ILogisticsStorage storage;
	int ticks;
	public LogisticEnvolop(ItemStack stack, BlockPos pos, ILogisticsStorage storage,int maxtime) {
		super();
		this.stack = stack;
		this.pos = pos;
		this.storage = storage;
		this.ticks=maxtime;
	}
	public boolean tick() {
		ticks--;
		if(ticks>0) {
			return false;
		}else {
			complete();
			return true;
		}
	}
	public void complete() {
		ItemHandlerHelper.insertItem(storage.getInventory(), stack, false);
	}

}
