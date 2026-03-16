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

/**
 * 数据容器配方的"已完成"表示形式，用于数据生成（datagen）阶段。
 * 实现 {@link FinishedRecipe} 接口，通过 {@link CodecRecipeSerializer} 的 Codec 将数据编码为 JSON 格式，
 * 以便写入配方 JSON 文件。此记录不包含进度（advancement）信息。
 * <p>
 * The "finished" representation of a data container recipe, used during data generation (datagen).
 * Implements {@link FinishedRecipe} and encodes data to JSON format using the {@link CodecRecipeSerializer}'s
 * Codec for writing to recipe JSON files. This record does not include advancement information.
 *
 * @param <T> 此配方承载的数据类型 / The type of data this recipe carries
 * @param pId 配方的资源位置标识符 / The resource location identifier for this recipe
 * @param serializer 用于编解码数据的序列化器 / The serializer used to encode/decode data
 * @param data 要序列化的数据对象 / The data object to be serialized
 * @see CodecRecipeSerializer#toFinished(ResourceLocation, Object)
 */
public record DataContainerFinishedRecipe<T>(ResourceLocation pId, CodecRecipeSerializer<T> serializer, T data) implements FinishedRecipe {

	/**
	 * 使用 Codec 将数据编码为 JSON 并合并到目标 JSON 对象中。
	 * <p>
	 * Encodes data to JSON using the Codec and merges it into the target JSON object.
	 *
	 * @param pJson 要写入配方数据的目标 JSON 对象 / The target JSON object to write recipe data into
	 */
	@Override
	public void serializeRecipeData(JsonObject pJson) {
		JsonObject jo=serializer.codec().encode(data, JsonOps.INSTANCE, pJson).getOrThrow(false, s->{}).getAsJsonObject();
		pJson.asMap().putAll(jo.asMap());


	}

	/**
	 * 获取此配方的资源位置标识符。
	 * <p>
	 * Gets the resource location identifier for this recipe.
	 *
	 * @return 配方 ID / The recipe ID
	 */
	@Override
	public ResourceLocation getId() {
		return pId;
	}

	/**
	 * 获取此配方的序列化器。
	 * <p>
	 * Gets the serializer for this recipe.
	 *
	 * @return 配方序列化器 / The recipe serializer
	 */
	@Override
	public RecipeSerializer<?> getType() {
		return serializer;
	}

	/**
	 * 返回 null，因为数据配方不包含进度信息。
	 * <p>
	 * Returns null since data recipes do not include advancement information.
	 *
	 * @return 始终返回 null / Always returns null
	 */
	@Override
	public JsonObject serializeAdvancement() {
		return null;
	}

	/**
	 * 返回 null，因为数据配方不包含进度 ID。
	 * <p>
	 * Returns null since data recipes do not include an advancement ID.
	 *
	 * @return 始终返回 null / Always returns null
	 */
	@Override
	public ResourceLocation getAdvancementId() {
		return null;
	}

}
