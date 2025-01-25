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

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class ItemHandlerListener implements IItemHandler, IItemHandlerModifiable {
	ItemStackHandler handler;
	ItemChangeListener listener;


	public ItemHandlerListener(ItemStackHandler handler, ItemChangeListener listener) {
		super();
		this.handler = handler;
		this.listener = listener;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		handler.setStackInSlot(slot, stack);
		listener.onSlotChange(slot, stack);
	}

	@Override
	public int getSlots() {
		return handler.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return handler.getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		int oldCount=handler.getStackInSlot(slot).getCount();
		ItemStack reminder=handler.insertItem(slot, stack, simulate);
		int newCount=handler.getStackInSlot(slot).getCount();
		
		if(!simulate&&oldCount!=newCount) {
			if(oldCount!=0) 
				listener.onCountChange(slot, oldCount, newCount);
			else
				listener.onSlotChange(slot, handler.getStackInSlot(slot));
		}
		return reminder;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		int origCount=handler.getStackInSlot(slot).getCount();
		ItemStack reminder=handler.extractItem(slot, amount, simulate);
		int newCount=handler.getStackInSlot(slot).getCount();
		if(!simulate&&origCount!=newCount) {
			if(newCount==0)
				listener.onSlotClear(slot);
			else 
				listener.onCountChange(slot, origCount, newCount);
		}
		return reminder;
	}

	@Override
	public int getSlotLimit(int slot) {
		return handler.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return handler.isItemValid(slot, stack);
	}
	

}
