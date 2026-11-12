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

package com.teammoeg.frostedheart.content.water.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class DurableDrinkContainerItem extends DrinkContainerItem {
    public DurableDrinkContainerItem(Properties properties, int capacity) {
        super(properties.stacksTo(1), capacity);
    }

    public int getFluidAmount(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(t->t.getFluidInTank(0).getAmount()).orElse(0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7cbeff;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13*(float)getFluidAmount(stack) / capacity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFluidAmount(stack) > 0;
    }
}
