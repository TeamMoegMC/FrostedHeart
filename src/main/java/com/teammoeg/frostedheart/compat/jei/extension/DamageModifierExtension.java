/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.compat.jei.extension;

import com.teammoeg.frostedheart.recipes.ModifyDamageRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class DamageModifierExtension implements ICraftingCategoryExtension {
    ModifyDamageRecipe fuel;

    public DamageModifierExtension(ModifyDamageRecipe rf) {
        fuel = rf;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, ICraftingGridHelper iCraftingGridHelper, IFocusGroup iFocusGroup) {
        ItemStack[] orig = fuel.tool.getItems();
        ItemStack[] copy = new ItemStack[orig.length];
        ItemStack[] out = new ItemStack[orig.length];
        for (int i = 0; i < orig.length; i++) {
            copy[i] = orig[i].copy();
            out[i] = orig[i].copy();
            if (fuel.modify > 0) {
                copy[i].setDamageValue(fuel.modify);
                out[i].setDamageValue(0);
            } else {
                copy[i].setDamageValue(0);
                out[i].setDamageValue(fuel.modify);
            }
        }
        iCraftingGridHelper.createAndSetInputs(iRecipeLayoutBuilder, Arrays.asList(Arrays.asList(copy), Arrays.asList(fuel.repair.getItems())), 3, 3);
        iCraftingGridHelper.createAndSetOutputs(iRecipeLayoutBuilder, Arrays.asList(out));
    }

    @Override
    public ResourceLocation getRegistryName() {
        return fuel.getId();
    }

//    @Override
//    public void setIngredients(IIngredients ingredients) {
//        ItemStack[] orig = fuel.tool.getItems();
//        ItemStack[] copy = new ItemStack[orig.length];
//        ItemStack[] out = new ItemStack[orig.length];
//        for (int i = 0; i < orig.length; i++) {
//            copy[i] = orig[i].copy();
//            out[i] = orig[i].copy();
//            if (fuel.modify > 0) {
//                copy[i].setDamageValue(fuel.modify);
//                out[i].setDamageValue(0);
//            } else {
//                copy[i].setDamageValue(0);
//                out[i].setDamageValue(fuel.modify);
//            }
//        }
//        ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(Arrays.asList(copy), Arrays.asList(fuel.repair.getItems())));
//        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(Arrays.asList(out)));
//    }

}
