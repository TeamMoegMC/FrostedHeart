package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.teammoeg.chorda.math.Rect;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class WhiteCurtainInfo {
	Rect affectedArea;
	ClimateEvent climate;
	Direction moveDirection;
	protected LinkedList<DayClimateData> dailyTempData;
	public List<WeatherForecast> forecastCache=new ArrayList<>();
	public WhiteCurtainInfo(Rect affectedArea,Direction moveDirection,ClimateEvent event) {
		dailyTempData=new LinkedList<>();
		climate=event;
		this.moveDirection=moveDirection;
		this.affectedArea=affectedArea;
		long emitHour=climate.getCalmEndTime()-climate.getStartTime();
		long startTime=climate.getStartTime();
		long emitDays=Mth.ceil(emitHour/24f);
		for(long i=0;i<emitDays;i++) {
			dailyTempData.add(new DayClimateData());
		}
		for(long i=0;i<getMaxDelta();i++) {
			forecastCache.add(new WeatherForecast());
		}
		for(long i=0;i<emitHour;i++) {
			long day=i/24;
			long hourInDay=i%24;
			ClimateResult clr=climate.getHourClimate(i+startTime);
			dailyTempData.get((int)day).setClimate((int) hourInDay, clr);
		}
	}

    public List<ForecastFrame> getFrames(WorldClimate climate,long delta){
    	return WeatherForecast.getFrames(getFutureClimateIterator(climate,delta),ClimateResult.EMPTY, (int) (120 - delta%3));
    }
	public boolean isAffected(ChunkPos pos) {
		return affectedArea.inRange(pos.x, pos.z);
	}
	public boolean isIntersected(Rect rect) {
		return affectedArea.intersects(rect);
	}
	public long getDeltaTime(ChunkPos pos) {
		return getDelta(pos)*WorldClockSource.secondsPerDay/2;
	}
	public long getDelta(ChunkPos pos) {
		switch(moveDirection){
		case NORTH:return (affectedArea.getY2()-pos.z);
		case SOUTH:return (pos.z-affectedArea.getY());
		case WEST:return (affectedArea.getX2()-pos.x);
		case EAST:return (pos.x-affectedArea.getX());
		}
		return 0;
	}
	public long getMaxDeltaTime() {
		return getMaxDelta()*WorldClockSource.secondsPerDay/2;
	}
	public int getMaxDelta() {
		switch(moveDirection){
		case NORTH:
		case SOUTH:return (affectedArea.getH());
		case WEST:
		case EAST:return (affectedArea.getW());
		}
		return 0;
	}
	public boolean isInvalid(long dtime) {
		return climate.getCalmEndTime()+getMaxDeltaTime()<dtime;
	}
	public short[] getFrames(WorldClimate wclimate,long hours,ChunkPos pos) {
		long dtime=hours-getDeltaTime(pos)-climate.getStartTime();
		WeatherForecast fcc=forecastCache.get((int) getDelta(pos));
		if(fcc.shouldUpdateNewFrames(dtime))
			fcc.updateFrames(dtime, this.getFrames(wclimate,dtime));
		return fcc.getFrames();
	}
	public ClimateResult getClimate(long dtime) {
		if(dtime>climate.getCalmEndTime())
			return ClimateResult.EMPTY;
		long emitHour=dtime-climate.getStartTime();
		if(emitHour<0)
			return ClimateResult.EMPTY;
		long day=emitHour/24;
		long hourInDay=emitHour%24;
		return dailyTempData.get((int)day).getClimate((int) hourInDay);
	}
	public float getTemperature(long sec,ChunkPos pos) {
		return getClimate(sec,pos).temperature();
	}
	public ClimateResult getClimate(long sec,ChunkPos pos) {
		return getClimate(sec-getDeltaTime(pos));
	}
	
    public Iterable<ClimateResult> getFutureClimateIterator(WorldClimate worldclimate,long thours) {
    	
    	long maxhours=climate.getCalmEndTime()-climate.getStartTime();
    	Iterator<ClimateResult> it=worldclimate.getFutureClimateIterator(worldclimate, (int) maxhours).iterator();
        int ddate = (int) (thours / 24);
        int dhours = (int) (thours % 24);
        return () -> new Iterator<ClimateResult>() {
            int curddate = ddate;
            int curdhours = dhours;

            @Override
            public boolean hasNext() {
                return curddate < maxhours&&it.hasNext();
            }

            @Override
            public ClimateResult next() {
                if (!hasNext()) return null;
                curdhours++;
                if (curdhours >= 24) {
                    curdhours = 0;
                    curddate++;
                }

                return dailyTempData.get(curddate).getClimate(curdhours).merge(it.next());
            }
        };
    }
    

	
}
