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

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.world.item.ItemStack;

public class FoodTempData implements ITempAdjustFood {


	public static final MapCodec<FoodTempData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("heat").forGetter(o->o.heat),
		CodecUtil.defaultValue(Codec.FLOAT,-15f).fieldOf("min").forGetter(o->o.min),
		CodecUtil.defaultValue(Codec.FLOAT,15f).fieldOf("max").forGetter(o->o.max)).apply(t, FoodTempData::new));

    float heat;
    float min;
    float max;
	public FoodTempData(float heat, float min, float max) {
		super();
		this.heat = heat;
		this.min = min;
		this.max = max;
	}
    @Override
    public float getHeat(ItemStack is, float env) {
        return heat;
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
