/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.recipe;

import com.google.gson.JsonObject;
import com.teammoeg.chorda.recipe.DataRecipe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;

public class ResearchPaperRecipe extends DataRecipe {
    public static RegistryObject<RecipeType<ResearchPaperRecipe>> TYPE;
    public static RegistryObject<RecipeSerializer<ResearchPaperRecipe>> SERIALIZER;
    public Ingredient paper;
    public int maxlevel;

    public ResearchPaperRecipe(ResourceLocation id, Ingredient paper, int maxlevel) {
        super(id);
        this.paper = paper;
        this.maxlevel = maxlevel;
    }

    @Override
	public RecipeSerializer<ResearchPaperRecipe> getSerializer() {
        return SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<ResearchPaperRecipe> {


        @Override
        public ResearchPaperRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new ResearchPaperRecipe(recipeId, Ingredient.fromNetwork(buffer), buffer.readVarInt());
        }

        @Override
        public ResearchPaperRecipe fromJson(ResourceLocation arg0, JsonObject arg1) {
            return new ResearchPaperRecipe(arg0, Ingredient.fromJson(arg1.get("item")), arg1.get("level").getAsInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ResearchPaperRecipe recipe) {
            recipe.paper.toNetwork(buffer);
            buffer.writeVarInt(recipe.maxlevel);
        }

    }

	@Override
	public RecipeType<?> getType() {
		return TYPE.get();
	}
}
