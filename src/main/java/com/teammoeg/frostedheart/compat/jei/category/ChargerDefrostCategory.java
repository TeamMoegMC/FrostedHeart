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

import java.util.Arrays;
import java.util.Collections;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public class ChargerDefrostCategory implements IRecipeCategory<CampfireDefrostRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "charger_defrost");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock charger = new StaticBlock(FHBlocks.charger.get().getDefaultState().with(BlockStateProperties.FACING, Direction.EAST));

    public ChargerDefrostCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(() -> new ItemStack(FHBlocks.charger.get()), () -> new ItemStack(FHItems.frozen_seeds.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(CampfireDefrostRecipe recipe, MatrixStack transform, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.draw(transform, 43, 4);
        AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 67, 7);


        AllGuiTextures.JEI_SHADOW.draw(transform, 72 - 17, 42 + 13);

        AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 112, 30);
        AllGuiTextures.JEI_SLOT.draw(transform, 117, 47);
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
    public Class<? extends CampfireDefrostRecipe> getRecipeClass() {
        return CampfireDefrostRecipe.class;
    }

    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".charger_defrost").getString());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void setIngredients(CampfireDefrostRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.getIngredients().get(0).getMatchingStacks())));
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.getIss())));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CampfireDefrostRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 43, 4);

        itemStacks.init(1, false, 117, 47);
        itemStacks.set(ingredients);
    }
}
