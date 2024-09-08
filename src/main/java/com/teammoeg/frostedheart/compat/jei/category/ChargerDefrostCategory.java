/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.util.TranslateUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class ChargerDefrostCategory implements IRecipeCategory<CampfireDefrostRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "charger_defrost");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock charger = new StaticBlock(FHBlocks.charger.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

    public ChargerDefrostCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(() -> new ItemStack(FHBlocks.charger.get()), () -> new ItemStack(FHItems.frozen_seeds.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(CampfireDefrostRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics transform, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.render(transform, 43, 4);
        AllGuiTextures.JEI_DOWN_ARROW.render(transform, 67, 7);


        AllGuiTextures.JEI_SHADOW.render(transform, 72 - 17, 42 + 13);

        AllGuiTextures.JEI_DOWN_ARROW.render(transform, 112, 30);
        AllGuiTextures.JEI_SLOT.render(transform, 117, 47);
        charger.draw(transform, 72, 42);
    }

    @Override
    public IDrawable getBackground() {
        return BACKGROUND;
    }


    @Override
    public IDrawable getIcon() {
        return ICON;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, CampfireDefrostRecipe recipe, IFocusGroup iFocusGroup) {

        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 16, 26).addIngredients(recipe.getIngredients().get(0));
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.OUTPUT, 117, 47).addItemStacks(List.of(recipe.getIss()));

    }

    @Override
    public RecipeType<CampfireDefrostRecipe> getRecipeType() {
        return JEICompat.ChargerDefrost;
    }

    public Component getTitle() {
        return TranslateUtils.translate("gui.jei.category." + FHMain.MODID + ".charger_defrost");
    }

}
