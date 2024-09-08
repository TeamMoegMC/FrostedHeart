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

import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.compat.jei.StaticBlock;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.util.TranslateUtils;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class SaunaCategory implements IRecipeCategory<SaunaRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "sauna");
    private IDrawable BACKGROUND;
    private IDrawable ICON;
    private StaticBlock sauna = new StaticBlock(FHBlocks.sauna.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

    public SaunaCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(FHBlocks.sauna.get()));
        this.BACKGROUND = new EmptyBackground(177, 70);
    }

    @Override
    public void draw(SaunaRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics transform, double mouseX, double mouseY) {
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, SaunaRecipe saunaRecipe, IFocusGroup iFocusGroup) {
        iRecipeLayoutBuilder.addSlot(RecipeIngredientRole.INPUT, 43, 4).addIngredients(Ingredient.merge(saunaRecipe.getIngredients()));
    }

    @Override
    public RecipeType<SaunaRecipe> getRecipeType() {
        return JEICompat.Sauna;
    }

    public Component getTitle() {
        return TranslateUtils.translate("gui.jei.category." + FHMain.MODID + ".sauna");
    }

    @Override
    public List<Component> getTooltipStrings(SaunaRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
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
//
//    @Override
//    public void setIngredients(SaunaRecipe recipe, IIngredients ingredients) {
//        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(recipe.input.getItems())));
//    }
//
//    @Override
//    public void setRecipe(IRecipeLayout recipeLayout, SaunaRecipe recipe, IIngredients ingredients) {
//        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
//        itemStacks.init(0, true, 43, 4);
////        itemStacks.init(1, false, 117, 47);
//        itemStacks.set(ingredients);
//    }
}
