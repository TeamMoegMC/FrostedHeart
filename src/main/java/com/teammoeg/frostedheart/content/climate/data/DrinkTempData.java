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
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record DrinkTempData(Fluid fluid,float heat) {
	public static final Codec<DrinkTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		ForgeRegistries.FLUIDS.getCodec().fieldOf("fluid").forGetter(o->o.fluid),
		Codec.FLOAT.optionalFieldOf("heat",0f).forGetter(o->o.heat)).apply(t, DrinkTempData::new));
	public static RegistryObject<CodecRecipeSerializer<DrinkTempData>> TYPE;
	public static Map<Fluid,DrinkTempData> cacheList=ImmutableMap.of();
    public float getHeat() {
        return heat;
    }
	public static float getDrinkHeat(FluidStack f) {
		DrinkTempData dtd = cacheList.get(f.getFluid());
		if (dtd != null)
			return dtd.getHeat();
		return -0.3f;
	}
}
