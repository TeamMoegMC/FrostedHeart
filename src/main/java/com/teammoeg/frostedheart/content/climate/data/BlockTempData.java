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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record BlockTempData(Block block,float temperature, boolean level, boolean lit){
	public static final Codec<BlockTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		ForgeRegistries.BLOCKS.getCodec().fieldOf("block").forGetter(o->o.block),
		Codec.FLOAT.optionalFieldOf("temperature",0f).forGetter(o->o.temperature),
		Codec.BOOL.optionalFieldOf("level_divide",false).forGetter(o->o.level),
		Codec.BOOL.optionalFieldOf("must_lit",false).forGetter(o->o.lit)).apply(t, BlockTempData::new));
	public static RegistryObject<CodecRecipeSerializer<BlockTempData>> TYPE;
	private static Map<Block,BlockTempData> CACHE =ImmutableMap.of();

    public static BlockTempData getData(Level level, Block block) {
        return CACHE.get(block);
    }

    public static void updateCache(RecipeManager manager) {
        Collection<Recipe<?>> recipes = manager.getRecipes();
        BlockTempData.CACHE = BlockTempData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().block(), t->t.getData()));
    }

    public float getTemp() {
        return temperature;
    }

    public boolean isLevel() {
        return level;
    }

    public boolean isLit() {
        return lit;
    }
    public FinishedRecipe toFinished(ResourceLocation name) {
    	return TYPE.get().toFinished(name, this);
    }
}
