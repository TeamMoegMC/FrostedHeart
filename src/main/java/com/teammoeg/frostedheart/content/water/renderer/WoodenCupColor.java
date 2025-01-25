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

package com.teammoeg.frostedheart.content.water.renderer;

import com.teammoeg.frostedheart.content.water.util.FluidHelper;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;


public class WoodenCupColor implements ItemColor {
    @Override
    public int getColor(ItemStack itemStack, int tintIndex) {
        if (tintIndex == 1) {
            int color = FluidUtil.getFluidHandler(itemStack).map(h -> h.getFluidInTank(0).getFluid()).map(FluidHelper::getColor).get();
            if (color == 0) {
                return -1;
            }
            return color;
        }
        else return -1;
    }
}
