/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.base.item;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class FHBlockItem extends BlockItem {
    public FHBlockItem(Block block) {
        this(block, new Item.Properties().tab(FHMain.itemGroup));
    }

    public FHBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }

    public FHBlockItem(Block block, Item.Properties props, String name) {
        this(block, new Item.Properties().tab(FHMain.itemGroup));
        this.setRegistryName(FHMain.MODID, name);
    }
}
