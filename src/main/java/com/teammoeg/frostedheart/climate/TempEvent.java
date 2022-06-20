package com.teammoeg.frostedheart.climate;

import net.minecraft.nbt.CompoundNBT;

import java.util.Random;

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

    // Generate TempEvent parameters
    public static TempEvent getTempEvent(long nextStart) {
        Random random = new Random();
        long secondsPerDay = 24*50;
        long length = secondsPerDay*5 + random.nextInt((int) (secondsPerDay*3));
        
        long nextPeak = nextStart + 8*50 + random.nextInt(16*50); // reach peak within 8-24h
        long nextBottom = nextPeak + (length / 5) + random.nextInt(24*50); // reach bottom around 20% length
        long nextEnd = nextStart + length;

        long calmLength = secondsPerDay*5 + random.nextInt((int) (secondsPerDay*3));
        long nextCalmEnd = nextEnd + calmLength;
        System.out.println(nextStart);
        System.out.println(nextCalmEnd-nextStart);
        float peakTemp = 4 + (float) (random.nextGaussian());
        float bottomTemp = - 20 + (float) (random.nextGaussian());

        return new TempEvent(nextStart, nextPeak, peakTemp, nextBottom, bottomTemp, nextEnd, nextCalmEnd, true);
    }

    public float getHourTemp(long t) {
        if (t >= startTime && t < peakTime) {
            return getPiecewiseTemp(t, startTime, peakTime, 0, peakTemp, 0, 0);
        } else if (t >= peakTime && t < bottomTime) {
            return getPiecewiseTemp(t, peakTime, bottomTime, peakTemp, bottomTemp, 0, 0);
        } else if (t >= bottomTime && t < endTime) {
            return getPiecewiseTemp(t, bottomTime, endTime, bottomTemp, 0, 0, 0);
        } else if (t >= endTime && t < calmEndTime) {
            return 0F;
        } else {
            return 0F;
        }
    }

    private float getPiecewiseTemp(long t, long t0, long t1, float T0, float T1, float dT0, float dT1) {

        float D1 = t - t0;
        float D2 = t1 - t0;
        float D3 = t - t1;
        float D4 = t0 - t1;

        float F1 = D3 / D4;
        float F2 = D1 / D2;

        float P1 = (float) Math.pow(F1, 2);
        float P2 = (float) Math.pow(F2, 2);

        return  T0 * (1 + 2 * F2) * P1 +
                T1 * (1 + 2 * F1) * P2 +
                dT0 * D1 * P1 +
                dT1 * D3 * P2;
    }

}
