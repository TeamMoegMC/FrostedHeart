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

package com.teammoeg.frostedheart.content.climate.data;

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import com.teammoeg.chorda.util.CRegistryHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.RegistryObject;

public record BiomeTempData(ResourceLocation biome,float temperature) {
	public static final Codec<BiomeTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		ResourceLocation.CODEC.fieldOf("biome").forGetter(o->o.biome),
		Codec.FLOAT.fieldOf("temperature").forGetter(o->o.temperature)
		).apply(t,BiomeTempData::new));
	public static RegistryObject<CodecRecipeSerializer<BiomeTempData>> TYPE;
	public static Map<ResourceLocation,BiomeTempData> cacheList=ImmutableMap.of();

	@Nonnull
	public static Float getBiomeTemp(Biome b) {
		if (b == null) return 0f;
		BiomeTempData data = cacheList.get(CRegistryHelper.getRegistryName(b));
		if (data != null)
			return data.temperature();
		return 0F;
	}
    public FinishedRecipe toFinished(ResourceLocation name) {
    	return TYPE.get().toFinished(name, this);
    }
}
