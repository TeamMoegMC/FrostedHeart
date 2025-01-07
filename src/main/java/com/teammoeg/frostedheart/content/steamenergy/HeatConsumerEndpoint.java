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

import lombok.Getter;
import lombok.ToString;

/**
 * A heat endpoint for heat consuming devices.
 * A device should properly receive power from the network.
 */
@ToString(callSuper = true)
public class HeatConsumerEndpoint extends HeatEndpoint {
    /**
     * Instantiates a new SteamNetworkConsumer.<br>
     *
     * @param priority  consumer priority, if power is low, endpoint with lower priority would detach first
     * @param capacity  the max heat capacity<br>
     * @param maxIntake the max heat requested from network<br>
     */
    public HeatConsumerEndpoint(int priority, float capacity, float maxIntake) {
        super(priority, Math.max(capacity, maxIntake));
        this.maxIntake = maxIntake;
    }

    /**
     * Instantiates a new SteamNetworkConsumer with recommended cache value.<br>
     * maxPower defaults to four times maxIntake. <br>
     *
     * @param maxIntake the max heat requested from network<br>
     */
    public HeatConsumerEndpoint(float maxIntake) {
        super(0, maxIntake * 4);
        this.maxIntake = maxIntake;
    }

    public HeatConsumerEndpoint(int priority, float maxIntake) {
        super(priority, maxIntake * 4);
    }

    /**
     * Drain heat from this endpoint.
     *
     * @param val the heat value to drain
     * @return the heat actually drain
     */
    public float drainHeat(float val) {
        float drained = Math.min(heat, val);
        heat -= drained;
        return drained;
    }

    public float receiveHeat(float filled, int level) {
        float required = Math.min(maxIntake, capacity - heat);
        tempLevel = level;
        if (required > 0) {
            if (filled >= required) {
                filled -= required;
                heat += required;
                return required;
            }
            heat += filled;
            return filled;
        }
        return 0;
    }

    /**
     * Try drain heat from this endpoint if there is enough heat.
     *
     * @param val the amount of heat to drain
     * @return if the heat is drained successfully
     */
    public boolean tryDrainHeat(float val) {
        if (heat >= val) {
            heat -= val;
            return true;
        }
        return false;
    }

    // Consumer cannot provide heat
    public boolean canProvideHeat() {
        return false;
    }

    // Consumer cannot provide heat
    @Override
    public float provideHeat() {
        return 0;
    }

    @Override
    public float getMaxIntake() {
        return maxIntake;
    }

    @Override
    public float getMaxOutput() {
        return 0;
    }
}
