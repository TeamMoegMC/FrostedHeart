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

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

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

    public static final float WATER_FREEZE_TEMP = 0;
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
     * The temperature when snow layer melts.
     */
    public static final float SNOW_MELT_TEMPERATURE = 0.5F;
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
    public static final float FOOD_FROZEN_TEMPERATURE = -5;

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
     * Get World temperature without climate.
     * @param w
     * @return
     */
    public static float dimension(LevelReader w) {
        float wt = 0;
        if (w instanceof Level level) {
            wt = worldbuffer.computeIfAbsent(level, (k) -> FHDataManager.getWorldTemp(level));
        }
        return wt;
    }

    /**
     * Get Biome temperature without climate.
     * @param w
     * @param pos
     * @return
     */
    public static float biome(LevelReader w, BlockPos pos) {
        Biome b = w.getBiome(pos).get();
        return biomebuffer.computeIfAbsent(b, FHDataManager::getBiomeTemp);
    }

    /**
     * Get World temperature with climate.
     * @param w
     * @return
     */
    public static float climate(LevelReader w) {
        if (w instanceof Level) {
            return WorldClimate.getTemp((Level) w);
        }
        return 0;
    }

    /**
     * Get World temperature for a specific world.
     *
     * Result = Dimension + Biome + Climate.
     *
     * Climate temperature is dynamic in game.
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float base(LevelReader w, BlockPos pos) {
        return dimension(w) + biome(w, pos) + climate(w);
    }

    /**
     * This is the most common method to get temperature.
     *
     * Result = Dimension + Biome + Climate + HeatAdjusts.
     *
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     */
    public static float get(LevelReader world, BlockPos pos) {
        return ChunkHeatData.get(world, new ChunkPos(pos)).map(t -> t.getTemperatureAtBlock(world, pos)).orElseGet(() -> base(world, pos));
    }

    public static boolean isBlizzard(LevelReader w) {
        if (w instanceof Level) {
            return WorldClimate.isBlizzard((Level) w);
        }
        return false;
    }

    public static int wind(LevelReader w) {
        if (w instanceof Level) {
            return WorldClimate.getWind((Level) w);
        }
        return 0;
    }

}
