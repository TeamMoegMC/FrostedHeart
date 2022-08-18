package com.teammoeg.frostedheart.client;

import com.teammoeg.frostedheart.climate.ClimateData.TemperatureFrame;

public class ClientForecastData {
	public static final TemperatureFrame[] tfs=new TemperatureFrame[40];
	public ClientForecastData() {
	}
	public static void clear() {
		for(int i=0;i<tfs.length;i++)
			tfs[i]=null;
	}
}
