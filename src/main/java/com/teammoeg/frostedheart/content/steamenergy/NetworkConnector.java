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
 * Any HeatPipe-like BlockEntity that network should treat it as transmitter.
 * The network would no longer check capability api for endpoints
 */
public interface NetworkConnector extends HeatNetworkProvider{

    /**
     * Check if this can connect to a certain direction.
     *
     * @param to the direction to connect to, outbounds the current block
     * @return true, if this can connect to the direction.
     */
    boolean canConnectTo(Direction to);


    /**
     * Heat network should call this to update cache for the pipe
     *
     * @param network
     * @return true, if connected successfully
     */
    void setNetwork(HeatNetwork network);
}
