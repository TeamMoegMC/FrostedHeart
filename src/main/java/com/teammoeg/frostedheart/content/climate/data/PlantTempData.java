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

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record PlantTempData(Block block,float minFertilize, float minGrow, float minSurvive,
	 float maxFertilize, float maxGrow, float maxSurvive,
	 boolean snowVulnerable, boolean blizzardVulnerable) implements PlantTemperature{
	public static final Codec<PlantTempData> CODEC=RecordCodecBuilder.create(t->t.group(
			ForgeRegistries.BLOCKS.getCodec().fieldOf("block").forGetter(o->o.block),
			// min
			Codec.FLOAT.optionalFieldOf("min_fertilize",PlantTemperature.DEFAULT_BONEMEAL_TEMP).forGetter(o->o.minFertilize),
			Codec.FLOAT.optionalFieldOf("min_grow",PlantTemperature.DEFAULT_GROW_TEMP).forGetter(o->o.minGrow),
			Codec.FLOAT.optionalFieldOf("min_survive",PlantTemperature.DEFAULT_SURVIVE_TEMP).forGetter(o->o.minSurvive),
			// max
			Codec.FLOAT.optionalFieldOf("max_fertilize",PlantTemperature.DEFAULT_BONEMEAL_MAX_TEMP).forGetter(o->o.maxFertilize),
			Codec.FLOAT.optionalFieldOf("max_grow",PlantTemperature.DEFAULT_GROW_MAX_TEMP).forGetter(o->o.maxGrow),
			Codec.FLOAT.optionalFieldOf("max_survive",PlantTemperature.DEFAULT_SURVIVE_MAX_TEMP).forGetter(o->o.maxSurvive),
			// vulnerability
			Codec.BOOL.optionalFieldOf("snow_vulnerable",PlantTemperature.DEFAULT_SNOW_VULNERABLE).forGetter(o->o.snowVulnerable),
			Codec.BOOL.optionalFieldOf("blizzard_vulnerable",PlantTemperature.DEFAULT_BLIZZARD_VULNERABLE).forGetter(o->o.blizzardVulnerable)
	).apply(t, PlantTempData::new));
	public static RegistryObject<CodecRecipeSerializer<PlantTempData>> TYPE;
	public static Map<Block,PlantTempData> cacheList=ImmutableMap.of();
	
	public static PlantTempData getPlantData(Block block) {
		return cacheList.get(block);
	}
	// default constructor
	public PlantTempData(Block blk) {
		this(blk,PlantTemperature.DEFAULT_BONEMEAL_TEMP, PlantTemperature.DEFAULT_GROW_TEMP, PlantTemperature.DEFAULT_SURVIVE_TEMP,
				PlantTemperature.DEFAULT_BONEMEAL_MAX_TEMP, PlantTemperature.DEFAULT_GROW_MAX_TEMP, PlantTemperature.DEFAULT_SURVIVE_MAX_TEMP,
				PlantTemperature.DEFAULT_SNOW_VULNERABLE, PlantTemperature.DEFAULT_BLIZZARD_VULNERABLE);
	}
    public FinishedRecipe toFinished(ResourceLocation name) {
    	return TYPE.get().toFinished(name, this);
    }
}
