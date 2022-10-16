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

package com.teammoeg.frostedheart.compat.jei.extension;

import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.content.temperature.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.content.temperature.handstoves.RecipeFueling;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

public class FuelingExtension implements ICraftingCategoryExtension {
    RecipeFueling fuel;

    public FuelingExtension(RecipeFueling rf) {
        fuel = rf;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return fuel.getId();
    }

    @Override
    public void setIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(Arrays.asList(new ItemStack(FHItems.hand_stove)), Arrays.asList(fuel.getIngredient().getMatchingStacks())));
        ItemStack out = new ItemStack(FHItems.hand_stove);
        CoalHandStove.setFuelAmount(out, fuel.getFuel());
        ingredients.setOutput(VanillaTypes.ITEM, out);
    }

}
