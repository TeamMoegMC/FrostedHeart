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

package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.util.FHGameRule;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {

    @Shadow
    @Final
    public IServerWorldInfo serverWorldInfo;

    protected MixinServerWorld(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType,
                               Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

    /**
     * @author khjxiaogu
     * @reason Not allow sleep over weather
     */
    @Overwrite
    private void resetRainAndThunder() {

    }

    /**
     * @author yuesha-yc
     * @reason this allows us to add our own weather logic since we disabled it
     * @see GameRulesMixin#disableWeatherCycle
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;hasSkyLight()Z"))
    private void tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (!((FHGameRule) this.getGameRules()).isWeatherCycle())//vanilla rules
            return;

        // ignore nether and end etc.
        if (!this.getDimensionType().hasSkyLight())
            return;

        // get hourly temp data
        //float currentTemp = WorldTemperature.getClimateTemperature(this);
        // System.out.println("Current Temp: " + currentTemp);

        // vanilla weather params
        int clearTime = this.serverWorldInfo.getClearWeatherTime();
        int rainTime = this.serverWorldInfo.getRainTime();
        int thunderTime = this.serverWorldInfo.getThunderTime();
        boolean isRaining = this.serverWorldInfo.isRaining();
        boolean isThundering = this.serverWorldInfo.isThundering();

        // calculate raining status and blizzard status based on our temp system
        // 'thundering' is replaced by our BlizzardRenderer
        isThundering = WorldClimate.isBlizzard(this);
        isRaining = WorldClimate.isSnowing(this)||isThundering;
        

        // To make vanilla weather commands work, we still implement the following
        // This overrides the previous calculation on isRaining and isThundering
        if (clearTime > 0) {
            --clearTime;
            isRaining = false;
            isThundering = false;
        }
        if (rainTime > 0) {
            --rainTime;
            isRaining = true;
            isThundering = false;
        }
        if (thunderTime > 0) {
            --thunderTime;
            isRaining = true;
            isThundering = true;
        }

        this.serverWorldInfo.setThunderTime(thunderTime);
        this.serverWorldInfo.setRainTime(rainTime);
        this.serverWorldInfo.setClearWeatherTime(clearTime);
        this.serverWorldInfo.setThundering(isThundering);
        this.serverWorldInfo.setRaining(isRaining);
    }

}
