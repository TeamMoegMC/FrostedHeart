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

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.util.CUtils;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;

import javax.annotation.Nullable;

// TODO: Auto-generated Javadoc
/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatNetwork implements MenuProvider, NBTSerializable {
    private transient boolean isBound;
    /**
     * All connected endpoints of the network.
     */
    transient PriorityQueue<LazyOptional<HeatEndpoint>> endpoints = 
    	new PriorityQueue<>(Comparator.<LazyOptional<HeatEndpoint>>comparingInt(t->t.map(HeatEndpoint::getPriority).orElse(-100))
    		.reversed().thenComparing(t->t.map(HeatEndpoint::getDistance).orElse(-100)));
    /** A blockpos-distance map for existing pipelines. */
    Map<BlockPos, Integer> propagated = new HashMap<>();
    /**
     * Interval ticks to re-scan network.
     */
    private transient int interval = 0;
    /**
     * Network connection handler.
     */
    private transient Runnable onConnect;

    /** Network current pressure. */
    @Getter
    @Setter
    private float totalEndpointOutput;
    
    /** The total endpoint intake. */
    @Getter
    @Setter
    private float totalEndpointIntake;
    @Getter
    private boolean valid=true;

    /**
     * Instantiates a new heat network.
     */
    public HeatNetwork() {

    }

    /**
     * Instantiates a new heat network.
     *
     * @param con the con
     */
    public HeatNetwork(Runnable con) {
        this();
        setReconnector(con);
    }

    /**
     * Save.
     *
     * @param nbt the nbt
     * @param isPacket is send by packet
     */
    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
    	if(!isPacket) {
    		nbt.put("pipes",
                SerializeUtil.toNBTList(propagated.entrySet(), (t, p) -> p.compound().putLong("pos", t.getKey().asLong()).putInt("len", t.getValue())));
    		
    	}
        nbt.putFloat("totalEndpointOutput", totalEndpointOutput);
        nbt.putFloat("totalEndpointIntake", totalEndpointIntake);
    }

    /**
     * Load.
     *
     * @param nbt the nbt
     * @param isPacket is send by packet
     */
    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
    	if(!isPacket) {
	        propagated.clear();
	        ListTag cn = nbt.getList("pipes", Tag.TAG_COMPOUND);
	        for (Tag ccn : cn) {
	            CompoundTag ccnbt = ((CompoundTag) ccn);
	            propagated.put(BlockPos.of(ccnbt.getLong("pos")), ccnbt.getInt("len"));
	        }
    	}
        totalEndpointOutput = nbt.getFloat("totalEndpointOutput");
        totalEndpointIntake = nbt.getFloat("totalEndpointIntake");
    }

    @Override
    public String toString() {
        return "HeatNetwork [endpoints=" + endpoints + "]";
    }

    /**
     * Add a connector to network.
     *
     * @param pos the position of connector
     * @param distance the distance
     * @return true, if the corresponding connector is not connected or connected through a longer distance
     */
    protected boolean addConnector(BlockPos pos, int distance) {
        int currentDistance = propagated.getOrDefault(pos, -1);
        if (currentDistance > distance || currentDistance == -1) {
            propagated.put(pos, distance);
            return true;
        }
        return false;
    }

    /**
     * Start connection from a block with outbound face.
     *
     * @param hpte the block entity
     * @param dir the direction outbounds the provided block.
     */
    public void startConnectionFromBlock(BlockEntity hpte, Direction dir) {
    	//System.out.println("started to"+dir);
    	connectTo(hpte.getLevel(),hpte.getBlockPos().relative(dir),hpte.getBlockPos(),dir.getOpposite());
    }
    /**
     * Start connection from a block for all faces.
     *
     * @param hpte the block entity
     */
    public void startConnectionFromBlock(BlockEntity hpte) {
    	//System.out.println("started to"+dir);
    	if(hpte instanceof NetworkConnector nc) {
	    	for(Direction dir:Direction.values())
	    		if(nc.canConnectTo(dir))
	    			connectTo(hpte.getLevel(),hpte.getBlockPos().relative(dir),hpte.getBlockPos(),dir.getOpposite());
    	}else {
    		for(Direction dir:Direction.values())
	    		connectTo(hpte.getLevel(),hpte.getBlockPos().relative(dir),hpte.getBlockPos(),dir.getOpposite());
    	}
    }
    /**
     * Connect to a block in a position, would silently fail if the corresponding block is not connectable
     *
     * @param level the level
     * @param pos the position
     * @param face the face of provided block
     */
    public void connectTo(Level level,BlockPos pos,BlockPos srcPos,Direction face) {
    	BlockEntity te = Utils.getExistingTileEntity(level, pos);
        
        if (te instanceof NetworkConnector nc)
        	startPropagation(level,pos,nc,face);
        else if (te != null) {
            LazyOptional<HeatEndpoint> heatcap = FHCapabilities.HEAT_EP.getCapability(te, face);
            if (heatcap.isPresent()) {
                boolean result = heatcap.orElse(null).reciveConnection(level, pos, this, face, 0);
                if (result) {
                    addEndpoint(heatcap, 0, level, pos);
                }
            }
        }
    }
    
    /**
     * Start propagation through connectors.
     *
     * @param l the level
     * @param pos the position of current connector
     * @param hpte the current connector
     * @param dir the direction inbounds the current connector
     */
    protected void startPropagation(Level l,BlockPos pos,NetworkConnector hpte, Direction dir) {
    	visitConnector(l,pos,hpte,dir, propagated.getOrDefault(pos, 0));
    }
    
    /**
     * walk through connectors, run DFS pathfinding.
     *
     * @param l the level
     * @param pos the position of current connector
     * @param connector the current connector
     * @param to the direction inbounds the current connector
     * @param ndist the distance of current connector
     * @return true, if successful
     */
    protected boolean visitConnector(Level l,BlockPos pos,NetworkConnector connector, Direction to, int ndist) {
    	 //System.out.println("Connection requested"+pos+":"+to);
        if (connector.getNetwork() == null || connector.getNetwork().getNetworkSize() <= getNetworkSize()) {
        	connector.setNetwork(this);
        }
        if (addConnector(pos, ndist)) {
        	//System.out.println("propagating");
            this.propagate(l,pos,connector,to, ndist);
        }
        return true;
    }

    /**
     * Propagate from a connector.
     *
     * @param l the level
     * @param fromPos the position of current connector
     * @param connector the current connector
     * @param from the inbound direction, it would avoid running into inbound direction.
     * @param distance the distance of current connector
     */
    protected void propagate(Level l,BlockPos fromPos,NetworkConnector connector,@Nullable Direction from, int distance) {
    	//Direction fromFace=from.getOpposite();
        for (Direction d : Direction.values()) {
            if (from == d) continue;
            if(!connector.canConnectTo(d))continue;
            
            BlockPos n = fromPos.relative(d);
            d = d.getOpposite();
            
            BlockEntity be= CUtils.getExistingTileEntity(l, n);
            //System.out.println("fetching pipeette from"+n+":"+d+"-"+be);
            if(be instanceof NetworkConnector nc) {
            	//System.out.println("trying to connect");
            	if(nc.canConnectTo(d))
            		visitConnector(l,n,nc,d,distance+1);
            }else
            	HeatCapabilities.connect(this,l, n, d, distance + 1);
        }	
    }
    
    /**
     * Gets the network size, higher sized network should override lower sized network during merging
     *
     * @return the network size
     */
    public int getNetworkSize() {
        return endpoints.size() + propagated.size();
    }

    /**
     * Gets connected endpoint count.
     *
     * @return the endpoint count
     */
    public int getNumEndpoints() {
        return endpoints.size();
    }
    public void refreshConnectedEndpoints(BlockPos pos) {
    }
    /**
     * Add endpoint to network, This method do not validate endpoint, endpoint should do the validation before adding
     *
     * @param capEndpoint the heat endpoint to add
     * @param dist the distance from of endpoint
     * @param level the level
     * @param pos the position of endpoint
     * @return true, if successful
     */
    public boolean addEndpoint(LazyOptional<HeatEndpoint> capEndpoint, int dist, Level level, BlockPos pos) {
    	HeatEndpoint heatEndpoint=capEndpoint.orElse(null);
    	if(heatEndpoint!=null)
	        if (endpoints.contains(capEndpoint)) {
	            if (dist < heatEndpoint.getDistance()||heatEndpoint.getDistance()==-1) {
	                heatEndpoint.setConnectionInfo(this, dist, pos, level);
	                return true;
	            }
	        } else {
	            heatEndpoint.setConnectionInfo(this, dist, pos, level);
	            endpoints.add(capEndpoint);
	            return true;
	        }
        return false;
    }
    /**
     * Remove endpoint from network, This method do not validate endpoint, endpoint should do the validation before removing
     *
     * @param heatEndpoint the heat endpoint to remove
     * @param level the level
     * @param pos the position of endpoint
     * @return true, if successful
     */
    public boolean removeEndpoint(HeatEndpoint heatEndpoint, Level level, BlockPos pos,Direction indir) {
    	heatEndpoint.clearConnection();
    	return endpoints.remove(heatEndpoint);
    }

    /**
     * Creates the menu.
     *
     * @param p1 the p 1
     * @param p2 the p 2
     * @param p3 the p 3
     * @return the abstract container menu
     */
    @Override
    public AbstractContainerMenu createMenu(int p1, Inventory p2, Player p3) {
        return new HeatStatContainer(p1, p3, this);
    }

    /**
     * Checks for bounded.
     *
     * @return true, if successful
     */
    public boolean hasBounded() {
        return isBound;
    }

    /**
     * set method to run when reconnect needed.
     * @param con the con
     */
    public void setReconnector(Runnable con) {
        isBound = true;
        this.onConnect = con;
    }

    /**
     * Request update.
     */
    public void requestUpdate() {
    	if(interval>10||interval<0)
    		interval = 10;
    }

    /**
     * Request slow update.
     */
    public void requestSlowUpdate() {
    	if(!isUpdateRequested())
    		interval = 20;
    }

    /**
     * Checks if is update requested.
     *
     * @return true, if is update requested
     */
    public boolean isUpdateRequested() {
        return interval >= 0;
    }
    
    /**
     * Clear all connection in network.
     *
     * @param level the level
     */
    public void clearConnection(Level level) {
        // Reset pipes Connection
        for (BlockPos bp : propagated.keySet()) {
        	NetworkConnector hpte = CUtils.getExistingTileEntity(level, bp, NetworkConnector.class);
            if (hpte != null) {
                hpte.setNetwork(null);
            }
        }
        // Clear pipes
        propagated.clear();
        // Reset endpoints
        for (LazyOptional<HeatEndpoint> bp : endpoints) {
        	HeatEndpoint ep=bp.orElse(null);
        	ep.clearConnection();
        }
        // Clear endpoints
        endpoints.clear();
    }
    
    /**
     * Tick.
     *
     * @param level the level
     */
    public void tick(Level level) {
        // Do update if requested
        if (interval > 0) {
            interval--;
        } else if (interval == 0) {
        	clearConnection(level);
            // Connect all pipes and endpoints again
        	// System.out.println("full rebuild triggered");
            if (onConnect != null)
                onConnect.run();
            interval = -1;
        }
        // Heat accumulated this tick!
        int tlevel = 1;
        
        // Retrieve heat from the endpoints
        float accumulated = 0;
        totalEndpointOutput = 0;
        endpoints.removeIf(t->!t.isPresent());
        for (LazyOptional<HeatEndpoint> lep : endpoints) {
        	HeatEndpoint endpoint=lep.orElse(null);
        	if(endpoint!=null)
	            if (endpoint.canProvideHeatToNetwork()) {
	                // logic
	                float provided = endpoint.provideHeatToNetwork();
	                accumulated += provided;
	                // fetch the highest tLevel
	                tlevel = Math.max(endpoint.getTempLevel(), tlevel);
	                // update display data
	                endpoint.output = provided;
	            }
        }

        totalEndpointOutput = accumulated;

        // Distribute heat to the endpoints
        totalEndpointIntake = 0;
        for (LazyOptional<HeatEndpoint> lep : endpoints) {
        	HeatEndpoint endpoint=lep.orElse(null);
        	if(endpoint!=null)
	            if (endpoint.canReceiveHeatFromNetwork()) {
	            	//first distribute
	                float received =0;
	                if(accumulated!=0) {
		                received=endpoint.receiveHeatFromNetwork(accumulated, tlevel);
		                totalEndpointIntake += received;
		                accumulated -= received;
	                }
	                endpoint.intake = received;
	                if(received!=0&&accumulated!=0) {//maximum 2 times input
	                	received = endpoint.receiveHeatFromNetwork(accumulated, tlevel);
		                totalEndpointIntake += received;
		                accumulated -= received;
		                endpoint.intake += received;
	                }
	            }
            //if(accumulated <= 0)
            //	break;
        }

        // Process data
        endpoints.forEach(t->t.ifPresent(HeatEndpoint::pushData));

    }

    /**
     * Invalidate.
     *
     * @param l the l
     */
    public void invalidate(Level l) {
    	clearConnection(l);
    	valid=false;
        interval = -1;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    @Override
    public Component getDisplayName() {
        return Lang.translateGui("heat_stat");
    }

    public List<HeatEndpoint> getEndpoints(){
    	List<HeatEndpoint> eplist=new ArrayList<>(endpoints.size());
    	for(LazyOptional<HeatEndpoint> i:endpoints)
    		if(i.isPresent()) {
    			eplist.add(i.orElse(null));
    		}
    	return eplist;
    }

    public void setEndpoints(Collection<HeatEndpoint> endpoints) {
        this.endpoints.clear();
        for(HeatEndpoint i : endpoints)
            this.endpoints.add(LazyOptional.of(()->i));
    }
}
