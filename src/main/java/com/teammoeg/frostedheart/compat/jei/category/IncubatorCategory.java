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

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.level.material.Fluids;

public class IncubatorCategory implements IRecipeCategory<IncubateRecipe> {
    public static RecipeType<IncubateRecipe> UID = RecipeType.create(FHMain.MODID, "incubator",IncubateRecipe.class);
    private IDrawable BACKGROUND;
    private IDrawable ICON;


    private IDrawableAnimated FIRE;
    private IDrawableAnimated PROC;
    private IDrawableAnimated EFF;

    public IncubatorCategory(IGuiHelper guiHelper) {
        ResourceLocation guiMain = new ResourceLocation(FHMain.MODID, "textures/gui/incubator.png");
        this.ICON = guiHelper.createDrawableItemStack(new ItemStack(FHBlocks.incubator1.get()));
        this.BACKGROUND = guiHelper.createDrawable(guiMain, 4, 4, 164, 72);
        IDrawableStatic tfire = guiHelper.createDrawable(guiMain, 198, 64, 14, 14);
        this.FIRE = guiHelper.createAnimatedDrawable(tfire, 80, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic tproc = guiHelper.createDrawable(guiMain, 176, 0, 32, 29);
        this.PROC = guiHelper.createAnimatedDrawable(tproc, 240, IDrawableAnimated.StartDirection.LEFT, false);
        IDrawableStatic teff = guiHelper.createDrawable(guiMain, 207, 29, 9, 35);
        this.EFF = guiHelper.createAnimatedDrawable(teff, 240, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public void draw(IncubateRecipe recipe, IRecipeSlotsView view, GuiGraphics transform, double mouseX, double mouseY) {
        FIRE.draw(transform, 31, 31);
        PROC.draw(transform, 76, 24);
        EFF.draw(transform, 15, 31);

        String burnTime;
        if (recipe.time < 1000 || recipe.time % 60 != 0)
            burnTime = recipe.time + " s";
        else
            burnTime = recipe.time / 60 + " m";
        int width = ClientUtils.mc().font.width(burnTime);
        transform.drawString(ClientUtils.mc().font, burnTime, 162 - width, 62, 0xFFFFFF);
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
        return (TranslateUtils.translate("gui.jei.category." + FHMain.MODID + ".incubator"));
    }


    public boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y
                && mouseX < x + w && mouseY < y + h;
    }



	@Override
	public RecipeType<IncubateRecipe> getRecipeType() {
		return UID;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, IncubateRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 57, 16).addFluidStack(Fluids.WATER, recipe.water)
		.setFluidRenderer(recipe.water*5, false, 16, 46).addTooltipCallback((v,t)->t.add(TranslateUtils.translateGui("mb_per_sec", recipe.water)));
		
		IRecipeSlotBuilder fluidout=builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 16).setFluidRenderer(1000, false, 16,46);
		if (!recipe.output_fluid.isEmpty())
			fluidout.setFluidRenderer(recipe.output_fluid.getAmount()*5, false, 16,46).addIngredient(ForgeTypes.FLUID_STACK, recipe.output_fluid)
			.addTooltipCallback((v,t)->{if(recipe.isFood)t.add(TranslateUtils.translateGui("per_food_value", recipe.output_fluid.getAmount()));});
		builder.addSlot(RecipeIngredientRole.INPUT, 29, 47);
		builder.addSlot(recipe.consume_catalyst?RecipeIngredientRole.INPUT:RecipeIngredientRole.CATALYST, 11, 12)
		.addItemStacks(Arrays.asList(recipe.catalyst.getMatchingStacks()))
		.addTooltipCallback((v,t)->{if(recipe.consume_catalyst)t.add(TranslateUtils.translateGui("not_consume"));});
		builder.addSlot(RecipeIngredientRole.INPUT, 29, 12).addItemStacks(Arrays.asList(recipe.input.getMatchingStacks()))
		.addTooltipCallback((v,t)->{if(recipe.isFood)t.add(TranslateUtils.translateGui("any_food"));});
		
		IRecipeSlotBuilder itemout=builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 31);
        if (!recipe.output.isEmpty())
        	itemout.addItemStack(recipe.output);
		
	}
}
