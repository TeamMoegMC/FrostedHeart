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

public class BlockTempData{
	float temperature;
	boolean level;
	boolean lit;
	public static final MapCodec<BlockTempData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("temperature").forGetter(o->o.temperature),
		CodecUtil.defaultValue(Codec.BOOL,false).fieldOf("level_divide").forGetter(o->o.level),
		CodecUtil.defaultValue(Codec.BOOL,false).fieldOf("must_lit").forGetter(o->o.lit)).apply(t, BlockTempData::new));
    

    public BlockTempData(float temperature, boolean level, boolean lit) {
		super();
		this.temperature = temperature;
		this.level = level;
		this.lit = lit;
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
}
