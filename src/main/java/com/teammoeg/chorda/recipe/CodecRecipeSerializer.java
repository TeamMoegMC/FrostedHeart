package com.teammoeg.chorda.recipe;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.util.io.CodecUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public record CodecRecipeSerializer<T>(Codec<T> codec,RecipeType<DataContainerRecipe<T>> type) implements RecipeSerializer<DataContainerRecipe<T>>{


	@Override
	public DataContainerRecipe<T> fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
		return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.strictDecodeOrThrow(codec.decode(JsonOps.INSTANCE, pSerializedRecipe)));
	}
	@Override
	public @Nullable DataContainerRecipe<T> fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
		return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.readCodec(pBuffer, codec));
	}
	@Override
	public void toNetwork(FriendlyByteBuf pBuffer, DataContainerRecipe<T> pRecipe) {
		CodecUtil.writeCodec(pBuffer, codec, pRecipe.data);
		
	}

}
