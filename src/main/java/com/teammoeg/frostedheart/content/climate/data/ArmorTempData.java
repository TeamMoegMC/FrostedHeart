/*
 * Copyright (c) 2021-2024 TeamMoeg
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
import com.teammoeg.chorda.util.io.CodecUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public record ArmorTempData(Item item,float insulation, float heat_proof, float wind_proof){
	public static final Codec<ArmorTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		CodecUtil.registryCodec(()->BuiltInRegistries.ITEM).fieldOf("item").forGetter(o->o.item),
		Codec.FLOAT.optionalFieldOf("factor",0f).forGetter(o->o.insulation),
		Codec.FLOAT.optionalFieldOf("heat_proof",0f).forGetter(o->o.heat_proof),
		Codec.FLOAT.optionalFieldOf("wind_proof",0f).forGetter(o->o.wind_proof)).apply(t, ArmorTempData::new));
	public static RegistryObject<CodecRecipeSerializer<ArmorTempData>> TYPE;
	public static Map<Item,ArmorTempData> cacheList=ImmutableMap.of();
	public float getInsulation() {
    	return insulation;
    }
    public float getHeatProof() {
    	return heat_proof;
    }
    public float getColdProof() {
    	return wind_proof;
    }

	@Override
	public String toString() {
		return "ArmorTempData [insulation=" + insulation + ", heat_proof=" + heat_proof + ", wind_proof=" + wind_proof + "]";
	}
}

