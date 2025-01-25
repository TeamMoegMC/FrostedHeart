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

import java.util.Arrays;

import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.util.client.Lang;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ChargerDefrostCategory implements IRecipeCategory<CampfireDefrostRecipe> {
    public static RecipeType<CampfireDefrostRecipe> UID =  RecipeType.create(FHMain.MODID, "charger_defrost",CampfireDefrostRecipe.class);
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock charger = new StaticBlock(FHBlocks.CHARGER.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

    public ChargerDefrostCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(() -> new ItemStack(FHBlocks.CHARGER.get()), () -> new ItemStack(FHItems.frozen_seeds.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(CampfireDefrostRecipe recipe,IRecipeSlotsView view , GuiGraphics transform, double mouseX, double mouseY) {
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


    public Component getTitle() {
        return (Lang.translateKey("gui.jei.category." + FHMain.MODID + ".charger_defrost"));
    }


	@Override
	public RecipeType<CampfireDefrostRecipe> getRecipeType() {
		return UID;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CampfireDefrostRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 43, 4).addIngredients(recipe.getIngredients().get(0));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 47).addItemStacks(Arrays.asList(recipe.getIss()));
		
	}
}
