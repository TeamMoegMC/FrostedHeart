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

package com.teammoeg.frostedheart.content.steamenergy.charger;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHBlocks;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class ChargerRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<ChargerRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.charger.get());
        }

        @Nullable
        @Override
        public ChargerRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            ItemStack output = buffer.readItemStack();
            IngredientWithSize input = IngredientWithSize.read(buffer);
            float cost = buffer.readFloat();
            return new ChargerRecipe(recipeId, output, input, cost);
        }

        @Override
        public ChargerRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack output = readOutput(json.get("result"));
            IngredientWithSize input = IngredientWithSize.deserialize(json.get("input"));
            float cost = JSONUtils.getInt(json, "cost");
            return new ChargerRecipe(recipeId, output, input, cost);
        }

        @Override
        public void write(PacketBuffer buffer, ChargerRecipe recipe) {
            buffer.writeItemStack(recipe.output);
            recipe.input.write(buffer);
            buffer.writeFloat(recipe.cost);
        }
    }
    public static IRecipeType<ChargerRecipe> TYPE;

    public static RegistryObject<IERecipeSerializer<ChargerRecipe>> SERIALIZER;
    public final IngredientWithSize input;

    public final ItemStack output;

    public final float cost;


    public ChargerRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, float cost2) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
        this.cost = cost2;
    }

    @Override
    protected IERecipeSerializer<ChargerRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }

}
