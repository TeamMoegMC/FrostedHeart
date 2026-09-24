/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class LogisticElement implements IItemHandler, IGridElement,IItemHandlerModifiable{
	List<LogisticHub> hubs=new ArrayList<>();
	boolean isChanged;
	@Getter
	Level level;
	@Getter
	BlockPos pos;

	@Override
	public int getSlots() {
		return 0;
	}
	@Override
	public @NotNull ItemStack getStackInSlot(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void revalidate() {
	}
	@Override
	public ItemStack pushItem(ItemKey ik,ItemStack is) {
		return is;
	}
	@Override
	public boolean fillable() {
		return false;
	}
	@Override
	public ItemStack takeItem(ItemKey key,int amount) {
		return ItemStack.EMPTY;
	}
	@Override
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		return stack;
	}
	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}
	@Override
	public int getSlotLimit(int slot) {
		return 0;
	}
	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return false;
	}
	@Override
	public boolean isChanged() {
		return isChanged;
	}
	@Override
	public boolean consumeChange() {
		boolean changed=isChanged;
		isChanged=false;
		return changed;
	}
	public LogisticElement(Level level, BlockPos pos) {
		super();
		this.level = level;
		this.pos = pos;
	}
	@Override
	public void setLevel(Level level) {
		this.level=level;
	}
	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		markChanged();
	}

	protected void markChanged() {
		isChanged=true;
	}
	@Override
	public void tick() {
		hubs.removeIf(t->!t.isValid());
	}
	@Override
	public ItemStack takeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}
	@Override
	public void registerSlots(LogisticHub hub) {
		hubs.add(hub);
	}
	
	@Override
	public void removeSlots() {
		for(LogisticHub hub:hubs) {
			hub.remove(pos);
		}
	}
}
