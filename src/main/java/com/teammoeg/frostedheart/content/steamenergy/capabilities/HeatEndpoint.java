package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class HeatEndpoint implements NBTSerializable{
    
    /**
     * The main network.<br>
     */
	protected HeatEnergyNetwork network;

    /**
     * The distance.<br>
     */
    protected int distance=-1;
    protected int tempLevel;
    /**
     * Is constant supply even unload
     * */
    public boolean persist;
	public HeatEnergyNetwork getNetwork() {
		return network;
	}

	public int getDistance() {
		return distance;
	}
    public boolean reciveConnection(World w,BlockPos pos,HeatEnergyNetwork manager,Direction d,int dist) {
    	return manager.addEndpoint(pos, this,dist);
    }
    public void connect(HeatEnergyNetwork network,int distance) {
    	this.network=network;
    	this.distance=distance;
    }
    public void clearConnection() {
    	this.network=null;
    	this.distance=-1;
    }
    public abstract boolean canSendHeat();
    public abstract float sendHeat(float filled,int level);
    public int getTemperatureLevel() {
    	return tempLevel;
    }

	public abstract boolean canProvideHeat();
	public abstract float provideHeat();
	public boolean hasValidNetwork() {
		return network!=null;
	}
    public abstract float getMaxIntake();

    
}
