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
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMultiblocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class GeneratorSteamRecipeSerializer extends IERecipeSerializer<GeneratorSteamRecipe> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHMultiblocks.generator);
    }

    @Override
    public GeneratorSteamRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
        FluidTagInput input = FluidTagInput.deserialize(JSONUtils.getJsonObject(json, "input"));
        float power = JSONUtils.getFloat(json, "energy");
        float tempMod = JSONUtils.getFloat(json, "temp_multiplier");
        float rangeMod = JSONUtils.getFloat(json, "range_multiplier");
        return new GeneratorSteamRecipe(recipeId, input, power, tempMod, rangeMod);
    }

    @Nullable
    @Override
    public GeneratorSteamRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        FluidTagInput input = FluidTagInput.read(buffer);
        float power = buffer.readFloat();
        float tempMod = buffer.readFloat();
        float rangeMod = buffer.readFloat();
        return new GeneratorSteamRecipe(recipeId, input, power, tempMod, rangeMod);
    }

    @Override
    public void write(PacketBuffer buffer, GeneratorSteamRecipe recipe) {
        recipe.input.write(buffer);
        buffer.writeFloat(recipe.power);
        buffer.writeFloat(recipe.tempMod);
        buffer.writeFloat(recipe.rangeMod);
    }
}
