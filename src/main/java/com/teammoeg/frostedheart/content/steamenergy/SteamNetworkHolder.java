/*
 * Copyright (c) 2022 TeamMoeg
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

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Class SteamNetworkHolder.
 *
 * Power network connector interface.
 */
public class SteamNetworkHolder {
    
    /** The main network.<br> */
    SteamEnergyNetwork sen;
    
    /** The distance.<br> */
    protected int dist;
    
    /** The counter.<br> */
    protected int counter;

    /**
     * Instantiates a new SteamNetworkHolder.<br>
     */
    public SteamNetworkHolder() {
    }
    
    /**
     * Recive connection from specific direction.<br>
     *
     * @param w the world of reciver<br>
     * @param pos the position of reciver<br>
     * @param from the direction connection from<br>
     * @param dist the distance<br>
     * @return true, if connected
     */
    public boolean reciveConnection(World w,BlockPos pos,Direction from,int dist) {
    	TileEntity te = Utils.getExistingTileEntity(w, pos.offset(from));
        if (te instanceof EnergyNetworkProvider) {
            this.connect(((EnergyNetworkProvider) te).getNetwork(), dist);
            return true;
        }
        return false;
    }
    
    /**
     * Recive Connection.
     *
     * @param sen the network<br>
     * @param dist the distance to master<br>
     */
    void connect(SteamEnergyNetwork sen, int dist) {
        this.counter = 10;
        this.sen = sen;
        this.dist = dist;
    }

    /**
     * Tick for revalidate network.
     */
    public boolean tick() {
    	if(!isValid())return false;
    	if(counter>0)
    		counter--;
    	else{
        	counter=0;
            this.sen = null;
            this.dist = Integer.MAX_VALUE;
        }
    	return false;
    }
    
    /**
     * Try drain heat from network.<br>
     * If no such power to drain, nothing would be drained
     * @param val the power to drain<br>
     * @return true, if the power actually drained
     */
    public boolean tryDrainHeat(float val) {
    	if (!isValid())return false;
    	if(sen.hasEnoughHeat(val)) {
    		sen.drainHeat(val);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Drain heat from network.<br>
     *
     * @param val the max power to drain<br>
     * @return returns drained heat
     */
    public float drainHeat(float val) {
        if (!isValid()) return 0;
        return sen.drainHeat(val);
    }

    /**
     * Get temperature level.
     *
     * @return temperature level<br>
     */
    public float getTemperatureLevel() {
        if (!isValid()) return 0;
        return sen.getTemperatureLevel();
    }

    /**
     * Get network.
     *
     * @return network<br>
     */
    public SteamEnergyNetwork getNetwork() {
        return sen;
    }

    /**
     * Checks if is valid.<br>
     *
     * @return if is valid,true.
     */
    public boolean isValid() {
        return sen != null && sen.isValid();
    }

    /**
     * Get distance.
     *
     * @return distance<br>
     */
    public int getDistance() {
        return dist;
    }
    /**
     * get Heat network controller(Generator etc)
     * @return heat controller
     * */
    public HeatController getController() {
		return sen.getController();
	}

	/**
     * To string.<br>
     *
     * @return returns to string
     */
    @Override
    public String toString() {
        return "NetworkHolder [sen=" + sen + ", dist=" + dist + ", counter=" + counter + "]";
    }
}
