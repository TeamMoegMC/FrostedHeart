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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMultiblocks;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

public class GeneratorSteamRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<GeneratorSteamRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHMultiblocks.generator);
        }

        @Nullable
        @Override
        public GeneratorSteamRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            FluidTagInput input = FluidTagInput.read(buffer);
            float power = buffer.readFloat();
            float tempMod = buffer.readFloat();
            return new GeneratorSteamRecipe(recipeId, input, power, tempMod);
        }

        @Override
        public GeneratorSteamRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            FluidTagInput input = FluidTagInput.deserialize(JSONUtils.getJsonObject(json, "input"));
            float power = JSONUtils.getFloat(json, "energy");
            float tempMod = JSONUtils.getFloat(json, "level");
            return new GeneratorSteamRecipe(recipeId, input, power, tempMod);
        }

        @Override
        public void write(PacketBuffer buffer, GeneratorSteamRecipe recipe) {
            recipe.input.write(buffer);
            buffer.writeFloat(recipe.power);
            buffer.writeFloat(recipe.level);
        }
    }
    public static IRecipeType<GeneratorSteamRecipe> TYPE;

    public static RegistryObject<IERecipeSerializer<GeneratorSteamRecipe>> SERIALIZER;

    // Initialized by reload listener
    public static Map<ResourceLocation, GeneratorSteamRecipe> recipeList = Collections.emptyMap();
    public final FluidTagInput input;
    public final float power;


    public final float level;

    public static GeneratorSteamRecipe findRecipe(FluidStack input) {
        for (GeneratorSteamRecipe recipe : recipeList.values())
            if (recipe.input.testIgnoringAmount(input))
                return recipe;
        return null;
    }

    public GeneratorSteamRecipe(ResourceLocation id, FluidTagInput input,
                                float power, float tempMod) {
        super(ItemStack.EMPTY, TYPE, id);
        this.input = input;
        this.power = power;
        this.level = tempMod;
    }

    @Override
    protected IERecipeSerializer<GeneratorSteamRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return super.outputDummy;
    }
}
