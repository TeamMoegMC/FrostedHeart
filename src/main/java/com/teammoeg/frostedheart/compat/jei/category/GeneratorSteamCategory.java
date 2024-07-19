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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class GeneratorSteamCategory implements IRecipeCategory<GeneratorSteamRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "generator_steam");
    private IDrawable BACKGROUND;
    private IDrawable SWITCH;
    private IDrawable ICON;
    private IDrawable TANK;
    private IDrawableStatic BAR;


    private IDrawableAnimated FIRE;

    public GeneratorSteamCategory(IGuiHelper guiHelper) {
        ResourceLocation guiMain = new ResourceLocation(FHMain.MODID, "textures/gui/generator_t2.png");
        this.ICON = guiHelper.createDrawableIngredient(new ItemStack(FHMultiblocks.generator_t2));
        this.TANK = guiHelper.createDrawable(guiMain, 178, 87, 16, 47);
        this.BACKGROUND = guiHelper.createDrawable(guiMain, 4, 4, 164, 72);
        IDrawableStatic tfire = guiHelper.createDrawable(guiMain, 179, 0, 9, 13);
        this.FIRE = guiHelper.createAnimatedDrawable(tfire, 80, IDrawableAnimated.StartDirection.TOP, true);
        this.SWITCH = guiHelper.createDrawable(guiMain, 232, 1, 19, 10);
        this.BAR = guiHelper.createDrawable(guiMain, 181, 28, 2, 54);
    }

    @Override
    public void draw(GeneratorSteamRecipe recipe, MatrixStack transform, double mouseX, double mouseY) {
        FIRE.draw(transform, 80, 28);
        SWITCH.draw(transform, 52, 31);
        int offset1 = (int) ((4 - recipe.level) * 14);
        BAR.draw(transform, 8, 9, offset1, 0, 0, 0);

        int offset3 = (int) ((1 - recipe.power / 100) * 56);
        BAR.draw(transform, 142, 9, offset3, 0, 0, 0);
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
    public Class<? extends GeneratorSteamRecipe> getRecipeClass() {
        return GeneratorSteamRecipe.class;
    }

    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".generator_steam").getString());
    }

    @Override
    public List<ITextComponent> getTooltipStrings(GeneratorSteamRecipe recipe, double mouseX, double mouseY) {
        List<ITextComponent> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 8, 9, 2, 54)) {
            tooltip.add(TranslateUtils.translateGui("generator.temperature.level").appendString(String.valueOf(recipe.level)));
        }


        if (isMouseIn(mouseX, mouseY, 142, 9, 2, 54)) {
            tooltip.add(TranslateUtils.translateGui("generator.power.level").appendString(String.valueOf(recipe.power)));
        }
        return tooltip;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    public boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y
                && mouseX < x + w && mouseY < y + h;
    }

    @Override
    public void setIngredients(GeneratorSteamRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(recipe.input.getMatchingFluidStacks()));
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, GeneratorSteamRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();
        guiFluidStacks.init(0, true, 26, 12, 16, 47, recipe.input.getAmount() * 5, false, TANK);
        guiFluidStacks.set(ingredients);
        guiItemStacks.init(0, true, 75, 7);
        guiItemStacks.init(1, false, 75, 46);
        List<GeneratorRecipe> recipes=FHUtils.filterRecipes(null, GeneratorRecipe.TYPE);
        guiItemStacks.set(0, recipes.stream().flatMap(t->Arrays.stream(t.input.getMatchingStacks())).collect(Collectors.toList()));
        guiItemStacks.set(1, recipes.stream().map(t->t.output).collect(Collectors.toList()));
    }
}
