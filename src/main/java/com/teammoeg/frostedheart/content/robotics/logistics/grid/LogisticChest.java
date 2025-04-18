package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticChest implements IItemHandler, IGridElement{
	private static class ItemData implements ItemCountProvider{
		IntArraySet slots=new IntArraySet();
		@Getter
		int totalCount;
		
	}
	private static final int MAX_SLOT=27;
	ItemStackHandler chest=new ItemStackHandler(MAX_SLOT);
	Map<ItemKey,ItemData> cachedData=new HashMap<>();
	ItemKey[] slotRef=new ItemKey[MAX_SLOT];
	boolean isChanged;
	@Getter
	int emptySlotCount=0;
	private boolean isCacheInvalidated;
	public LogisticChest() {
		
	}
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
	public ItemStack takeItem(ItemKey key,int amount) {
		ItemData id=cachedData.get(key);
		if(id==null||amount==0)
			return ItemStack.EMPTY;
		int totake=amount;
		IntIterator ii=id.slots.iterator();
		
		while(ii.hasNext()) {
			int slot=ii.nextInt();
			ItemStack inslot=chest.getStackInSlot(slot);
			if(inslot.isEmpty()||!key.isSameItem(inslot)) {
				isCacheInvalidated=true;
				continue;
			}
			int slotToReduce=Math.min(totake, inslot.getCount());
			
			if(slotToReduce>0) {
				id.totalCount-=slotToReduce;
				if(slotToReduce==inslot.getCount()) {
					id.slots.remove(slot);
					chest.setStackInSlot(slot, ItemStack.EMPTY);
					emptySlotCount++;
				}else {
					inslot.shrink(slotToReduce);
				}
				totake-=slotToReduce;
				if(totake<=0)
					break;
			}
		}
		return key.createStackWithSize(amount-totake);
		
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
			if(origin==null) {
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
	

}
