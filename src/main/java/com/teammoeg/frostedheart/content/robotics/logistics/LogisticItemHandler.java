package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticItemHandler extends ItemStackHandler {

	public LogisticItemHandler() {
	}

	public LogisticItemHandler(int size) {
		super(size);
	}

	public LogisticItemHandler(NonNullList<ItemStack> stacks) {
		super(stacks);
	}

}
