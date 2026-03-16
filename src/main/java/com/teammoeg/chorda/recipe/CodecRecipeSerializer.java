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

/**
 * 基于 Mojang {@link Codec} 的配方序列化器，用于将泛型数据类型 {@code T} 序列化和反序列化为
 * {@link DataContainerRecipe} 实例。支持 JSON 和网络字节缓冲区两种序列化方式。
 * 还提供了配方过滤、数据生成辅助以及与 {@link RecipeReloadListener} 集成的托管功能。
 * <p>
 * A Mojang {@link Codec}-based recipe serializer that serializes and deserializes a generic data type
 * {@code T} into {@link DataContainerRecipe} instances. Supports both JSON and network byte buffer
 * serialization. Also provides recipe filtering, datagen helpers, and managed integration with
 * {@link RecipeReloadListener}.
 *
 * @param <T> 被序列化的数据类型 / The data type being serialized
 * @param codec 用于编解码数据的 Codec / The Codec used to encode/decode the data
 * @param type 关联的配方类型 / The associated recipe type
 * @see DataContainerRecipe
 * @see DataContainerFinishedRecipe
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record CodecRecipeSerializer<T>(Codec<T> codec,RecipeType<DataContainerRecipe<T>> type) implements RecipeSerializer<DataContainerRecipe<T>>{


	/**
	 * 从 JSON 对象反序列化配方。使用 Codec 严格解码数据，失败时抛出 {@link JsonSyntaxException}。
	 * <p>
	 * Deserializes a recipe from a JSON object. Uses the Codec to strictly decode data,
	 * throwing {@link JsonSyntaxException} on failure.
	 *
	 * @param pRecipeId 配方的资源位置标识符 / The resource location identifier for the recipe
	 * @param pSerializedRecipe 包含序列化配方数据的 JSON 对象 / The JSON object containing serialized recipe data
	 * @return 反序列化后的数据容器配方 / The deserialized data container recipe
	 * @throws JsonSyntaxException 如果 Codec 解码失败 / If the Codec decoding fails
	 */
	@Override
	public DataContainerRecipe<T> fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
		try {
			return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.strictDecodeOrThrow(codec.decode(JsonOps.INSTANCE, pSerializedRecipe)));
		}catch(RuntimeException ex) {
			throw new JsonSyntaxException(ex.getMessage(),ex);
		}
	}

	/**
	 * 从网络字节缓冲区反序列化配方，用于客户端-服务端同步。
	 * <p>
	 * Deserializes a recipe from a network byte buffer, used for client-server synchronization.
	 *
	 * @param pRecipeId 配方的资源位置标识符 / The resource location identifier for the recipe
	 * @param pBuffer 包含序列化数据的网络字节缓冲区 / The network byte buffer containing serialized data
	 * @return 反序列化后的数据容器配方 / The deserialized data container recipe
	 */
	@Override
	public DataContainerRecipe<T> fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
		return new DataContainerRecipe<>(pRecipeId,this,CodecUtil.readCodec(pBuffer, codec));
	}

	/**
	 * 将配方数据序列化到网络字节缓冲区，用于客户端-服务端同步。
	 * <p>
	 * Serializes recipe data to a network byte buffer, used for client-server synchronization.
	 *
	 * @param pBuffer 要写入的网络字节缓冲区 / The network byte buffer to write to
	 * @param pRecipe 要序列化的数据容器配方 / The data container recipe to serialize
	 */
	@Override
	public void toNetwork(FriendlyByteBuf pBuffer, DataContainerRecipe<T> pRecipe) {
		CodecUtil.writeCodec(pBuffer, codec, pRecipe.data);

	}

	/**
	 * 将此序列化器注册为托管序列化器，使其在资源重载时自动接收通知。
	 * <p>
	 * Registers this serializer as a managed serializer so it automatically receives
	 * notifications on resource reloads.
	 *
	 * @return 此序列化器实例（用于链式调用）/ This serializer instance (for method chaining)
	 */
	public CodecRecipeSerializer<T> setManaged(){
		RecipeReloadListener.registeredSerializer.add(this);
		return this;
	}

	/**
	 * 从给定的配方集合中筛选出属于此序列化器类型的所有数据容器配方，并返回列表。
	 * <p>
	 * Filters all data container recipes belonging to this serializer's type from the given recipe
	 * collection and returns them as a list.
	 *
	 * @param recipes 要筛选的配方集合 / The collection of recipes to filter
	 * @return 匹配此类型的数据容器配方列表 / A list of data container recipes matching this type
	 */
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

	/**
	 * 创建用于数据生成的已完成配方表示。
	 * <p>
	 * Creates a finished recipe representation for data generation.
	 *
	 * @param rl 配方的资源位置标识符 / The resource location identifier for the recipe
	 * @param data 要序列化的数据对象 / The data object to be serialized
	 * @return 已完成的配方实例 / The finished recipe instance
	 */
	public FinishedRecipe toFinished(ResourceLocation rl,T data) {
		return new DataContainerFinishedRecipe<>(rl,this,data);

	}

	/**
	 * 从给定的配方集合中筛选出属于此序列化器类型的所有数据容器配方，并返回流。
	 * <p>
	 * Filters all data container recipes belonging to this serializer's type from the given recipe
	 * collection and returns them as a stream.
	 *
	 * @param recipes 要筛选的配方集合 / The collection of recipes to filter
	 * @return 匹配此类型的数据容器配方流 / A stream of data container recipes matching this type
	 */
	@SuppressWarnings("unchecked")
    public Stream<DataContainerRecipe<T>> filterRecipes(Collection<Recipe<?>> recipes){
		return recipes.stream().filter(r->r instanceof DataContainerRecipe&&r.getType()==type).map(t->(DataContainerRecipe<T>)t);
	}
}
