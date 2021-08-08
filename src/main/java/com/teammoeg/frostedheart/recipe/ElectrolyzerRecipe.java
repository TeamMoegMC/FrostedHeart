/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.recipe;

import com.teammoeg.frostedheart.FHMain;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipe;
import electrodynamics.common.recipe.recipeutils.CountableIngredient;
import electrodynamics.common.recipe.recipeutils.FluidIngredient;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

public class ElectrolyzerRecipe extends FluidItem2FluidRecipe {
    public static IRecipeType<ElectrolyzerRecipe> TYPE;
    public static RegistryObject<IRecipeSerializer<?>> SERIALIZER;
    public static final String RECIPE_GROUP = "electrolyzer_recipe";
    public static final ResourceLocation RECIPE_ID = new ResourceLocation(FHMain.MODID, RECIPE_GROUP);

    public ElectrolyzerRecipe(ResourceLocation recipeID, CountableIngredient inputItem, FluidIngredient inputFluid, FluidStack outputFluid) {
        super(recipeID, inputItem, inputFluid, outputFluid);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return Registry.RECIPE_TYPE.getOrDefault(RECIPE_ID);
    }


}
