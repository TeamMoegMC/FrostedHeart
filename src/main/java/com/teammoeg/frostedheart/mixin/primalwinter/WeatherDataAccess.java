package com.teammoeg.frostedheart.mixin.primalwinter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.alcatrazescapee.primalwinter.util.WeatherData;

import net.minecraftforge.common.util.LazyOptional;

@Mixin(WeatherData.class)
public interface WeatherDataAccess {
    @Accessor(remap = false)
    LazyOptional<WeatherData> getCapability();

    @Accessor(remap = false)
    boolean getAlreadySetWorldToWinter();

    @Accessor(remap = false)
    void setAlreadySetWorldToWinter(boolean flag);
}
