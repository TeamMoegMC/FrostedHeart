/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.render;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.render.weather.ClientWeatherFrame;
import com.teammoeg.frostedheart.content.climate.render.weather.ClientWeatherState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class is responsible for modifying the fog in the game.
 *
 * @author alcatrazEscapee
 * adapted by TeamMoeg
 *
 * License: MIT
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public class FogModification {
    private static float prevFogDensity = -1.0F;
    private static long prevFogTick = -1L;

    @SubscribeEvent
    public static void renderFogColors(ViewportEvent.ComputeFogColor event) {
        Camera camera = event.getCamera();
        double partialTick = event.getPartialTick();

        Entity var4 = camera.getEntity();
        if (var4 instanceof Player player) {
            if (camera.getFluidInCamera() == FogType.NONE && prevFogDensity > 0.0F) {
                int color = snowColor(player.level(), (float)partialTick);
                event.setRed(FastColor.ARGB32.red(color) / 255.0F);
                event.setGreen(FastColor.ARGB32.green(color) / 255.0F);
                event.setBlue(FastColor.ARGB32.blue(color) / 255.0F);
            }
        }

    }

    /** Blends the configured day and night snow colours for terrain fog and spatial extinction. */
    public static int snowColor(Level level, float partialTick) {
        float height = Mth.cos(level.getSunAngle(partialTick));
        float day = Mth.clamp((height + 0.4F) / 0.8F, 0.0F, 1.0F);
        return FastColor.ARGB32.lerp(day, FHConfig.CLIENT.fogColorNight.get(), FHConfig.CLIENT.fogColorDay.get());
    }

    @SubscribeEvent
    public static void renderFogDensity(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        Entity var3 = camera.getEntity();
        if (var3 instanceof Player player) {
            float expectedFogDensity = 0.0F;
            ClientWeatherFrame weatherFrame = ClientWeatherFrame.INSTANCE;
            if (weatherFrame.ownsPrecipitation()) {
                if (weatherFrame.usesDepthExtinction()) {
                    // Only global weather belongs in camera fog. The snow band already attenuates each scene ray.
                    ClimateType global = ClientClimateData.globalClimate;
                    if (global != null && global.isSnowyOrBlizzard()) {
                        expectedFogDensity = (global.isBlizzard() ? 0.65F : 0.15F) * ClientWeatherState.INSTANCE.cameraExposure();
                    }
                } else {
                    expectedFogDensity = Math.max(weatherFrame.cameraSample().whiteoutIntensity,
                            weatherFrame.cameraSample().snowIntensity * 0.2F);
                }
                prevFogDensity = expectedFogDensity;
                prevFogTick = -1L;
            } else {
                long thisTick = Util.getMillis();
                boolean firstTick = prevFogTick == -1L;
                float deltaTick = firstTick ? 1.0E10F : (float)(thisTick - prevFogTick) * 1.5E-4F;
                prevFogTick = thisTick;
                Level level = player.level();
                Biome biome = level.getBiome(camera.getBlockPosition()).value();
                if (level.isRaining() && biome.coldEnoughToSnow(camera.getBlockPosition())) {
                    int light = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition()));
                    expectedFogDensity = Mth.clampedMap((float)light, 0.0F, 15.0F, 0.0F, 0.5F);
                    if (level.isThundering()) {
                        expectedFogDensity *= 2F;
                    }
                }

                if (expectedFogDensity > prevFogDensity) {
                    prevFogDensity = Math.min(prevFogDensity + 4.0F * deltaTick, expectedFogDensity);
                } else if (expectedFogDensity < prevFogDensity) {
                    prevFogDensity = Math.max(prevFogDensity - deltaTick, expectedFogDensity);
                }
            }

            if (camera.getFluidInCamera() != FogType.NONE) {
                prevFogDensity = -1.0F;
                prevFogTick = -1L;
            }

            if (prevFogDensity > 0.0F) {
                float scaledDelta = 1.0F - (1.0F - prevFogDensity) * (1.0F - prevFogDensity);
                Double fogDensity = FHConfig.CLIENT.fogDensity.get();
                float farPlaneScale = (float) Mth.lerp(scaledDelta, 1.0F, fogDensity);
                float nearPlaneScale = (float) Mth.lerp(scaledDelta, 1.0F, 0.3F * fogDensity);
                event.scaleNearPlaneDistance(nearPlaneScale);
                event.scaleFarPlaneDistance(farPlaneScale);
                if (!weatherFrame.usesDepthExtinction() && weatherFrame.ownsPrecipitation()) {
                    float visibility = weatherFrame.cameraSample().visibilityBlocks;
                    if (Float.isFinite(visibility) && visibility > 0.0F) {
                        float farPlane = Math.min(event.getFarPlaneDistance(), visibility);
                        event.setFarPlaneDistance(farPlane);
                        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), farPlane * 0.2F));
                    }
                }
                event.setCanceled(true);
            }
        }

    }
}
