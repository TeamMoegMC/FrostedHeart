/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
