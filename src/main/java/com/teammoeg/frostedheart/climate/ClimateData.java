package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.LinkedList;

public class ClimateData implements ICapabilitySerializable<CompoundNBT> {

    /* Basic capability setup */

    @CapabilityInject(ClimateData.class)
    public static Capability<ClimateData> CAPABILITY;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "climate_data");
    private final LazyOptional<ClimateData> capability;

    public static void setup() {
        CapabilityManager.INSTANCE.register(ClimateData.class, new Capability.IStorage<ClimateData>() {
            public INBT writeNBT(Capability<ClimateData> capability,ClimateData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<ClimateData> capability,ClimateData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, () -> new ClimateData());
    }

    private static LazyOptional<ClimateData> getCapability(@Nullable IWorld world) {
        if (world instanceof ServerWorld) {
            return ((ServerWorld) world).getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAPABILITY ? capability.cast() : LazyOptional.empty();
    }

    /* Interface static methods that can be called from anywhere */

    /**
     * Get ClimateData attached to this world
     * @param world instance of ServerWorld
     * @return An instance of ClimateData
     */
    public static ClimateData get(ServerWorld world) {
        return getCapability(world).resolve().orElse(new ClimateData());
    }

    /**
     * Server call this each second (20 ticks) to update the clock source
     * @param world server side
     */
    public static void updateClock(ServerWorld world) {
        ClimateData data = get(world);
        data.clockSource.update(world);
    }

    /**
     * Server call this each game hour (1000 ticks) to update the hourly temperature cache
     * Removes the most recent hour temperature from the cache
     * Adds the hour temperature HOURLY_CACHE_LENGTH after the current hour to the cache
     * @param world server side
     */
    public static void updateHourlyTempStream(ServerWorld world) {
        ClimateData data = get(world);
        WorldClockSource clock = data.clockSource;
        if (data.hourlyTempStream.isEmpty()) {
            for (long i = 0; i < HOURLY_CACHE_LENGTH; i++) {
                data.hourlyTempStream.add(data.computeTemp(clock.getTimeSecs() + i * 50));
            }
        }
        data.hourlyTempStream.add(data.computeTemp(clock.getTimeSecs() + HOURLY_CACHE_LENGTH * 50));
        data.hourlyTempStream.remove();
    }

    /**
     * Server call this each game hour (1000 ticks) to trim the temperature event stream
     * Trims all TempEvents that end before current time
     * @param world server side
     */
    public static void trimTempEventStream(ServerWorld world) {
        ClimateData data = get(world);
        data.tempEventStreamTrim(data.clockSource.getTimeSecs());
    }

    /**
     * Retrieves hourly updated temperature from cache
     * Useful in client-side tick-frequency temperature rendering
     * @return temperature at current hour
     */
    public static float getTemp(ServerWorld world) {
        return getFutureTemp(world, 0);
    }

    /**
     * Retrieves hourly updated temperature from cache
     * If exceeds cache size, return 0
     * Useful in long range weather forecast
     * @param index number of hours after current hour, range: 0 - HOURLY_CACHE_LENGTH
     * @return temperature at hour at index
     */
    public static float getFutureTemp(ServerWorld world, int index) {
        ClimateData data = get(world);
        if (index < 0 || index > HOURLY_CACHE_LENGTH) {
            return 0F;
        } else {
            // initialization
            if (data.hourlyTempStream.size() < HOURLY_CACHE_LENGTH) {
                updateHourlyTempStream(world);
            }
            return data.hourlyTempStream.get(index);
        }
    }

    /* Private helper methods */

    /**
     * Get temperature at given time.
     * Grow tempEventStream as needed.
     * No trimming will be performed.
     * To perform trimming,
     * use {@link #tempEventStreamTrim(long) tempEventStreamTrim}.
     * @param time given in seconds
     * @return temperature at given time
     */
    private float computeTemp(long time) {
        tempEventStreamGrow(time);
        return tempEventStream
                .stream()
                .filter(e -> time <= e.calmEndTime && time >= e.startTime)
                .findFirst()
                .map(e -> e.getHourTemp(time))
                .get();
    }

    // Grows tempEventStream to contain temp events
    // that cover the given point of time
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

    // Trims all TempEvents that end before given time
    // time given in seconds
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

    /* Getters and Setters */

    public boolean isBlizzard() {
        return isBlizzard;
    }

    public int getBlizzardTime() {
        return blizzardTime;
    }

    public void setBlizzard(boolean blizzard) {
        isBlizzard = blizzard;
    }

    public void setBlizzardTime(int blizzardTime) {
        this.blizzardTime = blizzardTime;
    }

    /* Object structure */

    public static long HOURLY_CACHE_LENGTH = 7 * 24;
    public static long DAILY_CACHE_LENGTH = 7;

    private boolean isBlizzard;
    private int blizzardTime;
    private LinkedList<TempEvent> tempEventStream;
    private WorldClockSource clockSource;
    private LinkedList<Float> hourlyTempStream;

    public ClimateData() {
        capability = LazyOptional.of(() -> this);
        isBlizzard = false;
        blizzardTime = 0;
        tempEventStream = new LinkedList<>();
        clockSource = new WorldClockSource();
        hourlyTempStream = new LinkedList<>();
    }

    /* Serialization */

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("blizzardTime", blizzardTime);
        nbt.putBoolean("isBlizzard", isBlizzard);
        clockSource.serialize(nbt);

        ListNBT list1 = new ListNBT();
        for (TempEvent event : tempEventStream) {
            list1.add(event.serialize(new CompoundNBT()));
        }
        nbt.put("tempEventStream", list1);
        System.out.println("Serialized TES");

        ListNBT list2 = new ListNBT();
        for (float temp : hourlyTempStream) {
            list2.add(FloatNBT.valueOf(temp));
        }
        nbt.put("hourlyTempStream", list1);
        System.out.println("Serialized HTS");

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        blizzardTime = nbt.getInt("blizzardTime");
        isBlizzard = nbt.getBoolean("isBlizzard");
        clockSource.deserialize(nbt);
        ListNBT list1 = nbt.getList("tempEventStream", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list1.size(); i++) {
            TempEvent event = new TempEvent();
            event.deserialize(list1.getCompound(i));
            tempEventStream.add(event);
        }
        System.out.println("DESerialized TES");

        ListNBT list2 = nbt.getList("hourlyTempStream", Constants.NBT.TAG_FLOAT);
        for (int i = 0; i < list2.size(); i++) {
            hourlyTempStream.add(list2.getFloat(i));
        }
        System.out.println("DESerialized HTS");

    }
}
