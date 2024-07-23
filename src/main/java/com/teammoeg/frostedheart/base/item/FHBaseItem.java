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

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraft.item.Item.Properties;

public class FHBaseItem extends Item {
    Item repair;

    public FHBaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (repair == null) return false;
        return repair.getItem() == this.repair;
    }

    public Item setRepairItem(Item it) {
        repair = it;
        return this;
    }
}
