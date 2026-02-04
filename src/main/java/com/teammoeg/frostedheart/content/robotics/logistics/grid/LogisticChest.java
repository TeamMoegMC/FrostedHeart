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

package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticChest implements IItemHandler, IGridElement,IItemHandlerModifiable{
	private static class ItemData implements ItemCountProvider{
		IntArraySet slots=new IntArraySet();
		@Getter
		int totalCount;
		@Override
		public String toString() {
			return "ItemData [slots=" + slots + ", totalCount=" + totalCount + "]";
		}
		
	}
	private static final int MAX_SLOT=27;
	@Getter
	ItemStackHandler chest=new ItemStackHandler(MAX_SLOT);
	Map<ItemKey,ItemData> cachedData=new HashMap<>();
	ItemKey[] slotRef=new ItemKey[MAX_SLOT];
	boolean isChanged;
	@Getter
	Level level;
	@Getter
	BlockPos pos;
	@Setter
	@Getter
	ItemKey filter;
	@Getter
	int emptySlotCount=MAX_SLOT;
	private boolean isCacheInvalidated;

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
	private void onStackAdded(int slot) {
		ItemStack stack=chest.getStackInSlot(slot);
		if(stack.isEmpty())return;
		ItemKey key=new ItemKey(stack);
		onStackAdded(slot,key,stack.getCount());
	}
	private void onStackAdded(int slot,ItemKey key,int count) {
		if(count==0)return;
		ItemData id=cachedData.get(key);
		if(id==null) {
			id=new ItemData();
			cachedData.put(key, id);
		}
		slotRef[slot]=key;
		id.totalCount+=count;
		id.slots.add(slot);
		emptySlotCount--;
	}
	private void onStackRemoved(int slot,int count) {
		ItemKey origin=slotRef[slot];
		if(origin==null)return;
		slotRef[slot]=null;
		ItemData id=cachedData.get(origin);
		id.totalCount-=count;
		id.slots.remove(slot);
		if(id.slots.isEmpty()||id.totalCount==0)
			cachedData.remove(origin);
		emptySlotCount++;
	}
	private void modifySlotCount(int slot,int count) {
		ItemKey origin=slotRef[slot];
		ItemData id=cachedData.get(origin);
		id.totalCount+=count;
	}

	public void revalidate() {
		Arrays.fill(slotRef, null);
		cachedData.clear();
		this.emptySlotCount=this.getSlots();
		for(int i=0;i<chest.getSlots();i++)
			onStackAdded(i);
	}
	public void computeEmptySlots() {
		for(int i=0;i<chest.getSlots();i++) {
			emptySlotCount+=chest.getStackInSlot(i).isEmpty()?1:0;
		}
	}
	public void tick() {
		if(isCacheInvalidated) {
			revalidate();
			isCacheInvalidated=false;
		}
	}
	public ItemStack pushItem(ItemKey ik,ItemStack is,boolean fillEmpty) {
		ItemData id=cachedData.get(ik);
		ItemStack remain=is;
		if(id!=null) {
			IntIterator ii=id.slots.iterator();
			while(ii.hasNext()) {
				int oldCount=remain.getCount();
				int slot=ii.nextInt();
				remain=chest.insertItem(slot, remain, false);
				id.totalCount+=oldCount-remain.getCount();
				if(remain.isEmpty())
					return ItemStack.EMPTY;
			}
		}
		
		for(int i=0;i<chest.getSlots();i++) {
			if(chest.getStackInSlot(i).isEmpty()) {
				remain=chest.insertItem(i, remain, false);
				ItemStack stack=chest.getStackInSlot(i);
				if(!stack.isEmpty())
				onStackAdded(i,ik,stack.getCount());
				if(remain.isEmpty())
					return ItemStack.EMPTY;
			}
		}
		
		return remain;
		
	}
	public boolean fillable() {
		return true;
	}
	public ItemStack takeItem(ItemKey key,int amount) {
		ItemData id=cachedData.get(key);
		if(id==null||amount==0)
			return ItemStack.EMPTY;
		int totake=amount;
		IntIterator ii=id.slots.iterator();
		IntArrayList il=new IntArrayList();
		ItemStack taken=ItemStack.EMPTY;
		while(ii.hasNext()) {
			int slot=ii.nextInt();
			ItemStack inslot=chest.getStackInSlot(slot);
			if(inslot.isEmpty()||!key.isSameItem(inslot)) {
				isCacheInvalidated=true;
				continue;
			}
			if(!taken.isEmpty()&&!ItemStack.isSameItemSameTags(taken, inslot))continue;
			int slotToReduce=Math.min(totake, inslot.getCount());
			int oldCount=inslot.getCount();
			if(slotToReduce>0) {
				id.totalCount-=slotToReduce;
				if(taken.isEmpty()) {
					taken=chest.extractItem(slot, slotToReduce, false);
				}else {
					taken.grow(chest.extractItem(slot, slotToReduce, false).getCount());
				}
				if(slotToReduce==oldCount) {
					il.add(slot);
					slotRef[slot]=null;
					emptySlotCount++;
				}
				totake-=slotToReduce;
				if(totake<=0)
					break;
			}
		}
		id.slots.removeAll(il);
		if(id.slots.isEmpty()||id.totalCount<=0){
			cachedData.remove(key);
		}
		return taken;
		
	}
	public Map<ItemKey,? extends ItemCountProvider> getAllItems(){
		return this.cachedData;
	}
	@Override
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if(simulate)return chest.insertItem(slot, stack, simulate);
		ItemKey origin=slotRef[slot];
		int originCount=chest.getStackInSlot(slot).getCount();
		ItemStack remain=chest.insertItem(slot, stack, simulate);
		if(remain.getCount()<=stack.getCount()) {
			ItemStack after=chest.getStackInSlot(slot);
			isChanged=true;
			if(origin==null||originCount==0) {
				onStackAdded(slot);
			}else {
				modifySlotCount(slot,after.getCount()-originCount);
			}
		}
		return remain;
	}
	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		if(simulate)return chest.extractItem(slot, amount, simulate);
		ItemKey origin=slotRef[slot];
		int originCount=chest.getStackInSlot(slot).getCount();
		ItemStack extracted=chest.extractItem(slot, amount, simulate);
		if(!extracted.isEmpty()) {
			ItemStack after=chest.getStackInSlot(slot);
			isChanged=true;
			if(after.isEmpty()) {
				onStackRemoved(slot,originCount);
			}else {
				modifySlotCount(slot,after.getCount()-originCount);
			}
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
	@Override
	public String toString() {
		return "LogisticChest [chest=" + chest + ", cachedData=" + cachedData + "]";
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
	public LogisticChest(Level level, BlockPos pos) {
		super();
		this.level = level;
		this.pos = pos;
	}
	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		ItemStack original=chest.getStackInSlot(slot);
		onStackRemoved(slot,original.getCount());
		chest.setStackInSlot(slot, stack);
		onStackAdded(slot);
		isChanged=true;
	}
	

}
