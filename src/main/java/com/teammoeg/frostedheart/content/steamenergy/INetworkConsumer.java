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

package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.core.Direction;

/**
 * Interface INetworkConsumer.
 * For any heat powered device
 */
public interface INetworkConsumer {

    /**
     * Check if this can connect to a certain direction.
     *
     * @param to the direction to connect to.
     * @return true, if this can connect to the direction.
     */
    boolean canConnectTo(Direction to);


    /**
     * Received Connection from any heat provider.<br>
     * Usually you should call receiveConnection.
     *
     * @param d the direction connection from
     * @param distance the distance
     * @return true, if connected successfully
     */
    boolean connect(HeatEnergyNetwork network, Direction d, int distance);


    /**
     * Try to connect to a certain direction.<br>
     * Provider should use this to connect to devices.
     *
     * @param dir the direction
     * @param distance the distance
     * @return true, if connected successfully
     */
    default boolean tryConnectTo(HeatEnergyNetwork network, Direction dir, int distance) {
        if (canConnectTo(dir))
            return connect(network, dir, distance);
        return false;
    }
}
