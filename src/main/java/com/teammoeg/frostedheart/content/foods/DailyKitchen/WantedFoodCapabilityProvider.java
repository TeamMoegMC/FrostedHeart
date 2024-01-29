package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WantedFoodCapabilityProvider implements ICapabilitySerializable<CompoundNBT> {
    private IWantedFoodCapability capability;

    @Nonnull
    private IWantedFoodCapability getOrCreateCapability(){
        if(capability == null) {
            capability = new WantedFoodCapability();
        }
        return capability;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == DailyKitchen.WANTED_FOOD_CAPABILITY ? LazyOptional.of(this::getOrCreateCapability).cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        getOrCreateCapability().deserializeNBT(nbt);
    }
}
