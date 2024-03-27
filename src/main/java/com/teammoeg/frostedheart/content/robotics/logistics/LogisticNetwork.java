package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.logistics.item.filter.FilterItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

public class LogisticNetwork {
	Map<Item,LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>>> slots=new HashMap<>();
	Function<Item,LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>>> islots=t->new LinkedList<>();

	Map<LogisticSlot,Item> slotsi=new HashMap<>();
	
	public LogisticNetwork() {
	}
	public <T extends TileEntity&ILogisticsStorage> void updateSlot(BlockPos pos,T storage,int slot,ItemStack stack) {
		LogisticSlot sl=new LogisticSlot(pos,storage,slot);
		updateSlot(sl,stack);
	}
	public void put(LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>> list,LogisticSlot slot,ItemStack stack) {
		ItemStack item1=ItemHandlerHelper.copyStackWithSize(stack, 1);
		for(Pair<ItemStack,LinkedHashSet<LogisticSlot>> pi:list) {
			if(ItemStack.areItemStackTagsEqual(item1, stack)) {
				pi.getSecond().add(slot);
				return;
			}
		}
		if(!item1.hasTag()) {
			list.add(0,Pair.of(item1, new LinkedHashSet<>()));
		}else
			list.add(Pair.of(item1, new LinkedHashSet<>()));
		
	}
	public void updateSlot(LogisticSlot sl,ItemStack newItem) {
		Item i=slotsi.get(sl);
		if(i!=null) {
			LinkedList<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> crnset= slots.get(i);
			if(crnset!=null) {
				for(Pair<ItemStack, LinkedHashSet<LogisticSlot>> ii:crnset) {
					if(ii.getSecond().remove(sl))
						break;
				}
			}
		}
		put(slots.computeIfAbsent(newItem.getItem(), islots),sl,newItem);
		
		slotsi.put(sl, newItem.getItem());
	}
	public ItemStack fetchItem(World w,ItemStack filter,boolean useNBT) {
		LinkedList<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> lslot=slots.get(filter.getItem());
		List<LogisticSlot> using=new ArrayList<>();
		if(lslot!=null) {
			Iterator<Pair<ItemStack, LinkedHashSet<LogisticSlot>>> slot=lslot.iterator();
			ItemStack actual=ItemStack.EMPTY;
			int cnt=64;
			if(useNBT) {
				while(slot.hasNext()) {
					Pair<ItemStack, LinkedHashSet<LogisticSlot>> sl=slot.next();
					if(ItemStack.areItemStackTagsEqual(sl.getFirst(), filter)) {
						if(sl.getSecond().isEmpty())continue;
						for(LogisticSlot csl:sl.getSecond()) {
							using.add(csl);
							ItemStack crnstack=csl.getItem();
							if(actual.isEmpty()) {
								actual=crnstack;
							}else {
								if(cnt>=crnstack.getCount()) {
									actual.grow(crnstack.getCount());
									cnt-=crnstack.getCount();
									crnstack.shrink(crnstack.getCount());
								}else {
									actual.grow(cnt);
									cnt=0;
									crnstack.shrink(cnt);
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
					if(sl.getSecond().isEmpty())continue;
					for(LogisticSlot csl:sl.getSecond()) {
						using.add(csl);
						ItemStack crnstack=csl.getItem();
						if(actual.isEmpty()) {
							actual=crnstack;
						}else {
							if(cnt>=crnstack.getCount()) {
								actual.grow(crnstack.getCount());
								cnt-=crnstack.getCount();
								crnstack.shrink(crnstack.getCount());
							}else {
								actual.grow(cnt);
								cnt=0;
								crnstack.shrink(cnt);
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
}
