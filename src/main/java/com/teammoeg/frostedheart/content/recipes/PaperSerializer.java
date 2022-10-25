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

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class PaperSerializer extends IERecipeSerializer<PaperRecipe> {

    public PaperSerializer() {
    }

    @Override
    public PaperRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        return new PaperRecipe(recipeId, Ingredient.read(buffer), buffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer buffer, PaperRecipe recipe) {
        recipe.paper.write(buffer);
        buffer.writeVarInt(recipe.maxlevel);
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.PAPER);
    }

    @Override
    public PaperRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
        return new PaperRecipe(arg0, Ingredient.deserialize(arg1.get("item")), arg1.get("level").getAsInt());
    }

}
