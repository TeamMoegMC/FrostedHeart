/*
 * Copyright (c) 2022 TeamMoeg
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
