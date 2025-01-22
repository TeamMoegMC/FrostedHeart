package com.teammoeg.chorda.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public class DataContainerRecipe<T> extends DataRecipe {
	CodecRecipeSerializer<T> serializer;
	T data;

	public DataContainerRecipe(ResourceLocation pId, CodecRecipeSerializer<T> serializer, T data) {
		super(pId);
		this.serializer = serializer;
		this.data = data;
	}

	@Override
	public RecipeType<?> getType() {
		return serializer.type();
	}

	@Override
	public CodecRecipeSerializer<T> getSerializer() {
		return serializer;
	}

}
