/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.recipes.ShapelessCopyDataRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShapelessCopyDataExtension implements ICraftingCategoryExtension {
    ShapelessCopyDataRecipe recipe;

    public ShapelessCopyDataExtension(ShapelessCopyDataRecipe rf) {
        recipe = rf;
        FHMain.LOGGER.info("Loading extension for " + rf.getId());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, ICraftingGridHelper iCraftingGridHelper, IFocusGroup iFocusGroup) {
        List<List<ItemStack>> input = new ArrayList<>();
        recipe.getIngredients().forEach(i -> input.add(List.of(i.getItems())));
        iCraftingGridHelper.createAndSetInputs(iRecipeLayoutBuilder, input, 3, 3);
        iCraftingGridHelper.createAndSetOutputs(iRecipeLayoutBuilder, Collections.singletonList(recipe.getResultItem(null)));
    }

    @Override
    public ResourceLocation getRegistryName() {
        return recipe.getId();
    }

//    @Override
//    public void setIngredients(IIngredients ingredients) {
//        ingredients.setInputIngredients(recipe.getIngredients());
//        ingredients.setOutput(VanillaTypes.ITEM, recipe.getResultItem());
//    }

}
