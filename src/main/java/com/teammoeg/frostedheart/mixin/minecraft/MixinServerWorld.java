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

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.climate.WorldClimate;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
/**
 * Weather modify
 * */
@Mixin(ServerLevel.class)
public abstract class MixinServerWorld extends Level {

    protected MixinServerWorld(WritableLevelData pLevelData, ResourceKey<Level> pDimension, RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
		Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed, int pMaxChainedNeighborUpdates) {
		super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug, pBiomeZoomSeed, pMaxChainedNeighborUpdates);
	}

	@Shadow
    @Final
    public ServerLevelData serverLevelData;


    /**
     * @author khjxiaogu
     * @reason Not allow sleep over weather
     */
    @Overwrite
    private void resetWeatherCycle() {

    }
    
    /**
     * @author yuesha-yc
     * @reason this allows us to add our own weather logic since we disabled it
     * @see GameRulesMixin#disableWeatherCycle
     */
    @Overwrite
    private void advanceWeatherCycle() {
        if (!((BooleanValue)(((GameRuleAccessor)this.getGameRules()).getRules().get(GameRules.RULE_WEATHER_CYCLE))).get())//vanilla rules
            return;

        // ignore nether and end etc.
        if (!this.dimensionType().hasSkyLight())
            return;
        // get hourly temp data
        //float currentTemp = WorldTemperature.getClimateTemperature(this);
        // System.out.println("Current Temp: " + currentTemp);

        // vanilla weather params
        int clearTime = this.serverLevelData.getClearWeatherTime();
        int rainTime = this.serverLevelData.getRainTime();
        int thunderTime = this.serverLevelData.getThunderTime();
        boolean isRaining = this.serverLevelData.isRaining();
        boolean isThundering = this.serverLevelData.isThundering();

        // calculate raining status and blizzard status based on our temp system
        // 'thundering' is replaced by our BlizzardRenderer
        isThundering = WorldClimate.isBlizzard(this);
        isRaining = WorldClimate.isSnowing(this) || isThundering;


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

        this.serverLevelData.setThunderTime(thunderTime);
        this.serverLevelData.setRainTime(rainTime);
        this.serverLevelData.setClearWeatherTime(clearTime);
        this.serverLevelData.setThundering(isThundering);
        this.serverLevelData.setRaining(isRaining);
    }

}
