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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;

public class WorldClimate {

    /**
     * Constant WORLD_TEMPERATURE.<br>
     */
    public static final byte WORLD_TEMPERATURE = -20;

    /**
     * Constant VANILLA_PLANT_GROW_TEMPERATURE.<br>
     */
    public static final int VANILLA_PLANT_GROW_TEMPERATURE = 20;

    /**
     * Constant HEMP_GROW_TEMPERATURE.<br>
     */
    public static final int HEMP_GROW_TEMPERATURE = 0;

    /**
     * Get World temperature for a specific world, affected by weather and so on
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static byte getWorldTemperature(IWorldReader w, BlockPos pos) {
        Byte temp = FHDataManager.getBiomeTemp(w.getBiome(pos));
        if (temp != null)
            return (byte) MathHelper.clamp(WORLD_TEMPERATURE + temp, -127, 127);
        return WORLD_TEMPERATURE;
    }
}
