/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import static com.teammoeg.frostedheart.content.climate.WorldTemperature.*;

import java.util.Random;

import com.mojang.datafixers.util.Pair;

import net.minecraft.nbt.CompoundNBT;

/**
 * A climate event defined by a set of timestamps and temperature
 * parameters.
 * Allows computation of temperature at any timestamp within this event.
 */
public class ClimateEvent {
    private static final long secondsPerDay = 24 * 50;
    public long startTime;
    public long peakTime;
    public float peakTemp;
    public long bottomTime;
    public float bottomTemp;
    public long endTime;
    public boolean isCold;
    public boolean isBlizzard;

    public long calmEndTime;

    public static ClimateEvent getBlizzardClimateEvent(long startTime) {
        Random random = new Random();
        long peakTime = 0, bottomTime = 0, endTime = 0;
        float peakTemp = 0, bottomTemp = 0;
        int add=(int) Math.max(0,10-(startTime/secondsPerDay-15)/10);
        switch (random.nextInt(10)+add) {
            case 0:
                bottomTemp += COLD_PERIOD_BOTTOM_T8;
                break;
            case 1:
            case 2:
                bottomTemp += COLD_PERIOD_BOTTOM_T7;
                break;
            case 3:
            case 4:
            case 5:
                bottomTemp += COLD_PERIOD_BOTTOM_T6;
                break;
            default:
                bottomTemp += COLD_PERIOD_BOTTOM_T5;
                break;
        }

        long length = 0;
        length = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        endTime = startTime + length;
        long padding = 8 * 50 + random.nextInt(16 * 50);
        peakTime = startTime + padding; // reach peak within 8-24h
        bottomTime = startTime + padding + (length - padding) / 4;
        peakTemp = BLIZZARD_WARM_PEAK - (float) Math.abs(random.nextGaussian());
        bottomTemp += (float) (random.nextGaussian());
        long calmLength = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        long calmEndTime = endTime + calmLength;

        return new ClimateEvent(startTime, peakTime, peakTemp, bottomTime, bottomTemp, endTime, calmEndTime, true, true);
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
    public static ClimateEvent getClimateEvent(long startTime) {
        Random random = new Random();
        switch (random.nextInt(10)+((startTime/secondsPerDay<=15)?5:0)) {
            case 0:
            case 1:
            	return getBlizzardClimateEvent(startTime);
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            	return getColdClimateEvent(startTime);
            default:
            	return getWarmClimateEvent(startTime);
        }
    }

    public static ClimateEvent getColdClimateEvent(long startTime) {
        Random random = new Random();
        long peakTime = 0, bottomTime = 0, endTime = 0;
        float peakTemp = 0, bottomTemp = 0;
        switch (random.nextInt(10)) {
            case 0:
                bottomTemp += COLD_PERIOD_BOTTOM_T4;
                break;
            case 1:
            case 2:
                bottomTemp += COLD_PERIOD_BOTTOM_T3;
                break;
            case 3:
            case 4:
            case 5:
                bottomTemp += COLD_PERIOD_BOTTOM_T2;
                break;
            default:
                bottomTemp += COLD_PERIOD_BOTTOM_T1;
                break;
        }

        long length = 0;
        length = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        endTime = startTime + length;
        long padding = 8 * 50 + random.nextInt(16 * 50);
        peakTime = startTime + padding; // reach peak within 8-24h
        bottomTime = startTime + padding + (length - padding) / 4;
        peakTemp = COLD_PERIOD_PEAK - (float) Math.abs(random.nextGaussian());
        bottomTemp += (float) (random.nextGaussian());
        long calmLength = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        long calmEndTime = endTime + calmLength;

        return new ClimateEvent(startTime, peakTime, peakTemp, bottomTime, bottomTemp, endTime, calmEndTime, true, false);
    }

    public static ClimateEvent getWarmClimateEvent(long startTime) {
        Random random = new Random();
        long peakTime = 0, bottomTime = 0, endTime = 0;
        float peakTemp = 0, bottomTemp = 0;
        long length = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        endTime = startTime + length;
        long padding = 8 * 50 + random.nextInt(16 * 50); // 8-24h
        peakTime = startTime + padding + (length - padding) / 2;
        peakTemp = WARM_PERIOD_PEAK - 2 * (float) Math.abs(random.nextGaussian());

        long calmLength = secondsPerDay * 2 + random.nextInt((int) (secondsPerDay * 5)); // 2 - 7 days length
        long calmEndTime = endTime + calmLength;

        return new ClimateEvent(startTime, peakTime, peakTemp, bottomTime, bottomTemp, endTime, calmEndTime, false, false);
    }

    public ClimateEvent() {

    }

    public ClimateEvent(long startTime, long peakTime, float peakTemp, long bottomTime, float bottomTemp, long endTime,
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

    public void deserialize(CompoundNBT cnbt) {
        startTime = cnbt.getLong("startTime");
        peakTime = cnbt.getLong("peakTime");
        peakTemp = cnbt.getFloat("peakTemp");
        bottomTime = cnbt.getLong("bottomTime");
        bottomTemp = cnbt.getFloat("bottomTemp");
        endTime = cnbt.getLong("endTime");
        isCold = cnbt.getBoolean("isCold");
        isBlizzard = cnbt.getBoolean("isBlizzard");
        calmEndTime = cnbt.getLong("calmEndTime");
    }

    public Pair<Float, ClimateType> getHourClimate(long t) {
        ClimateType type = ClimateType.NONE;
        float temp = getHourTemp(t);
        if (isBlizzard) {
            if (temp <= WorldTemperature.BLIZZARD_TEMPERATURE) {
                type = ClimateType.BLIZZARD;
            } else if (temp <= WorldTemperature.SNOW_TEMPERATURE && t < bottomTime) {
                type = ClimateType.SNOW_BLIZZARD;
            } else if (t > bottomTime) {
                type = ClimateType.SUN;
            }
        } else if (temp <= WorldTemperature.SNOW_TEMPERATURE) {
            type = ClimateType.SNOW;
        }
        return Pair.of(temp, type);
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
        Random random = new Random();

        if (isCold) {
            if (t >= startTime && t < peakTime) {
                return getPiecewiseTemp(t, startTime, peakTime, 0, peakTemp, 0, 0);
            } else if (t >= peakTime && t < bottomTime) {
                return getPiecewiseTemp(t, peakTime, bottomTime, peakTemp, bottomTemp, 0, 0);
            } else if (t >= bottomTime && t < endTime) {
                return getPiecewiseTemp(t, bottomTime, endTime, bottomTemp, 0, 0, 0);
            } else if (t >= endTime && t <= calmEndTime) {
                return 0 + (float) (random.nextGaussian());
            } else {
                return 0 + (float) (random.nextGaussian());
            }
        }
        if (t >= startTime && t < peakTime) {
            return getPiecewiseTemp(t, startTime, peakTime, 0, peakTemp, 0, 0);
        } else if (t >= peakTime && t < endTime) {
            return getPiecewiseTemp(t, peakTime, endTime, peakTemp, 0, 0, 0);
        } else if (t >= endTime && t <= calmEndTime) {
            return 0 + (float) (random.nextGaussian());
        } else {
            return 0 + (float) (random.nextGaussian());
        }

    }

    /**
     * Interpolation algorithm.
     */
    private float getPiecewiseTemp(long t, long t0, long t1, float T0, float T1, float dT0, float dT1) {

        float D1 = t - t0;
        float D2 = t1 - t0;
        float D3 = t - t1;
        float D4 = t0 - t1;

        float F1 = D3 / D4;
        float F2 = D1 / D2;

        float P1 = (float) Math.pow(F1, 2);
        float P2 = (float) Math.pow(F2, 2);

        return T0 * (1 + 2 * F2) * P1 + T1 * (1 + 2 * F1) * P2 + dT0 * D1 * P1 + dT1 * D3 * P2;
    }

    public CompoundNBT serialize(CompoundNBT cnbt) {
        cnbt.putLong("startTime", startTime);
        cnbt.putLong("peakTime", peakTime);
        cnbt.putFloat("peakTemp", peakTemp);
        cnbt.putLong("bottomTime", bottomTime); // not used when not is cold
        cnbt.putFloat("bottomTemp", bottomTemp); // not used when not is cold
        cnbt.putLong("endTime", endTime);
        cnbt.putBoolean("isCold", isCold);
        cnbt.putBoolean("isBlizzard", isBlizzard);
        cnbt.putLong("calmEndTime", calmEndTime);
        return cnbt;
    }

    @Override
    public String toString() {
        return "TempEvent [startTime=" + startTime + ", peakTime=" + peakTime + ", peakTemp=" + peakTemp
                + ", bottomTime=" + bottomTime + ", bottomTemp=" + bottomTemp + ", endTime=" + endTime + ", isCold="
                + isCold + ", calmEndTime=" + calmEndTime + "]";
    }

}
