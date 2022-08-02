package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.events.CommonEvents;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.climate.FHClimatePacket;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;

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
 * To provide performance, we introduced a cache system for temperature data for {@link #DAY_CACHE_LENGTH} days.
 * Hence, the cache is updated each second on server side:
 * {@link CommonEvents#onServerTick(TickEvent.WorldTickEvent)}
 * <p>
 * @author yuesha-yc
 * @author khjxiaogu
 * @author JackyWangMislantiaJnirvana
 * @author Lyuuke
 */
public class ClimateData implements ICapabilitySerializable<CompoundNBT> {

    @CapabilityInject(ClimateData.class)
    public static Capability<ClimateData> CAPABILITY;
    private final LazyOptional<ClimateData> capability;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "climate_data");
    public static final int DAY_CACHE_LENGTH = 7;

    private LinkedList<TempEvent> tempEventStream;
    private WorldClockSource clockSource;
    private LinkedList<DayTemperatureData> dailyTempData;

    private float hourcache = 0;
    private long lasthour = -1;
    private DayTemperatureData daycache;
    private long lastday = -1;

    public ClimateData() {
        capability = LazyOptional.of(() -> this);
        tempEventStream = new LinkedList<>();
        clockSource = new WorldClockSource();
        dailyTempData = new LinkedList<>();
    }

    /**
     * Setup capability's serialization to disk.
     */
    public static void setup() {
        CapabilityManager.INSTANCE.register(ClimateData.class, new Capability.IStorage<ClimateData>() {
            public INBT writeNBT(Capability<ClimateData> capability, ClimateData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<ClimateData> capability, ClimateData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, ClimateData::new);
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData if data exists on the world, otherwise return empty.
     */
    private static LazyOptional<ClimateData> getCapability(@Nullable IWorld world) {
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
    public static ClimateData get(IWorld world) {
        return getCapability(world).resolve().orElse(new ClimateData());
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     *
     * @return temperature at current hour
     */
    public static float getTemp(IWorld world) {
        return get(world).hourcache;
    }

    /**
     * Retrieves hourly updated temperature from cache.
     * Useful in long range weather forecast.
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
     * Retrieves hourly updated temperature from cache
     * Useful in long range weather forecast
     *
     * @param data an instance of ClimateData
     * @param deltaDays delta days from now to forecast
     * @param deltaHours in that day to forecast
     * @return temperature at hour at index. If exceeds cache size, return NaN.
     */
    private static float getFutureTemp(ClimateData data, int deltaDays, int deltaHours) {
        if (deltaDays < 0 || deltaDays > DAY_CACHE_LENGTH) {
            return Float.NaN;
        }
        if (deltaHours < 0 || deltaHours >= 24)
            throw new IllegalArgumentException("Hours must be in range [0,24)");
        if (data.dailyTempData.size() <= DAY_CACHE_LENGTH) { //this rarely happens, but for safety
            data.populateDays();
        }
        return data.dailyTempData.get(deltaDays).getTemp(deltaHours);
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
        ClimateData data = get(world);
        long thours = data.clockSource.getHours() + deltaHours;
        long ddate = thours / 24 - data.clockSource.getDate();
        long dhours = thours % 24;
        return getFutureTemp(data, (int) ddate, (int) dhours);
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
            // Send to client if hour increases
            // PacketHandler.send(PacketDistributor.DIMENSION.with(serverWorld::getDimensionKey), new FHClimatePacket(this));
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
     * Trims all TempEvents that end before current time.
     * Called every second in server world tick loop.
     */
    public void trimTempEventStream() {
        this.tempEventStreamTrim(this.clockSource.getTimeSecs());
    }

    /**
     * Read cache during serialization.
     */
    private void readCache() {
        long hours = clockSource.getHours();
        long date = clockSource.getDate();
        updateDayCache(date);
        updateHourCache(hours);
    }

    /**
     * Update daily cache.
     * @param date in absolute days given by clock source.
     */
    private void updateDayCache(long date) {
        if (dailyTempData.isEmpty()) {
            dailyTempData.offer(generateDay(date, 0, 0));
        }
        while (dailyTempData.peek().day < date) {
            dailyTempData.poll();
            DayTemperatureData last = dailyTempData.peekLast();
            dailyTempData.offer(generateDay(last.day + 1, last.dayNoise, last.dayHumidity));
        }
        populateDays();
        daycache = dailyTempData.peek();
        lastday = daycache.day;
        if (daycache.day != date) {
            clockSource.setDate(daycache.day);//Clock Source goes a little slow, so update.
        }
    }

    /**
     * Populate daily cache to DAY_CACHE_LENGTH.
     */
    private void populateDays() {
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
        hourcache = daycache.getTemp(clockSource);
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
            dtd.setHourTemp(i, this.computeTemp(startTime + i * 50)); // Removed daynoise
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
    private float computeTemp(long time) {
        if (time < clockSource.getTimeSecs()) return 0;
        tempEventStreamGrow(time);
        while (true) {
            Optional<Float> f = tempEventStream
                    .stream()
                    .filter(e -> time <= e.calmEndTime && time >= e.startTime)
                    .findFirst()
                    .map(e -> e.getHourTemp(time));
            if (f.isPresent())
                return f.get();
            rebuildTempEventStream(time);
        }
    }

    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     * @param time given in absolute seconds relative to clock source.
     */
    private void tempEventStreamGrow(long time) {
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

    /**
     * Grows tempEventStream to contain temp events that cover the given point of time.
     * TODO: need clarification from @JackyWang
     * @param time given in absolute seconds relative to clock source.
     */
    private void rebuildTempEventStream(long time) {
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
    private void tempEventStreamTrim(long time) {
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
}
