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

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class GeneratorSteamRecipe extends IESerializableRecipe {
    public static IRecipeType<GeneratorSteamRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<GeneratorSteamRecipe>> SERIALIZER;

    public GeneratorSteamRecipe(ResourceLocation id, FluidTagInput input,
                                float power, float tempMod, float rangeMod) {
        super(ItemStack.EMPTY, TYPE, id);
        this.input = input;
        this.power = power;
        this.tempMod = tempMod;
        this.rangeMod = rangeMod;
    }

    public final FluidTagInput input;
    public final float power;
    public final float tempMod;
    public final float rangeMod;


    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

    // Initialized by reload listener
    public static Map<ResourceLocation, GeneratorSteamRecipe> recipeList = Collections.emptyMap();

    public static GeneratorSteamRecipe findRecipe(FluidStack input) {
        for (GeneratorSteamRecipe recipe : recipeList.values())
            if (recipe.input.testIgnoringAmount(input))
                return recipe;
        return null;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return super.outputDummy;
    }
}
