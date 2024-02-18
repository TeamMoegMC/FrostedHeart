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

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Class SteamNetworkConsumer.
 * <p>
 * Integrated power cache manager for power devices
 * A device should properly "request" power from the network
 */
public class SteamNetworkConsumer{

    /**
     * The max power.<br>
     */
    public final float maxPower;

    /**
     * The max intake.<br>
     */
    public final float maxIntake;
    private float power;
    /**
     * Is constant supply even unload
     * */
    public boolean persist;
    
    /**
     * The main network.<br>
     */
	HeatEnergyNetwork sen;

    /**
     * The distance.<br>
     */
    protected int dist;

    /**
     * Instantiates a new SteamNetworkConsumer.<br>
     *
     * @param maxPower  the max power to store<br>
     * @param maxIntake the heat requested from network<br>
     */
    public SteamNetworkConsumer(float maxPower, float maxIntake) {
        super();
        this.maxPower = maxPower;
        this.maxIntake = maxIntake;
        if(maxPower<=maxIntake)
        	maxPower=maxIntake;
    }
    /**
     * Instantiates a new SteamNetworkConsumer with recommended cache value.<br>
     *
     * @param maxIntake the heat requested from network<br>
     */
    public SteamNetworkConsumer(float maxIntake) {
        super();
        this.maxPower = maxIntake*4;
        this.maxIntake = maxIntake;
    }
    public float drainHeat(float val) {
        float drained = Math.min(power, val);
        power -= drained;
        return drained;
    }
    public boolean canFillHeat() {
    	return power<maxPower;
    }
    public float fillHeat(float filled) {
    	float required=Math.min(maxIntake, maxPower-power);
    	if(required>0) {
	    	if(filled>=required) {
	    		filled-=required;
	    		power+=required;
	    		return filled;
	    	}
	    	power+=filled;
	    	return 0;
    	}
    	return filled;
    }
    public boolean reciveConnection(World w,BlockPos pos,HeatEnergyNetwork manager,Direction d,int dist) {
    	if(this.dist==0||this.dist>dist) {
	    	sen=manager;
	    	this.dist=dist;
	    	sen.addEndpoint(pos, this);
	    	return true;
    	}else return false;
    }
    /**
     * Get max power.
     *
     * @return max power<br>
     */
    public float getMaxPower() {
        return maxPower;
    }

    /**
     * Get power stored.
     *
     * @return power<br>
     */
    public float getPower() {
        return power;
    }
    public int getTemperatureLevel() {
    	return 1;
    }
    /**
     * Load.
     *
     * @param nbt the nbt<br>
     */
    public void load(CompoundNBT nbt) {
        power = nbt.getFloat("net_power");
    }

    /**
     * Save.
     *
     * @param nbt the nbt<br>
     */
    public void save(CompoundNBT nbt) {
        nbt.putFloat("net_power", power);
    }

    /**
     * set power stored.
     *
     * @param power value to set power to.
     */
    public void setPower(float power) {
        this.power = power;
    }


    public boolean tryDrainHeat(float val) {
        if (power >= val) {
            power -= val;
            return true;
        }
        return false;
    }
}
