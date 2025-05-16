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
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public record PlantTempData(Block block, float growTimeDays, float minFertilize, float minGrow, float minSurvive,
							float maxFertilize, float maxGrow, float maxSurvive,
							boolean snowVulnerable, boolean blizzardVulnerable, Block dead, boolean willDie,
							int heatCapacity, int minSkylight, int maxSkylight) implements PlantTemperature{
	public static final Codec<PlantTempData> CODEC=RecordCodecBuilder.create(t->t.group(
			ForgeRegistries.BLOCKS.getCodec().fieldOf("block").forGetter(o->o.block),
			// grow_time_days: unit is the expected game days to grow a crop / tree
			Codec.FLOAT.optionalFieldOf("grow_time_days",PlantTemperature.DEFAULT_GROW_TIME_GAME_DAYS).forGetter(o->o.growTimeDays),
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
			Codec.BOOL.optionalFieldOf("blizzard_vulnerable",PlantTemperature.DEFAULT_BLIZZARD_VULNERABLE).forGetter(o->o.blizzardVulnerable),
			// turns to what when dead
			ForgeRegistries.BLOCKS.getCodec().fieldOf("dead").forGetter(o->o.dead),
			Codec.BOOL.optionalFieldOf("will_die", true).forGetter(o->o.willDie),
			Codec.INT.optionalFieldOf("heat_capacity",1).forGetter(o->o.heatCapacity),
			Codec.INT.optionalFieldOf("min_skylight",1).forGetter(o->o.minSkylight),
			Codec.INT.optionalFieldOf("max_skylight",15).forGetter(o->o.maxSkylight)
			).apply(t, PlantTempData::new));
	public static RegistryObject<CodecRecipeSerializer<PlantTempData>> TYPE;
	public static Map<Block,PlantTempData> cacheList=ImmutableMap.of();

	@Nullable
	public static PlantTempData getPlantData(Block block) {
		return cacheList.get(block);
	}
	// default constructor
	public PlantTempData(Block blk) {
		this(blk, PlantTemperature.DEFAULT_GROW_TIME_GAME_DAYS, PlantTemperature.DEFAULT_BONEMEAL_TEMP, PlantTemperature.DEFAULT_GROW_TEMP, PlantTemperature.DEFAULT_SURVIVE_TEMP,
				PlantTemperature.DEFAULT_BONEMEAL_MAX_TEMP, PlantTemperature.DEFAULT_GROW_MAX_TEMP, PlantTemperature.DEFAULT_SURVIVE_MAX_TEMP,
				PlantTemperature.DEFAULT_SNOW_VULNERABLE, PlantTemperature.DEFAULT_BLIZZARD_VULNERABLE, Blocks.DEAD_BUSH, true, 1, 1, 15);
	}
    public FinishedRecipe toFinished(ResourceLocation name) {
    	return TYPE.get().toFinished(name, this);
    }
}
