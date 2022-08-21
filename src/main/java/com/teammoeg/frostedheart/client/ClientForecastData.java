package com.teammoeg.frostedheart.client;

import com.teammoeg.frostedheart.climate.ClimateData.TemperatureFrame;

public class ClientForecastData {
	public static final TemperatureFrame[] tfs=new TemperatureFrame[40];
	public static long secs=0;
	public ClientForecastData() {
	}
	public static void clear() {
		secs=0;
		for(int i=0;i<tfs.length;i++)
			tfs[i]=null;
	}
    public static int getHourInDay() {
        return (int) ((secs / 50) % 24);
    }

    public static long getDate() {
        return (secs / 50) / 24;
    }

    public static long getMonth() {
        return (secs / 50) / 24 / 30;
    }

    public static long getHours() {
        return (secs / 50);
    }

    public static long getTimeSecs() {
        return secs;
    }
}
