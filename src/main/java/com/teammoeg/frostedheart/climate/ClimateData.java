package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

public class ClimateData implements ICapabilitySerializable<CompoundNBT> {
    @CapabilityInject(ClimateData.class)
    public static Capability<ClimateData> CAPABILITY;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "climate_data");

    public static void setup() {
        CapabilityManager.INSTANCE.register(ClimateData.class, new Capability.IStorage<ClimateData>() {
            public INBT writeNBT(Capability<ClimateData> capability,ClimateData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<ClimateData> capability,ClimateData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, () -> {
            return new ClimateData();
        });
    }

    private final LazyOptional<ClimateData> capability;

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

    private float getCurrentHourTemp(long currentGameTick) {
        //TODO: need initial tempEvent
        TempEvent head = tempEventStream.peek();
        if (head != null) {
            while (currentGameTick > head.calmEndTime) {
                //TODO: add random temp event
                tempEventStream.remove();
                long prevEnd = head.calmEndTime;
                while (tempEventStream.size() <= 2) {
                    tempEventStream.add(TempEvent.getTempEvent(prevEnd));
                    prevEnd = tempEventStream.peek().calmEndTime;
                }
                head = tempEventStream.peek();
            }
            TempEvent newHeadEvent = tempEventStream.peek();
            return newHeadEvent.getHourTemp(currentGameTick);
        }
		//TODO: throw exception
		return 0F;
    }

    /**
     * Get temperature at given time.
     * Grow tempEventStream as needed.
     * No trimming will be performed.
     * To perform trimming,
     * use {@link #tempEventStreamTrim(long) tempEventStreamTrim}.
     * @param time given in seconds
     * @return temperature at given time
     */
    public float getTemp(long time) {
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
        // TODO: get current time from WorldClockSource
        long currentTime = 0;
        if (tempEventStream.isEmpty()) {
            tempEventStream.add(TempEvent.getTempEvent(currentTime));
        }

        TempEvent head = tempEventStream.getLast();
        while (head.calmEndTime < time) {
            tempEventStream.add(head = TempEvent.getTempEvent(head.calmEndTime));
        }
    }

    // Trims all TempEvents that end before given time
    public void tempEventStreamTrim(long time) {
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

    /**
     * Called every hour (1000 ticks) from ForgeEvents#onServerTick()
     * to update the hourTemp;
     * @param currentGameTick should be multiple of 1000
     */
    public void updateHourTemp(long currentGameTick) {
        this.hourTemp = getCurrentHourTemp(currentGameTick);
    }

    /**
     * Lightweight getter that can be called every tick from client
     * @return the baseline temperature at this hour
     */
    public float getTemp() {
        return hourTemp;
    }

    private boolean isBlizzard;
    private int blizzardTime;
    private LinkedList<TempEvent> tempEventStream;
    private float hourTemp;

    public ClimateData() {
        capability = LazyOptional.of(() -> this);
        isBlizzard = false;
        blizzardTime = 0;
        tempEventStream = new LinkedList<>();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAPABILITY ? capability.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("blizzardTime", blizzardTime);
        nbt.putBoolean("isBlizzard", isBlizzard);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        blizzardTime = nbt.getInt("blizzardTime");
        isBlizzard = nbt.getBoolean("isBlizzard");
    }
}
