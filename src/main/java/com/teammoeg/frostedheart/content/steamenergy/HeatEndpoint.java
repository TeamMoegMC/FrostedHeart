package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

import lombok.Getter;
import lombok.ToString;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The Endpoint base for heat network.
 * 
 */
@Getter
@ToString()
public abstract class HeatEndpoint implements NBTSerializable {
    
    /**
     * The main network.
     */
	@ToString.Exclude
	protected HeatEnergyNetwork network;

	/**
     * The distance to center.
     */
    protected int distance=-1;
    
    /**
	 * Temperature level of the network.
	 *
	 * This is normally defined by the Generator, or a Heat Debugger.
	 */
    protected int tempLevel;

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
    public boolean reciveConnection(Level w,BlockPos pos,HeatEnergyNetwork manager,Direction d,int dist) {
    	return manager.addEndpoint(w,pos, this,dist);
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
     * Can receive heat from the network.
	 * <p>
     * The network would call this to check if this is a consumer.
     * If this returns true, this endpoint would be added to the consumer list with or without actually consume.
     * The network may also put heat into this network.
	 * <p>
     * This should be only called by the network, You should not call this method.
     * 
     * @return true, if successful
     */
	protected abstract boolean canReceiveHeat();
    
    /**
     * Receive heat from the network.
	 * <p>
     * If the heat provided lesser than max intake then the heat statistics would show red
	 * <p>
	 * This should be only called by the network, You should not call this method.
     *
     * @param filled the amount of heat that the network fills to this endpoint
     * @param level the actual temperature level, which my differ from HeatEndpoint#tempLevel
     * @return the amount of heat actually filled
     */
    protected abstract float receiveHeat(float filled, int level);

	/**
	 * Whether this endpoint can provide heat to the network.
	 * <p>
	 * The network would call this to check if this is a provider.
	 * If this returns true, this endpoint would be added to the generator list with or without actually generate.
	 * <p>
	 * This should be only called by the network, You should not call this method.
	 *
	 * @return true, if successful
	 */
	protected abstract boolean canProvideHeat();
	
	/**
	 * Provide heat to the network.
	 * <p>
	 * This should be only called by the network, You should not call this method.
	 *
	 * @return the amount of heat actually provided
	 */
	protected abstract float provideHeat();
	
	/**
	 * Checks if the network valid.
	 *
	 * @return true, if successful
	 */
	public boolean hasValidNetwork() {
		return network != null;
	}
    
    /**
     * The maximum heat to receive from the network.
     *
     * @return the max intake
     */
    public abstract float getMaxIntake();

	/**
	 * Gets the detach priority.
	 *
	 * @return the priority to detatch
	 */
    public abstract int getPriority();
}
