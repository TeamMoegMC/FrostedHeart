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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.util.FHUtils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatEnergyNetwork {
    private int interval = 0;
    private Consumer<BiConsumer<BlockPos, Direction>> onConnect;
    World world;
    TileEntity cur;
    private BiConsumer<BlockPos, Direction> connect = (pos, d) -> {
    	if(getWorld()!=null) {
	        TileEntity te = Utils.getExistingTileEntity(getWorld(), pos);
	        if (te instanceof INetworkConsumer)
	            ((INetworkConsumer) te).tryConnectAt(this,d, 1);
	        else if(te!=null)
	        	te.getCapability(HeatCapabilities.ENDPOINT_CAPABILITY,d).ifPresent(t->t.reciveConnection(getWorld(),pos,this,d,1));
    	}
    };
    
    @Override
	public String toString() {
		return "HeatEnergyNetwork [endpoints=" + endpoints + "]";
	}

    BlockPos master;
	private boolean isValid = true;
    PriorityQueue<HeatEndpoint> endpoints=new PriorityQueue<>(Comparator.comparingInt(t->t.distance));
    Map<BlockPos,Integer> propagated=new HashMap<>();
    public boolean shouldPropagate(BlockPos pos,int dist) {
    	int odist=propagated.getOrDefault(pos,0);
    	if(odist>dist||odist==0) {
    		propagated.put(pos, dist);
    		return true;
    	}
    	return false;
    }
    public void startPropagation(HeatPipeTileEntity hpte,Direction dir) {
    	hpte.connectTo(dir, this, propagated.getOrDefault(hpte.getPos(),-1));
    }
    public boolean addEndpoint(BlockPos pos,HeatEndpoint heatEndpoint,int dist) {
    	if(endpoints.contains(heatEndpoint)) {
    		if(dist<heatEndpoint.distance) {
    			heatEndpoint.distance=dist;
    			heatEndpoint.network=this;
    			return true;
    		}
    	}else {
    		heatEndpoint.distance=dist;
			heatEndpoint.network=this;
    		endpoints.add(heatEndpoint);
    		return true;
    	}
    	return false;
    }
    /**
     * Instantiates a new HeatProviderManager.<br>
     *
     * @param cur the current tile entity<br>
     * @param con the function that called when refresh is required. Should provide connect direction and location when called.<br>
     */
    public HeatEnergyNetwork(TileEntity cur, Consumer<BiConsumer<BlockPos, Direction>> con) {
    	this.cur=cur;

        this.master=cur.getPos();
        this.onConnect = con;
    }
    public World getWorld() {
    	if(world==null)
            this.world = cur.getWorld();
    	return world;
    }
    public void requestUpdate() {
    	interval=10;
    }
    /**
     * Tick.
     */
    public void tick() {
        if (interval > 0) {
            interval--;
        }else if(interval==0){
        	for(BlockPos bp:propagated.keySet()) {
        		HeatPipeTileEntity hpte=FHUtils.getExistingTileEntity(getWorld(), bp, HeatPipeTileEntity.class);
        		if(hpte!=null) {
        			hpte.ntwk=null;
        		}
        	}
        	propagated.clear();
        	for(HeatEndpoint bp:endpoints) {
        		bp.network=null;
        		bp.distance=0;
        	}
        	endpoints.clear();
            onConnect.accept(connect);
            interval = -1;
        }
    }
    public void fillHeat(float value) {
    	boolean shouldFill=true;
    	while(shouldFill) {
    		shouldFill=false;
	    	for(HeatEndpoint endpoint:endpoints) {
	    		value=endpoint.fillHeat(value);
	    		if(value<=0)return;
	    		shouldFill|=endpoint.canFillHeat();
	    		
	    	}
    	}
    };

    public void invalidate() {
        isValid = false;
    }
}
