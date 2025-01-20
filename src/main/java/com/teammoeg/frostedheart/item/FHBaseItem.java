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

package com.teammoeg.frostedheart.item;

import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FHBaseItem extends Item implements ICreativeModeTabItem{
    Item repair;

    public FHBaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (repair == null) return false;
        return repair.getItem() == this.repair;
    }

    public FHBaseItem setRepairItem(Item it) {
        repair = it;
        return this;
    }

	@Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
		if(helper.isType(FHTabs.itemGroup))
			helper.accept(this);
		
	}
}
