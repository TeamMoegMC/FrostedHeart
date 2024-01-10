/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.climate.data;

import com.teammoeg.frostedheart.climate.player.ITempAdjustFood;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class CupTempAdjustProxy implements ITempAdjustFood {
    float efficiency;
    ITempAdjustFood defData;

    public CupTempAdjustProxy(float efficiency, ITempAdjustFood defaultData) {
        this.efficiency = efficiency;
        this.defData = defaultData;
    }

    @Override
    public float getMaxTemp(ItemStack is) {
        if (defData != null)
            return defData.getMaxTemp(is);
        return ITempAdjustFood.super.getMaxTemp(is);
    }

    @Override
    public float getMinTemp(ItemStack is) {
        if (defData != null)
            return defData.getMinTemp(is);
        return ITempAdjustFood.super.getMinTemp(is);
    }

    @Override
    public float getHeat(ItemStack is,float env) {
        LazyOptional<IFluidHandlerItem> ih = FluidUtil.getFluidHandler(is);
        if (ih.isPresent()) {
            IFluidHandlerItem f = ih.resolve().get();
            FluidStack fs = f.getFluidInTank(0);
            if (!fs.isEmpty()) {
            	float dh=FHDataManager.getDrinkHeat(fs);
            	if((env>37&&dh<0)||(env<37&&dh>0))
            		return dh * efficiency;
            	if(env==37)
            		return dh;
            	
            	return dh * (2-efficiency);
            }
        }
        if (defData != null)
            return defData.getHeat(is,env);
        return 0;
    }

}
