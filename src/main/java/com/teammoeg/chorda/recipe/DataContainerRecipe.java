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

package com.teammoeg.chorda.recipe;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public class DataContainerRecipe<T> extends DataRecipe {
	CodecRecipeSerializer<T> serializer;
	@Getter
	@Setter
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
