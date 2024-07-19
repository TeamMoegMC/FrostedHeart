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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.util.TranslateUtils;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.config.Constants;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class CampfireDefrostCategory implements IRecipeCategory<CampfireDefrostRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "defrost_campfire");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private LoadingCache<Integer, IDrawableAnimated> cachedArrows;
    private IDrawable animatedFlame;
    private IDrawableStatic staticFlame;

    public CampfireDefrostCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(() -> new ItemStack(Blocks.CAMPFIRE), () -> new ItemStack(FHItems.frozen_seeds.get()));
        this.BACKGROUND = guiHelper.drawableBuilder(Constants.RECIPE_GUI_VANILLA, 0, 186, 82, 34)
                .addPadding(0, 10, 0, 0)
                .build();
        this.cachedArrows = CacheBuilder.newBuilder()
                .maximumSize(25)
                .build(new CacheLoader<Integer, IDrawableAnimated>() {
                    @Override
                    public IDrawableAnimated load(Integer cookTime) {
                        return guiHelper.drawableBuilder(Constants.RECIPE_GUI_VANILLA, 82, 128, 24, 17)
                                .buildAnimated(cookTime, IDrawableAnimated.StartDirection.LEFT, false);
                    }
                });
        staticFlame = guiHelper.createDrawable(Constants.RECIPE_GUI_VANILLA, 82, 114, 14, 14);
        animatedFlame = guiHelper.createAnimatedDrawable(staticFlame, 300, IDrawableAnimated.StartDirection.TOP, true);
    }

    @Override
    public void draw(CampfireDefrostRecipe recipe, MatrixStack transform, double mouseX, double mouseY) {
        animatedFlame.draw(transform, 1, 20);
        IDrawableAnimated arrow = getArrow(recipe);
        arrow.draw(transform, 24, 8);
        drawCookTime(recipe, transform, 35);
    }

    protected void drawCookTime(CampfireDefrostRecipe recipe, MatrixStack matrixStack, int y) {
        int cookTime = recipe.getCookTime();
        if (cookTime > 0) {
            int cookTimeSeconds = cookTime / 20;
            ITextComponent timeString = TranslateUtils.translate("gui.jei.category.smelting.time.seconds", cookTimeSeconds);
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer fontRenderer = minecraft.fontRenderer;
            int stringWidth = fontRenderer.getStringPropertyWidth(timeString);
            fontRenderer.drawText(matrixStack, timeString, BACKGROUND.getWidth() - stringWidth, y, 0xFF808080);
        }
    }


    protected IDrawableAnimated getArrow(CampfireDefrostRecipe recipe) {
        int cookTime = recipe.getCookTime();
        if (cookTime <= 0) {
            cookTime = 100;
        }
        return this.cachedArrows.getUnchecked(cookTime);
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
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".defrost_campfire").getString());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void setIngredients(CampfireDefrostRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.getIngredient().getMatchingStacks())));
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.getIss())));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CampfireDefrostRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        guiItemStacks.init(0, true, 0, 0);
        guiItemStacks.init(1, false, 60, 8);

        guiItemStacks.set(ingredients);
    }
}
