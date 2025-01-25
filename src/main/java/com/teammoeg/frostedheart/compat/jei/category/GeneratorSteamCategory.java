/*
 * Copyright (c) 2024 TeamMoeg
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
import java.util.List;
import java.util.stream.Collectors;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.util.client.Lang;

import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GeneratorSteamCategory implements IRecipeCategory<GeneratorSteamRecipe> {
    public static RecipeType<GeneratorSteamRecipe> UID = RecipeType.create(FHMain.MODID, "generator_steam",GeneratorSteamRecipe.class);
    private IDrawable BACKGROUND;
    private IDrawable SWITCH;
    private IDrawable ICON;
    private IDrawable TANK;
    private IDrawableStatic BAR;


    private IDrawableAnimated FIRE;

    public GeneratorSteamCategory(IGuiHelper guiHelper) {
        ResourceLocation guiMain = new ResourceLocation(FHMain.MODID, "textures/gui/generator_t2.png");
        this.ICON = guiHelper.createDrawableItemStack(new ItemStack(FHMultiblocks.Registration.GENERATOR_T2.blockItem().get()));
        this.TANK = guiHelper.createDrawable(guiMain, 178, 87, 16, 47);
        this.BACKGROUND = guiHelper.createDrawable(guiMain, 4, 4, 164, 72);
        IDrawableStatic tfire = guiHelper.createDrawable(guiMain, 179, 0, 9, 13);
        this.FIRE = guiHelper.createAnimatedDrawable(tfire, 80, IDrawableAnimated.StartDirection.TOP, true);
        this.SWITCH = guiHelper.createDrawable(guiMain, 232, 1, 19, 10);
        this.BAR = guiHelper.createDrawable(guiMain, 181, 28, 2, 54);
    }

    @Override
    public void draw(GeneratorSteamRecipe recipe, IRecipeSlotsView view , GuiGraphics transform, double mouseX, double mouseY) {
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

    public Component getTitle() {
        return (Lang.translateKey("gui.jei.category." + FHMain.MODID + ".generator_steam"));
    }

    @Override
    public List<Component> getTooltipStrings(GeneratorSteamRecipe recipe,IRecipeSlotsView view, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();

        if (isMouseIn(mouseX, mouseY, 8, 9, 2, 54)) {
            tooltip.add(Lang.translateGui("generator.temperature.level").append(String.valueOf(recipe.level)));
        }


        if (isMouseIn(mouseX, mouseY, 142, 9, 2, 54)) {
            tooltip.add(Lang.translateGui("generator.power.level").append(String.valueOf(recipe.power)));
        }
        return tooltip;
    }


    public boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y
                && mouseX < x + w && mouseY < y + h;
    }


	@Override
	public RecipeType<GeneratorSteamRecipe> getRecipeType() {
		return UID;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, GeneratorSteamRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 26, 12).addIngredients(ForgeTypes.FLUID_STACK, recipe.input.getMatchingFluidStacks()).setFluidRenderer(recipe.input.getAmount()*5, false, 16, 47).setOverlay(TANK, 0, 0);
		List<GeneratorRecipe> recipes= CUtils.filterRecipes(null, GeneratorRecipe.TYPE);
		builder.addSlot(RecipeIngredientRole.INPUT, 75, 7).addItemStacks(recipes.stream().flatMap(t->Arrays.stream(t.input.getMatchingStacks())).collect(Collectors.toList()));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 75, 46).addItemStacks(recipes.stream().map(t->t.output).collect(Collectors.toList()));
	}
}
