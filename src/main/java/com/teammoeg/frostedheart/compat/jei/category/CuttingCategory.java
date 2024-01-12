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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.CuttingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CuttingCategory implements IRecipeCategory<CuttingRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "knife_cutting");
    public static List<Item> matching;
    public static final ResourceLocation ktag = new ResourceLocation(FHMain.MODID, "knife");
    private IDrawable BACKGROUND;
    private IDrawable ICON;

    public CuttingCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(() -> new ItemStack(Items.IRON_SWORD), () -> new ItemStack(FHItems.brown_mushroombed));
        this.BACKGROUND = new EmptyBackground(120, 50);
    }

    @Override
    public void draw(CuttingRecipe recipe, MatrixStack transform, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.draw(transform, 8, 4);
        AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 29, 7);
        AllGuiTextures.JEI_SLOT.draw(transform, 34, 24);
        AllGuiTextures.JEI_ARROW.draw(transform, 54, 28);
        AllGuiTextures.JEI_SLOT.draw(transform, 96, 24);
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
    public Class<? extends CuttingRecipe> getRecipeClass() {
        return CuttingRecipe.class;
    }

    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".knife_cutting").getString());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void setIngredients(CuttingRecipe recipe, IIngredients ingredients) {
        ArrayList<List<ItemStack>> als = new ArrayList<>(2);
        als.add(Arrays.asList(recipe.in));
        if (matching != null)
            als.add(matching.stream().map(ItemStack::new).collect(Collectors.toList()));
        ingredients.setInputLists(VanillaTypes.ITEM, als);
        ingredients.setOutput(VanillaTypes.ITEM, recipe.out);
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CuttingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 34, 24);
        itemStacks.init(1, true, 8, 4);
        itemStacks.init(2, false, 96, 24);
        itemStacks.set(ingredients);
    }
}
