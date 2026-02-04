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

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ItemHandlerWrapper implements IItemHandlerModifiable {
	private final Supplier<IItemHandlerModifiable> intern;
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		intern.get().setStackInSlot(slot, stack);
	}
	public int getSlots() {
		return intern.get().getSlots();
	}
	public @NotNull ItemStack getStackInSlot(int slot) {
		return intern.get().getStackInSlot(slot);
	}
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		return intern.get().insertItem(slot, stack, simulate);
	}
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		return intern.get().extractItem(slot, amount, simulate);
	}
	public int getSlotLimit(int slot) {
		return intern.get().getSlotLimit(slot);
	}
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return intern.get().isItemValid(slot, stack);
	}
	public ItemHandlerWrapper(Supplier<IItemHandlerModifiable> intern) {
		super();
		this.intern = intern;
	}


}
