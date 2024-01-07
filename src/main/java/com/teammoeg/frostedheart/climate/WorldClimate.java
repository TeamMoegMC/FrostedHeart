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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.climate.DayTemperatureData.HourTemperatureData;
import com.teammoeg.frostedheart.climate.data.FHDataManager;
import com.teammoeg.frostedheart.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.events.CommonEvents;
import com.teammoeg.frostedheart.mixin.minecraft.MixinServerWorld;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * Climate Data Capability attached to a world.
 * Currently, only attached to the Overworld dimension.
 * <p>
 * The overarching idea is by dividing continuous time into blocks called {@link TempEvent}
 * Each temperature event is either a cold period, calm period, or warm period.
 * The temperature event stream will grow and trim automatically {@link #trimTempEventStream()}
 * <p>
 * Note that the temperature provided here is not layered with environment temperature like
 * dimension, biome, block, light level, or time in day (sunshine).
 * We only provide a baseline climate temperature controlled by temperature events.
 * The granularity of temperature is by game hour (50 seconds, or 1000 ticks) to ensure performance.
 * <p>
 * Users should use the explicit interfaces with either a client side or server side world instance:
 * <p>
 * {@link #getTemp(IWorld world)}
 * returns the climate temperature at current hour.
 * <p>
 * {@link #getFutureTemp(IWorld world, int deltaHours)}
 * returns the climate temperature after deltaHours.
 * <p>
 * {@link #getFutureTemp(IWorld world, int deltaDays, int deltaHours)}
 * returns the climate temperature after deltaDays and deltaHours.
 * <p>
 * To ensure time synchronization, we also implemented {@link WorldClockSource} as a universal timestamp generator.
 * Hence, the clock source is updated each second on server side:
 * {@link CommonEvents#onServerTick(TickEvent.WorldTickEvent)}
 * <p>
 * To improve performance, we introduced a cache system for temperature data for {@link #DAY_CACHE_LENGTH} days.
 * Hence, the cache is updated each second on server side:
 * {@link CommonEvents#onServerTick(TickEvent.WorldTickEvent)}
 * <p>
 * @author yuesha-yc
 * @author khjxiaogu
 * @author JackyWangMislantiaJnirvana
 * @author Lyuuke
 */
public class WorldClimate implements ICapabilitySerializable<CompoundNBT> {
	private static class NopClimateData extends WorldClimate{

		@Override
		public void updateFrames() {
		}

		@Override
		public boolean updateNewFrames() {
			return false;
		}

		@Override
		public List<TemperatureFrame> getFrames(int min, int max) {
			return ImmutableList.of();
		}

		@Override
		public void updateCache(ServerWorld serverWorld) {
		}

		@Override
		public void trimTempEventStream() {
		}

		@Override
		protected Pair<Float, Boolean> computeTemp(long time) {
			return Pair.of(0f, false);
		}

		@Override
		protected void tempEventStreamGrow(long time) {
		}

		@Override
		public void addInitTempEvent(ServerWorld w) {
		}

		@Override
		public void resetTempEvent(ServerWorld w) {
		}

		@Override
		protected void rebuildTempEventStream(long time) {
		}

		@Override
		protected void tempEventStreamTrim(long time) {
		}


		@Override
		protected void readCache() {
		}

		@Override
		protected void populateDays() {
			while (dailyTempData.size() <= DAY_CACHE_LENGTH) {
	            dailyTempData.offer(new DayTemperatureData(0));
	        }
		}

		@Override
		public String toString() {
			return "No temp data for this world";
		}
		
	}
    @CapabilityInject(WorldClimate.class)
    public static Capability<WorldClimate> CAPABILITY;
    private static final NopClimateData NOP=new NopClimateData();
    private final LazyOptional<WorldClimate> capability;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "climate_data");
    public static final int DAY_CACHE_LENGTH = 8;

    protected LinkedList<TempEvent> tempEventStream;
    protected WorldClockSource clockSource;
    protected LinkedList<DayTemperatureData> dailyTempData;
    protected short[] frames=new short[40];
    protected long lastforecast;

    protected HourTemperatureData hourcache = new HourTemperatureData();
    protected long lasthour = -1;
    protected DayTemperatureData daycache;
    protected long lastday = -1;

    public WorldClimate() {
        capability = LazyOptional.of(() -> this);
        tempEventStream = new LinkedList<>();
        clockSource = new WorldClockSource();
        dailyTempData = new LinkedList<>();
    }

    /**
     * Setup capability's serialization to disk.
     */
    public static void setup() {
        CapabilityManager.INSTANCE.register(WorldClimate.class, new Capability.IStorage<WorldClimate>() {
            public INBT writeNBT(Capability<WorldClimate> capability, WorldClimate instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<WorldClimate> capability, WorldClimate instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, WorldClimate::new);
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData if data exists on the world, otherwise return empty.
     */
    private static LazyOptional<WorldClimate> getCapability(@Nullable IWorld world) {
        if (world instanceof World) {
            return ((World) world).getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData exists on the world, otherwise return a new ClimateData instance.
     */
    public static WorldClimate get(IWorld world) {
        return getCapability(world).resolve().orElse(NOP);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     *
     * @return temperature at current hour
     */
    public static float getTemp(IWorld world) {
        return get(world).hourcache.getTemp();
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     *
     * @return temperature at current hour
     */
    public static boolean isBlizzard(IWorld world) {
        return get(world).hourcache.isBlizzard();
    }
	public static boolean isSnowing(World world) {
		return get(world).hourcache.isSnow();
	}
    /**
     * Retrieves hourly updated temperature from cache.
     * Useful in weather forecast.
     *
     * @param world world instance
     * @param deltaDays delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    public static float getFutureTemp(IWorld world, int deltaDays, int deltaHours) {
        return getFutureTemp(get(world), deltaDays, deltaHours);
    }
    /**
     * Retrieves hourly updated temperature from cache.
     * Useful in weather forecast.
     *
     * @param world world instance
     * @param deltaDays delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    public static boolean isFutureBlizzard(IWorld world, int deltaDays, int deltaHours) {
        return getFutureBlizzard(get(world), deltaDays, deltaHours);
    }
    /**
     * Retrieves hourly updated temperature from cache
     * Useful in weather forecast
     *
     * @param data an instance of ClimateData
     * @param deltaDays delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    private static float getFutureTemp(WorldClimate data, int deltaDays, int deltaHours) {
        if (deltaDays < 0 || deltaDays > DAY_CACHE_LENGTH) {
            return Float.NaN;
        }
        if (deltaHours < 0 || deltaHours >= 24)
            throw new IllegalArgumentException("Hours must be in range [0,24)");
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        return data.dailyTempData.get(deltaDays+1).getTemp(deltaHours);
    }
    private static boolean getFutureBlizzard(WorldClimate data, int deltaDays, int deltaHours) {
        if (deltaDays < 0 || deltaDays > DAY_CACHE_LENGTH) {
            return false;
        }
        if (deltaHours < 0 || deltaHours >= 24)
            throw new IllegalArgumentException("Hours must be in range [0,24)");
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        return data.dailyTempData.get(deltaDays+1).isBlizzard(deltaHours);
    }
    public static long getWorldDay(IWorld w) {
    	return get(w).getDay();
    }
    /**
     * Retrieves hourly updated temperature from cache
     * If exceeds cache size, return NaN
     * Useful in long range weather forecast
     *
     * @param world world instance
     * @param deltaHours delta hours from now to forecast;
     * @return temperature at hour at index
     */
    public static float getFutureTemp(IWorld world, int deltaHours) {

        return get(world).getFutureTemp(deltaHours);
    }
    public float getFutureTemp(int deltaHours) {
        long thours = this.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - this.clockSource.getDate();
        long dhours = thours % 24;
        if(dhours<0)return 0;
        return getFutureTemp(this, (int) ddate, (int) dhours);
    }
    /**
     * Retrieves hourly updated temperature from cache
     * If exceeds cache size, return NaN
     * Useful in long range weather forecast
     *
     * @param world world instance
     * @param deltaHours delta hours from now to forecast;
     * @return temperature at hour at index
     */
    public static boolean isFutureBlizzard(IWorld world, int deltaHours) {
        WorldClimate data = get(world);
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate();
        long dhours = thours % 24;
        return getFutureBlizzard(data, (int) ddate, (int) dhours);
    }
    /**
     * Retrieves a iterator for future temperature until end of cache
     * Useful in long range weather forecast
     * Suitable for iteration
     *
     * @param data climate data instance
     * @param deltaHours delta hours from now to forecast;
     * @return Iterable of temperature
     */
    public static Iterable<Float> getFutureTempIterator(WorldClimate data, int deltaHours) {
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate()+1;
        long dhours = thours % 24;
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        if(ddate<0||dhours<0||ddate>=DAY_CACHE_LENGTH)return ImmutableList.of();
        return new Iterable<Float>(){
			@Override
			public Iterator<Float> iterator() {
				return new Iterator<Float>() {
					int curddate=(int) ddate;
					int curdhours=(int) (dhours-1);
					@Override
					public boolean hasNext() {
						return curddate<DAY_CACHE_LENGTH;
					}

					@Override
					public Float next() {
						if(!hasNext())return null;
						curdhours++;
						if(curdhours>=24) {
							curdhours=0;
							curddate++;
						}
						
				        return data.dailyTempData.get(curddate).getTemp(curdhours);
					}
					
				};
			}
        	
        };
    }
    public static Iterable<Pair<Float,Boolean>> getFutureTempBlizzardIterator(WorldClimate data, int deltaHours) {
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate()+1;
        long dhours = thours % 24;
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        if(ddate<0||dhours<0||ddate>=DAY_CACHE_LENGTH)return ImmutableList.of();
        return new Iterable<Pair<Float,Boolean>>(){
			@Override
			public Iterator<Pair<Float,Boolean>> iterator() {
				return new Iterator<Pair<Float,Boolean>>() {
					int curddate=(int) ddate;
					int curdhours=(int) (dhours-1);
					@Override
					public boolean hasNext() {
						return curddate<DAY_CACHE_LENGTH;
					}

					@Override
					public Pair<Float, Boolean> next() {
						if(!hasNext())return null;
						curdhours++;
						if(curdhours>=24) {
							curdhours=0;
							curddate++;
						}
						
				        return Pair.of(data.dailyTempData.get(curddate).getTemp(curdhours),data.dailyTempData.get(curddate).isBlizzard(curdhours));
					}
					
				};
			}
        	
        };
    }

    /**
     * Get the number of hours after temperature first reach below lowTemp.
     *
     * @param world instance
     * @param withinHours within how many hours to check
     * @param lowTemp the temperature to check
     * @return number of hours after temperature first reach below lowTemp.
     * Return -1 if such hour not found within limit.
     */
    public static int getFirstHourLowerThan(IWorld world, int withinHours, float lowTemp) {
        int firstHour = 0;
        for (float f:getFutureTempIterator(get(world),0)) {
            if (f < lowTemp)
                return firstHour;
            firstHour++;
            if(firstHour>withinHours)break;
        }
        return -1;
    }

    public static int getFirstHourGreaterThan(IWorld world, int withinHours, float highTemp) {
        int firstHour = 0;
        for (float f:getFutureTempIterator(get(world),0)) {
            if (f > highTemp)
                return firstHour;
            firstHour++;
            if(firstHour>withinHours)break;
        }
        return -1;
    }

    /**
     * A class to represent temperature change, basically like a key frame.
     * A frame class means, the temperature increase in warm or decrease in cold. If it just goes back to calm, the increase or decrease would both be false.
     * It stores hours from now and temperature level it transform to.
     */
    public static class TemperatureFrame{
    	public enum FrameType{
    		NOP(0),
    		INCRESING(1),
    		DECREASING(2),
    		SNOWING(3),
    		STORMING(4),
    		RETREATING(5),
    		CLOUDY(6);
    		int id;
    		public boolean isIncresingEvent() {
    			return this==INCRESING;
    		}
    		public boolean isDecresingEvent() {
    			return this==DECREASING;
    		}
    		public boolean isWeatherEvent() {
    			return id>=3;
    		}
    		FrameType(int id) {this.id=id;}
    	}
    	public final FrameType type;
    	public final short dhours;
    	public final byte toState;
		public TemperatureFrame(FrameType type, int dhours, byte toState) {
			super();
			this.type=type;
			this.dhours = (short) dhours;
			this.toState = toState;
		}
		public static TemperatureFrame unpack(int val) {
			if(val==0)return null;
			return new TemperatureFrame(val);
		}
		private TemperatureFrame(int packed) {
			super();
			this.type=FrameType.values()[packed&0x7F];
			this.dhours = (short) ((packed>>16)&0xFFFF);
			this.toState = (byte) ((packed>>8)&0xFF);
		}
		private static TemperatureFrame increase(int hour,int to) {
			return new TemperatureFrame(FrameType.INCRESING,hour,(byte)to);
		}
		private static TemperatureFrame decrease(int hour,int to) {
			return new TemperatureFrame(FrameType.DECREASING,hour,(byte)to);
		}
		private static TemperatureFrame blizzard(int hour,int to) {
			return new TemperatureFrame(FrameType.STORMING,hour,(byte)to);
		}
		private static TemperatureFrame snow(int hour,int to) {
			return new TemperatureFrame(FrameType.SNOWING,hour,(byte)to);
		}
		private static TemperatureFrame sun(int hour,int to) {
			return new TemperatureFrame(FrameType.RETREATING,hour,(byte)to);
		}
		private static TemperatureFrame calm(int hour,int to) {
			return new TemperatureFrame(FrameType.NOP,hour,(byte)to);
		}
		public int pack() {
			int ret=0;
			ret|=type.ordinal();
			ret|=0x80;//exist flag
			ret|=toState<<8;
			ret|=dhours<<16;
			return ret;
		}
		/**
		 * Serialize but without hour to reduce network cost
		 * */
		public short packNoHour() {
			short ret=0;
			ret|=type.ordinal();
			ret|=0x80;//exist flag
			ret|=toState<<8;
			return ret;
		}
		@Override
		public String toString() {
			return "[type=" + type + ", dhours="
					+ dhours + ", toState=" + toState + "]";
		}
    }
    /**
     * Present a total update for forecast data
     */
    public void updateFrames() {
    	int crt=clockSource.getHourInDay();
    	int delta=crt%3;
    	TemperatureFrame[] toRender = new TemperatureFrame[40];
    	for (TemperatureFrame te : getFrames(0,120-delta)) {
    		System.out.println(te);
			int renderIndex = (te.dhours+delta)/ 3;
			if(renderIndex>=40)break;
			TemperatureFrame prev = toRender[renderIndex];
			if(prev!=null&&prev.type.isWeatherEvent()&&!te.type.isWeatherEvent())continue;//Always not omit weather event
			if (prev == null ||//previous null, overwrite
				te.type.isWeatherEvent() ||//respect weather event
				(te.toState < 0 && prev.toState >=0 ) ||//lower in value
				(te.toState < 0 && prev.toState < 0 && te.toState < prev.toState)|| //lowest in value
				(te.toState > 0 && prev.toState <= 0) || //higher in value
				(te.toState > 0 && prev.toState > 0 && te.toState > prev.toState)//highest in value
				//te.toState == 0
				) {
				toRender[renderIndex] = te;
			}
		}
    	lastforecast=clockSource.getHours()+120-delta;
    	int i=0;
    	for(TemperatureFrame tf:toRender) {
    		if(tf!=null)
    			frames[i++]=tf.packNoHour();
    		else
    			frames[i++]=0;
    	}
    }
    /**
     * Present a minor update for forecast data
     */
    public boolean updateNewFrames() {
    	long cur=clockSource.getHours();
    	if(cur>=lastforecast) {//times goes too fast.
    		updateFrames();
    		return true;
    	}
    	int crt=clockSource.getHourInDay();
    	int delta=crt%3;
    	int from=(int) (lastforecast-cur);
    	int to=120-delta;
    	if(to-from<3)return false;
    	updateFrames();
    	return true;
    }
    public List<TemperatureFrame> getFrames(int min,int max) {
    	List<TemperatureFrame> frames=new ArrayList<>();
    	float lastTemp=0;
    	
    	int i=0;//(int) (this.clockSource.getHours()%3);
    	int lastlevel=0;
    	boolean isBlizzard=false;
        for (Pair<Float, Boolean> pf:getFutureTempBlizzardIterator(this,min)) {
        	if(i>=max)break;
        	final float f=pf.getFirst();
        	final boolean bz=pf.getSecond();
        	System.out.println(bz+","+lastlevel+","+f);
        	
        	if(i==0) {
        		lastTemp=f;
        		if(f>=WorldTemperature.WARM_PERIOD_PEAK-2) {
        			lastlevel=2;
        			
        			frames.add(TemperatureFrame.increase(i,2));
            	}else if(f>=WorldTemperature.WARM_PERIOD_LOWER_PEAK-3) {
            		lastlevel=1;
            		frames.add(TemperatureFrame.increase(i,1));
            	}else if(f<=0-2) {
            		for(int j=WorldTemperature.BOTTOMS.length-1;j>=0;j--) {//check out its level
	            		float b=WorldTemperature.BOTTOMS[j];
	            		if(f<b) {//just acrosss a level
	            			lastlevel=-j-1;
	            			frames.add(TemperatureFrame.decrease(i,lastlevel));//mark as decreased
	            			break;
	            		}
            		}
            	}
        		i++;
        		continue;
        	}
        	
        	if(bz) {
        		if(!isBlizzard) {
        			isBlizzard=true;
        			frames.add(TemperatureFrame.blizzard(i,-7));//mark as decreased
        		}
        		lastlevel=-7;
        		lastTemp=f;
        	}else if(isBlizzard) {
        		isBlizzard=false;
        		frames.add(TemperatureFrame.sun(i,-3));//mark as sun
        		lastlevel=-4;
        		lastTemp=f;
        	}else if(lastTemp>f) {//when temperature decreasing
            	if(f<-2) {//if lower than base line
            		for(int j=WorldTemperature.BOTTOMS.length-1;j>=-lastlevel&&j>=0;j--) {//check out its level
	            		if(f<WorldTemperature.BOTTOMS[j]) {//just acrosss a level
	            			lastlevel=-j-1;
	            			frames.add(TemperatureFrame.decrease(i,lastlevel));//mark as decreased
	            			break;
	            		}
            		}
            		
            	}else if(f<=0+WorldTemperature.WARM_PERIOD_LOWER_PEAK-3) {//check out if its just go back to calm
            		if(lastlevel>0) {
	            		lastlevel=0;
	            		frames.add(TemperatureFrame.calm(i,0));
            		}
            	}else if(f<=WorldTemperature.WARM_PERIOD_PEAK-2) {//check out if its just go down from level 2
            		if(lastlevel>1) {
	            		lastlevel=1;
	            		frames.add(TemperatureFrame.calm(i,1));
            		}
            	}
            }else if(f>lastTemp) {//when temperature increasing
            	
            	if(f>WorldTemperature.WARM_PERIOD_PEAK-2) {
            		if(lastlevel<2) {
	            		lastlevel=2;
	            		frames.add(TemperatureFrame.increase(i,2));
            		}
            	}else if(f>0+WorldTemperature.WARM_PERIOD_LOWER_PEAK-3) {
            		if(lastlevel<1) {
            			lastlevel=1;
            			frames.add(TemperatureFrame.increase(i,1));
            		}
            	}else if(f>=0-2) {
            		if(lastlevel<0) {
	            		lastlevel=0;
	            		frames.add(TemperatureFrame.calm(i,0));
            		}
            	}else if(lastlevel<0) {//if lower than base line
            		for(int j=WorldTemperature.BOTTOMS.length-1;j>=-lastlevel&&j>=0;j--) {//check out its level
	            		if(f<WorldTemperature.BOTTOMS[j]) {//just acrosss a level
	            			lastlevel=-j-1;
	            			frames.add(TemperatureFrame.calm(i,lastlevel));//mark as decreased
	            			break;
	            		}
            		}
            		
            	}
            }
            i++;
        }
        return frames;
    }
    public static long getMonth(IWorld world) {
        return get(world).clockSource.getDate();
    }


    public static long getDay(IWorld world) {
        return get(world).clockSource.getDate();
    }
    public long getDay() {
        return clockSource.getDate();
    }
    public static long getHour(IWorld world) {
        return get(world).clockSource.getHours();
    }

    public static long getSec(IWorld world) {
        return get(world).clockSource.getTimeSecs();
    }
    public long getSec() {
        return clockSource.getTimeSecs();
    }
    public static int getHourInDay(IWorld world) {
        return get(world).clockSource.getHourInDay();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAPABILITY ? capability.cast() : LazyOptional.empty();
    }

    /**
     * Check and refresh whole cache.
     * Sync updated data to client each hour.
     * Called every second in server world tick loop.
     * @param serverWorld must be server side.
     */
    public void updateCache(ServerWorld serverWorld) {
        long hours = clockSource.getHours();
        if (hours != lasthour) {
            long date = clockSource.getDate();
            if (date != lastday) {
                updateDayCache(date);
            }
            updateHourCache(hours);
            this.updateNewFrames();
            // Send to client if hour increases
            FHPacketHandler.send(PacketDistributor.DIMENSION.with(serverWorld::getDimensionKey), new FHClimatePacket(this));
        }
    }

    /**
     * Keep the clock source going.
     * Called every second in server world tick loop.
     * @param serverWorld must be server side.
     */
    public void updateClock(ServerWorld serverWorld) {
        this.clockSource.update(serverWorld);
    }

    /**
     * Trims all TempEvents that end before current time and a day.
     * Called every second in server world tick loop.
     */
    public void trimTempEventStream() {
        this.tempEventStreamTrim(this.clockSource.getTimeSecs()-1200);
    }

    /**
     * Read cache during serialization.
     */
    protected void readCache() {
        long hours = clockSource.getHours();
        long date = clockSource.getDate();
        updateDayCache(date);
        updateHourCache(hours);
        this.updateFrames();
    }

    /**
     * Update daily cache.
     * @param date in absolute days given by clock source.
     */
    private void updateDayCache(long date) {
        if (dailyTempData.isEmpty()) {
            dailyTempData.offer(new DayTemperatureData(date-1));
        }
        while (dailyTempData.peek().day < date-1) {
            dailyTempData.poll();
            DayTemperatureData last = dailyTempData.peekLast();
            dailyTempData.offer(generateDay(last.day + 1, last.dayNoise, last.dayHumidity));
        }
        populateDays();
        daycache = dailyTempData.get(1);
        lastday = daycache.day;
        if (daycache.day != date) {
            clockSource.setDate(daycache.day);//Clock Source goes a little slow, so update.
        }
    }

    /**
     * Populate daily cache to DAY_CACHE_LENGTH.
     */
    protected void populateDays() {
        if (dailyTempData.isEmpty()) {
            dailyTempData.offer(generateDay(clockSource.getDate(), 0, 0));
        }
        while (dailyTempData.size() <= DAY_CACHE_LENGTH) {
            DayTemperatureData last = dailyTempData.peekLast();
            dailyTempData.offer(generateDay(last.day + 1, last.dayNoise, last.dayHumidity));
        }
    }

    /**
     * Update hour cache.
     * @param hours in absolute hours relative to clock source.
     */
    private void updateHourCache(long hours) {
        hourcache = daycache.getData(clockSource);
        lasthour = hours;
    }

    /**
     * Used to populate daily cache.
     * @param day give in absolute date relative to clock source.
     * @param lastnoise prev noise level
     * @param lasthumid prev humidity
     * @return a newly computed instance of DayTemperatureData for the day specified.
     */
    private DayTemperatureData generateDay(long day, float lastnoise, float lasthumid) {
        DayTemperatureData dtd = new DayTemperatureData();
        Random rnd = new Random();
        long startTime = day * 1200;
        dtd.day = day;
        dtd.dayNoise = (float) MathHelper.clamp(rnd.nextGaussian() * 5 + lastnoise, -5d, 5d);
        dtd.dayHumidity = (float) MathHelper.clamp(rnd.nextGaussian() * 5 + lasthumid, 0d, 50d);
        for (int i = 0; i < 24; i++) {
        	Pair<Float, Boolean> temp=this.computeTemp(startTime + i * 50);
            dtd.setTemp(i, temp.getFirst()); // Removed daynoise
       	 	dtd.setBlizzard(i, false);
       	 	dtd.setSnow(i, false);
            if(temp.getFirst()<=WorldTemperature.BLIZZARD_TEMPERATURE&&temp.getSecond()) {
            	dtd.setBlizzard(i, true);
            }else if(temp.getFirst()<=WorldTemperature.SNOW_TEMPERATURE&&!temp.getSecond()){
            	dtd.setSnow(i,true);
            }
        }
        return dtd;
    }

    /**
     * Get temperature at given time.
     * Grow tempEventStream as needed.
     * No trimming will be performed.
     * To perform trimming,
     * use {@link #tempEventStreamTrim(long) tempEventStreamTrim}.
     *
     * @param time given in absolute seconds relative to clock source.
     * @return temperature at given time
     */
    protected Pair<Float,Boolean> computeTemp(long time) {
        if (time < clockSource.getTimeSecs()) return Pair.of(0f,false);
        tempEventStreamGrow(time);
        while (true) {
            Optional<Pair<Float,Boolean>> f = tempEventStream
                    .stream()
                    .filter(e -> time <= e.calmEndTime && time >= e.startTime)
                    .findFirst()
                    .map(e -> Pair.of(e.getHourTemp(time),e.isBlizzard));
            if (f.isPresent())
                return f.get();
            rebuildTempEventStream(time);
        }
    }

    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamGrow(long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.

        long currentTime = clockSource.getTimeSecs();
        if (tempEventStream.isEmpty()) {
            tempEventStream.add(TempEvent.getTempEvent(currentTime));
        }

        TempEvent head = tempEventStream.getLast();
        while (head.calmEndTime < time) {
            tempEventStream.add(head = TempEvent.getTempEvent(head.calmEndTime));
        }
    }
    public void addInitTempEvent(ServerWorld w) {
    	this.tempEventStream.clear();
    	this.dailyTempData.clear();
    	long s=clockSource.secs;
//    	this.tempEventStream.add(new TempEvent(s-60*50,s-45*50,-5,s+32*50,-23,s+100*50,s+136*50,true));
    	//model : 8->0 : 0->-30 : -30->-50= 1 : 2 : 2
    	int f12cptime=12*50;//1/2 storm period time
    	long warmpeak=s+48*50;//warm period time-1/4 storm period time
    	long coldpeak=(long) (warmpeak+2.5*f12cptime);
    	long coldend=(long) (coldpeak+2*f12cptime);
    	//this.tempEventStream.add(new TempEvent(s-2*50,s+12*50,0,s+24*50,0,s+36*50,s+42*50,true,true));
		this.tempEventStream.add(new TempEvent(s+0*50,warmpeak,8,coldpeak,-50,coldend,coldend+72*50,true,true));
		lasthour = -1;
        lastday = -1;
    	this.updateCache(w);
    	this.updateFrames();
    }
    public void resetTempEvent(ServerWorld w) {
    	this.tempEventStream.clear();
    	this.dailyTempData.clear();
		lasthour = -1;
        lastday = -1;
    	this.populateDays();
    	this.updateCache(w);
    	this.updateFrames();
    }
    public void rebuildCache(ServerWorld w) {
    	this.dailyTempData.clear();
		lasthour = -1;
        lastday = -1;
    	this.populateDays();
    	this.updateCache(w);
    	this.updateFrames();
    }
    public void appendTempEvent(Function<Long,TempEvent> generator) {
        TempEvent head = tempEventStream.getLast();
        tempEventStream.add(generator.apply(head.calmEndTime));
    }
    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     * TODO: need clarification from @JackyWang
     * @param time given in absolute seconds relative to clock source.
     */
    protected void rebuildTempEventStream(long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.
        FHMain.LOGGER.error("Temperature Data corrupted, rebuilding temperature data");
        long currentTime = clockSource.getTimeSecs();
        if (tempEventStream.isEmpty() || tempEventStream.getFirst().startTime > currentTime) {
            tempEventStream.clear();
            tempEventStream.add(TempEvent.getTempEvent(currentTime));
        }

        TempEvent head = tempEventStream.getFirst();
        tempEventStream.clear();
        tempEventStream.add(head);
        while (head.calmEndTime < time) {
            tempEventStream.add(head = TempEvent.getTempEvent(head.calmEndTime));
        }
    }

    /**
     * Trims all TempEvents that end before given time.
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamTrim(long time) {
        TempEvent head = tempEventStream.peek();
        if (head != null) {
            while (head.calmEndTime < time) {
                // Protection mechanism:
                // it would be a disaster if the stream is trimmed to empty
                if (tempEventStream.size() <= 1) {
                    break;
                }
                tempEventStream.remove();
                head = tempEventStream.peek();
            }
        }
    }

    /* Serialization */

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        clockSource.serialize(nbt);

        ListNBT list1 = new ListNBT();
        for (TempEvent event : tempEventStream) {
            list1.add(event.serialize(new CompoundNBT()));
        }
        nbt.put("tempEventStream", list1);

        ListNBT list2 = new ListNBT();
        for (DayTemperatureData temp : dailyTempData) {
            list2.add(temp.serialize());
        }
        nbt.put("hourlyTempStream", list2);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        clockSource.deserialize(nbt);
        ListNBT list1 = nbt.getList("tempEventStream", Constants.NBT.TAG_COMPOUND);
        tempEventStream.clear();
        for (int i = 0; i < list1.size(); i++) {
            TempEvent event = new TempEvent();
            event.deserialize(list1.getCompound(i));
            tempEventStream.add(event);
        }

        ListNBT list2 = nbt.getList("hourlyTempStream", Constants.NBT.TAG_COMPOUND);
        dailyTempData.clear();
        for (int i = 0; i < list2.size(); i++) {
            dailyTempData.add(DayTemperatureData.read(list2.getCompound(i)));
        }
        readCache();
        
    }

	@Override
	public String toString() {
		return "{tempEventStream=\n" + String.join(",",tempEventStream.stream().map(Object::toString).collect(Collectors.toList())) + ",\n clockSource=" + clockSource + ",\n hourcache="
				+ hourcache + ",\n daycache=" + dailyTempData.stream().flatMap(t->Arrays.stream(t.hourData)).map(t->t.getTemp()).map(Object::toString).reduce("",(a,b)->a+b+",") + ",\n frames="+String.join(",",IntStream.range(0, frames.length).mapToObj(i->frames[i]).map(TemperatureFrame::unpack).map(String::valueOf).collect(Collectors.toList())) + "}";
	}

	public short[] getFrames() {
		return frames;
	}


}
