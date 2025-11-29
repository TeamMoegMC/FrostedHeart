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

package com.teammoeg.frostedheart.compat.jei.extension;

import java.util.Arrays;
import java.util.Collections;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.utility.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FuelingExtension implements ICraftingCategoryExtension {
    FuelingRecipe fuel;

    public FuelingExtension(FuelingRecipe rf) {
        fuel = rf;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return fuel.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    	builder.setShapeless();
    	craftingGridHelper.createAndSetInputs(builder, Arrays.asList(Collections.singletonList(new ItemStack(FHItems.hand_stove.get())), Arrays.asList(fuel.getIngredient().getItems())),0,0);
        ItemStack out = new ItemStack(FHItems.hand_stove.get());
        CoalHandStove.setFuelAmount(out, fuel.getFuel());
        craftingGridHelper.createAndSetOutputs(builder, Arrays.asList(out));
    }

}
