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

import org.jetbrains.annotations.NotNull;

import com.teammoeg.frostedheart.content.robotics.logistics.data.Index.Coord;
import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticChest extends LogisticElement implements IItemHandler,IItemHandlerModifiable{
	private static final int MAX_SLOT=27;
	@Getter
	ItemStackHandler chest=new ItemStackHandler(MAX_SLOT);
	@Setter
	@Getter
	ItemKey filter;

	public CompoundTag serialize() {
		return chest.serializeNBT();
	}
	public void deserialize(CompoundTag nbt) {
		chest.deserializeNBT(nbt);
		revalidate();
	}
	@Override
	public int getSlots() {
		return MAX_SLOT;
	}
	@Override
	public @NotNull ItemStack getStackInSlot(int slot) {
		return chest.getStackInSlot(slot);
	}
	protected void onStackModified(int slot) {
		ItemStack stack=chest.getStackInSlot(slot);
		if(stack.isEmpty()) {
			onStackRemoved(slot);
			return;
		}
		for(LogisticHub hub:hubs){
			hub.set(new Coord(pos,slot), stack);
		}
	}
	protected void onStackRemoved(int slot) {
		for(LogisticHub hub:hubs){
			if(filter==null)
				hub.set(new Coord(pos,slot), ItemStack.EMPTY);
			else
				hub.set(new Coord(pos,slot), filter);
		}
	}

	@Override
	public void revalidate() {
		for(int i=0;i<chest.getSlots();i++)
			onStackModified(i);
	}
	@Override
	public ItemStack pushItem(ItemKey ik,ItemStack is) {
		return ItemHandlerHelper.insertItem(chest, is, false);
	}
	@Override
	public boolean fillable() {
		return true;
	}
	@Override
	public ItemStack takeItem(ItemKey key,int amount) {
		for(int i=0;i<chest.getSlots();i++) {
			ItemStack stack=chest.getStackInSlot(i);
			if(key.isSameItem(stack)) {
				return chest.extractItem(i, amount, false);
			}
		}
		return ItemStack.EMPTY;
		
	}
	@Override
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if(simulate)return chest.insertItem(slot, stack, simulate);
		ItemStack remain=chest.insertItem(slot, stack, simulate);
		if(remain.getCount()<stack.getCount()) {
			markChanged();
			onStackModified(slot);
		}
		return remain;
	}
	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		if(simulate)return chest.extractItem(slot, amount, simulate);
		ItemStack extracted=chest.extractItem(slot, amount, simulate);
		if(!extracted.isEmpty()) {
			markChanged();
			onStackModified(slot);
		}
		return extracted;
	}
	@Override
	public int getSlotLimit(int slot) {
		return chest.getSlotLimit(slot);
	}
	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return chest.isItemValid(slot, stack);
	}
	public LogisticChest(Level level, BlockPos pos) {
		super(level,pos);
	}
	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		chest.setStackInSlot(slot, stack);
		markChanged();
		onStackModified(slot);
	}
	@Override
	public ItemStack takeItem(int slot, int amount) {
		return chest.extractItem(slot, amount, false);
	}
	@Override
	public void registerSlots(LogisticHub hub) {
		super.registerSlots(hub);
		for(int i=0;i<chest.getSlots();i++) {
			ItemStack stack=chest.getStackInSlot(i);
			hub.set(new Coord(pos,i), stack);
		}
	}
}
