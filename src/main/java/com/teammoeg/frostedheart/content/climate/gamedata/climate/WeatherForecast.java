package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import lombok.Getter;

public class WeatherForecast {
	@Getter
    protected short[] frames = new short[40];
    protected long lastforecast=-1;
	public WeatherForecast() {
	}

    /**
     * Present a minor update for forecast data
     */
    public boolean shouldUpdateNewFrames(long cur) {
    	int delta = (int) (cur % 3);
        if (cur >= lastforecast) {//times goes too fast.
            return true;
        }
        
        int from = (int) (lastforecast - cur);
        int to = 120 - delta;
        if (to - from < 3) return false;
        return true;
    }

    /**
     * Present a total update for forecast data
     */
    public void updateFrames(long cur,List<ForecastFrame> orgframes) {
        int delta = (int) (cur % 3);
        ForecastFrame[] toRender = new ForecastFrame[40];
        for (ForecastFrame te : orgframes) {
            int renderIndex = (te.dhours + delta) / 3;
            if (renderIndex >= 40) break;
            ForecastFrame prev = toRender[renderIndex];
            if (prev != null && prev.type.isWeatherEvent() && !te.type.isWeatherEvent())
                continue;//Always not omit weather event
            if (prev == null ||//previous null, overwrite
                    te.type.isWeatherEvent() ||//respect weather event
                    (te.toState < 0 && prev.toState >= 0) ||//lower in value
                    (te.toState < 0 && prev.toState < 0 && te.toState < prev.toState) || //lowest in value
                    (te.toState > 0 && prev.toState <= 0) || //higher in value
                    (te.toState > 0 && prev.toState > 0 && te.toState > prev.toState)//highest in value
                //te.toState == 0
            ) {
                toRender[renderIndex] = te;
            }
        }
        lastforecast = cur + 120 - delta;
        int i = 0;
        for (ForecastFrame tf : toRender) {
            if (tf != null)
                frames[i++] = tf.packNoHour();
            else
                frames[i++] = 0;
        }
    }

    public static int getTemperatureLevel(float temp) {
        if (temp >= FHConfig.SERVER.CLIMATE.warmPeakCelsius.get()
                - FHConfig.SERVER.CLIMATE.forecastSensitivityCelsius.get() * 2) {
            return 1;
        } else if (temp <= -2) {
            float[] bottoms = forecastBottoms();
            for (int j = bottoms.length - 1; j >= 0; j--) {//check out its level
                float b = bottoms[j]
                        + FHConfig.SERVER.CLIMATE.forecastSensitivityCelsius.get().floatValue();
                if (temp < b) {//just acrosss a level
                    return -j - 1;
                }
            }
        }
        return 0;
    }
    private static float getLevelTemperature(int level) {
    	if(level>0) {
	    	switch(level) {
			case 2:return FHConfig.SERVER.CLIMATE.warmPeakCelsius.get().floatValue();
	    	case 1:return WorldTemperature.WARM_PERIOD_LOWER_PEAK;
	    	default:return 0;
	    	}
    	}else if(level==0)
    		return 0;
	    return forecastBottoms()[-level-1];
    }

    private static float[] forecastBottoms() {
        return new float[]{
                FHConfig.SERVER.CLIMATE.coldBottomNormalCelsius.get().floatValue(),
                FHConfig.SERVER.CLIMATE.coldBottomStrongCelsius.get().floatValue(),
                FHConfig.SERVER.CLIMATE.coldBottomSevereCelsius.get().floatValue(),
                FHConfig.SERVER.CLIMATE.coldBottomExtremeCelsius.get().floatValue(),
                WorldTemperature.COLD_PERIOD_BOTTOM_T6,
                WorldTemperature.COLD_PERIOD_BOTTOM_T7,
                WorldTemperature.COLD_PERIOD_BOTTOM_T8,
                WorldTemperature.COLD_PERIOD_BOTTOM_T9,
                WorldTemperature.COLD_PERIOD_BOTTOM_T10
        };
    }
    private static float getLevelMaxThresold(float temp) {
        return temp + 10
                - FHConfig.SERVER.CLIMATE.forecastSensitivityCelsius.get().floatValue();
    }
    private static float getLevelMinThresold(float temp) {
        int delta=10;
        if(temp>0)
            delta=8;
        return temp - delta
                + FHConfig.SERVER.CLIMATE.forecastSensitivityCelsius.get().floatValue();
    }
    public static List<ForecastFrame> getFrames(Iterable<ClimateResult> futureTemp,ClimateResult lastClimate,int nframes) {
        List<ForecastFrame> frames = new ArrayList<>();
        int lastLevel=getTemperatureLevel(lastClimate.temperature());
        ClimateType lastType = ClimateType.NONE;
        int i = 0;//(int) (this.clockSource.getHours()%3);
        float ltemp=getLevelTemperature(lastLevel);
        float tmin=getLevelMinThresold(ltemp);
        float tmax=getLevelMaxThresold(ltemp);
        int curLevel=lastLevel;
        lastLevel=0;
        for (ClimateResult pf : futureTemp) {
            if (i >= nframes) break;
            final float f = pf.temperature();
            final ClimateType bz = pf.climate();
            //System.out.println(bz+","+lastlevel+","+f);
            if(tmin>f||tmax<f){//over the thresold
            	curLevel=getTemperatureLevel(f);
            	ltemp=getLevelTemperature(curLevel);
            	tmin=getLevelMinThresold(ltemp);
            	tmax=getLevelMaxThresold(ltemp);
            }
            if (lastType.typeId != bz.typeId) {
                switch (bz) {
                    case SNOW_BLIZZARD:
                    case BLIZZARD:
                    	frames.add(ForecastFrame.weather(i, bz, -7));break;
                    default:
                    	frames.add(ForecastFrame.weather(i, bz, curLevel));
                }
                lastType = bz;
            } else if (lastType == ClimateType.BLIZZARD || lastType == ClimateType.SNOW_BLIZZARD) {

            } else /*if(i==0) {
            	if(lastLevel>0)
            		frames.add(ForecastFrame.increase(i, lastLevel));
            	else if(lastLevel<0)
            		frames.add(ForecastFrame.decrease(i, lastLevel));
            } else */if(curLevel!=lastLevel) {
            	if(lastLevel>curLevel)
            		frames.add(ForecastFrame.decrease(i, curLevel));
            	else
            		frames.add(ForecastFrame.increase(i, curLevel));
            }
            lastLevel=curLevel;
            i++;
        }
        return frames;
    }
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
    	sb.append("frame=");
    	for(int i=0;i<frames.length;i++) {
    		sb.append(ForecastFrame.unpack(frames[i])).append(",");
    	}
		return sb.toString();
	}

}
