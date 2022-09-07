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

package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.data.JsonHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class DamageModifySerializer extends IERecipeSerializer<RecipeModifyDamage> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    @Override
    public RecipeModifyDamage readFromJson(ResourceLocation recipeId, JsonObject json) {
        Ingredient input = Ingredient.deserialize(json.get("tool"));
        Ingredient input2 = Ingredient.deserialize(json.get("item"));
        int dura = JsonHelper.getIntOrDefault(json, "modify", 1);
        return new RecipeModifyDamage(recipeId, input,input2, dura);
    }

    @Nullable
    @Override
    public RecipeModifyDamage read(ResourceLocation recipeId, PacketBuffer buffer) {
        Ingredient input = Ingredient.read(buffer);
        Ingredient input2 = Ingredient.read(buffer);
        int dura = buffer.readVarInt();
        return new RecipeModifyDamage(recipeId, input,input2, dura);
    }

    @Override
    public void write(PacketBuffer buffer, RecipeModifyDamage recipe) {
        recipe.tool.write(buffer);
        recipe.repair.write(buffer);
        
        buffer.writeVarInt(recipe.modify);
    }
}
