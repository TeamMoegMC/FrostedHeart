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

import java.util.Arrays;
import java.util.stream.Collectors;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.utility.recipe.ShapelessCopyDataRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.resources.ResourceLocation;

public class ShapelessCopyDataExtension implements ICraftingCategoryExtension {
    ShapelessCopyDataRecipe recipe;

    public ShapelessCopyDataExtension(ShapelessCopyDataRecipe rf) {
        recipe = rf;
        FHMain.LOGGER.info("Loading extension for " + rf.getId());
    }

    @Override
    public ResourceLocation getRegistryName() {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    	craftingGridHelper.createAndSetInputs(builder, recipe.getIngredients().stream().map(t->Arrays.asList(t.getItems())).collect(Collectors.toList()), 0, 0);
    	craftingGridHelper.createAndSetOutputs(builder, Arrays.asList(RecipeUtil.getResultItem(recipe)));
    }

}
