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
     * Check can recive connect from direction.<br>
     *
     * @param to the to<br>
     * @return true, if can recive connect from direction
     */
    boolean canConnectAt(Direction to);


    /**
     * Recived Connection from any heat provider.<br>
     * Usually you should call reciveConnection of your holder
     *
     * @param d        the direction connection from<br>
     * @param distance the distance<br>
     * @return true, if connected
     */
    boolean connect(HeatEnergyNetwork network, Direction d, int distance);


    /**
     * Try to connect at.<br>
     * Provider should use this to connect to devices
     *
     * @param d        the d<br>
     * @param distance the distance<br>
     * @return true, if
     */
    default boolean tryConnectAt(HeatEnergyNetwork network, Direction d, int distance) {
        if (canConnectAt(d))
            return connect(network, d, distance);
        return false;
    }
}
