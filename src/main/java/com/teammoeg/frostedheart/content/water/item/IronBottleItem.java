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

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraftforge.fluids.FluidStack;

public class IronBottleItem extends DurableDrinkContainerItem{
    public IronBottleItem(Properties properties, int capacity) {
        super(properties, capacity);
    }
    @Override
	public boolean isValidFluid(FluidStack stack) {

		return super.isValidFluid(stack)&&stack.getFluid().is(FHTags.Fluids.IRON_CUP_DRINK.tag);
	}

}
