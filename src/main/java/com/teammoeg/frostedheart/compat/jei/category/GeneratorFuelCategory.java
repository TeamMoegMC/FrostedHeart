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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public class GeneratorFuelCategory implements IRecipeCategory<GeneratorRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "generator_fuel");
    private IDrawable BACKGROUND;
    private IDrawable SWITCH;
    private IDrawable ICON;
    private IDrawableAnimated FIRE;

    public GeneratorFuelCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableIngredient(new ItemStack(FHMultiblocks.generator));
        ResourceLocation guiMain = new ResourceLocation(FHMain.MODID, "textures/gui/generator_t1.png");
        this.BACKGROUND = guiHelper.createDrawable(guiMain, 24, 3, 134, 72);
        IDrawableStatic tfire = guiHelper.createDrawable(guiMain, 179, 0, 9, 13);
        this.FIRE = guiHelper.createAnimatedDrawable(tfire, 80, IDrawableAnimated.StartDirection.TOP, true);
        this.SWITCH = guiHelper.createDrawable(guiMain, 232, 1, 19, 10);
    }

    @Override
    public void draw(GeneratorRecipe recipe, MatrixStack transform, double mouseX, double mouseY) {
        FIRE.draw(transform, 60, 30);
        SWITCH.draw(transform, 32, 32);
        String burnTime = recipe.time + " ticks";
        ClientUtils.mc().fontRenderer.drawString(transform, burnTime, 80, 60, 0xFFFFFF);
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
    public Class<? extends GeneratorRecipe> getRecipeClass() {
        return GeneratorRecipe.class;
    }

    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".generator_fuel").getString());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void setIngredients(GeneratorRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.input.getMatchingStacks())));
        ingredients.setOutput(VanillaTypes.ITEM, recipe.output);
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, GeneratorRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        guiItemStacks.init(0, true, 55, 8);
        guiItemStacks.init(1, false, 55, 47);
        guiItemStacks.set(ingredients);
    }
}
