package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Endpoint base for heat network.
 * 
 */
public abstract class HeatEndpoint implements NBTSerializable{
    
    /**
     * The main network.<br>
     */
	protected HeatEnergyNetwork network;

    /**
     * The distance.<br>
     */
    protected int distance=-1;
    
    /** The temp level. */
    protected int tempLevel;
    
    /** Is constant supply even unload. */
    public boolean persist;
	
	/**
	 * Gets the whole network.
	 *
	 * @return the network
	 */
	public HeatEnergyNetwork getNetwork() {
		return network;
	}

	/**
	 * Gets the distance to central.
	 *
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}
    
    /**
     * Recive connection from network.
     *
     * @param w current world
     * @param pos current pos
     * @param manager the network
     * @param d direction from
     * @param dist the distance to central
     * @return true, if successful
     */
    public boolean reciveConnection(World w,BlockPos pos,HeatEnergyNetwork manager,Direction d,int dist) {
    	return manager.addEndpoint(pos, this,dist);
    }
    
    /**
     * Connect to a network to distance.
     *
     * @param network the network
     * @param distance the distance
     */
    public void connect(HeatEnergyNetwork network,int distance) {
    	this.network=network;
    	this.distance=distance;
    }
    
    /**
     * Clear current connection.
     */
    public void clearConnection() {
    	this.network=null;
    	this.distance=-1;
    }
    
    /**
     * Can receive heat from network.
     * The network would call this to check if this is a consumer.
     * If this returns true, this endpoint would be added to the consumer list with or without actually consume.
     * The network may also put heat into this network.
     * This should only called by network, You should not call this method.
     * 
     * @return true, if successful
     */
    public abstract boolean canSendHeat();
    
    /**
     * The network calls this to put heat into this endpoint.
     * If the heat provided lesser than max intake then the heat statistics would show red
     * This should only called by network, You should not call this method.
     *
     * @param filled the network fills heat to this endpoint
     * @param level the actual heat level
     * @return the heat actually filled
     */
    public abstract float sendHeat(float filled,int level);
    
    /**
     * Get the temperature level.
     *
     * @return the temperature level
     */
    public int getTemperatureLevel() {
    	return tempLevel;
    }

	/**
	 * Can draw heat from network.
	 * The network would call this to check if this is a provider.
	 * If this returns true, this endpoint would be added to the generator list with or without actually generate.
	 * This should only called by network, You should not call this method.
	 *
	 * @return true, if successful
	 */
	public abstract boolean canProvideHeat();
	
	/**
	 * Provide heat to the network.
	 * This should only called by network, You should not call this method.
	 *
	 * @return heat provided to the network
	 */
	public abstract float provideHeat();
	
	/**
	 * Checks if the network valid.
	 *
	 * @return true, if successful
	 */
	public boolean hasValidNetwork() {
		return network!=null;
	}
    
    /**
     * Gets the max intake.
     *
     * @return the max intake
     */
    public abstract float getMaxIntake();

    public abstract int getPriority();
}
