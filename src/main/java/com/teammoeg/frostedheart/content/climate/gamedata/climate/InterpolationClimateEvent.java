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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import lombok.Getter;
import net.minecraft.util.RandomSource;

/**
 * A climate event defined by a set of timestamps and temperature
 * parameters.
 * Allows computation of temperature at any timestamp within this event.
 */
public class InterpolationClimateEvent implements ClimateEvent {
    @Getter
    private long startTime;
    private long peakTime;
    private float peakTemp;
    private long bottomTime;
    private float bottomTemp;
    private long endTime;
    private boolean isCold;
    private boolean isBlizzard;
    @Getter
    private long calmEndTime;

    public static InterpolationClimateEvent getBlizzardClimateEvent(RandomSource random,long startTime) {
        long peakTime = 0, bottomTime = 0, endTime = 0;
        float peakTemp = 0, bottomTemp = 0;
        int add=(int) Math.max(0,10-(startTime/WorldClockSource.secondsPerDay-15)/10);
        switch (random.nextInt(10)+add) {
            case 0:
                bottomTemp += WorldTemperature.COLD_PERIOD_BOTTOM_T8;
                break;
            case 1:
            case 2:
                bottomTemp += WorldTemperature.COLD_PERIOD_BOTTOM_T7;
                break;
            case 3:
            case 4:
            case 5:
                bottomTemp += WorldTemperature.COLD_PERIOD_BOTTOM_T6;
                break;
            default:
                bottomTemp += WorldTemperature.COLD_PERIOD_BOTTOM_T5;
                break;
        }

        long length = 0;
        length = WorldClockSource.secondsPerDay * 2 + random.nextInt((int) (WorldClockSource.secondsPerDay * 5)); // 2 - 7 days length
        endTime = startTime + length;
        long padding = 8 * 50 + random.nextInt(16 * 50);
        peakTime = startTime + padding; // reach peak within 8-24h
        bottomTime = startTime + padding + (length - padding) / 4;
        peakTemp = WorldTemperature.BLIZZARD_WARM_PEAK - (float) Math.abs(random.nextGaussian());
        bottomTemp += (float) (random.nextGaussian());
        long calmLength = WorldClockSource.secondsPerDay * 1 + random.nextInt((int) (WorldClockSource.secondsPerDay * 2)); // 1 - 3 days length
        long calmEndTime = endTime + calmLength;

        return new InterpolationClimateEvent(startTime, peakTime, peakTemp, bottomTime, bottomTemp, endTime, calmEndTime, true, true);
    }

    /**
     * Creates a new TempEvent consisting of a cold or warm period followed by a
     * calm period.
     * This essentially generates a set of parameters that can be used in later
     * computation.
     * <p>
     * Cold period lasts 2-7 days.
     * At beginning, temperature quickly rises to a peak within 8-24 hours.
     * Then, temperature quickly drops to a bottom at around 20% time into the cold
     * period.
     * Bottom temperature has three levels: normal, intense, extreme.
     * The chances for these three levels happening are, respectively: 70%, 20%,
     * 10%.
     * <p>
     * Calm periods lasts 2-7 days.
     * The temperature will be gaussian-style fluctuating around a fixed value.
     * <p>
     * Warm periods lasts 2-7 days.
     * Temperature will slowly rise to a peak around 50% into the cold period.
     * <p>
     * For more details regarding the numerical values mentioned above, see
     * {@link WorldTemperature}.
     *
     * @param startTime the start timestamp of next cold/warm-calm period, in
     *                  seconds.
     * @return a new TempEvent.
     */
    public static InterpolationClimateEvent getClimateEvent(RandomSource random,long startTime) {
        return fromDefinition(ClimateEventModel.generate(
                random, startTime, FHConfig.SERVER.CLIMATE.eventModelParameters()));
    }

    public static InterpolationClimateEvent getColdClimateEvent(RandomSource random,long startTime) {
        return fromDefinition(ClimateEventModel.generateCold(
                random, startTime, FHConfig.SERVER.CLIMATE.eventModelParameters()));
    }

    public static InterpolationClimateEvent getWarmClimateEvent(RandomSource random,long startTime) {
        return fromDefinition(ClimateEventModel.generateWarm(
                random, startTime, FHConfig.SERVER.CLIMATE.eventModelParameters()));
    }

    private static InterpolationClimateEvent fromDefinition(ClimateEventModel.EventDefinition event) {
        return new InterpolationClimateEvent(
                event.startTime(), event.peakTime(), event.peakTemperatureCelsius(),
                event.bottomTime(), event.bottomTemperatureCelsius(), event.endTime(),
                event.calmEndTime(), event.cold(), event.blizzard());
    }

    public InterpolationClimateEvent() {

    }

    public InterpolationClimateEvent(long startTime, long peakTime, float peakTemp, long bottomTime, float bottomTemp, long endTime,
                        long calmEndTime, boolean isCold, boolean isBlizzard) {
        this.startTime = startTime;
        this.peakTime = peakTime;
        this.peakTemp = peakTemp;
        this.bottomTime = bottomTime;
        this.bottomTemp = bottomTemp;
        this.endTime = endTime;
        this.isCold = isCold;
        this.calmEndTime = calmEndTime;
        this.isBlizzard = isBlizzard;
    }
    public static final MapCodec<InterpolationClimateEvent> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
    	Codec.LONG .fieldOf("startTime").forGetter(o->o.startTime),
    	Codec.LONG .fieldOf("peakTime").forGetter(o->o.peakTime),
    	Codec.FLOAT.fieldOf("peakTemp").forGetter(o->o.peakTemp),
    	Codec.LONG.fieldOf("bottomTime").forGetter(o->o.bottomTime),
    	Codec.FLOAT.fieldOf("bottomTemp").forGetter(o->o.bottomTemp),
    	Codec.LONG.fieldOf("endTime").forGetter(o->o.endTime),
    	Codec.LONG.fieldOf("calmEndTime").forGetter(o->o.calmEndTime),
    	Codec.BOOL.fieldOf("isCold").forGetter(o->o.isCold),
    	Codec.BOOL.fieldOf("isBlizzard").forGetter(o->o.isBlizzard)
    	).apply(t, InterpolationClimateEvent::new));
    public ClimateResult getHourClimate(long t) {
        ClimateType type = ClimateType.NONE;
        float temp = getHourTemp(t);
        if (isBlizzard) {
            if (temp <= WorldTemperature.BLIZZARD_REACHES_GROUND) {
                type = ClimateType.BLIZZARD;
            } else if (temp <= WorldTemperature.SNOW_REACHES_GROUND && t < endTime) {
                type = ClimateType.SNOW_BLIZZARD;
            } else if (t < endTime&&t>bottomTime) {
                type = ClimateType.SUN;
            }
        } else if (temp <= WorldTemperature.SNOW_REACHES_GROUND) {
            type = ClimateType.SNOW;
        }
        return new ClimateResult(temp, type);
    }

    /**
     * Compute the temperature at a given time according to this temperature event.
     * This algorithm is based on a piecewise interpolation technique.
     *
     * @param t given in seconds.
     * @return temperature at given time.
     * @author JackyWangMislantiaJnirvana <wmjwld@live.cn>
     */
    public float getHourTemp(long t) {
        return ClimateEventModel.temperatureAt(definition(), t);
    }

    /**
     * Interpolation algorithm.
     */
    private ClimateEventModel.EventDefinition definition() {
        return new ClimateEventModel.EventDefinition(
                startTime, peakTime, peakTemp, bottomTime, bottomTemp,
                endTime, calmEndTime, isCold, isBlizzard);
    }

    @Override
    public String toString() {
        return "TempEvent [startTime=" + startTime + ", peakTime=" + peakTime + ", peakTemp=" + peakTemp
                + ", bottomTime=" + bottomTime + ", bottomTemp=" + bottomTemp + ", endTime=" + endTime + ", isCold="
                + isCold + ", calmEndTime=" + calmEndTime + "]";
    }

}
