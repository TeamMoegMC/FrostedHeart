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
