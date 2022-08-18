package com.teammoeg.frostedheart.climate;

import java.util.Arrays;

import net.minecraft.nbt.CompoundNBT;

public class DayTemperatureData {
    int[] hourData = new int[24];
    float dayHumidity;
    float dayNoise;
    long day;

    public DayTemperatureData() {
    }
    public DayTemperatureData(int nop) {
    	for(int i=0;i<24;i++)
    		setHourTemp(i,0);
    }

    public float getTemp(WorldClockSource wcs) {
        return getTemp(wcs.getHourInDay());
    }

    public float getTemp(int hourInDay) {
        return Float.intBitsToFloat(hourData[hourInDay]);
    }

    void setHourTemp(int h, float temp) {
        hourData[h] = Float.floatToRawIntBits(temp);
    }

    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putIntArray("data", hourData);
        cnbt.putFloat("humidity", dayHumidity);
        cnbt.putFloat("noise", dayNoise);
        cnbt.putLong("day", day);
        return cnbt;
    }

    public void deserialize(CompoundNBT cnbt) {
        int[] iar = cnbt.getIntArray("data");
        for (int i = 0; i < iar.length; i++)
            hourData[i] = iar[i];
        dayHumidity = cnbt.getFloat("humidity");
        dayNoise = cnbt.getFloat("noise");
        day = cnbt.getLong("day");
    }

    public static DayTemperatureData read(CompoundNBT data) {
        DayTemperatureData dtd = new DayTemperatureData();
        dtd.deserialize(data);
        return dtd;
    }

	@Override
	public String toString() {
		return "DayTemperatureData [hourData=" + Arrays.toString(hourData) + ", dayHumidity=" + dayHumidity
				+ ", dayNoise=" + dayNoise + ", day=" + day + "]";
	}
}
