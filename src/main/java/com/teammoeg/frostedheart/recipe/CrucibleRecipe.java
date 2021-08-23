/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class CrucibleRecipe extends IESerializableRecipe {
    public static IRecipeType<CrucibleRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<CrucibleRecipe>> SERIALIZER;

    public final IngredientWithSize input;
    public final IngredientWithSize input2;
    public final ItemStack output;
    public final int time;

    public CrucibleRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, IngredientWithSize input2, int time) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
        this.input2 = input;
        this.time = time;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }

    // Initialized by reload listener
    public static Map<ResourceLocation, CrucibleRecipe> recipeList = Collections.emptyMap();

    public static CrucibleRecipe findRecipe(ItemStack input) {
        for (CrucibleRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input)) {
                return recipe;
            } else if (ItemUtils.stackMatchesObject(input, recipe.input2)) {
                return recipe;
            }
        return null;
    }

    public static CrucibleRecipe Recipe(ItemStack input, ItemStack input2) {
        for (CrucibleRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                if (ItemUtils.stackMatchesObject(input2, recipe.input2))
                    return recipe;
        return null;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();
        nonnulllist.add(this.input.getBaseIngredient());
        nonnulllist.add(this.input2.getBaseIngredient());
        return nonnulllist;
    }
}
