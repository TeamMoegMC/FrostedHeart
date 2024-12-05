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
import java.util.List;

import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.util.lang.Lang;

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

public class SaunaCategory implements IRecipeCategory<SaunaRecipe> {
    public static RecipeType<SaunaRecipe> UID = RecipeType.create(FHMain.MODID, "sauna",SaunaRecipe.class);
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock sauna = new StaticBlock(FHBlocks.SAUNA_VENT.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

    public SaunaCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableItemStack(new ItemStack(FHBlocks.SAUNA_VENT.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(SaunaRecipe recipe, IRecipeSlotsView view, GuiGraphics transform, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.render(transform, 43, 4);
        AllGuiTextures.JEI_DOWN_ARROW.render(transform, 67, 7);
        AllGuiTextures.JEI_SHADOW.render(transform, 72 - 17, 42 + 13);

//        AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 112, 30);
//        AllGuiTextures.JEI_SLOT.draw(transform, 117, 47);

        sauna.draw(transform, 72, 42);
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
        return (Lang.translateKey("gui.jei.category." + FHMain.MODID + ".sauna"));
    }

    @Override
    public List<Component> getTooltipStrings(SaunaRecipe recipe,IRecipeSlotsView view , double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        if (isMouseIn(mouseX, mouseY, 43 + 18, 4 + 18, 36, 36)) {
            tooltip.add(recipe.effect.getDisplayName());
        }
        return tooltip;
    }



    public boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y
                && mouseX < x + w && mouseY < y + h;
    }


	@Override
	public RecipeType<SaunaRecipe> getRecipeType() {
		return UID;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SaunaRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 43, 4).addIngredients(recipe.input);
	}
}
