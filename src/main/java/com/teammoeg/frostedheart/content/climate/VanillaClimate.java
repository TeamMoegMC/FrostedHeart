/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.noise.INoise1D;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;

/**
 * No longer use
 * This is only valid in the overworld!
 */
@Deprecated
public final class VanillaClimate {
    /**
     * Constants for temperature calculation. Do not reference these directly, they do not have much meaning outside the context they are used in
     */
    public static final float MINIMUM_TEMPERATURE_SCALE = -24f;
    public static final float MAXIMUM_TEMPERATURE_SCALE = 30f;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_AMPLITUDE = -6.5f;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_MEAN = 13.5f;
    public static final float REGIONAL_TEMPERATURE_SCALE = 2f;
    public static final float REGIONAL_RAINFALL_SCALE = 50f;

    /**
     * Magic numbers. These probably mean something
     */
    public static final float MINIMUM_RAINFALL = 0f;
    public static final float MAXIMUM_RAINFALL = 500f;
    public static final float SNOW_MELT_TEMPERATURE = 4f;
    public static final float SNOW_STACKING_TEMPERATURE = -4f;

    //private static final Random RANDOM = new Random(); // Used for daily temperature variations

    public static float calculateMonthlyTemperature(int z, int y, float averageTemperature, float monthTemperatureModifier) {
        float temperatureScale = 20000;
        float monthTemperature = monthTemperatureModifier * INoise1D.triangle(LATITUDE_TEMPERATURE_VARIANCE_AMPLITUDE, LATITUDE_TEMPERATURE_VARIANCE_MEAN, 1 / (2 * temperatureScale), 0, z);
        float elevationTemperature = MathHelper.clamp((y - 63) * 0.16225f, 0, 17.822f);
        return averageTemperature + monthTemperature - elevationTemperature;
    }

    public static float calculateTemperature(BlockPos pos, float averageTemperature) {
        return calculateTemperature(pos.getZ(), pos.getY(), averageTemperature);
    }

    public static float calculateTemperature(int z, int y, float averageTemperature) {
        // Finally, add elevation based temperature
        // Internationally accepted average lapse time is 6.49 K / 1000 m, for the first 11 km of the atmosphere. Our temperature is scales the 110 m against 2750 m, so that gives us a change of 1.6225 / 10 blocks.
        float elevationTemperature = MathHelper.clamp((y - 63) * 0.16225f, 0, 17.822f);

        // Sum all different temperature values.
        return averageTemperature - elevationTemperature;
    }

    /**
     * Used to calculate the actual temperature at a world and position.
     * Will be valid when used on both logical sides.
     * MUST NOT be used by world generation, it should use {@link VanillaClimate#calculateTemperature(BlockPos, float)} instead, with the average temperature obtained through the correct chunk data source
     */
    public static float getTemperature(IWorld world, BlockPos pos) {
        return calculateTemperature(pos.getZ(), pos.getY(), ChunkHeatData.getTemperature(world, pos));
    }

    /**
     * The reverse of {@link VanillaClimate#toVanillaTemperature(float)}
     */
    public static float toActualTemperature(float vanillaTemperature) {
        return (vanillaTemperature - 0.15f) / 0.0217f;
    }

    /**
     * Calculates the temperature, scaled to vanilla like values.
     * References: 0.15 ~ 0 C (freezing point of water). Vanilla typically ranges from -0.5 to +1 in the overworld.
     * This scales 0 C -> 0.15, -30 C -> -0.51, +30 C -> 0.801
     */
    public static float toVanillaTemperature(float actualTemperature) {
        return actualTemperature * 0.0217f + 0.15f;
    }

    private VanillaClimate() {
    }
}