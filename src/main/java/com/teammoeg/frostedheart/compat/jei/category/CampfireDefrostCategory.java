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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.util.lang.Lang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class CampfireDefrostCategory implements IRecipeCategory<CampfireDefrostRecipe> {
    public static RecipeType<CampfireDefrostRecipe> UID = RecipeType.create(FHMain.MODID, "defrost_campfire", CampfireDefrostRecipe.class);
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
    public void draw(CampfireDefrostRecipe recipe,IRecipeSlotsView view , GuiGraphics transform, double mouseX, double mouseY) {
        animatedFlame.draw(transform, 1, 20);
        IDrawableAnimated arrow = getArrow(recipe);
        arrow.draw(transform, 24, 8);
        drawCookTime(recipe, transform, 35);
    }

    protected void drawCookTime(CampfireDefrostRecipe recipe, GuiGraphics matrixStack, int y) {
        int cookTime = recipe.getCookingTime();
        if (cookTime > 0) {
            int cookTimeSeconds = cookTime / 20;
            Component timeString = Lang.translateKey("gui.jei.category.smelting.time.seconds", cookTimeSeconds);
            Minecraft minecraft = Minecraft.getInstance();
            Font fontRenderer = minecraft.font;
            int stringWidth = fontRenderer.width(timeString);
            matrixStack.drawString(fontRenderer, timeString, BACKGROUND.getWidth() - stringWidth, y, 0xFF808080);
        }
    }


    protected IDrawableAnimated getArrow(CampfireDefrostRecipe recipe) {
        int cookTime = recipe.getCookingTime();
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


    public Component getTitle() {
        return (Lang.translateKey("gui.jei.category." + FHMain.MODID + ".defrost_campfire"));
    }



    @Override
    public void setRecipe(IRecipeLayoutBuilder recipeLayout, CampfireDefrostRecipe recipe, IFocusGroup ingredients) {
        recipeLayout.addSlot(RecipeIngredientRole.INPUT, 0, 0).addIngredients(recipe.getIngredient());
        recipeLayout.addSlot(RecipeIngredientRole.OUTPUT, 60, 8).addItemStacks(Arrays.asList(recipe.getIss()));
    }

	@Override
	public RecipeType<CampfireDefrostRecipe> getRecipeType() {
		return UID;
	}

}
