/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Immersive Industry.
 *
 * Immersive Industry is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Immersive Industry is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Immersive Industry. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.compat.jei.category;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHContent.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.SmokingRecipe;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;

public class ChargerCookingCategory implements IRecipeCategory<SmokingRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "charge_cooking");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock charger = new StaticBlock(FHBlocks.charger.getDefaultState().with(BlockStateProperties.FACING,Direction.EAST));
    /**
	 * @param guiHelper  
	 */
    public ChargerCookingCategory(IGuiHelper guiHelper) {
        this.ICON = new DoubleItemIcon(()->new ItemStack(FHContent.FHBlocks.charger),()->new ItemStack(Items.COOKED_BEEF));
        this.BACKGROUND = new EmptyBackground(177,70);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends SmokingRecipe> getRecipeClass() {
        return SmokingRecipe.class;
    }


    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + FHMain.MODID + ".charger_cooking").getString());
    }
	@Override
	public void draw(SmokingRecipe recipe, MatrixStack transform, double mouseX, double mouseY)
	{
		AllGuiTextures.JEI_SLOT.draw(transform, 43, 4);
		AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 67, 7);
		
		
		AllGuiTextures.JEI_SHADOW.draw(transform, 72 - 17, 42 + 13);
		
		AllGuiTextures.JEI_DOWN_ARROW.draw(transform,112,30);
		AllGuiTextures.JEI_SLOT.draw(transform, 117 , 47);
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
    public void setIngredients(SmokingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM,Arrays.asList(Arrays.asList(recipe.getIngredients().get(0).getMatchingStacks())));
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout,SmokingRecipe recipe, IIngredients ingredients) {
		IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
		itemStacks.init(0, true, 43, 4);
		
		itemStacks.init(1, false,117, 47);
		itemStacks.set(ingredients);
    }
}
