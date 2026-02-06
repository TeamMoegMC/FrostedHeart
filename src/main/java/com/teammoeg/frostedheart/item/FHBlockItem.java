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

package com.teammoeg.frostedheart.item;

import com.teammoeg.chorda.creativeTab.TabType;
import com.teammoeg.chorda.item.CBlockItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FHBlockItem extends CBlockItem {

    public FHBlockItem(Block arg0, Properties arg1, TabType arg2) {
		super(arg0, arg1, arg2);
	}
	public FHBlockItem(Block block) {
        this(block, new Item.Properties(), FHTabs.itemGroup);
        
    }
    public FHBlockItem(Block block, Item.Properties props) {
    	this(block, props, FHTabs.itemGroup);
    }

}
