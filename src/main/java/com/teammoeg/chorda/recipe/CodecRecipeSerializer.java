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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.MethodsReturnNonnullByDefault;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.io.CodecUtil;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record CodecRecipeSerializer<T>(Codec<T> codec,RecipeType<DataContainerRecipe<T>> type) implements RecipeSerializer<DataContainerRecipe<T>>{


	@Override
	public DataContainerRecipe<T> fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
		try {
			return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.strictDecodeOrThrow(codec.decode(JsonOps.INSTANCE, pSerializedRecipe)));
		}catch(RuntimeException ex) {
			throw new JsonSyntaxException(ex.getMessage(),ex);
		}
	}
	@Override
	public DataContainerRecipe<T> fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
		return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.readCodec(pBuffer, codec));
	}
	@Override
	public void toNetwork(FriendlyByteBuf pBuffer, DataContainerRecipe<T> pRecipe) {
		CodecUtil.writeCodec(pBuffer, codec, pRecipe.data);
		
	}
	public CodecRecipeSerializer<T> setManaged(){
		RecipeReloadListener.registeredSerializer.add(this);
		return this;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
    public List<DataContainerRecipe<T>> getRecipes(Collection<Recipe<?>> recipes){
		List<DataContainerRecipe<T>> recipeList=new ArrayList<>();
		for(Recipe r:recipes) {
			if(r instanceof DataContainerRecipe dcr&&r.getType()==type) {
				recipeList.add(dcr);
			}
		}
		return recipeList;
	}
	public FinishedRecipe toFinished(ResourceLocation rl,T data) {
		return new DataContainerFinishedRecipe<>(rl,this,data);
		
	}
	
	@SuppressWarnings("unchecked")
    public Stream<DataContainerRecipe<T>> filterRecipes(Collection<Recipe<?>> recipes){
		return recipes.stream().filter(r->r instanceof DataContainerRecipe&&r.getType()==type).map(t->(DataContainerRecipe<T>)t);
	}
}
