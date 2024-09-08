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

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.util.RegistryUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InnerExtension implements ICraftingCategoryExtension {
    InstallInnerRecipe inner;

    public InnerExtension(InstallInnerRecipe ri) {
        inner = ri;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, ICraftingGridHelper iCraftingGridHelper, IFocusGroup iFocusGroup) {
        List<ItemStack> armors = new ArrayList<>();
        ArrayList<ItemStack> armorsout = new ArrayList<>();
        RegistryUtils.getItems().stream().map(ItemStack::new).filter(i -> inner.matches(i)).forEach(armors::add);
        armorsout.ensureCapacity(armors.size());
        armors.forEach(e -> {
            ItemStack n = e.copy();
            ItemNBTHelper.putString(n, "inner_cover", inner.getBuffType().toString());
            armorsout.add(n);
        });
        iCraftingGridHelper.createAndSetInputs(iRecipeLayoutBuilder, Arrays.asList(armors, Arrays.asList(inner.getIngredient().getItems())), 1, 2);
        iCraftingGridHelper.createAndSetOutputs(iRecipeLayoutBuilder, armorsout);
    }

    @Override
    public ResourceLocation getRegistryName() {
        return inner.getId();
    }

//    @Override
//    public void setIngredients(IIngredients ingredients) {
//        List<ItemStack> armors = new ArrayList<>();
//        ArrayList<ItemStack> armorsout = new ArrayList<>();
//        RegistryUtils.getItems().stream().map(ItemStack::new).filter(i -> inner.matches(i)).forEach(armors::add);
//        armorsout.ensureCapacity(armors.size());
//        armors.forEach(e -> {
//            ItemStack n = e.copy();
//            ItemNBTHelper.putString(n, "inner_cover", inner.getBuffType().toString());
//            armorsout.add(n);
//        });
//        ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(armors, Arrays.asList(inner.getIngredient().getItems())));
//
//        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(armorsout));
//    }

}
