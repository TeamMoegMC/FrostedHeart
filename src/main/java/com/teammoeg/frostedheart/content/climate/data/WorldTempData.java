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
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

public record WorldTempData(ResourceLocation world,float temperature){
	public static final Codec<WorldTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		ResourceLocation.CODEC.fieldOf("world").forGetter(o->o.world),
		Codec.FLOAT.optionalFieldOf("temperature",0f).forGetter(o->o.temperature)).apply(t, WorldTempData::new));
	public static RegistryObject<CodecRecipeSerializer<WorldTempData>> TYPE;
	public static Map<ResourceLocation,WorldTempData> cacheList=ImmutableMap.of();
	@Nonnull
	public static Float getWorldTemp(Level w) {
		WorldTempData data = cacheList.get(w.dimension().location());
		if (data != null)
			return data.getTemp();
		return -10F;
	}
	public float getTemp() {
        return temperature;
    }
	   public FinishedRecipe toFinished(ResourceLocation name) {
	    	return TYPE.get().toFinished(name, this);
	    }
}
