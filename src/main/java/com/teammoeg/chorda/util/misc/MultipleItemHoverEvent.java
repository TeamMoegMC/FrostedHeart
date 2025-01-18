package com.teammoeg.chorda.util.misc;

import java.util.List;

import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.world.item.ItemStack;
/*
 * client only event to display multiple items
 * This is a hack and can not be sent by network
 * */
public class MultipleItemHoverEvent extends ItemStackInfo {
	List<ItemStack> stacks;
	public MultipleItemHoverEvent(List<ItemStack> pStack) {
		super(pStack.get(0));
		stacks=pStack;
	}
	@Override
	public ItemStack getItemStack() {
		return stacks.get((int) ((System.currentTimeMillis()/1000)%stacks.size()));
	}
	

}
