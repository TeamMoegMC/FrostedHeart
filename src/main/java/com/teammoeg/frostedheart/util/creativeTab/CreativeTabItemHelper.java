/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Caupona.
 *
 * Caupona is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Caupona is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Specially, we allow this software to be used alongside with closed source software Minecraft(R) and Forge or other modloader.
 * Any mods or plugins can also use apis provided by forge or com.teammoeg.caupona.api without using GPL or open source.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caupona. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.util.creativeTab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class CreativeTabItemHelper implements Output{
	private static class Entry{
		public ItemStack is;
		public CreativeModeTab.TabVisibility tab;
		int sortnum;
		int insnum;
		public Entry(ItemStack is, TabVisibility tab, int sortnum, int insnum) {
			super();
			this.is = is;
			this.tab = tab;
			this.sortnum = sortnum;
			this.insnum = insnum;
		}
		public int getSortnum() {
			return sortnum;
		}
		public int getInsnum() {
			return insnum;
		}
	}
	private CreativeModeTab tab;
	private ResourceKey<CreativeModeTab> key;
	private int num=Integer.MIN_VALUE;
	private List<Entry> items=new ArrayList<>();
	public CreativeModeTab getTab() {
		return tab;
	}
	public CreativeTabItemHelper(ResourceKey<CreativeModeTab> key,CreativeModeTab tab) {
		super();
		this.key= key;
		this.tab = tab;
	}
	public boolean isType(TabType tab) {
		if(tab==null)return false;
		return tab.test(this.key);
	}
	public void register(Output event) {
		items.sort(Comparator.comparingInt(Entry::getSortnum).thenComparing(Entry::getInsnum));
		for(Entry e:items)
			event.accept(e.is, e.tab);
	}
	
    @Override
    public void accept(ItemStack stack, TabVisibility visibility)
    {
    	this.accept(stack,0,visibility);
    }

    public void accept(Supplier<? extends ItemLike> item, CreativeModeTab.TabVisibility visibility)
    {
       this.accept(item.get(), visibility);
    }

    public void accept(Supplier<? extends ItemLike> item)
    {
       this.accept(item.get());
    }
    public void accept(ItemStack pStack,int sortNum,CreativeModeTab.TabVisibility pTabVisibility) {
    	items.add(new Entry(pStack,pTabVisibility,sortNum,num++));
    };

    public void accept(ItemStack pStack,int sortNum) {
       this.accept(pStack,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public void accept(ItemLike pItem,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       this.accept(new ItemStack(pItem),sortNum, pTabVisibility);
    }

    public void accept(ItemLike pItem,int sortNum) {
       this.accept(new ItemStack(pItem),sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public void acceptAll(Collection<ItemStack> pStacks,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       pStacks.forEach((p_252337_) -> {
          this.accept(p_252337_,sortNum, pTabVisibility);
       });
    }

    public void acceptAll(Collection<ItemStack> pStacks,int sortNum) {
       this.acceptAll(pStacks,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
