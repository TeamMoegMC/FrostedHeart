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
 * 创造模式物品栏注册事件的包装器。
 * 先按提供的排序编号排序，排序编号相同的按注册顺序排序，
 * 注册顺序也相同的按插入顺序排序。
 * <p>
 * Wrapper for creative tab registration event.
 * Items are sorted by provided sort order first, then by registry order
 * for items with the same sort order, and finally by insertion order.
 */
public class CreativeTabItemHelper implements Output{
	/**
	 * 创造模式物品栏中的条目记录，包含物品栈、可见性、排序编号和插入编号。
	 * <p>
	 * Entry record for creative tab items, containing item stack, visibility, sort number and insertion number.
	 *
	 * @param is 物品栈 / The item stack
	 * @param tab 标签页可见性 / The tab visibility
	 * @param sortnum 排序编号 / The sort number
	 * @param insnum 插入编号 / The insertion number
	 */
	private static record Entry(ItemStack is, TabVisibility tab, int sortnum, int insnum){
		public int getSortnum() {
			return sortnum;
		}
		public int getInsnum() {
			return insnum;
		}
	}
	/** 当前创造模式标签页的资源键 / The resource key of the current creative mode tab */
	private ResourceKey<CreativeModeTab> key;
	/** 插入编号计数器 / Insertion number counter */
	private int num=Integer.MIN_VALUE;
	/** 待注册的物品条目列表 / List of item entries to be registered */
	private List<Entry> items=new ArrayList<>();
	/**
	 * 创建一个创造模式物品栏辅助器。
	 * <p>
	 * Creates a creative tab item helper.
	 *
	 * @param key 创造模式标签页的资源键 / The resource key of the creative mode tab
	 */
	public CreativeTabItemHelper(ResourceKey<CreativeModeTab> key) {
		super();
		this.key= key;
	}
	/**
	 * 检查当前标签页是否匹配指定的标签类型。
	 * <p>
	 * Checks whether the current tab matches the specified tab type.
	 *
	 * @param tab 要检查的标签类型 / The tab type to check
	 * @return 如果匹配则为true / true if it matches
	 */
	public boolean isType(TabType tab) {
		if(tab==null)return false;
		return tab.test(this.key);
	}
	/**
	 * 计算排序并输出最终结果到事件。
	 * <p>
	 * Computes sorting and makes the final output to the event.
	 *
	 * @param event 创造模式标签页输出事件 / The creative mode tab output event
	 */
	public void register(Output event) {
		items.sort(Comparator.comparingInt(Entry::getSortnum).thenComparing(Entry::getInsnum));
		for(Entry e:items)
			event.accept(e.is, e.tab);
	}
    /**
     * 以默认排序编号0注册物品栈。
     * <p>
     * Simply registers an item stack with a sort number of 0.
     *
     * @param stack 要注册的物品栈 / The item stack to register
     * @param visibility 标签页可见性 / The tab visibility
     */
    @Override
    public void accept(ItemStack stack, TabVisibility visibility)
    {
    	this.accept(stack,0,visibility);
    }
    /**
     * 以默认排序编号0注册物品供应器。
     * <p>
     * Simply registers an item supplier with a sort number of 0.
     *
     * @param item 物品供应器 / The item supplier
     * @param visibility 标签页可见性 / The tab visibility
     */
    public void accept(Supplier<? extends ItemLike> item, CreativeModeTab.TabVisibility visibility)
    {
       this.accept(item.get(), visibility);
    }
    /**
     * 以默认排序编号0注册物品供应器，使用默认可见性。
     * <p>
     * Simply registers an item supplier with a sort number of 0 and default visibility.
     *
     * @param item 物品供应器 / The item supplier
     */
    public void accept(Supplier<? extends ItemLike> item)
    {
       this.accept(item.get());
    }
    /**
     * 以指定排序编号注册物品栈。
     * <p>
     * Registers an item stack with the specified sort number.
     *
     * @param pStack 要注册的物品栈 / The item stack to register
     * @param sortNum 排序编号 / The sort number
     * @param pTabVisibility 标签页可见性 / The tab visibility
     */
    public void accept(ItemStack pStack,int sortNum,CreativeModeTab.TabVisibility pTabVisibility) {
    	items.add(new Entry(pStack,pTabVisibility,sortNum,num++));
    };
    /**
     * 以指定排序编号注册物品栈，使用默认可见性。
     * <p>
     * Registers an item stack with the specified sort number and default visibility.
     *
     * @param pStack 要注册的物品栈 / The item stack to register
     * @param sortNum 排序编号 / The sort number
     */
    public void accept(ItemStack pStack,int sortNum) {
       this.accept(pStack,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    /**
     * 以指定排序编号注册ItemLike对象。
     * <p>
     * Registers an ItemLike with the specified sort number.
     *
     * @param pItem 要注册的物品类对象 / The item-like object to register
     * @param sortNum 排序编号 / The sort number
     * @param pTabVisibility 标签页可见性 / The tab visibility
     */
    public void accept(ItemLike pItem,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       this.accept(new ItemStack(pItem),sortNum, pTabVisibility);
    }
    /**
     * 以指定排序编号注册ItemLike对象，使用默认可见性。
     * <p>
     * Registers an ItemLike with the specified sort number and default visibility.
     *
     * @param pItem 要注册的物品类对象 / The item-like object to register
     * @param sortNum 排序编号 / The sort number
     */
    public void accept(ItemLike pItem,int sortNum) {
       this.accept(new ItemStack(pItem),sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
    /**
     * 以指定排序编号批量注册物品栈集合。
     * <p>
     * Registers a collection of item stacks with the specified sort number.
     *
     * @param pStacks 要注册的物品栈集合 / The collection of item stacks to register
     * @param sortNum 排序编号 / The sort number
     * @param pTabVisibility 标签页可见性 / The tab visibility
     */
    public void acceptAll(Collection<ItemStack> pStacks,int sortNum, CreativeModeTab.TabVisibility pTabVisibility) {
       pStacks.forEach((p_252337_) -> {
          this.accept(p_252337_,sortNum, pTabVisibility);
       });
    }
    /**
     * 以指定排序编号批量注册物品栈集合，使用默认可见性。
     * <p>
     * Registers a collection of item stacks with the specified sort number and default visibility.
     *
     * @param pStacks 要注册的物品栈集合 / The collection of item stacks to register
     * @param sortNum 排序编号 / The sort number
     */
    public void acceptAll(Collection<ItemStack> pStacks,int sortNum) {
       this.acceptAll(pStacks,sortNum, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
