package com.teammoeg.chorda.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public record DataContainerFinishedRecipe<T>(ResourceLocation pId, CodecRecipeSerializer<T> serializer, T data) implements FinishedRecipe {

	@Override
	public void serializeRecipeData(JsonObject pJson) {
		serializer.codec().encode(data, JsonOps.INSTANCE, pJson);
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
