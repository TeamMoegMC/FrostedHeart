/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public record DataContainerFinishedRecipe<T>(ResourceLocation pId, CodecRecipeSerializer<T> serializer, T data) implements FinishedRecipe {

	@Override
	public void serializeRecipeData(JsonObject pJson) {
		JsonObject jo=serializer.codec().encode(data, JsonOps.INSTANCE, pJson).getOrThrow(false, s->{}).getAsJsonObject();
		pJson.asMap().putAll(jo.asMap());
		
		
	}

	@Override
	public ResourceLocation getId() {
		return pId;
	}

	@Override
	public RecipeSerializer<?> getType() {
		return serializer;
	}

	@Override
	public JsonObject serializeAdvancement() {
		return null;
	}

	@Override
	public ResourceLocation getAdvancementId() {
		return null;
	}

}
