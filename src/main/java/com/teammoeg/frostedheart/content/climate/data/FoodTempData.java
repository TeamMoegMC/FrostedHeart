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

package com.teammoeg.frostedheart.content.climate.data;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record FoodTempData(Item item,float heat, float min, float max) implements ITempAdjustFood {


	public static final Codec<FoodTempData> CODEC=RecordCodecBuilder.create(t->t.group(
		ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(o->o.item),
		Codec.FLOAT.optionalFieldOf("heat",0f).forGetter(o->o.heat),
		Codec.FLOAT.optionalFieldOf("min",-15f).forGetter(o->o.min),
		Codec.FLOAT.optionalFieldOf("max",15f).forGetter(o->o.max)).apply(t, FoodTempData::new));
	public static RegistryObject<CodecRecipeSerializer<FoodTempData>> TYPE;
	public static Map<Item,FoodTempData> cacheList=ImmutableMap.of();
	/**
	 * Get the temperature adjuster for the food item.
	 * If stack is a cup, return the cup's temperature adjuster.
	 * If stack has food temp data, return the food's temperature adjuster.
	 * Otherwise, return null.
	 *
	 * @param stack the item stack
	 * @return the temperature adjuster
	 */
	public static @Nullable ITempAdjustFood getTempAdjustFood(ItemStack stack) {
		return getTempAdjustFood(stack.getItem());
	}

	public static @Nullable ITempAdjustFood getTempAdjustFood(Item item) {
		if (item instanceof ITempAdjustFood) {
			return (ITempAdjustFood) item;
		}
		CupData data = CupData.cacheList.get(item);
		if (data != null) {
			return new CupTempAdjustProxy(data.getEfficiency(), cacheList.get(item));
		}
		return cacheList.get(item);
	}
    @Override
    public float getHeat(ItemStack is, float env) {
        return heat;
    }

    // rounded heat to 1 decimal place
    public float getHeatRounded() {
        return Math.round(heat * 10) / 10.0f;
    }

    @Override
    public float getMaxTemp(ItemStack is) {
        return max;
    }

    @Override
    public float getMinTemp(ItemStack is) {
        return min;
    }
}
