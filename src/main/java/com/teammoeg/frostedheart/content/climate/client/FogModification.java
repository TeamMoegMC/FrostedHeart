/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.client;

import com.teammoeg.frostedheart.FHConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * This class is responsible for modifying the fog in the game.
 *
 * @author alcatrazEscapee
 * adapted by TeamMoeg
 *
 * License: MIT
 */
public class FogModification {
    private static float prevFogDensity = -1.0F;
    private static long prevFogTick = -1L;

    public static void renderFogColors(ViewportEvent.ComputeFogColor event) {
        Camera camera = event.getCamera();
        double partialTick = event.getPartialTick();

        Entity var4 = camera.getEntity();
        if (var4 instanceof Player player) {
            if (camera.getFluidInCamera() == FogType.NONE && prevFogDensity > 0.0F) {
                float angle = player.level().getSunAngle((float) partialTick);
                float height = Mth.cos(angle);
                float delta = Mth.clamp((height + 0.4F) / 0.8F, 0.0F, 1.0F);
                int colorDay = Integer.parseInt(FHConfig.CLIENT.fogColorDay.get().toString(), 16);
                int colorNight = Integer.parseInt(FHConfig.CLIENT.fogColorNight.get().toString(), 16);
                float red = (float)(colorDay >> 16 & 255) * delta + (float)(colorNight >> 16 & 255) * (1.0F - delta);
                float green = (float)(colorDay >> 8 & 255) * delta + (float)(colorNight >> 8 & 255) * (1.0F - delta);
                float blue = (float)(colorDay & 255) * delta + (float)(colorNight & 255) * (1.0F - delta);
                event.setRed(red / 255.0F);
                event.setGreen(green / 255.0F);
                event.setBlue(blue / 255.0F);
            }
        }

    }

    public static void renderFogDensity(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        Entity var3 = camera.getEntity();
        if (var3 instanceof Player player) {
            long thisTick = Util.getMillis();
            boolean firstTick = prevFogTick == -1L;
            float deltaTick = firstTick ? 1.0E10F : (float)(thisTick - prevFogTick) * 1.5E-4F;
            prevFogTick = thisTick;
            float expectedFogDensity = 0.0F;
            Level level = player.level();
            Biome biome = (Biome)level.getBiome(camera.getBlockPosition()).value();
            if (level.isRaining() && biome.coldEnoughToSnow(camera.getBlockPosition())) {
                int light = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition()));
                expectedFogDensity = Mth.clampedMap((float)light, 0.0F, 15.0F, 0.0F, 1.0F);
            }

            if (expectedFogDensity > prevFogDensity) {
                prevFogDensity = Math.min(prevFogDensity + 4.0F * deltaTick, expectedFogDensity);
            } else if (expectedFogDensity < prevFogDensity) {
                prevFogDensity = Math.max(prevFogDensity - deltaTick, expectedFogDensity);
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
                event.setCanceled(true);
            }
        }

    }
}
