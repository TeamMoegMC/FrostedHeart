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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import lombok.Getter;

public class EmptyClimateEvent implements ClimateEvent {
    public static final MapCodec<EmptyClimateEvent> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
    	Codec.LONG .fieldOf("startTime").forGetter(o->o.startTime),
    	Codec.LONG.fieldOf("calmEndTime").forGetter(o->o.calmEndTime)
    	).apply(t, EmptyClimateEvent::new));
    @Getter
    private long startTime;
    @Getter
    private long calmEndTime;

	@Override
	public ClimateResult getHourClimate(long t) {
		return ClimateResult.EMPTY;
	}

	@Override
	public float getHourTemp(long t) {
		return 0;
	}

	public EmptyClimateEvent(long startTime, long calmEndTime) {
		super();
		this.startTime = startTime;
		this.calmEndTime = calmEndTime;
	}

}
