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

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.util.SerializeUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class IncubateRecipeSerializer extends IERecipeSerializer<IncubateRecipe> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHMultiblocks.generator);
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
        if (json.has("seed"))
            seed = IngredientWithSize.deserialize(json.get("seed"));
        float seed_conserve = 0;
        if (json.has("seed_cost"))
            seed_conserve = json.get("seed_cost").getAsFloat();
        int water = 0;
        if (json.has("water"))
            water = json.get("water").getAsInt();
        int time = 100;
        if (json.has("time"))
            time = json.get("time").getAsInt();
        return new IncubateRecipe(recipeId, input, output, output_fluid, seed, seed_conserve, water, time);
    }

    @Nullable
    @Override
    public IncubateRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        //return new IncubateRecipe(recipeId, IngredientWithSize.read(buffer), buffer.readItemStack(), buffer.readFluidStack(), SerializeUtil.readOptional(buffer, IngredientWithSize::read).orElse(null), buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt());
    	return null;
    }

    @Override
    public void write(PacketBuffer buffer, IncubateRecipe recipe) {
        /*recipe.input.write(buffer);
        buffer.writeItemStack(recipe.output);
        buffer.writeFluidStack(recipe.output_fluid);
        buffer.writeVarInt(recipe.water);
        buffer.writeVarInt(recipe.time);*/
    }
}
