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
import com.teammoeg.frostedheart.util.io.CodecUtil;
import lombok.Getter;

@Getter
public class PlantTempData {
	public static final MapCodec<PlantTempData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
			CodecUtil.defaultValue(Codec.FLOAT,10f).fieldOf("bonemeal").forGetter(o->o.bonemeal),
			CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("grow").forGetter(o->o.grow),
			CodecUtil.defaultValue(Codec.FLOAT,-10f).fieldOf("survive").forGetter(o->o.survive),
			CodecUtil.defaultValue(Codec.BOOL,true).fieldOf("snow_vulnerable").forGetter(o->o.snowVulnerable),
			CodecUtil.defaultValue(Codec.BOOL,true).fieldOf("blizzard_vulnerable").forGetter(o->o.blizzardVulnerable)
	).apply(t, PlantTempData::new));
	// the temperature at which bonemeal can be used on the plant
	private final float bonemeal;
	// above this temperature, the plant will not grow
	private final float grow;
	// above this temperature, the plant will shrink to default state but not die
	// below this temperature, the plant will die
	private final float survive;
	private final boolean snowVulnerable;
	private final boolean blizzardVulnerable;


    public PlantTempData(float bonemeal, float grow, float survive, boolean snowVulnerable, boolean blizzardVulnerable) {
		this.bonemeal = bonemeal;
		this.grow = grow;
		this.survive = survive;
		this.snowVulnerable = snowVulnerable;
		this.blizzardVulnerable = blizzardVulnerable;
	}

}
