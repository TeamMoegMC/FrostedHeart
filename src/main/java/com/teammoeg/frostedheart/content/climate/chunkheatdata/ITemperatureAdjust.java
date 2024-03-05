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

package com.teammoeg.frostedheart.content.climate.chunkheatdata;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

/**
 * Interface to adjust temperature
 */
public interface ITemperatureAdjust extends NBTSerializable {

    /**
     * Factory construct temperature adjust from NBT<br>
     *
     * @param nc the nbt compound<br>
     * @return returns adjust
     */
    static ITemperatureAdjust valueOf(CompoundNBT nc) {
        switch (nc.getInt("type")) {
            case 1:
                return new CubicTemperatureAdjust(nc);
            case 2:
                return new PillarTemperatureAdjust(nc);
            default:
                return new CubicTemperatureAdjust(nc);
        }
    }

    /**
     * Factory construct temperature adjust from packet buffer.<br>
     *
     * @param buffer the buffer<br>
     * @return returns adjust
     */
    public static ITemperatureAdjust valueOf(PacketBuffer buffer) {
        int packetId = buffer.readByte();
        switch (packetId) {
            case 1:
                return new CubicTemperatureAdjust(buffer);
            case 2:
                return new PillarTemperatureAdjust(buffer);
            default:
                return new CubicTemperatureAdjust(buffer);
        }
    }

    ;

    /**
     * Deserialize.
     *
     * @param buffer the buffer<br>
     */
    void deserialize(PacketBuffer buffer);

    /**
     * Get center X.
     *
     * @return center X<br>
     */
    int getCenterX();

    /**
     * Get center Y.
     *
     * @return center Y<br>
     */
    int getCenterY();

    /**
     * Get center Z.
     *
     * @return center Z<br>
     */
    int getCenterZ();

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

    /**
     * Serialize.
     *
     * @param buffer the buffer<br>
     */
    void serialize(PacketBuffer buffer);

    void setValue(int value);
}
