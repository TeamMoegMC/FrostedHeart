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
import java.util.BitSet;

import net.minecraft.nbt.CompoundNBT;

public class DayTemperatureData {
	public static class HourTemperatureData{
		private float temp=0;
		private boolean isBlizzard=false;
		private boolean isSnow=false;
		private boolean isSunny=false;
		private boolean isCloudy=false;
		//6
		private byte windSpeed;//unsigned
		
		public HourTemperatureData() {
			super();
			setWindSpeed(0);
		}
		
		public HourTemperatureData(int packed) {
			this();
			unpack(packed);
		}

	
		public HourTemperatureData(float temp, boolean isBlizzard, boolean isSnow, boolean isSunny, boolean isCloudy,
				int windSpeed) {
			super();
			this.temp = temp;
			this.isBlizzard = isBlizzard;
			this.isSnow = isSnow;
			this.isSunny = isSunny;
			this.isCloudy = isCloudy;
			setWindSpeed(windSpeed);
		}

		private void setWindSpeed(int val) {
			windSpeed=(byte) (val);
		}
		public int getWindSpeed() {
			return windSpeed&0xFF;
		}
		public float getTemp() {
			return temp;
		}
		public boolean isBlizzard() {
			return isBlizzard;
		}
		public boolean isSnow() {
			return isSnow;
		}
		public boolean isSunny() {
			return isSunny;
		}

		public boolean isCloudy() {
			return isCloudy;
		}

		public int pack() {
			int val=0;
			short tempInt=(short)(temp*100);
			val|=(tempInt&0xFFFF);
			val|=windSpeed<<16;
			byte vd=0;
			if(isBlizzard)
				vd|=0x1;
			if(isSnow)
				vd|=0x2;
			if(isSunny)
				vd|=0x4;
			if(isCloudy)
				vd|=0x8;
			val|=vd<<24;
			return val;
		}
		public void unpack(int val) {
			short tempInt=(short) (val&0xFFFF);
			temp=tempInt/100f;
			windSpeed=(byte) ((val>>16)&0xff);
			int vx=(val>>24);
			isBlizzard=(vx&0x1)>0;
			isSnow=    (vx&0x2)>0;
			isSunny=   (vx&0x4)>0;
			isCloudy=  (vx&0x8)>0;
		}

		@Override
		public String toString() {
			return "{temp=" + temp + ",Bz/Sn/Su/Cl=" + (isBlizzard?1:0) + "/" + (isSnow?1:0)
					+ "/" + (isSunny?1:0) + "/" + (isCloudy?1:0) + ",windSpeed=" + getWindSpeed() + "}";
		}

		
	}
	HourTemperatureData[] hourData = new HourTemperatureData[24];
    float dayHumidity;
    float dayNoise;
    long day;

    public DayTemperatureData() {
		for(int i=0;i<24;i++)
			hourData[i]=new HourTemperatureData();
    }
    public DayTemperatureData(long day) {
		this();
		this.day = day;

	}

    void setTemp(int hourInDay, float temp) {
        hourData[hourInDay].temp=temp;;
    }
    public float getTemp(WorldClockSource wcs) {
        return getTemp(wcs.getHourInDay());
    }
    public float getTemp(int hourInDay) {
        return hourData[hourInDay].getTemp();
    }
    
    public void setBlizzard(int hourInDay,boolean data) {
    	hourData[hourInDay].isBlizzard=data;
    }
    public boolean isBlizzard(WorldClockSource wcs) {
    	return isBlizzard(wcs.getHourInDay());
    }  
    public boolean isBlizzard(int hourInDay) {
    	return hourData[hourInDay].isBlizzard();
    }  
    
	public void setSnow(int hourInDay, boolean data) {
		hourData[hourInDay].isSnow=data;
	}
    public boolean isSnow(WorldClockSource wcs) {
    	return isSnow(wcs.getHourInDay());
    }  
    public boolean isSnow(int hourInDay) {
    	return hourData[hourInDay].isSnow();
    }
    
	public void setCloudy(int hourInDay, boolean data) {
		hourData[hourInDay].isCloudy=data;
	}
    public boolean isCloudy(WorldClockSource wcs) {
    	return isCloudy(wcs.getHourInDay());
    }  
    public boolean isCloudy(int hourInDay) {
    	return hourData[hourInDay].isCloudy();
    }
    
	public void setSunny(int hourInDay, boolean data) {
		hourData[hourInDay].isSunny=data;
	}
    public boolean isSunny(WorldClockSource wcs) {
    	return isSunny(wcs.getHourInDay());
    }  
    public boolean isSunny(int hourInDay) {
    	return hourData[hourInDay].isSunny();
    }
    
	public HourTemperatureData getData(WorldClockSource clockSource) {
		return getData(clockSource.getHourInDay());
	}
    public HourTemperatureData getData(int hourInDay) {
    	return hourData[hourInDay];
    }



    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putIntArray("ndata", Arrays.stream(hourData).mapToInt(HourTemperatureData::pack).toArray());
       // cnbt.putIntArray("data", hourData);
        cnbt.putFloat("humidity", dayHumidity);
        cnbt.putFloat("noise", dayNoise);
        cnbt.putLong("day", day);
        //cnbt.putLongArray("blizzard", blizzard.toLongArray());
        return cnbt;
    }

    public void deserialize(CompoundNBT cnbt) {
        int[] iar = cnbt.getIntArray("ndata");
        for (int i = 0; i < iar.length; i++)
            hourData[i].unpack(iar[i]);
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
		return "{hourData=" + Arrays.stream(hourData).map(Object::toString).reduce("",(a,b)->a+b+",") + ",rH=" + dayHumidity
				+ ",noise=" + dayNoise+ "}";
	}


}
