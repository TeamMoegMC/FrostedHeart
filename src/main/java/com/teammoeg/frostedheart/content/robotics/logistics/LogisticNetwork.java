package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticNetwork {
	public static final Function<Item, LinkedList<SlotSet>> ISLOTS = t -> new LinkedList<>();

	Map<Item, LinkedList<SlotSet>> indexedSlot = new HashMap<>();
	Map<LogisticSlot, Item> slotsi = new HashMap<>();
	// List<LogisticEnvolop> envolops=new ArrayList<>();
	List<TileEntityLogisticsStorage> storages = new ArrayList<>();
	LinkedHashMap<BlockPos, TileEntityLogisticsStorage> endpoints = new LinkedHashMap<>();
	World world;
	BlockPos centerPos;

	public LogisticNetwork() {
	}

	public World getWorld() {
		return world;
	}

	public void updateSlot(BlockPos pos, int slot, ItemStack stack) {
		updateSlot(new LogisticSlot(endpoints.get(pos), slot), stack);
	}

	public void updateSlot(TileEntityLogisticsStorage storage, int slot, ItemStack stack) {
		updateSlot(new LogisticSlot(storage, slot), stack);
	}

	public <T extends TileEntity & ILogisticsStorage> void update(T tile) {
		TileEntityLogisticsStorage storage = new TileEntityLogisticsStorage(tile);
		endpoints.put(tile.getPos(), storage);
		ItemStackHandler inv = storage.getInventory();
		for (int i = 0; i < inv.getSlots(); i++) {
			if (!inv.getStackInSlot(i).isEmpty())
				updateSlot(storage, i, inv.getStackInSlot(i));
		}
	}

	public void invalidCheck() {
		endpoints.values().removeIf(t -> t.isRemoved());
		storages.removeIf(t -> t.isRemoved());
		slotsi.keySet().removeIf(t -> !t.exists());
		indexedSlot.values().forEach(t -> t.forEach(t1 -> t1.removeIf(t2 -> !t2.exists())));
	}

	public LogisticSlot getFirstEmptySlotFor(ItemStack stack) {
		LinkedList<SlotSet> list = indexedSlot.get(stack.getItem());
		for (SlotSet p : list) {
			if (p.testStack(stack, true)) {
				for (LogisticSlot lsl : p) {
					if (lsl.hasSize(stack))
						return lsl;
				}
				break;
			}
		}
		for (TileEntityLogisticsStorage storage : storages) {
			if (storage.isValidFor(stack)) {
				ItemStackHandler inv = storage.getInventory();
				for (int i = 0; i < inv.getSlots(); i++) {
					if (inv.getStackInSlot(i).isEmpty())
						return new LogisticSlot(storage, i);
				}
			}
		}
		return null;
	}

	public void internalTransit(LogisticSlot src, LogisticSlot dest, int transitSize) {
		ItemStack extracted = src.extract(transitSize);
		extracted = dest.insert(extracted);
		src.insert(extracted);
		if (src.getItem().isEmpty()) {
			remove(src);
		}
	}

	public void importTransit(LogisticSlot src, int transitSize) {
		LogisticSlot dest = getFirstEmptySlotFor(src.getItem());
		if (dest != null)
			internalTransit(src, dest, transitSize);
	}

	public void exportTransit(ILogisticsStorage storage, ItemStack filter, int transitSize) {
		ItemHandlerHelper.insertItemStacked(storage.getInventory(), fetchItem(filter, false, transitSize), false);
	}

	private void remove(LogisticSlot slot) {
		Item i = slotsi.remove(slot);
		if (i != null) {
			LinkedList<SlotSet> crnset = indexedSlot.get(i);
			if (crnset != null) {
				for (SlotSet ii : crnset) {
					if (ii.remove(slot))
						break;
				}
			}
		}
	}

	private void put(LinkedList<SlotSet> list, LogisticSlot slot, ItemStack stack) {
		ItemStack item1 = ItemHandlerHelper.copyStackWithSize(stack, 1);
		for (SlotSet pi : list) {
			if (pi.testStack(stack, true)) {
				pi.add(slot);
				return;
			}
		}
		if (!item1.hasTag()) {
			list.add(0, new ItemStackSlotSet(stack));
		} else {
			list.add(new ItemStackSlotSet(stack));
		}

	}

	public void updateSlot(LogisticSlot sl, ItemStack newItem) {
		remove(sl);
		put(indexedSlot.computeIfAbsent(newItem.getItem(), ISLOTS), sl, newItem);
		slotsi.put(sl, newItem.getItem());
	}
	public SlotSet findSlotsFor(ItemStack filter,boolean useNBT) {
		LinkedList<SlotSet> lslot = indexedSlot.get(filter.getItem());
		if(lslot==null)return SlotSet.EMPTY;
		Iterator<SlotSet> slot = lslot.iterator();
		while (slot.hasNext()) {
			SlotSet sl = slot.next();
			if (sl.testStack(filter, useNBT)) {
				if (sl.isEmpty()) {
					slot.remove();
					continue;
				}
				return sl;
			}
		}
		return SlotSet.EMPTY;
	}
	public ItemStack fetchItem(ItemStack filter, boolean useNBT, int maxcnt) {
		SingleSlotHandler actual=new SingleSlotHandler();
		Iterator<LogisticSlot> it = findSlotsFor(filter,useNBT).iterator();
		while (it.hasNext()) {
			LogisticSlot csl = it.next();
			csl.insert(actual.insertItem(0,csl.extract(maxcnt-actual.getSlotCount()), false));
		}
		return actual.getStackInSlot(0);
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public BlockPos getCenterPos() {
		return centerPos;
	}

	public void setCenterPos(BlockPos centerPos) {
		this.centerPos = centerPos;
	}
}
