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

package com.teammoeg.frostedheart.content.climate.render.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Overworld effects that remove the vanilla sunrise fan and its fog tint while the calculated sky is drawn, and light the terrain with the sky's daylight instead of vanilla's sky light.
 */
@OnlyIn(Dist.CLIENT)
public final class CalculatedSkyEffects extends DimensionSpecialEffects.OverworldEffects {
    /**
     * The share of the lightmap color that LightTexture.updateLightTexture keeps when it blends toward gray.
     */
    private static final float VANILLA_KEPT_SHARE = 0.96F;
    private static final float[] daylight = new float[SkyAtmosphere.CHANNEL_COUNT];
    private static boolean daylightShown;
    private static float redChange;
    private static float greenChange;
    private static float blueChange;

    @Nullable
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null && CalculatedSkyRenderer.isDrawing(level)
                && minecraft.gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE) {
            return null;
        }
        return super.getSunriseColor(timeOfDay, partialTick);
    }

    /**
     * Replaces vanilla's sky light with SkyViewSet.daylightColor while the calculated sky is drawn, except during
     * lightning flashes. The change is computed at the first texel of each lightmap update and passes through vanilla's
     * blend toward gray and its darkening during boss fights, which run before this call.
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
            float skyLight, int pixelX, int pixelY, Vector3f colors) {
        if (pixelX == 0 && pixelY == 0) {
            updateDaylight(level, partialTicks, skyDarken);
        }
        if (!daylightShown) {
            return;
        }
        float brightness = LightTexture.getBrightness(level.dimensionType(), pixelY);
        colors.add(redChange * brightness, greenChange * brightness, blueChange * brightness);
    }

    private static void updateDaylight(ClientLevel level, float partialTicks, float skyDarken) {
        daylightShown = level.getSkyFlashTime() <= 0
                && CalculatedSkyRenderer.daylightColor(level, partialTicks, skyDarken, daylight);
        if (!daylightShown) {
            return;
        }
        float darkenWorld = Minecraft.getInstance().gameRenderer.getDarkenWorldAmount(partialTicks);
        redChange = (daylight[0] - SkyView.vanillaSkyLight(0, skyDarken)) * VANILLA_KEPT_SHARE
                * (1.0F - darkenWorld * 0.3F);
        greenChange = (daylight[1] - SkyView.vanillaSkyLight(1, skyDarken)) * VANILLA_KEPT_SHARE
                * (1.0F - darkenWorld * 0.4F);
        blueChange = (daylight[2] - SkyView.vanillaSkyLight(2, skyDarken)) * VANILLA_KEPT_SHARE
                * (1.0F - darkenWorld * 0.4F);
    }
}
