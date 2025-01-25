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

package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import net.minecraft.world.item.ItemStack;

public class LeatherWaterBagItem extends DurableDrinkContainerItem{
    public LeatherWaterBagItem( Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(FHItems.LEATHER_WATER_BAG.get());
    }

    @Override
    public ItemStack getDrinkItem() {
        return new ItemStack(FHItems.LEATHER_WATER_BAG.get());
    }
}
