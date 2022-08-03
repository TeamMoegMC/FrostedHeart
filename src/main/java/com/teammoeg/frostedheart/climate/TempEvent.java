package com.teammoeg.frostedheart.climate;

import net.minecraft.nbt.CompoundNBT;

import java.util.Random;

import static com.teammoeg.frostedheart.climate.WorldClimate.*;

/**
 * A temperature event defined by a set of timestamps and temperature parameters.
 * Allows computation of temperature at any timestamp within this event.
 */
public class TempEvent {
    public long startTime;
    public long peakTime;
    public float peakTemp;
    public long bottomTime;
    public float bottomTemp;
    public long endTime;
    public boolean isCold;
    public long calmEndTime;

    public TempEvent() {

    }

    public TempEvent(long startTime, long peakTime, float peakTemp, long bottomTime, float bottomTemp, long endTime,
                     long calmEndTime, boolean isCold) {
        this.startTime = startTime;
        this.peakTime = peakTime;
        this.peakTemp = peakTemp;
        this.bottomTime = bottomTime;
        this.bottomTemp = bottomTemp;
        this.endTime = endTime;
        this.isCold = isCold;
        this.calmEndTime = calmEndTime;
    }

    public CompoundNBT serialize(CompoundNBT cnbt) {
        cnbt.putLong("startTime", startTime);
        cnbt.putLong("peakTime", peakTime);
        cnbt.putFloat("peakTemp", peakTemp);
        cnbt.putLong("bottomTime", bottomTime);
        cnbt.putFloat("bottomTemp", bottomTemp);
        cnbt.putLong("endTime", endTime);
        cnbt.putBoolean("isCold", isCold);
        cnbt.putLong("calmEndTime", calmEndTime);
        return cnbt;
    }

    public void deserialize(CompoundNBT cnbt) {
        startTime = cnbt.getLong("startTime");
        peakTime = cnbt.getLong("peakTime");
        peakTemp = cnbt.getFloat("peakTemp");
        bottomTime = cnbt.getLong("bottomTime");
        bottomTemp = cnbt.getFloat("bottomTemp");
        endTime = cnbt.getLong("endTime");
        isCold = cnbt.getBoolean("isCold");
        calmEndTime = cnbt.getLong("calmEndTime");
    }

    /**
     * Creates a new TempEvent consisting of a cold period followed by a calm period.
     * This essentially generates a set of parameters that can be used in later computation.
     *
     * Cold period lasts 5-8 days.
     * At beginning, temperature quickly rises to a peak within 8-24 hours.
     * Then, temperature quickly drops to a bottom at around 20% time into the cold period.
     * Bottom temperature has three levels: normal, intense, extreme.
     * The chances for these three levels happening are, respectively: 70%, 20%, 10%.
     *
     * Calm periods lasts 5-8 days.
     * The temperature will be gaussian-style fluctuating around a fixed value.
     *
     * TODO: implement warm period
     * Warm periods lasts 5-8 days.
     * Temperature will slowly rise to a peak around 50% into the cold period.
     *
     *
     * For more details regarding the numerical values mentioned above, see {@link WorldClimate}.
     *
     * @param nextStart the start timestamp of next cold-calm period, in seconds.
     * @return a new TempEvent.
     */
    public static TempEvent getTempEvent(long nextStart) {
        Random random = new Random();
        long secondsPerDay = 24 * 50;

        // Cold Period
        long length = secondsPerDay * 5 + random.nextInt((int) (secondsPerDay * 3)); // 5 - 8 days length
        long nextPeak = nextStart + 8 * 50 + random.nextInt(16 * 50); // reach peak within 8-24h
        long nextBottom = nextPeak + (length / 5) + random.nextInt(24 * 50); // reach bottom around 20% length
        long nextEnd = nextStart + length;

        // Calm Period
        long calmLength = secondsPerDay * 5 + random.nextInt((int) (secondsPerDay * 3)); // 5 - 8 days length
        long nextCalmEnd = nextEnd + calmLength;

        float peakTemp = COLD_PERIOD_PEAK + (float) (random.nextGaussian());

        // 10% Extreme cold, 20% Intense cold, 70% Cold
        int typeBottom = random.nextInt(10);
        float bottomTemp = (float) (random.nextGaussian());
        if (typeBottom == 0) {
            bottomTemp += COLD_PERIOD_BOTTOM_EXTREME;
        } else if (typeBottom <= 2) {
            bottomTemp += COLD_PERIOD_BOTTOM_INTENSE;
        } else {
            bottomTemp += COLD_PERIOD_BOTTOM;
        }

        return new TempEvent(nextStart, nextPeak, peakTemp, nextBottom, bottomTemp, nextEnd, nextCalmEnd, true);
    }

    /**
     * Compute the temperature at a given time according to this temperature event.
     * This algorithm is based on a piecewise interpolation technique.
     * @author JackyWangMislantiaJnirvana <wmjwld@live.cn>
     *
     * @param t given in seconds.
     * @return temperature at given time.
     */
    public float getHourTemp(long t) {
        Random random = new Random();
        if (t >= startTime && t < peakTime) {
            return getPiecewiseTemp(t, startTime, peakTime, 0, peakTemp, 0, 0);
        } else if (t >= peakTime && t < bottomTime) {
            return getPiecewiseTemp(t, peakTime, bottomTime, peakTemp, bottomTemp, 0, 0);
        } else if (t >= bottomTime && t < endTime) {
            return getPiecewiseTemp(t, bottomTime, endTime, bottomTemp, 0, 0, 0);
        } else if (t >= endTime && t <= calmEndTime) {
            return CALM_PERIOD_BASELINE + (float) (random.nextGaussian());
        } else {
            return CALM_PERIOD_BASELINE + (float) (random.nextGaussian());
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

        return T0 * (1 + 2 * F2) * P1 +
                T1 * (1 + 2 * F1) * P2 +
                dT0 * D1 * P1 +
                dT1 * D3 * P2;
    }

}
