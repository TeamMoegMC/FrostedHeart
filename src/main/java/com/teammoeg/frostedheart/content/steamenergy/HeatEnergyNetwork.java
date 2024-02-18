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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

// TODO: Auto-generated Javadoc

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatEnergyNetwork {
    private int interval = 0;
    private TileEntity cur;
    private Consumer<BiConsumer<BlockPos, Direction>> onConnect;
    private BiConsumer<BlockPos, Direction> connect = (pos, d) -> {
    	if(cur!=null) {
	        TileEntity te = Utils.getExistingTileEntity(cur.getWorld(), pos);
	        if (te instanceof INetworkConsumer)
	            ((INetworkConsumer) te).tryConnectAt(this,d, 0);
    	}
    };
    
    private boolean isValid = true;
    PriorityQueue<SteamNetworkConsumer> endpoints=new PriorityQueue<>(Comparator.comparingInt(t->t.dist));
    Map<BlockPos,Integer> propagated=new HashMap<>();
    public boolean shouldPropagate(BlockPos pos,int dist) {
    	int odist=propagated.get(pos);
    	if(odist>dist) {
    		propagated.put(pos, dist);
    		return true;
    	}
    	return false;
    }
    public void addEndpoint(BlockPos pos,SteamNetworkConsumer consumer) {
    	endpoints.add(consumer);
    }
    /**
     * Instantiates a new HeatProviderManager.<br>
     *
     * @param cur the current tile entity<br>
     * @param con the function that called when refresh is required. Should provide connect direction and location when called.<br>
     */
    public HeatEnergyNetwork(TileEntity cur, Consumer<BiConsumer<BlockPos, Direction>> con) {
        this.cur = cur;
        this.onConnect = con;
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
        	propagated.clear();
        	endpoints.clear();
            onConnect.accept(connect);
            interval = -1;
        }
    }
    public void fillHeat(float value) {
    	boolean shouldFill=true;
    	while(shouldFill) {
    		shouldFill=false;
	    	for(SteamNetworkConsumer endpoint:endpoints) {
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
