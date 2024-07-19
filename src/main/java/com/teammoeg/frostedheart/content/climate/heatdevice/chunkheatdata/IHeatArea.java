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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import net.minecraft.util.math.BlockPos;

/**
 * Interface to adjust temperature
 */
public interface IHeatArea {
	

    BlockPos getCenter();

    int getRadius();

    /**
     * Get temperature at location, would check if it is in range.
     *
     * @param pos the location<br>
     * @return temperature value at location<br>
     */
    default int getTemperatureAt(BlockPos pos) {
        return getTemperatureAt(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Get temperature at location, would check if it is in range.
     *
     * @param x the locate x<br>
     * @param y the locate y<br>
     * @param z the locate z<br>
     * @return temperature value at location<br>
     */
    int getTemperatureAt(int x, int y, int z);

    /**
     * Get value at location, wont do range check.
     *
     * @param pos the location<br>
     * @return value for that location<br>
     */
    float getValueAt(BlockPos pos);

    /**
     * Checks if location is in range(or, this adjust is effective for this location).<br>
     *
     * @param pos the location<br>
     * @return if this adjust is effective for location, true.
     */
    default boolean isEffective(BlockPos pos) {
        return isEffective(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Checks if location is in range(or, this adjust is effective for this location).<br>
     *
     * @param x the x<br>
     * @param y the y<br>
     * @param z the z<br>
     * @return if this adjust is effective for location, true.
     */
    boolean isEffective(int x, int y, int z);

    void setValue(int value);
}
