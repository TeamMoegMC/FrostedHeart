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

package com.teammoeg.chorda.menu.slots;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DynamicIndexHandler extends Slot
{
    public DynamicIndexHandler(Container pContainer, int pSlot, int pX, int pY) {
		super(pContainer, pSlot, pX, pY);
	}

	private int index;


    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        return true;
    }

    @Override
    public ItemStack getItem()
    {
        return container.getItem(index);
    }

    @Override
    public void set(ItemStack stack)
    {
    	container.setItem(index, stack);
        this.setChanged();
    }


    @Override
    public ItemStack remove(int amount)
    {
    	return this.container.removeItem(index, amount);
    }

}
