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

import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 带有泛型数据容器的数据配方实现。
 * 该类将任意类型的数据对象 {@code T} 包装在 Minecraft 配方系统中，
 * 通过 {@link CodecRecipeSerializer} 实现数据的序列化与反序列化。
 * 数据可通过 Lombok 生成的 getter/setter 访问。
 * <p>
 * A data recipe implementation with a generic data container.
 * Wraps an arbitrary data object of type {@code T} within the Minecraft recipe system,
 * using {@link CodecRecipeSerializer} for serialization and deserialization.
 * Data is accessible via Lombok-generated getter/setter.
 *
 * @param <T> 此配方承载的数据类型 / The type of data this recipe carries
 * @see DataRecipe
 * @see CodecRecipeSerializer
 */
public class DataContainerRecipe<T> extends DataRecipe {
	CodecRecipeSerializer<T> serializer;
	@Getter
	@Setter
	T data;

	/**
	 * 使用指定的资源位置、序列化器和数据对象构造数据容器配方。
	 * <p>
	 * Constructs a data container recipe with the specified resource location, serializer, and data object.
	 *
	 * @param pId 配方的资源位置标识符 / The resource location identifier for this recipe
	 * @param serializer 用于编解码数据的序列化器 / The serializer used to encode/decode data
	 * @param data 此配方承载的数据对象 / The data object carried by this recipe
	 */
	public DataContainerRecipe(ResourceLocation pId, CodecRecipeSerializer<T> serializer, T data) {
		super(pId);
		this.serializer = serializer;
		this.data = data;
	}

	/**
	 * {@inheritDoc}
	 * 返回由序列化器提供的配方类型。
	 * <p>
	 * Returns the recipe type provided by the serializer.
	 */
	@Override
	public RecipeType<?> getType() {
		return serializer.type();
	}

	/**
	 * {@inheritDoc}
	 * 返回此配方使用的 Codec 序列化器。
	 * <p>
	 * Returns the Codec serializer used by this recipe.
	 */
	@Override
	public CodecRecipeSerializer<T> getSerializer() {
		return serializer;
	}

}
