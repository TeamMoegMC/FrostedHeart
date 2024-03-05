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

package com.teammoeg.frostedheart.content.steamenergy.capabilities;

/**
 * Class HeatProviderEndPoint.
 * <p>
 * Integrated power cache manager for power generating devices
 * A device should properly "gives" power from the network
 */
public class HeatProviderEndPoint extends HeatPowerEndpoint{


    /**
     * The max generate.<br>
     */
    public final float maxGenerate;
    /**
     * Is constant supply even unload
     * */
    public boolean persist;


    /**
     * Instantiates HeatProviderEndPoint.<br>
     *
     * @param maxPower  the max power to store<br>
     * @param maxGenerate the max heat put to network<br>
     */
    public HeatProviderEndPoint(float maxPower, float maxGenerate) {
        super(Math.max(maxPower, maxGenerate));
        this.maxGenerate = maxGenerate;

    }
    /**
     * Instantiates HeatProviderEndPoint with recommended cache value.<br>
     *
     * @param maxGenerate the max heat put to network<br>
     */
    public HeatProviderEndPoint(float maxGenerate) {
        super(maxGenerate*4);
        this.maxGenerate = maxGenerate;
    }
    public boolean canSendHeat() {
    	return false;
    }
	@Override
	public float sendHeat(float filled, int level) {
		return filled;
	}
	/**
	 * Adds heat to the endpoint, if cache exceed, tbe remaining would be disposed.
	 * The heat actually added to the endpoint still depends on the max generation
	 * */
	public void addHeat(float np) {
		power=Math.min(maxPower, power+np);
	}
	@Override
	public float provideHeat() {
		float provided=Math.min(power, maxGenerate);
		power-=provided;
		return provided;
	}
	@Override
	public float getMaxIntake() {
		return 0;
	}
}
