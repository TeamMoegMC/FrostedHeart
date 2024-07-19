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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class InnerExtension implements ICraftingCategoryExtension {
    InstallInnerRecipe inner;

    public InnerExtension(InstallInnerRecipe ri) {
        inner = ri;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return inner.getId();
    }

    @Override
    public void setIngredients(IIngredients ingredients) {
        List<ItemStack> armors = new ArrayList<>();
        ArrayList<ItemStack> armorsout = new ArrayList<>();
        RegistryUtils.getItems().stream().map(ItemStack::new).filter(i -> inner.matches(i)).forEach(armors::add);
        armorsout.ensureCapacity(armors.size());
        armors.forEach(e -> {
            ItemStack n = e.copy();
            ItemNBTHelper.putString(n, "inner_cover", inner.getBuffType().toString());
            armorsout.add(n);
        });
        ingredients.setInputLists(VanillaTypes.ITEM, Arrays.asList(armors, Arrays.asList(inner.getIngredient().getMatchingStacks())));

        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(armorsout));
    }

}
