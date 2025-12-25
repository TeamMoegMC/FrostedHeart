/*
 * Copyright (c) 2024 TeamMoeg
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

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;

import net.minecraft.nbt.CompoundTag;

public class DayTemperatureData {
    public static class HourData {
    	public static final Codec<HourData> CODEC=Codec.INT.xmap(HourData::new,HourData::pack);
        private float temp = 0;

        private ClimateType type = ClimateType.NONE;
        //6
        private byte windSpeed;//unsigned

        public HourData() {
            super();
            setWindSpeed(0);
        }

        public HourData(ClimateType type) {
            super();
            this.type = type;
        }


        public HourData(float temp, ClimateType type,
                        int windSpeed) {
            super();
            this.temp = temp;
            this.type = type;
            setWindSpeed(windSpeed);
        }

        public HourData(int packed) {
            this();
            unpack(packed);
        }

        public float getTemp() {
            return temp;
        }

        public ClimateType getType() {
            return type == null ? ClimateType.NONE : type;//IDK why this happens, but this would fix
        }


        public int getWindSpeed() {
            return windSpeed & 0xFF;
        }

        public int pack() {
            int val = 0;
            short tempInt = (short) (temp * 100);
            val |= (tempInt & 0xFFFF);
            val |= windSpeed << 16;

            val |= (getType().ordinal() & 0xff) << 24;
            return val;
        }

        private void setWindSpeed(int val) {
            windSpeed = (byte) (val);
        }

        @Override
        public String toString() {
            return "{T=" + temp + ",W=" + getType() + ",V=" + getWindSpeed() + "}";
        }

        public void unpack(int val) {
            short tempInt = (short) (val & 0xFFFF);
            temp = tempInt / 100f;
            windSpeed = (byte) ((val >> 16) & 0xff);
            int vx = (val >> 24);
            type = ClimateType.values()[vx];
        }


    }
    public static final Codec<DayTemperatureData> CODEC=RecordCodecBuilder.create(t->t.group(
    	CodecUtil.array(HourData.CODEC,new HourData[0]).fieldOf("ndata").forGetter(o->o.hourData),
    	Codec.FLOAT.fieldOf("humidity").forGetter(o->o.dayHumidity),
    	Codec.FLOAT.fieldOf("noise").forGetter(o->o.dayNoise),
    	Codec.LONG.fieldOf("day").forGetter(o->o.day)).apply(t, DayTemperatureData::new));
    HourData[] hourData = new HourData[24];
    float dayHumidity;
    float dayNoise;
    long day;

    public static DayTemperatureData read(CompoundTag data) {
        DayTemperatureData dtd = new DayTemperatureData();
        dtd.deserialize(data);
        return dtd;
    }

    public DayTemperatureData(HourData[] hourData, float dayHumidity, float dayNoise, long day) {
		super();
		this.hourData = hourData;
		this.dayHumidity = dayHumidity;
		this.dayNoise = dayNoise;
		this.day = day;
	}

	public DayTemperatureData() {
        for (int i = 0; i < 24; i++)
            hourData[i] = new HourData();
    }

    public DayTemperatureData(long day) {
        this();
        this.day = day;
    }

    public void deserialize(CompoundTag cnbt) {
        int[] iar = cnbt.getIntArray("ndata");
        for (int i = 0; i < iar.length; i++)
            hourData[i].unpack(iar[i]);
        dayHumidity = cnbt.getFloat("humidity");
        dayNoise = cnbt.getFloat("noise");
        day = cnbt.getLong("day");
    }

    public HourData getData(int hourInDay) {
        return hourData[hourInDay];
    }

    public HourData getData(WorldClockSource clockSource) {
        return getData(clockSource.getHourInDay());
    }

    public float getTemp(int hourInDay) {
        return hourData[hourInDay].getTemp();
    }

    public float getTemp(WorldClockSource wcs) {
        return getTemp(wcs.getHourInDay());
    }

    public ClimateType getType(int hourInDay) {
        return hourData[hourInDay].type;
    }

    public ClimateType getType(WorldClockSource wcs) {
        return getType(wcs.getHourInDay());
    }

    public int getWind(int hourInDay) {
        return hourData[hourInDay].getWindSpeed();
    }

    public int getWind(WorldClockSource wcs) {
        return getWind(wcs.getHourInDay());
    }

    public boolean isBlizzard(int hourInDay) {
        return hourData[hourInDay].getType() == ClimateType.BLIZZARD;
    }

    public boolean isBlizzard(WorldClockSource wcs) {
        return isBlizzard(wcs.getHourInDay());
    }

    public boolean isCloudy(int hourInDay) {
        return hourData[hourInDay].getType() == ClimateType.CLOUDY;
    }

    public boolean isCloudy(WorldClockSource wcs) {
        return isCloudy(wcs.getHourInDay());
    }

    public boolean isSnow(int hourInDay) {
        ClimateType type = hourData[hourInDay].getType();
        return type == ClimateType.SNOW || type == ClimateType.SNOW_BLIZZARD;
    }

    public boolean isSnow(WorldClockSource wcs) {
        return isSnow(wcs.getHourInDay());
    }

    public boolean isSunny(int hourInDay) {
        return hourData[hourInDay].getType() == ClimateType.SUN;
    }

    public boolean isSunny(WorldClockSource wcs) {
        return isSunny(wcs.getHourInDay());
    }

    public CompoundTag serialize() {
        CompoundTag cnbt = new CompoundTag();
        cnbt.putIntArray("ndata", Arrays.stream(hourData).mapToInt(HourData::pack).toArray());
        // cnbt.putIntArray("data", hourData);
        cnbt.putFloat("humidity", dayHumidity);
        cnbt.putFloat("noise", dayNoise);
        cnbt.putLong("day", day);
        //cnbt.putLongArray("blizzard", blizzard.toLongArray());
        return cnbt;
    }

    public void setBlizzard(int hourInDay) {
        hourData[hourInDay].type = ClimateType.BLIZZARD;
    }

    public void setCloudy(int hourInDay) {
        hourData[hourInDay].type = ClimateType.CLOUDY;
    }

    public void setSnow(int hourInDay) {
        hourData[hourInDay].type = ClimateType.SNOW;
    }

    public void setSunny(int hourInDay) {
        hourData[hourInDay].type = ClimateType.SUN;
    }

    void setTemp(int hourInDay, float temp) {
        hourData[hourInDay].temp = temp;
    }

    public void setType(int hourInDay, ClimateType type) {
        hourData[hourInDay].type = type;
    }

    public void setWind(int hourInDay, int speed) {
        hourData[hourInDay].setWindSpeed(speed);
    }

    @Override
    public String toString() {
        return "{hourData=" + Arrays.stream(hourData).map(Object::toString).reduce("", (a, b) -> a + b + ",") + ",rH=" + dayHumidity
                + ",noise=" + dayNoise + "}";
    }


}
