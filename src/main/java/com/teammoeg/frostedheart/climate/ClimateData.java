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

import java.util.LinkedList;
import java.util.Queue;

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
        //TODO: need initial tempevent
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
        } else {
            //TODO: throw exception
            return 0F;
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
    private Queue<TempEvent> tempEventStream;
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
