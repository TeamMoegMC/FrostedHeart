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

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;

public class SaunaCategory implements IRecipeCategory<SaunaRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "sauna");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock sauna = new StaticBlock(FHBlocks.sauna.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

    public SaunaCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableIngredient(new ItemStack(FHBlocks.sauna.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(SaunaRecipe recipe, PoseStack transform, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.draw(transform, 43, 4);
        AllGuiTextures.JEI_DOWN_ARROW.draw(transform, 67, 7);
        AllGuiTextures.JEI_SHADOW.draw(transform, 72 - 17, 42 + 13);

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

    @Override
    public Class<? extends SaunaRecipe> getRecipeClass() {
        return SaunaRecipe.class;
    }

    public String getTitle() {
        return (TranslateUtils.translate("gui.jei.category." + FHMain.MODID + ".sauna").getString());
    }

    @Override
    public List<Component> getTooltipStrings(SaunaRecipe recipe, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        if (isMouseIn(mouseX, mouseY, 43 + 18, 4 + 18, 36, 36)) {
            tooltip.add(recipe.effect.getDisplayName());
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
    public void setIngredients(SaunaRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.input.getItems())));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, SaunaRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 43, 4);
//        itemStacks.init(1, false, 117, 47);
        itemStacks.set(ingredients);
    }
}
