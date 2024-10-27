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

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
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

    @Shadow
    @Final
    private MinecraftServer server;


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
        // Vanilla game rule logic
        if (!((BooleanValue)(((GameRuleAccessor)this.getGameRules()).getRules().get(GameRules.RULE_WEATHER_CYCLE))).get())
            return;

        // Only in overworld
        if (!this.dimensionType().hasSkyLight())
            return;

        boolean flag = this.serverLevelData.isRaining();

        // vanilla weather params: these are only possible to set with vanilla weather commands,
        // as we disable vanilla weather logic and use our own from WorldClimate!
        int clearTime = this.serverLevelData.getClearWeatherTime();
        /*
            rainTime and thunderTime are the same if set by vanilla weather command:
            /weather rain <time> or /weather thunder <time>
            See ServerLevel#setWeatherParameters
         */
        int rainTime = this.serverLevelData.getRainTime();
        int thunderTime = this.serverLevelData.getThunderTime();
        // if you use /weather rain <time>
        boolean isRaining = this.serverLevelData.isRaining();
        // if you use /weather thunder <time>
        boolean isThundering = this.serverLevelData.isThundering();

        // calculate raining status and blizzard status based on our temp system
        // 'thundering' is replaced by our BlizzardRenderer
        boolean climateBlizzard = WorldClimate.isBlizzard(this);
        boolean climateSnowing = WorldClimate.isSnowing(this) || climateBlizzard;


        // To make vanilla weather commands work, we still implement the following
        // This overrides the previous calculation from WorldClimate
        // to create temporary weather periods
        if (clearTime > 0) {
            --clearTime;
            climateBlizzard = false;
            climateSnowing = false;
        }
        if (rainTime > 0 && isRaining) {
            --rainTime;
            climateSnowing = true;
            climateBlizzard = false;
        }
        if (thunderTime > 0 && isThundering) {
            --thunderTime;
            climateSnowing = true; // Note we must set climate snowing to be true here to make thundering work
            climateBlizzard = true;
        }

        this.serverLevelData.setThunderTime(thunderTime);
        this.serverLevelData.setRainTime(rainTime);
        this.serverLevelData.setClearWeatherTime(clearTime);
        this.serverLevelData.setThundering(climateBlizzard);
        this.serverLevelData.setRaining(climateSnowing);

        // Vanilla 1.20 Code Start
        this.oThunderLevel = this.thunderLevel;
        if (this.levelData.isThundering()) {
            this.thunderLevel += 0.01F;
        } else {
            this.thunderLevel -= 0.01F;
        }

        this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
        this.oRainLevel = this.rainLevel;
        if (this.levelData.isRaining()) {
            this.rainLevel += 0.01F;
        } else {
            this.rainLevel -= 0.01F;
        }

        this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);

        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        if (flag != this.isRaining()) {
            if (flag) {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F), this.dimension());
            } else {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F), this.dimension());
            }

            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        // Vanilla 1.20 Code End
    }

}
