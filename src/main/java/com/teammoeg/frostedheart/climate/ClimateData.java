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

    private boolean isBlizzard;
    private int blizzardTime;

    public ClimateData() {
        capability = LazyOptional.of(() -> this);
        isBlizzard = false;
        blizzardTime = 0;
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
