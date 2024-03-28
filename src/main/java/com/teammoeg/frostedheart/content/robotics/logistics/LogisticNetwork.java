package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticNetwork {
	Map<Item,LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>>> slots=new HashMap<>();
	transient Function<Item,LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>>> islots=t->new LinkedList<>();
	Map<LogisticSlot,Item> slotsi=new HashMap<>();
	//List<LogisticEnvolop> envolops=new ArrayList<>();
	List<ILogisticsStorage> storages=new ArrayList<>();
	List<ILogisticsStorage> endpoints=new ArrayList<>();
	World world;
	BlockPos centerPos;
	public LogisticNetwork() {
	}
	public World getWorld() {
		return world;
	}
	public void updateSlot(ILogisticsStorage storage,int slot,ItemStack stack) {
		updateSlot(new LogisticSlot(storage,slot),stack);
	}
	public void register(ILogisticsStorage storage) {
		endpoints.add(storage);
		ItemStackHandler inv=storage.getInventory();
		for(int i=0;i<inv.getSlots();i++) {
			if(!inv.getStackInSlot(i).isEmpty())
				updateSlot(storage,i,inv.getStackInSlot(i));
		}
	}
	public LogisticSlot getFirstEmptySlotFor(ItemStack stack) {
		LinkedList<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> list=slots.get(stack.getItem());
		for(Pair<ItemStack, LinkedHashSet<LogisticSlot>> p:list) {
			if(ItemStack.areItemStackTagsEqual(stack, p.getFirst())) {
				for(LogisticSlot lsl:p.getSecond()) {
					if(lsl.hasSize(stack))
						return lsl;
				}
				break;
			}
		}
		for(ILogisticsStorage storage:storages) {
			if(storage.isValidFor(stack)) {
				ItemStackHandler inv=storage.getInventory();
				for(int i=0;i<inv.getSlots();i++) {
					if(inv.getStackInSlot(i).isEmpty())
						return new LogisticSlot(storage, i);
				}
			}
		}
		return null;
	}
	public void internalTransit(LogisticSlot src,LogisticSlot dest,int transitSize) {
		ItemStack stack=src.getItem();
		ItemStack dststack=dest.getItem();
		if(!dststack.isEmpty())
			transitSize=Math.min(transitSize, dststack.getMaxStackSize()-dststack.getCount());
		else
			transitSize=Math.min(transitSize, stack.getMaxStackSize());
		int siz=Math.min(transitSize, stack.getCount());
		stack.shrink(siz);
		if(siz==stack.getCount()) {
			remove(src);
		}
		if(dststack.isEmpty()) {
			ItemStack moving=ItemHandlerHelper.copyStackWithSize(stack, siz);
			dest.setItem(moving);
			put(slots.get(moving.getItem()),dest,moving);
		}else {
			dststack.grow(siz);
		}
		stack.shrink(siz);
	}
	public void importTransit(LogisticSlot src,int transitSize) {
		internalTransit(src,getFirstEmptySlotFor(src.getItem()),transitSize);
	}
	public void exportTransit(ILogisticsStorage storage,ItemStack filter,int transitSize) {
		ItemHandlerHelper.insertItemStacked(storage.getInventory(), fetchItem(getWorld(),filter,false,transitSize), false);
	}
	private void remove(LogisticSlot slot) {
		Item i=slotsi.get(slot);
		if(i!=null) {
			LinkedList<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> crnset= slots.get(i);
			if(crnset!=null) {
				for(Pair<ItemStack, LinkedHashSet<LogisticSlot>> ii:crnset) {
					if(ii.getSecond().remove(slot))
						break;
				}
			}
		}
	}
	private void put(LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>> list,LogisticSlot slot,ItemStack stack) {
		ItemStack item1=ItemHandlerHelper.copyStackWithSize(stack, 1);
		for(Pair<ItemStack,LinkedHashSet<LogisticSlot>> pi:list) {
			if(ItemStack.areItemStackTagsEqual(item1, stack)) {
				pi.getSecond().add(slot);
				return;
			}
		}
		if(!item1.hasTag()) {
			list.add(0,Pair.of(item1, new LinkedHashSet<>()));
		}else {
			list.add(Pair.of(item1, new LinkedHashSet<>()));
		}
		
	}
	public void updateSlot(LogisticSlot sl,ItemStack newItem) {
		remove(sl);
		put(slots.computeIfAbsent(newItem.getItem(), islots),sl,newItem);
		
		slotsi.put(sl, newItem.getItem());
	}
	/*public void tick() {
		envolops.removeIf(LogisticEnvolop::tick);
	}*/
	public ItemStack fetchItem(World w,ItemStack filter,boolean useNBT,int maxcnt) {
		LinkedList<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> lslot=slots.get(filter.getItem());
		List<LogisticSlot> using=new ArrayList<>();
		if(lslot!=null) {
			Iterator<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> slot=lslot.iterator();
			ItemStack actual=ItemStack.EMPTY;
			int cnt=maxcnt;
			if(useNBT) {
				while(slot.hasNext()) {
					Pair<ItemStack, LinkedHashSet<LogisticSlot>> sl=slot.next();
					if(ItemStack.areItemStackTagsEqual(sl.getFirst(), filter)) {
						if(sl.getSecond().isEmpty()) {
							slot.remove();
							continue;
						}
						Iterator<LogisticSlot> it=sl.getSecond().iterator();
						while(it.hasNext()) {
							LogisticSlot csl=it.next();
							using.add(csl);
							ItemStack crnstack=csl.getItem();
							if(actual.isEmpty()) {
								actual=crnstack;
							}else {
								if(cnt>=crnstack.getCount()) {
									actual.grow(crnstack.getCount());
									cnt-=crnstack.getCount();
									crnstack.shrink(crnstack.getCount());
									it.remove();
								}else {
									actual.grow(cnt);
									crnstack.shrink(cnt);
									cnt=0;
								}
							}
							if(cnt<=0)break;
						}
						break;
					}
				}
			}else {
				while(slot.hasNext()) {
					Pair<ItemStack, LinkedHashSet<LogisticSlot>> sl=slot.next();
					if(sl.getSecond().isEmpty()) {
						slot.remove();
						continue;
					}
					Iterator<LogisticSlot> it=sl.getSecond().iterator();
					while(it.hasNext()) {
						LogisticSlot csl=it.next();
						using.add(csl);
						ItemStack crnstack=csl.getItem();
						if(actual.isEmpty()) {
							actual=crnstack;
						}else {
							if(cnt>=crnstack.getCount()) {
								actual.grow(crnstack.getCount());
								cnt-=crnstack.getCount();
								crnstack.shrink(crnstack.getCount());
								it.remove();
							}else {
								actual.grow(cnt);
								crnstack.shrink(cnt);
								cnt=0;
							}
						}
						if(cnt<=0)break;
					}
				}
			}
			return actual;
		}
		return ItemStack.EMPTY;
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
