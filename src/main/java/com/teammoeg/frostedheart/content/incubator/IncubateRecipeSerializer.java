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

package com.teammoeg.frostedheart.content.incubator;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.util.SerializeUtil;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class IncubateRecipeSerializer extends IERecipeSerializer<IncubateRecipe> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHBlocks.incubator1);
    }

    @Override
    public IncubateRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
        IngredientWithSize input = IngredientWithSize.deserialize(JSONUtils.getJsonObject(json, "input"));
        ItemStack output = ItemStack.EMPTY;
        if (json.has("output"))
            output = readOutput(json.get("output"));
        FluidStack output_fluid = FluidStack.EMPTY;
        if (json.has("fluid"))
            output_fluid = ApiUtils.jsonDeserializeFluidStack(json.get("fluid").getAsJsonObject());
        IngredientWithSize seed = null;
        if (json.has("catalyst"))
            seed = IngredientWithSize.deserialize(json.get("catalyst"));
        boolean use_catalyst=false;
        if (json.has("consume_catalyst"))
        	use_catalyst = json.get("consume_catalyst").getAsBoolean();
        int water = 0;
        if (json.has("water"))
            water = json.get("water").getAsInt();
        int time = 100;
        if (json.has("time"))
            time = json.get("time").getAsInt();

        return new IncubateRecipe(recipeId, input,seed, output, output_fluid,use_catalyst, water, time);
    }

    @Nullable
    @Override
    public IncubateRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        return new IncubateRecipe(recipeId, IngredientWithSize.read(buffer),SerializeUtil.readOptional(buffer, IngredientWithSize::read).orElse(null), buffer.readItemStack(), buffer.readFluidStack(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer buffer, IncubateRecipe recipe) {
        recipe.input.write(buffer);
        SerializeUtil.writeOptional(buffer,recipe.catalyst,IngredientWithSize::write);

        buffer.writeItemStack(recipe.output);
        buffer.writeFluidStack(recipe.output_fluid);
        buffer.writeBoolean(recipe.consume_catalyst);
        buffer.writeVarInt(recipe.water);
        buffer.writeVarInt(recipe.time);
    }
}
