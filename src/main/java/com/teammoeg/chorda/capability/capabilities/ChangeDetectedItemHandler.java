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

package com.teammoeg.chorda.capability.capabilities;

import org.jetbrains.annotations.NotNull;

import com.teammoeg.chorda.block.entity.SyncableBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

public class ChangeDetectedItemHandler implements IItemHandler{
	IItemHandler handler;
	Runnable onchange;
	public ChangeDetectedItemHandler(IItemHandler handler,Runnable onchange) {
		super();
		this.handler = handler;
		this.onchange = onchange;
	}
	public static <T extends BlockEntity> IItemHandler fromBESetChanged(T blockEntity,IItemHandler nested) {
		return new ChangeDetectedItemHandler(nested,()->{blockEntity.setChanged();});
	}
	
	public static <T extends BlockEntity& SyncableBlockEntity> IItemHandler fromBESynced(T blockEntity, IItemHandler nested) {
		return new ChangeDetectedItemHandler(nested,()->{blockEntity.setChanged();blockEntity.syncData();});
	}
	public int getSlots() {
		return handler.getSlots();
	}
	public @NotNull ItemStack getStackInSlot(int slot) {
		return handler.getStackInSlot(slot);
	}
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		ItemStack ret= handler.insertItem(slot, stack, simulate);
		if(!simulate&&ret.getCount()!=stack.getCount())
			onContentsChanged(slot);
		return ret;
	}
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack extracted= handler.extractItem(slot, amount, simulate);
		if(!simulate&&!extracted.isEmpty())
			onContentsChanged(slot);
		return extracted;
	}
	public int getSlotLimit(int slot) {
		return handler.getSlotLimit(slot);
	}
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return handler.isItemValid(slot, stack);
	}
	protected void onContentsChanged(int slot) {
		onchange.run();
	}

}
