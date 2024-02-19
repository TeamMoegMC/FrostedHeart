package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class HeatEndpoint {
    
    /**
     * The main network.<br>
     */
	protected HeatEnergyNetwork network;

    /**
     * The distance.<br>
     */
    protected int distance;

	public HeatEnergyNetwork getNetwork() {
		return network;
	}

	public int getDistance() {
		return distance;
	}
    public boolean reciveConnection(World w,BlockPos pos,HeatEnergyNetwork manager,Direction d,int dist) {
    	return manager.addEndpoint(pos, this,dist);
    }
    public abstract boolean canFillHeat();
    public abstract float fillHeat(float filled);
    
}
