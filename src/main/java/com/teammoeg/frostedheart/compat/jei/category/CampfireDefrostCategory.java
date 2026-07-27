/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.compat.jei.category;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.library.plugins.vanilla.cooking.AbstractCookingCategory;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.world.level.block.Blocks;

public class CampfireDefrostCategory extends AbstractCookingCategory<CampfireDefrostRecipe> {
    public static RecipeType<CampfireDefrostRecipe> UID = RecipeType.create(FHMain.MODID, "defrost_campfire", CampfireDefrostRecipe.class);

    public CampfireDefrostCategory(IGuiHelper guiHelper) {
        super(guiHelper, UID, Blocks.CAMPFIRE, "gui.jei.category." + FHMain.MODID + ".defrost_campfire", 400, 82, 44);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CampfireDefrostRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1)
                .setStandardSlotBackground()
                .addIngredients(recipe.getIngredients().get(0));

        builder.addOutputSlot(61, 9)
                .setOutputSlotBackground()
                .addItemStack(RecipeUtil.getResultItem(recipe));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CampfireDefrostRecipe recipe, IFocusGroup focuses) {
        int cookTime = recipe.getCookingTime();
        if (cookTime <= 0) {
            cookTime = regularCookTime;
        }
        builder.addAnimatedRecipeArrow(cookTime)
                .setPosition(26, 7);
        builder.addAnimatedRecipeFlame(300)
                .setPosition(1, 20);

        addCookTime(builder, recipe);
    }

    @Override
    public boolean isHandled(CampfireDefrostRecipe recipe) {
        return true;
    }
}
