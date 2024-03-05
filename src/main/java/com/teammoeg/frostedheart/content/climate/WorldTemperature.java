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

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHDataManager;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class WorldTemperature {

    /**
     * Baseline temperature for temperate period.
     */
    public static final float CALM_PERIOD_BASELINE = -10;

    /**
     * The temporary uprising peak temperature of a cold period.
     */
    public static final float COLD_PERIOD_PEAK = -5;

    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_PEAK = 8;
    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_LOWER_PEAK = 6;
    /**
     * The peak temperature of a blizzard period.
     */
    public static final float BLIZZARD_WARM_PEAK = WARM_PERIOD_LOWER_PEAK;

    /**
     * The bottom temperature of a cold period.
     */
    public static final float COLD_PERIOD_BOTTOM_T1 = -5;
    public static final float COLD_PERIOD_BOTTOM_T2 = -10;
    public static final float COLD_PERIOD_BOTTOM_T3 = -20;
    public static final float COLD_PERIOD_BOTTOM_T4 = -30;
    public static final float COLD_PERIOD_BOTTOM_T5 = -40;
    public static final float COLD_PERIOD_BOTTOM_T6 = -50;
    public static final float COLD_PERIOD_BOTTOM_T7 = -60;
    public static final float COLD_PERIOD_BOTTOM_T8 = -70;
    public static final float COLD_PERIOD_BOTTOM_T9 = -80;
    public static final float COLD_PERIOD_BOTTOM_T10 = -90;
    public static final float[] BOTTOMS = new float[]{
            COLD_PERIOD_BOTTOM_T1,
            COLD_PERIOD_BOTTOM_T2,
            COLD_PERIOD_BOTTOM_T3,
            COLD_PERIOD_BOTTOM_T4,
            COLD_PERIOD_BOTTOM_T5,
            COLD_PERIOD_BOTTOM_T6,
            COLD_PERIOD_BOTTOM_T7,
            COLD_PERIOD_BOTTOM_T8,
            COLD_PERIOD_BOTTOM_T9,
            COLD_PERIOD_BOTTOM_T10
    };
    public static final float CO2_FREEZE_TEMP = -78;
    public static final float O2_FREEZE_TEMP = -218;
    public static final float O2_LIQUID_TEMP = -182;
    public static final float N2_FREEZE_TEMP = -209;
    public static final float N2_LIQUID_TEMP = -195;

    /**
     * The temperature when snow can reach the ground.
     */
    public static final float SNOW_TEMPERATURE = -13;
    /**
     * The temperature when snow becomes blizzard.
     */
    public static final float BLIZZARD_TEMPERATURE = -30;
    /**
     * The temperature when cold plant can grow.
     */
    public static final int COLD_RESIST_GROW_TEMPERATURE = -20;
    /**
     * The temperature when hemp can grow.
     */
    public static final float HEMP_GROW_TEMPERATURE = -15;
    /**
     * The temperature when vanilla plants can grow.
     */
    public static final float VANILLA_PLANT_GROW_TEMPERATURE = -10;
    public static final int BONEMEAL_TEMPERATURE = 5;


    public static final float ANIMAL_ALIVE_TEMPERATURE = -9f;

    public static final float FEEDED_ANIMAL_ALIVE_TEMPERATURE = -30f;

    public static final float VANILLA_PLANT_GROW_TEMPERATURE_MAX = 50;

    public static Map<Object, Float> worldbuffer = new HashMap<>();
    public static Map<Biome, Float> biomebuffer = new HashMap<>();

    public static void clear() {
        worldbuffer.clear();
        biomebuffer.clear();
    }

    /**
     * Get World temperature for a specific world, affected by weather and so on
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float getBaseTemperature(IWorldReader w, BlockPos pos) {
        Biome b = w.getBiome(pos);
        Float temp = null;
        if (b != null)
            temp = biomebuffer.computeIfAbsent(b, FHDataManager::getBiomeTemp);
        float wt = 0;
        if (w instanceof World) {
            wt = worldbuffer.computeIfAbsent(w, (k) -> {
                Float fw = FHDataManager.getWorldTemp((World) w);
                if (fw == null) return -10F;
                return fw;
            });
        }
        if (temp != null)
            return wt + temp;
        return wt;
    }

    public static float getClimateTemperature(IWorldReader w) {
        if (w instanceof World) {
            return WorldClimate.getTemp((World) w);
        }
        return 0;
    }
    public static int getClimateWind(IWorldReader w) {
        if (w instanceof World) {
            return WorldClimate.getWind((World) w);
        }
        return 0;
    }
    /**
     * Get World temperature for a specific world, affected by weather and so on
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float getTemperature(IWorldReader w, BlockPos pos) {
        Biome b = w.getBiome(pos);
        Float temp = null;
        if (b != null)
            temp = biomebuffer.computeIfAbsent(b, FHDataManager::getBiomeTemp);
        float wt = 0;
        if (w instanceof World) {
            wt = worldbuffer.computeIfAbsent(w, (k) -> {
                Float fw = FHDataManager.getWorldTemp((World) w);
                if (fw == null) return -10F;
                return fw;
            });

            // Add dynamic temperature baseline
            wt += WorldClimate.getTemp((World) w) * 0.25f;
        }

        if (temp != null)
            return wt + temp;
        return wt;
    }

    public static boolean isWorldBlizzard(IWorldReader w) {
        if (w instanceof World) {
            return WorldClimate.isBlizzard((World) w);
        }
        return false;
    }
}
