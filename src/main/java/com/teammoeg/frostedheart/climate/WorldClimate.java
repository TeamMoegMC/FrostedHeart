/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.data.FHDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class WorldClimate {

    /**
     * Baseline temperature for temperate period.
     */
    public static final float CALM_PERIOD_BASELINE = 0;

    /**
     * The temporary uprising peak temperature of a cold period.
     */
    public static final float COLD_PERIOD_PEAK = 4;

    /**
     * The bottom temperature of a cold period.
     */
    public static final float COLD_PERIOD_BOTTOM = -20;
    public static final float COLD_PERIOD_BOTTOM_INTENSE = -30;
    public static final float COLD_PERIOD_BOTTOM_EXTREME = -40;

    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_PEAK = 10;

    /**
     * The temperature when snow can reach the ground.
     */
    public static final float SNOW_TEMPERATURE = -5;

    /**
     * The temperature when snow becomes blizzard.
     */
    public static final float BLIZZARD_TEMPERATURE = -15;

    /**
     * The temperature when vanilla plants can grow.
     */
    public static final float VANILLA_PLANT_GROW_TEMPERATURE = 20;

    /**
     * The temperature when hemp can grow.
     */
    public static final float HEMP_GROW_TEMPERATURE = 0;

    public static final float VANILLA_PLANT_GROW_TEMPERATURE_MAX = 50;

    public static Map<Object, Float> worldbuffer = new HashMap<>();
    public static Map<Biome, Float> biomebuffer = new HashMap<>();

    /**
     * Get World temperature for a specific world, affected by weather and so on
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float getWorldTemperature(IWorldReader w, BlockPos pos) {
        Float temp = biomebuffer.computeIfAbsent(w.getBiome(pos), FHDataManager::getBiomeTemp);
        float wt = CALM_PERIOD_BASELINE;
        if (w instanceof World) {
            wt = worldbuffer.computeIfAbsent(w, (k) -> {
                Float fw = FHDataManager.getWorldTemp((World) w);
                if (fw == null) return CALM_PERIOD_BASELINE;
                return fw;
            });

            // Add dynamic temperature baseline
            wt += ClimateData.getTemp((World) w);
        }

        if (temp != null)
            return wt + temp;
        return wt;
    }

    public static void clear() {
        worldbuffer.clear();
        biomebuffer.clear();
    }
}
