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

package com.teammoeg.chorda.item;

import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.chorda.creativeTab.TabType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Chorda方块物品基类，支持自动创造模式标签页注册。
 * 如果对应的方块实现了ICreativeModeTabItem接口，则委托给方块处理；否则根据标签类型自行注册。
 * <p>
 * Chorda block item base class with automatic creative mode tab registration support.
 * If the corresponding block implements ICreativeModeTabItem, it delegates to the block;
 * otherwise, it registers itself based on the tab type.
 */
public class CBlockItem extends BlockItem implements ICreativeModeTabItem{
	/** 此方块物品所属的创造模式标签页类型 / The creative mode tab type this block item belongs to */
	TabType tab;

	/**
	 * 创建Chorda方块物品。
	 * <p>
	 * Creates a Chorda block item.
	 *
	 * @param block 关联的方块 / The associated block
	 * @param props 物品属性 / The item properties
	 * @param tab 创造模式标签页类型 / The creative mode tab type
	 */
    public CBlockItem(Block block, Item.Properties props,TabType tab) {
        super(block, props);
        this.tab=tab;
    }


	/**
	 * {@inheritDoc}
	 * 如果方块实现了ICreativeModeTabItem则委托给方块，否则根据标签类型注册自身。
	 * <p>
	 * Delegates to the block if it implements ICreativeModeTabItem, otherwise registers itself based on tab type.
	 */
	@Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
		if(getBlock() instanceof ICreativeModeTabItem item)
			item.fillItemCategory(helper);
		else if(helper.isType(tab))
			helper.accept(this);
	}
}
