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

package com.teammoeg.chorda.creativeTab;

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

/**
 * Wrapper for creative tab registration event.
 * This would sort items by a provided sort order first.
 * Items with same sort order would be first sorted by registry order
 * Items with same registry order would be then sorted by insertion order
 * 
 * */
public class CreativeTabItemHelper implements Output{
	private static record Entry(ItemStack is, TabVisibility tab, int sortnum, int insnum){
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
	/**
	 * Compute and make final output.
	 * */
	public void register(Output event) {
		items.sort(Comparator.comparingInt(Entry::getSortnum).thenComparing(Entry::getInsnum));
		for(Entry e:items)
			event.accept(e.is, e.tab);
	}
    /**
     * Simply register item, with a sortnum of 0
     * */
    @Override
    public void accept(ItemStack stack, TabVisibility visibility)
    {
    	this.accept(stack,0,visibility);
    }
    /**
     * Simply register item, with a sortnum of 0
     * */
    public void accept(Supplier<? extends ItemLike> item, CreativeModeTab.TabVisibility visibility)
    {
       this.accept(item.get(), visibility);
    }
    /**
     * Simply register item, with a sortnum of 0
     * */
    public void accept(Supplier<? extends ItemLike> item)
    {
       this.accept(item.get());
    }
    /**
     * Register item with sortnum, default sortnum is 0
     * */
    public void accept(ItemStack pStack,int sortNum,CreativeModeTab.TabVisibility pTabVisibility) {
    	items.add(new Entry(pStack,pTabVisibility,sortNum,num++));
    };
    /**
     * Register item with sortnum, default sortnum is 0
     * */
    public void accept(ItemStack pStack,int sortNum) {
       this.accept(pStack,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    /**
     * Register itemlike with sortnum, default sortnum is 0
     * */
    public void accept(ItemLike pItem,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       this.accept(new ItemStack(pItem),sortNum, pTabVisibility);
    }
    /**
     * Register itemlike with sortnum, default sortnum is 0
     * */
    public void accept(ItemLike pItem,int sortNum) {
       this.accept(new ItemStack(pItem),sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    /**
     * Register list of items with sortnum, default sortnum is 0
     * */
    public void acceptAll(Collection<ItemStack> pStacks,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       pStacks.forEach((p_252337_) -> {
          this.accept(p_252337_,sortNum, pTabVisibility);
       });
    }
    /**
     * Register list of items with sortnum, default sortnum is 0
     * */
    public void acceptAll(Collection<ItemStack> pStacks,int sortNum) {
       this.acceptAll(pStacks,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
