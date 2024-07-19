/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.recipes;

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class InspireRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<InspireRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.PAPER);
        }

        @Override
        public InspireRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new InspireRecipe(recipeId, Ingredient.read(buffer), buffer.readInt());
        }

        @Override
        public InspireRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
            return new InspireRecipe(arg0, Ingredient.deserialize(arg1.get("item")), arg1.get("amount").getAsInt());
        }

        @Override
        public void write(PacketBuffer buffer, InspireRecipe recipe) {
            recipe.item.write(buffer);
            buffer.writeInt(recipe.inspire);
        }

    }
    public static IRecipeType<InspireRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<InspireRecipe>> SERIALIZER;
    public Ingredient item;

    public int inspire;

    public InspireRecipe(ResourceLocation id, Ingredient item, int inspire) {
        super(ItemStack.EMPTY, TYPE, id);
        this.item = item;
        this.inspire = inspire;
    }

    @Override
    protected IERecipeSerializer<InspireRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
