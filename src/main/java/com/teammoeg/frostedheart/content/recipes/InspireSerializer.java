/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class InspireSerializer extends IERecipeSerializer<InspireRecipe> {

    public InspireSerializer() {
    }

    @Override
    public InspireRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        return new InspireRecipe(recipeId, Ingredient.read(buffer), buffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer buffer, InspireRecipe recipe) {
        recipe.item.write(buffer);
        buffer.writeVarInt(recipe.inspire);
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.PAPER);
    }

    @Override
    public InspireRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
        return new InspireRecipe(arg0, Ingredient.deserialize(arg1.get("item")), arg1.get("amount").getAsInt());
    }

}
