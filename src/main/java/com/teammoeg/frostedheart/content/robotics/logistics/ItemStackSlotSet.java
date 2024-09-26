package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.LinkedHashSet;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class ItemStackSlotSet extends LinkedHashSet<LogisticSlot> implements SlotSet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ItemStack stack;
	public ItemStackSlotSet(ItemStack type) {
		super();
		if(type.hasTag())
			stack=ItemHandlerHelper.copyStackWithSize(type,1);
	}
	public boolean testStack(ItemStack out,boolean strictNBT) {
		if(stack==null) {
			return strictNBT?(!out.hasTag()):true;
		}
		return ItemStack.isSameItemSameTags(out, stack);
	}

}
