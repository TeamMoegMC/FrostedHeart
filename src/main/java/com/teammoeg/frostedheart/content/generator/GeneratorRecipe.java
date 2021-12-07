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

package com.teammoeg.frostedheart.content.generator;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GeneratorRecipe extends IESerializableRecipe {
    public static IRecipeType<GeneratorRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<GeneratorRecipe>> SERIALIZER;

    public final IngredientWithSize input;
    public final ItemStack output;
    public final int time;

    public GeneratorRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, int time) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
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
    public static Map<ResourceLocation, GeneratorRecipe> recipeList = Collections.emptyMap();

    public static GeneratorRecipe findRecipe(ItemStack input) {
        for (GeneratorRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                return recipe;
        return null;
    }
    public static List<ItemStack> listAll(){
    	ArrayList<ItemStack> all=new ArrayList<>();
    	recipeList.values().stream().map(e->e.input.getMatchingStacks()).forEach(e->{for(ItemStack i:e)if(!all.contains(i))all.add(i);});
    	return all;
    }
    public static List<ItemStack> listOut(){
    	ArrayList<ItemStack> all=new ArrayList<>();
    	recipeList.values().stream().map(e->e.output).forEach(i->{if(!all.contains(i))all.add(i);});
    	return all;
    }
}
