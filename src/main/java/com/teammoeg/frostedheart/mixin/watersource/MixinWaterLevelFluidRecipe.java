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

package com.teammoeg.frostedheart.mixin.watersource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import gloridifice.watersource.common.recipe.WaterLevelFluidRecipe;
import gloridifice.watersource.common.recipe.WaterLevelItemRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

@Mixin(WaterLevelFluidRecipe.class)
public class MixinWaterLevelFluidRecipe extends WaterLevelItemRecipe {


    public MixinWaterLevelFluidRecipe(ResourceLocation idIn, String groupIn, Ingredient ingredient, int waterLevel,
                                      int waterSaturationLevel) {
        super(idIn, groupIn, ingredient, waterLevel, waterSaturationLevel);
    }

    /**
     * @author khjxiaogu
     * @reason fix nbt checking bug
     */
    @Overwrite(remap = false)
    @Override
    public boolean conform(ItemStack stack) {
        if (ingredient.test(stack)) {
            LazyOptional<IFluidHandlerItem> handler = FluidUtil.getFluidHandler(stack);
            LazyOptional<IFluidHandlerItem> handler2 = FluidUtil.getFluidHandler(ingredient.getMatchingStacks()[0]);
            if (handler != null && handler.isPresent()) {
                if (handler.map(data -> handler2.map(data1 -> data1.getFluidInTank(0).getFluid() == data.getFluidInTank(0).getFluid()).orElse(false)).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
    }
}