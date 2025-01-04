/*
 * Copyright (c) 2024 TeamMoeg
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

import lombok.ToString;

/**
 * A heat endpoint for heat generating devices.
 * A device should properly provide power to the network.
 */
@ToString(callSuper = true)
public class HeatProviderEndPoint extends HeatEndpoint {


    /**
     * Instantiates HeatProviderEndPoint.<br>
     *
     * @param priority  if power is low, endpoint with lower priority would detach first
     * @param capacity  the max power to store<br>
     * @param maxOutput the max heat put to network<br>
     */
    public HeatProviderEndPoint(int priority, float capacity, float maxOutput) {
        super(priority, Math.max(capacity, maxOutput));
        this.maxOutput = maxOutput;
    }

    /**
     * Instantiates with default heat capacity. <br>
     */
    public HeatProviderEndPoint(float maxOutput) {
        super(0, maxOutput * 4);
        this.maxOutput = maxOutput;
    }

    public HeatProviderEndPoint(float capacity, float maxOutput) {
        super(0, Math.max(capacity, maxOutput));
        this.maxOutput = maxOutput;
    }

    public boolean canReceiveHeat() {
        return false;
    }

    @Override
    public float receiveHeat(float filled, int level) {
        return filled;
    }

    /**
     * Adds heat to the endpoint, if capacity exceed, tbe remaining would be disposed.
     * The heat actually added to the endpoint still depends on the generation.
     */
    public void addHeat(float added) {
        heat = Math.min(capacity, heat + added);
    }

    /**
     * Provide heat bounded by the maximum output.
     *
     * @return the amount of heat actually provided
     */
    @Override
    public float provideHeat() {
        float provided = Math.min(heat, maxOutput);
        heat -= provided;
        return provided;
    }

    // Not possible to receive heat
    @Override
    public float getMaxIntake() {
        return 0;
    }

    @Override
    public float getMaxOutput() {
        return maxOutput;
    }
}
