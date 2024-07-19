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

public class ResearchPaperRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<ResearchPaperRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.PAPER);
        }

        @Override
        public ResearchPaperRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new ResearchPaperRecipe(recipeId, Ingredient.read(buffer), buffer.readVarInt());
        }

        @Override
        public ResearchPaperRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
            return new ResearchPaperRecipe(arg0, Ingredient.deserialize(arg1.get("item")), arg1.get("level").getAsInt());
        }

        @Override
        public void write(PacketBuffer buffer, ResearchPaperRecipe recipe) {
            recipe.paper.write(buffer);
            buffer.writeVarInt(recipe.maxlevel);
        }

    }
    public static IRecipeType<ResearchPaperRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<ResearchPaperRecipe>> SERIALIZER;
    public Ingredient paper;

    public int maxlevel;

    public ResearchPaperRecipe(ResourceLocation id, Ingredient paper, int maxlevel) {
        super(ItemStack.EMPTY, TYPE, id);
        this.paper = paper;
        this.maxlevel = maxlevel;
    }

    @Override
    protected IERecipeSerializer<ResearchPaperRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}
