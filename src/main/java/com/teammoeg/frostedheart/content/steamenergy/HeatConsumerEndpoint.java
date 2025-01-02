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
 * Class SteamNetworkConsumer.
 * <p>
 * Integrated power cache manager for power devices
 * A device should properly "request" power from the network
 */
@ToString(callSuper = true)
public class HeatConsumerEndpoint extends HeatCapacityEndpoint {
    /**
     * The max intake value.<br>
     */
    public final float maxIntake;
    
    /**
     * Instantiates a new SteamNetworkConsumer.<br>
     *
     * @param priority consumer priority, if power is low, endpoint with lower priority would detach first
     * @param maxPower  the max power to store<br>
     * @param maxIntake the max heat requested from network<br>
     */
    public HeatConsumerEndpoint(int priority, float maxPower, float maxIntake) {
        super(priority, Math.max(maxPower, maxIntake));
        this.maxIntake = maxIntake;
    }
    /**
     * Instantiates a new SteamNetworkConsumer with recommended cache value.<br>
     * maxPower defaults to four times maxIntake. <br>
     * @param maxIntake the max heat requested from network<br>
     */
    public HeatConsumerEndpoint(float maxIntake) {
        super(0, maxIntake*4);
        this.maxIntake = maxIntake;
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
    	float required=Math.min(maxIntake, capacity - heat);
    	tempLevel=level;
    	if(required>0) {
	    	if(filled>=required) {
	    		filled-=required;
	    		heat +=required;
	    		return filled;
	    	}
	    	heat +=filled;
	    	return 0;
    	}
    	return filled;
    }



    /**
     * Try drain heat from this endpoint.
     *
     * @param val the heat value to drain
     * @return true, if the heat value can all drained
     */
    public boolean tryDrainHeat(float val) {
        if (heat >= val) {
            heat -= val;
            return true;
        }
        return false;
    }
	public boolean canProvideHeat() {
		return false;
	}

	@Override
	public float provideHeat() {
		return 0;
	}
	public float getMaxIntake() {
		return maxIntake;
	}
}
