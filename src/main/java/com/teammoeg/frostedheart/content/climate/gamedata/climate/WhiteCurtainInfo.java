package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.math.BaseRandomSource;
import com.teammoeg.chorda.math.Rect;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

public class WhiteCurtainInfo {
	public static final Codec<WhiteCurtainInfo> CODEC=RecordCodecBuilder.create(t->t.group(
		Rect.CODEC.fieldOf("area").forGetter(o->o.affectedArea),
		Direction.CODEC.fieldOf("move").forGetter(o->o.moveDirection),
		ClimateEventTrack.CODEC.fieldOf("climate").forGetter(o->o.climate)
		).apply(t,WhiteCurtainInfo::new));
	public static final Codec<List<WhiteCurtainInfo>> LIST_CODEC=Codec.list(CODEC);
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
		long emitHour=(climate.getCalmEndTime()-climate.getStartTime())/50;
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
			ClimateResult clr=climate.getHourClimate(i*50+startTime);
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
	public static long getSecondsPerChunk() {
		return WorldClockSource.secondsPerDay/4;
	}
	public long getDeltaTime(ChunkPos pos) {
		return getDelta(pos)*getSecondsPerChunk();
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
		return getMaxDelta()*getSecondsPerChunk();
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
	public short[] getFrames(WorldClimate wclimate,long seconds,ChunkPos pos) {
		long dtime=(seconds-getDeltaTime(pos)-climate.getStartTime())/50;
		WeatherForecast fcc=forecastCache.get((int) getDelta(pos));
		if(fcc.shouldUpdateNewFrames(seconds/50))
			fcc.updateFrames(seconds/50, this.getFrames(wclimate,dtime));
		return fcc.getFrames();
	}
	@Override
	public String toString() {
		return "[affectedArea=" + affectedArea + ", climate=" + climate + ", moveDirection=" + moveDirection + "]";
	}

	public ClimateResult getClimate(long dtime) {
		if(dtime>climate.getCalmEndTime())
			return ClimateResult.EMPTY;
		long emitHour=(dtime-climate.getStartTime())/50;
		if(emitHour<0)
			return ClimateResult.EMPTY;
		long day=emitHour/24;
		if(day>=dailyTempData.size())
			return ClimateResult.EMPTY;
		long hourInDay=emitHour%24;
		return dailyTempData.get((int)day).getClimate((int) hourInDay);
	}
	public float getTemperature(long sec,ChunkPos pos) {
		return getClimate(sec,pos).temperature();
	}
	public ClimateResult getClimate(long sec,ChunkPos pos) {
		return getClimate(sec-getDeltaTime(pos));
	}
    public Iterable<ClimateResult> getFutureClimateIterator(long thours) {
    	thours--;
        int ddate = (int) (thours / 24);
        int dhours = (int) (thours % 24);
        return () -> new Iterator<ClimateResult>() {
            int curddate = ddate;
            int curdhours = dhours;

            @Override
            public boolean hasNext() {
               	int correctday=curddate;
            	if(curdhours>=23)
            		correctday++;
            	return correctday < dailyTempData.size();
            }

            @Override
            public ClimateResult next() {
                if (!hasNext()) return null;
                curdhours++;
                if (curdhours >= 24) {
                    curdhours = 0;
                    curddate++;
                }
                return dailyTempData.get(curddate).getClimate(curdhours);
            }
        };
    }
    public Iterable<ClimateResult> getFutureClimateIterator(WorldClimate worldclimate,long thours) {
    	Iterator<ClimateResult> it=worldclimate.getFutureClimateIterator(worldclimate, (int)( thours%3)).iterator();
    	thours--;
        int ddate = (int) (thours / 24);
        int dhours = (int) (thours % 24);
        return () -> new Iterator<ClimateResult>() {
            int curddate = ddate;
            int curdhours = dhours;

            @Override
            public boolean hasNext() {
            	return it.hasNext();
            }

            @Override
            public ClimateResult next() {
                
                curdhours++;
                if (curdhours >= 24) {
                    curdhours = 0;
                    curddate++;
                }
                if (curddate>=dailyTempData.size())
                	return it.next();
                if(curddate<0||curdhours<0)
                	return it.next();
                return dailyTempData.get(curddate).getClimate(curdhours).merge(it.next());
            }
        };
    }
    public static Rect generateArea(ChunkPos initial,Direction front,int x1,int x2,int z1,int z2) {
		Direction left=front.getCounterClockWise();

		int tlx=initial.x+z1*front.getStepX()+x1*left.getStepX();
		int tlz=initial.z+z1*front.getStepZ()+x1*left.getStepZ();
		int brx=initial.x-z2*front.getStepX()-x2*left.getStepX();
		int brz=initial.z-z2*front.getStepZ()-x2*left.getStepZ();
		return Rect.delta(tlx, tlz, brx, brz);
    }
	public static WhiteCurtainInfo generateWhiteCurtain(RandomSource rs,long start,BlockPos pos) {

		InterpolationClimateEvent ite=InterpolationClimateEvent.getBlizzardClimateEvent(rs, start);
		Direction dir=Direction.Plane.HORIZONTAL.getRandomDirection(rs);
		int totalchunks=(int) ((ite.getCalmEndTime()-ite.getStartTime())/WhiteCurtainInfo.getSecondsPerChunk());
		int x1=rs.nextInt(6)+4;
		int x2=rs.nextInt(6)+4;
		int z1=rs.nextInt(6)+6;
		int z2=rs.nextInt(8)+totalchunks;
		Rect rect=generateArea(new ChunkPos(pos),dir,x1,x2,z1,z2);
		WhiteCurtainInfo info=new WhiteCurtainInfo(rect,dir,ite);
		System.out.println(rect+","+dir+","+ite);
		return info;
	}
    public static void main(String[] args) {
		SharedConstants.tryDetectVersion();
		SharedConstants.enableDataFixerOptimizations();
		Bootstrap.bootStrap();
		System.out.println();
		
    	BaseRandomSource rs=new BaseRandomSource(5678);
    	InterpolationClimateEvent ice=InterpolationClimateEvent.getBlizzardClimateEvent(rs, 1000);
    	System.out.println(ice);
    	WhiteCurtainInfo wci=new WhiteCurtainInfo(new Rect(10,10,20,20),Direction.NORTH,ice);
    	int sx=Direction.NORTH.getStepX();
    	int sz=Direction.NORTH.getStepZ();
    	final int thours=14300;
    	for(int i=0;i<30;i++) {
    		System.out.println(new ChunkPos(15,i)+"="+wci.getClimate(thours,new ChunkPos(15,i)));
    	}
    	System.out.println(wci.isInvalid(14300));
    	//StringBuilder sb=new StringBuilder();
    	//StreamSupport.stream(wci.getFutureClimateIterator((thours-wci.getDeltaTime(new ChunkPos(15,16)))/50).spliterator(), false).map(String::valueOf).forEach(t->sb.append(t).append("\n"));
    	//System.out.println(sb);
    }

	
}
