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

import net.minecraft.tileentity.TileEntity;

/**
 * The Interface HeatController.
 * Central controller for heat network.
 */
public interface HeatController extends EnergyNetworkProvider {

    /**
     * Drain heat from the network.
     *
     * @param value the value
     * @return the heat drained
     */
    float drainHeat(float value);

    /**
     * Fill heat into the network.
     *
     * @param value the value
     * @return the heat not filled
     */
    float fillHeat(float value);

    /**
     * get controller entity
     *
     * @return the controller entity
     */
    TileEntity getEntity();

    /**
     * Gets max heat storage value.
     *
     * @return the max heat
     */
    float getMaxHeat();

    /**
     * get temperature level for the network.
     *
     * @return the temperature level
     */
    float getTemperatureLevel();

}
