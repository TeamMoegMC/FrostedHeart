package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mojang.serialization.Codec;
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
	public static final Codec<WhiteCurtainInfo> CODEC = WhiteCurtainDescriptor.CODEC
		.xmap(WhiteCurtainInfo::new, WhiteCurtainInfo::descriptor);
	public static final Codec<List<WhiteCurtainInfo>> LIST_CODEC=Codec.list(CODEC);
	private final WhiteCurtainDescriptor descriptor;
	long snowStartHours=-1;
	long blizzardStartHours=-1;
	long snowEndHours=-1;
	long blizzardEndHours=-1;
	protected LinkedList<DayClimateData> dailyTempData;
	public List<WeatherForecast> forecastCache=new ArrayList<>();
	public WhiteCurtainInfo(Rect affectedArea,Direction moveDirection,ClimateEvent event) {
		this(new WhiteCurtainDescriptor(affectedArea, moveDirection, event));
	}
	public WhiteCurtainInfo(WhiteCurtainDescriptor descriptor) {
		this.descriptor = descriptor;
		dailyTempData=new LinkedList<>();
		ClimateEvent climate = descriptor.climate();
		long emitHour=(climate.getCalmEndTime()-climate.getStartTime())/50;
		long startTime=climate.getStartTime();
		long emitDays=Mth.ceil(emitHour/24f);
		for(long i=0;i<emitDays;i++) {
			dailyTempData.add(new DayClimateData());
		}
		for(long i=0;i<=getMaxDelta();i++) {
			forecastCache.add(new WeatherForecast());
		}
		
		for(long i=0;i<emitHour;i++) {
			long day=i/24;
			long hourInDay=i%24;
			ClimateResult clr=climate.getHourClimate(i*50+startTime);
			dailyTempData.get((int)day).setClimate((int) hourInDay, clr);
			switch(clr.climate()) {
			case SNOW_BLIZZARD:if(snowStartHours==-1)snowStartHours=i;if(blizzardStartHours!=-1&&blizzardEndHours==-1)blizzardEndHours=i-1;break;
			case BLIZZARD:if(blizzardStartHours==-1)blizzardStartHours=i;break;
			case SUN:
			case NONE:if(blizzardStartHours!=-1&&blizzardEndHours==-1)blizzardEndHours=i-1;
			if(snowStartHours!=-1&&snowEndHours==-1)snowEndHours=i-1;
				
			}
		}
	}
	public WhiteCurtainDescriptor descriptor() {
		return descriptor;
	}

    public List<ForecastFrame> getFrames(WorldClimate climate,long delta){
    	return WeatherForecast.getFrames(getFutureClimateIterator(climate,delta),ClimateResult.EMPTY, (int) (120 - delta%3));
    }
	public boolean isAffected(ChunkPos pos) {
		return WhiteCurtainFieldModel.isAffected(descriptor, pos);
	}
	public boolean isIntersected(Rect rect) {
		return WhiteCurtainFieldModel.isIntersected(descriptor, rect);
	}
	public static long getSecondsPerChunk() {
		return WhiteCurtainFieldModel.SECONDS_PER_CHUNK;
	}
	public static long getHoursPerChunk() {
		return WhiteCurtainFieldModel.HOURS_PER_CHUNK;
	}
	public long getDeltaTime(ChunkPos pos) {
		return WhiteCurtainFieldModel.deltaSeconds(descriptor, pos);
	}
	public long getDelta(ChunkPos pos) {
		return WhiteCurtainFieldModel.deltaChunks(descriptor, pos);
	}
	public long getMaxDeltaTime() {
		return WhiteCurtainFieldModel.maxDeltaSeconds(descriptor);
	}
	public int getMaxDelta() {
		return WhiteCurtainFieldModel.maxDeltaChunks(descriptor);
	}
	public boolean isInvalid(long dtime) {
		return WhiteCurtainFieldModel.isInvalid(descriptor, dtime);
	}
	public Rect getSnowRect(long dtime) {
		long climatestartEndpoint=(dtime-getMaxDeltaTime()-descriptor.climate().getStartTime())/50;
		int chunkStart=Mth.ceil((snowStartHours-climatestartEndpoint)*1f/getHoursPerChunk());
		int chunkEnd=Mth.floor((snowEndHours-climatestartEndpoint)*1f/getHoursPerChunk());
		return getPartialRect(chunkStart,chunkEnd).and(descriptor.affectedArea());
	}
	public Rect getBlizzardRect(long dtime) {
		long climatestartEndpoint=(dtime-getMaxDeltaTime()-descriptor.climate().getStartTime())/50;
		int chunkStart=Mth.ceil((blizzardStartHours-climatestartEndpoint)*1f/getHoursPerChunk());
		int chunkEnd=Mth.floor((blizzardEndHours-climatestartEndpoint)*1f/getHoursPerChunk());
		return getPartialRect(chunkStart,chunkEnd).and(descriptor.affectedArea());
	}
	public Rect getPartialRect(int chunkStart,int chunkEnd) {
		int chunkLen=chunkEnd-chunkStart;
		Rect area = descriptor.affectedArea();
		switch(descriptor.moveDirection()) {
		case NORTH:return new Rect(area.getX(),area.getY()+chunkStart,area.getW(),chunkLen);
		case SOUTH:return new Rect(area.getX(),area.getY2()-chunkEnd,area.getW(),chunkLen);
		case WEST:return new Rect(area.getX()+chunkStart,area.getY(),chunkLen,area.getH());
		case EAST:return new Rect(area.getX2()-chunkEnd,area.getY(),chunkLen,area.getH());
		}
		return Rect.NONE;
	}
	public short[] getFrames(WorldClimate wclimate,long seconds,ChunkPos pos) {
		long dtime=(seconds-getDeltaTime(pos)-descriptor.climate().getStartTime())/50;
		WeatherForecast fcc=forecastCache.get((int) getDelta(pos));
		if(fcc.shouldUpdateNewFrames(seconds/50))
			fcc.updateFrames(seconds/50, this.getFrames(wclimate,dtime));
		return fcc.getFrames();
	}
	@Override
	public String toString() {
		return descriptor.toString();
	}

	public ClimateResult getClimate(long dtime) {
		ClimateEvent climate = descriptor.climate();
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
		return WhiteCurtainFieldModel.sampleGameplay(descriptor, sec, pos);
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
    	Direction dir=Direction.SOUTH;
    	{
	    	WhiteCurtainInfo wci=new WhiteCurtainInfo(new Rect(10,10,20,20),dir,ice);
	    	final int thours=2000;
	    	for(int i=0;i<30;i++) {
	    		System.out.println(new ChunkPos(i,i)+"="+wci.getClimate(thours,new ChunkPos(i,i)));
	    	}
	    	System.out.println(wci.getBlizzardRect(thours).toCornerString());
    	}
    	//StringBuilder sb=new StringBuilder();
    	//StreamSupport.stream(wci.getFutureClimateIterator((thours-wci.getDeltaTime(new ChunkPos(15,16)))/50).spliterator(), false).map(String::valueOf).forEach(t->sb.append(t).append("\n"));
    	//System.out.println(sb);
    }

	
}
