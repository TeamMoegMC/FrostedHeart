/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.DayTemperatureData.HourData;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.events.CommonEvents;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Climate Data Capability attached to a world.
 * Currently, only attached to the Overworld dimension.
 * <p>
 * The overarching idea is by dividing continuous time into blocks called {@link ClimateEvent}
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
 *
 * @author yuesha-yc
 * @author khjxiaogu
 * @author JackyWangMislantiaJnirvana
 * @author Lyuuke
 */
public class WorldClimate implements NBTSerializable {

    public static final int DAY_CACHE_LENGTH = 8;

    protected LinkedList<ClimateEvent> tempEventStream;
    protected WorldClockSource clockSource;
    protected LinkedList<DayTemperatureData> dailyTempData;
    protected short[] frames = new short[40];
    protected long lastforecast;
    protected long lasthour = -1;
    protected int hourInDay = 0;
    protected DayTemperatureData daycache;
    protected long lastday = -1;

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData exists on the world, otherwise return null
     */
    @Nullable
    public static WorldClimate get(LevelAccessor world) {
        return getCapability(world).orElse(null);
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData if data exists on the world, otherwise return empty.
     */
    public static LazyOptional<WorldClimate> getCapability(@Nullable LevelAccessor world) {
        return FHCapabilities.CLIMATE_DATA.getCapability(world);
    }

    public static long getDay(LevelAccessor world) {
    	return getCapability(world).map(t->t.clockSource.getDate()).orElse(0L);
    }

    public static int getFirstHourGreaterThan(LevelAccessor world, int withinHours, float highTemp) {
        int firstHour = 0;
        for (float f : getFutureTempIterator(get(world), 0)) {
            if (f > highTemp)
                return firstHour;
            firstHour++;
            if (firstHour > withinHours) break;
        }
        return -1;
    }

    /**
     * Get the number of hours after temperature first reach below lowTemp.
     *
     * @param world       instance
     * @param withinHours within how many hours to check
     * @param lowTemp     the temperature to check
     * @return number of hours after temperature first reach below lowTemp.
     * Return -1 if such hour not found within limit.
     */
    public static int getFirstHourLowerThan(LevelAccessor world, int withinHours, float lowTemp) {
        int firstHour = 0;
        for (float f : getFutureTempIterator(get(world), 0)) {
            if (f < lowTemp)
                return firstHour;
            firstHour++;
            if (firstHour > withinHours) break;
        }
        return -1;
    }

    private static boolean getFutureBlizzard(WorldClimate data, int deltaDays, int deltaHours) {
    	if(data==null)
    		return false;
        if (deltaDays < 0 || deltaDays > DAY_CACHE_LENGTH) {
            return false;
        }
        if (deltaHours < 0 || deltaHours >= 24)
            throw new IllegalArgumentException("Hours must be in range [0,24)");
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        return data.dailyTempData.get(deltaDays + 1).isBlizzard(deltaHours);
    }

    public static Iterable<Pair<Float, ClimateType>> getFutureClimateIterator(WorldClimate data, int deltaHours) {
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate() + 1;
        long dhours = thours % 24;
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        if (ddate < 0 || dhours < 0 || ddate >= DAY_CACHE_LENGTH) return ImmutableList.of();
        return () -> new Iterator<Pair<Float, ClimateType>>() {
            int curddate = (int) ddate;
            int curdhours = (int) (dhours - 1);

            @Override
            public boolean hasNext() {
                return curddate < DAY_CACHE_LENGTH;
            }

            @Override
            public Pair<Float, ClimateType> next() {
                if (!hasNext()) return null;
                curdhours++;
                if (curdhours >= 24) {
                    curdhours = 0;
                    curddate++;
                }

                return Pair.of(data.dailyTempData.get(curddate).getTemp(curdhours), data.dailyTempData.get(curddate).getType(curdhours));
            }

        };
    }

    /**
     * Retrieves hourly updated temperature from cache
     * If exceeds cache size, return NaN
     * Useful in long range weather forecast
     *
     * @param world      world instance
     * @param deltaHours delta hours from now to forecast;
     * @return temperature at hour at index
     */
    public static float getFutureTemp(LevelAccessor world, int deltaHours) {

        return getCapability(world).map(t->t.getFutureTemp(deltaHours)).orElse(0f);
    }

    /**
     * Retrieves hourly updated temperature from cache.
     * Useful in weather forecast.
     *
     * @param world      world instance
     * @param deltaDays  delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    public static float getFutureTemp(LevelAccessor world, int deltaDays, int deltaHours) {
        return getFutureTemp(get(world), deltaDays, deltaHours);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in weather forecast
     *
     * @param data       an instance of ClimateData
     * @param deltaDays  delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    private static float getFutureTemp(WorldClimate data, int deltaDays, int deltaHours) {
    	if(data==null)
    		return 0;
        if (deltaDays < 0 || deltaDays > DAY_CACHE_LENGTH) {
            return Float.NaN;
        }
        if (deltaHours < 0 || deltaHours >= 24)
            throw new IllegalArgumentException("Hours must be in range [0,24)");
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        return data.dailyTempData.get(deltaDays + 1).getTemp(deltaHours);
    }

    /**
     * Retrieves a iterator for future temperature until end of cache
     * Useful in long range weather forecast
     * Suitable for iteration
     *
     * @param data       climate data instance
     * @param deltaHours delta hours from now to forecast;
     * @return Iterable of temperature
     */
    public static Iterable<Float> getFutureTempIterator(WorldClimate data, int deltaHours) {
    	if(data==null)return Collections.emptyList();
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate() + 1;
        long dhours = thours % 24;
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        if (ddate < 0 || dhours < 0 || ddate >= DAY_CACHE_LENGTH) return ImmutableList.of();
        return () -> new Iterator<Float>() {
            int curddate = (int) ddate;
            int curdhours = (int) (dhours - 1);

            @Override
            public boolean hasNext() {
                return curddate < DAY_CACHE_LENGTH;
            }

            @Override
            public Float next() {
                if (!hasNext()) return null;
                curdhours++;
                if (curdhours >= 24) {
                    curdhours = 0;
                    curddate++;
                }

                return data.dailyTempData.get(curddate).getTemp(curdhours);
            }

        };
    }
    
    public static long getHour(LevelAccessor world) {
        return getCapability(world).map(t->t.clockSource.getHours()).orElse(0L);
    }

    public static int getHourInDay(LevelAccessor world) {
        return getCapability(world).map(t->t.clockSource.getHourInDay()).orElse(0);
    }

    public static long getMonth(LevelAccessor world) {
        return getCapability(world).map(t->t.clockSource.getDate()).orElse(0L);
    }

    public static long getSec(LevelAccessor world) {
        return getCapability(world).map(t->t.clockSource.getTimeSecs()).orElse(0L);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     *
     * @return temperature at current hour
     */
    public static float getTemp(LevelAccessor world) {
        return getCapability(world).map(t->t.getTemp()).orElse(0f);
    }
    public static int getWind(LevelAccessor world) {
        return getCapability(world).map(t->t.getWind()).orElse(0);
    }

    public static long getWorldDay(LevelAccessor w) {
        return getCapability(w).map(t->t.getDay()).orElse(0L);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     *
     * @return temperature at current hour
     */
    public static boolean isBlizzard(LevelAccessor world) {
        return getCapability(world).map(t->t.getHourData().getType() == ClimateType.BLIZZARD).orElse(false);
    }

    public static boolean isCloudy(Level world) {
        return getCapability(world).map(t->t.getHourData().getType() == ClimateType.CLOUDY).orElse(false);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * If exceeds cache size, return NaN
     * Useful in long range weather forecast
     *
     * @param world      world instance
     * @param deltaHours delta hours from now to forecast;
     * @return temperature at hour at index
     */
    public static boolean isFutureBlizzard(LevelAccessor world, int deltaHours) {
        WorldClimate data = get(world);
        if(data==null)return false;
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate();
        long dhours = thours % 24;
        return getFutureBlizzard(data, (int) ddate, (int) dhours);
    }

    /**
     * Retrieves hourly updated temperature from cache.
     * Useful in weather forecast.
     *
     * @param world      world instance
     * @param deltaDays  delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    public static boolean isFutureBlizzard(LevelAccessor world, int deltaDays, int deltaHours) {
        return getFutureBlizzard(get(world), deltaDays, deltaHours);
    }

    public static boolean isSnowing(Level world) {
        return getCapability(world).map(t->t.getHourData().getType() == ClimateType.SNOW).orElse(false);
    }

    public static boolean isSun(LevelAccessor world) {
        return getCapability(world).map(t->t.getHourData().getType() == ClimateType.SUN).orElse(false);
    }

    public WorldClimate() {
        tempEventStream = new LinkedList<>();
        clockSource = new WorldClockSource();
        dailyTempData = new LinkedList<>();
    }

    public void addInitTempEvent(ServerLevel w) {
        this.tempEventStream.clear();
        this.dailyTempData.clear();
        long s = clockSource.secs;
//    	this.tempEventStream.add(new TempEvent(s-60*50,s-45*50,-5,s+32*50,-23,s+100*50,s+136*50,true));
        //model : 8->0 : 0->-30 : -30->-50= 1 : 2 : 2
        int f12cptime = 12 * 50;//1/2 storm period time
        long warmpeak = s + 56 * 50;//warm period time-1/4 storm period time
        long coldpeak = warmpeak + 3 * f12cptime;
        long coldend = coldpeak + f12cptime;
        //this.tempEventStream.add(new TempEvent(s-2*50,s+12*50,0,s+24*50,0,s+36*50,s+42*50,true,true));
        this.tempEventStream.add(new ClimateEvent(s, warmpeak, 8, coldpeak, -50, coldend, coldend + 72 * 50, true, true));
        lasthour = -1;
        lastday = -1;
        this.updateCache(w);
        this.updateFrames();
    }

    public void appendTempEvent(Function<Long, ClimateEvent> generator) {
        ClimateEvent head = tempEventStream.getLast();
        tempEventStream.add(generator.apply(head.calmEndTime));
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
    protected Pair<Float, ClimateType> computeTemp(long time) {
        if (time < clockSource.getTimeSecs()) return Pair.of(0f, ClimateType.NONE);
        tempEventStreamGrow(time);
        while (true) {
            Optional<Pair<Float, ClimateType>> f = tempEventStream
                    .stream()
                    .filter(e -> time <= e.calmEndTime && time >= e.startTime)
                    .findFirst()
                    .map(e -> e.getHourClimate(time));
            if (f.isPresent())
                return f.get();
            rebuildTempEventStream(time);
        }
    }




    /**
     * Used to populate daily cache.
     *
     * @param day       give in absolute date relative to clock source.
     * @param lastnoise prev noise level
     * @param lasthumid prev humidity
     * @return a newly computed instance of DayTemperatureData for the day specified.
     */
    private DayTemperatureData generateDay(long day, float lastnoise, float lasthumid,int lastWind) {
        DayTemperatureData dtd = new DayTemperatureData();
        Random rnd = new Random();
        long startTime = day * 1200;
        dtd.day = day;
        dtd.dayNoise = (float) Mth.clamp(rnd.nextGaussian() * 5 + lastnoise, -5d, 5d);
        dtd.dayHumidity = (float) Mth.clamp(rnd.nextGaussian() * 5 + lasthumid, 0d, 50d);
        for (int i = 0; i < 24; i++) {
            Pair<Float, ClimateType> temp = this.computeTemp(startTime + i * 50);
            dtd.setTemp(i, temp.getFirst()); // Removed daynoise
            dtd.setType(i, temp.getSecond());
            dtd.setWind(i, getWind(lastWind,temp.getSecond(),temp.getFirst()));
        }
        return dtd;
    }
    private int getWind(int lastWind,ClimateType climate,float temp) {
    	return 30;
    }
    public ClimateType getClimate() {
        return this.getHourData().getType();
    }

    public long getDay() {
        return clockSource.getDate();
    }

    public short[] getFrames() {
        return frames;
    }

    public List<TemperatureFrame> getFrames(int min, int max) {
        List<TemperatureFrame> frames = new ArrayList<>();
        float lastTemp = 0;

        int i = 0;//(int) (this.clockSource.getHours()%3);
        int lastLevel = 0;
        ClimateType lastType = ClimateType.NONE;
        for (Pair<Float, ClimateType> pf : getFutureClimateIterator(this, min)) {
            if (i >= max) break;
            final float f = pf.getFirst();
            final ClimateType bz = pf.getSecond();
            //System.out.println(bz+","+lastlevel+","+f);

            if (i == 0) {
                lastTemp = f;
                lastLevel = getTemperatureLevel(f);
                if (bz != ClimateType.NONE) {
                    frames.add(TemperatureFrame.weather(i, bz, lastLevel));
                }
                if (lastLevel > 0) {
                    frames.add(TemperatureFrame.increase(i, lastLevel));
                } else if (lastLevel < 0) {
                    frames.add(TemperatureFrame.decrease(i, lastLevel));
                }
                i++;
                continue;
            }

            if (lastType.typeId != bz.typeId) {
                switch (bz) {
                    case SNOW_BLIZZARD:
                    case BLIZZARD:
                        lastLevel = -7;
                    default:
                        lastLevel = getTemperatureLevel(f);
                }
                frames.add(TemperatureFrame.weather(i, bz, lastLevel));
                lastType = bz;
                lastTemp = f;
            } else if (lastType == ClimateType.BLIZZARD || lastType == ClimateType.SNOW_BLIZZARD) {

            } else if (lastTemp > f) {//when temperature decreasing
                if (f < -2) {//if lower than base line
                    for (int j = WorldTemperature.BOTTOMS.length - 1; j >= -lastLevel && j >= 0; j--) {//check out its level
                        if (f < WorldTemperature.BOTTOMS[j]) {//just acrosss a level
                            lastLevel = -j - 1;
                            frames.add(TemperatureFrame.decrease(i, lastLevel));//mark as decreased
                            break;
                        }
                    }

                } else if (f <= 0 + WorldTemperature.WARM_PERIOD_LOWER_PEAK - 3) {//check out if its just go back to calm
                    if (lastLevel > 0) {
                        lastLevel = 0;
                        frames.add(TemperatureFrame.calm(i, 0));
                    }
                } else if (f <= WorldTemperature.WARM_PERIOD_PEAK - 2) {//check out if its just go down from level 2
                    if (lastLevel > 1) {
                        lastLevel = 1;
                        frames.add(TemperatureFrame.calm(i, 1));
                    }
                }
            } else if (f > lastTemp) {//when temperature increasing

                if (f > WorldTemperature.WARM_PERIOD_PEAK - 2) {
                    if (lastLevel < 2) {
                        lastLevel = 2;
                        frames.add(TemperatureFrame.increase(i, 2));
                    }
                } else if (f > 0 + WorldTemperature.WARM_PERIOD_LOWER_PEAK - 3) {
                    if (lastLevel < 1) {
                        lastLevel = 1;
                        frames.add(TemperatureFrame.increase(i, 1));
                    }
                } else if (f >= -2) {
                    if (lastLevel < 0) {
                        lastLevel = 0;
                        frames.add(TemperatureFrame.calm(i, 0));
                    }
                } else if (lastLevel < 0) {//if lower than base line
                    for (int j = WorldTemperature.BOTTOMS.length - 1; j >= -lastLevel && j >= 0; j--) {//check out its level
                        if (f < WorldTemperature.BOTTOMS[j]) {//just acrosss a level
                            lastLevel = -j - 1;
                            frames.add(TemperatureFrame.calm(i, lastLevel));//mark as decreased
                            break;
                        }
                    }

                }
            }
            i++;
        }
        return frames;
    }

    public float getFutureTemp(int deltaHours) {
        long thours = this.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - this.clockSource.getDate();
        long dhours = thours % 24;
        if (dhours < 0) return 0;
        return getFutureTemp(this, (int) ddate, (int) dhours);
    }

    public HourData getHourData() {
    	if(daycache!=null)
    		return daycache.getData(hourInDay);
    	return new HourData(ClimateType.NONE);
    }

    public long getSec() {
        return clockSource.getTimeSecs();
    }

    public float getTemp() {
    	if(daycache!=null)
    		return daycache.getTemp(hourInDay);
    	return 0;
    }
    public int getWind() {
    	if(daycache!=null)
    		return daycache.getWind(hourInDay);
    	return 0;
    }
    private int getTemperatureLevel(float temp) {
        if (temp >= WorldTemperature.WARM_PERIOD_PEAK - 2) {
            return 2;
        } else if (temp >= WorldTemperature.WARM_PERIOD_LOWER_PEAK - 3) {
            return 1;
        } else if (temp <= -2) {
            for (int j = WorldTemperature.BOTTOMS.length - 1; j >= 0; j--) {//check out its level
                float b = WorldTemperature.BOTTOMS[j];
                if (temp < b) {//just acrosss a level
                    return -j - 1;
                }
            }
        }
        return 0;
    }

    /**
     * Populate daily cache to DAY_CACHE_LENGTH.
     */
    protected void populateDays() {
        if (dailyTempData.isEmpty()) {
            dailyTempData.offer(generateDay(clockSource.getDate(), 0, 0,30));
        }
        while (dailyTempData.size() <= DAY_CACHE_LENGTH) {
            DayTemperatureData last = dailyTempData.peekLast();
            dailyTempData.offer(generateDay(last.day + 1, last.dayNoise, last.dayHumidity,last.getWind(23)));
        }
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

    public void rebuildCache(ServerLevel w) {
        this.dailyTempData.clear();
        lasthour = -1;
        lastday = -1;
        this.populateDays();
        this.updateCache(w);
        this.updateFrames();
    }

    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     * TODO: need clarification from @JackyWang
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void rebuildTempEventStream(long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.
        FHMain.LOGGER.error("Temperature Data corrupted, rebuilding temperature data");
        long currentTime = clockSource.getTimeSecs();
        if (tempEventStream.isEmpty() || tempEventStream.getFirst().startTime > currentTime) {
            tempEventStream.clear();
            tempEventStream.add(ClimateEvent.getClimateEvent(currentTime));
        }

        ClimateEvent head = tempEventStream.getFirst();
        tempEventStream.clear();
        tempEventStream.add(head);
        while (head.calmEndTime < time) {
            tempEventStream.add(head = ClimateEvent.getClimateEvent(head.calmEndTime));
        }
    }

    public void resetTempEvent(ServerLevel w) {
        this.tempEventStream.clear();
        this.dailyTempData.clear();
        lasthour = -1;
        lastday = -1;
        this.populateDays();
        this.updateCache(w);
        this.updateFrames();
    }


    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamGrow(long time) {
        // If tempEventStream becomes empty for some reason,
        // start generating TempEvent from current time.

        long currentTime = clockSource.getTimeSecs();
        if (tempEventStream.isEmpty()) {
            tempEventStream.add(ClimateEvent.getClimateEvent(currentTime));
        }

        ClimateEvent head = tempEventStream.getLast();
        while (head.calmEndTime < time) {
            tempEventStream.add(head = ClimateEvent.getClimateEvent(head.calmEndTime));
        }
    }

    /**
     * Trims all TempEvents that end before given time.
     *
     * @param time given in absolute seconds relative to clock source.
     */
    protected void tempEventStreamTrim(long time) {
        ClimateEvent head = tempEventStream.peek();
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

    @Override
    public String toString() {
        return "{tempEventStream=\n" + tempEventStream.stream().map(Object::toString).collect(Collectors.joining(",")) + ",\n clockSource=" + clockSource
                + ",\n daycache=" + dailyTempData.stream().flatMap(t -> Arrays.stream(t.hourData)).map(HourData::getTemp).map(Object::toString).reduce("", (a, b) -> a + b + ",") + ",\n frames=" + IntStream.range(0, frames.length).mapToObj(i -> frames[i]).map(TemperatureFrame::unpack).map(String::valueOf).collect(Collectors.joining(",")) + "}";
    }

    /**
     * Trims all TempEvents that end before current time and a day.
     * Called every second in server world tick loop.
     */
    public void trimTempEventStream() {
        this.tempEventStreamTrim(this.clockSource.getTimeSecs() - 1200);
    }

    /**
     * Check and refresh whole cache.
     * Sync updated data to client each hour.
     * Called every second in server world tick loop.
     *
     * @param serverWorld must be server side.
     */
    public void updateCache(ServerLevel serverWorld) {
        long hours = clockSource.getHours();
        if (hours != lasthour) {
            long date = clockSource.getDate();
            if (date != lastday) {
                updateDayCache(date);
            }
            updateHourCache(hours);
            this.updateNewFrames();
            // Send to client if hour increases
            FHNetwork.send(PacketDistributor.DIMENSION.with(serverWorld::dimension), new FHClimatePacket(this));
        }
    }

    /* Serialization */

    /**
     * Keep the clock source going.
     * Called every second in server world tick loop.
     *
     * @param serverWorld must be server side.
     */
    public void updateClock(ServerLevel serverWorld) {
        this.clockSource.update(serverWorld);
    }

    /**
     * Update daily cache.
     *
     * @param date in absolute days given by clock source.
     */
    private void updateDayCache(long date) {
        if (dailyTempData.isEmpty()) {
            dailyTempData.offer(new DayTemperatureData(date - 1));
        }
        while (dailyTempData.peek().day < date - 1) {
            dailyTempData.poll();
            DayTemperatureData last = dailyTempData.peekLast();
            dailyTempData.offer(generateDay(last.day + 1, last.dayNoise, last.dayHumidity,last.getWind(23)));
        }
        populateDays();
        daycache = dailyTempData.get(1);
        lastday = daycache.day;
        if (daycache.day != date) {
            clockSource.setDate(daycache.day);//Clock Source goes a little slow, so update.
        }
    }

    /**
     * Present a total update for forecast data
     */
    public void updateFrames() {
        int crt = clockSource.getHourInDay();
        int delta = crt % 3;
        TemperatureFrame[] toRender = new TemperatureFrame[40];
        for (TemperatureFrame te : getFrames(0, 120 - delta)) {
            int renderIndex = (te.dhours + delta) / 3;
            if (renderIndex >= 40) break;
            TemperatureFrame prev = toRender[renderIndex];
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
        lastforecast = clockSource.getHours() + 120 - delta;
        int i = 0;
        for (TemperatureFrame tf : toRender) {
            if (tf != null)
                frames[i++] = tf.packNoHour();
            else
                frames[i++] = 0;
        }
    }

    /**
     * Update hour cache.
     *
     * @param hours in absolute hours relative to clock source.
     */
    private void updateHourCache(long hours) {

        lasthour = hours;
        hourInDay = clockSource.getHourInDay();
    }

    /**
     * Present a minor update for forecast data
     */
    public boolean updateNewFrames() {
        long cur = clockSource.getHours();
        if (cur >= lastforecast) {//times goes too fast.
            updateFrames();
            return true;
        }
        int crt = clockSource.getHourInDay();
        int delta = crt % 3;
        int from = (int) (lastforecast - cur);
        int to = 120 - delta;
        if (to - from < 3) return false;
        updateFrames();
        return true;
    }
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
        clockSource.serialize(nbt);
        nbt.put("tempEventStream", CodecUtil.toNBTList(tempEventStream, ClimateEvent.CODEC));
        nbt.put("hourlyTempStream", CodecUtil.toNBTList(dailyTempData, DayTemperatureData.CODEC));
	}

	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
        clockSource.deserialize(nbt);
        tempEventStream.clear();
        tempEventStream.addAll(CodecUtil.fromNBTList(nbt.getList("tempEventStream", Tag.TAG_COMPOUND), ClimateEvent.CODEC));
        dailyTempData.clear();
        dailyTempData.addAll(CodecUtil.fromNBTList(nbt.getList("hourlyTempStream", Tag.TAG_COMPOUND), DayTemperatureData.CODEC));
        readCache();
	}
}
