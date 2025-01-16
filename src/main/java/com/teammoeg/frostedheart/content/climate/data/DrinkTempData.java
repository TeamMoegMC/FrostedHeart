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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.io.CodecUtil;

public class DrinkTempData {
	public static final MapCodec<DrinkTempData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("heat").forGetter(o->o.heat)).apply(t, DrinkTempData::new));
	float heat;
	public DrinkTempData(float heat) {
		super();
		this.heat = heat;
	}

    public float getHeat() {
        return heat;
    }
}
