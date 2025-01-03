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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.lang.Lang;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatNetwork implements MenuProvider, NBTSerializable {
    transient BlockEntity cur;
    transient boolean isBound;
    /**
     * All Endpoints of the network.
     */
    transient PriorityQueue<HeatEndpoint> endpoints = new PriorityQueue<>(Comparator.comparingInt(HeatEndpoint::getPriority).reversed().thenComparing(HeatEndpoint::getDistance));
    /**
     * A blockpos-distance map for existing pipelines
     * */
    Map<BlockPos, Integer> propagated = new HashMap<>();
    /**
     * Interval ticks to re-scan network.
     */
    private transient int interval = 0;
    /**
     * Network connection handler.
     */
    private transient Consumer<HeatConnector> onConnect;
    private final HeatConnector connect = (level, pos, d) -> {
        BlockEntity te = Utils.getExistingTileEntity(level, pos);
        
        if (te instanceof NetworkConnector nc)
        	startPropagation(level,pos,nc,d);
        else if (te != null)
            FHCapabilities.HEAT_EP.getCapability(te, d).ifPresent(t -> t.reciveConnection(level, pos, this, d, 0));
    };

    /**
     * Network current pressure: The
     */
    @Getter
    @Setter
    private float totalEndpointOutput;
    @Getter
    @Setter
    private float totalEndpointIntake;

    public HeatNetwork() {

    }

    public HeatNetwork(BlockEntity cur, Consumer<HeatConnector> con) {
        this();
        bind(cur, con);
    }

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
    	if(!isPacket)
    		nbt.put("pipes",
                SerializeUtil.toNBTList(propagated.entrySet(), (t, p) -> p.compound().putLong("pos", t.getKey().asLong()).putInt("len", t.getValue())));
        nbt.putFloat("totalEndpointOutput", totalEndpointOutput);
        nbt.putFloat("totalEndpointIntake", totalEndpointIntake);
    }

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

    public boolean shouldPropagate(BlockPos pos, int dist) {
        int odist = propagated.getOrDefault(pos, -1);
        if (odist > dist || odist == -1) {
            propagated.put(pos, dist);
            return true;
        }
        return false;
    }

    public <T extends BlockEntity&NetworkConnector> void startPropagation(T hpte, Direction dir) {
    	propagate(hpte.getLevel(),hpte.getBlockPos(),hpte,dir, propagated.getOrDefault(hpte.getBlockPos(), -1));
    }
    public void startPropagation(Level l,BlockPos pos,NetworkConnector hpte, Direction dir) {
    	connect(l,pos,hpte,dir, propagated.getOrDefault(pos, 0));
    }
    public  boolean connect(Level l,BlockPos pos,NetworkConnector connector, Direction to, int ndist) {
    	 System.out.println("Connection requested"+pos+":"+to);
        if (connector.getNetwork() == null || connector.getNetwork().getNetworkSize() <= getNetworkSize()) {
        	connector.setNetwork(this);
        }
        if (shouldPropagate(pos, ndist)) {
        	System.out.println("propagating");
            this.propagate(l,pos,connector,to, ndist);
        }
        return true;
    }

    protected void propagate(Level l,BlockPos fromPos,NetworkConnector connector,Direction from, int lengthx) {
    	//Direction fromFace=from.getOpposite();
        for (Direction d : Direction.values()) {
            if (from == d) continue;
            if(!connector.canConnectTo(d))continue;
            
            BlockPos n = fromPos.relative(d);
            d = d.getOpposite();
            
            BlockEntity be=FHUtils.getExistingTileEntity(l, n);
            System.out.println("fetching pipeette from"+n+":"+d+"-"+be);
            if(be instanceof NetworkConnector nc) {
            	System.out.println("trying to connect");
            	if(nc.canConnectTo(d))
            		connect(l,n,nc,d,lengthx+1);
            }else
            	HeatCapabilities.connect(this,l, n, d, lengthx + 1);
        }	
    }
    public int getNetworkSize() {
        return endpoints.size() + propagated.size();
    }

    public int getNumEndpoints() {
        return endpoints.size();
    }

    public boolean addEndpoint(HeatEndpoint heatEndpoint, int dist, Level level, BlockPos pos) {
        if (endpoints.contains(heatEndpoint)) {
            if (dist < heatEndpoint.getDistance()) {
                heatEndpoint.setConnectionInfo(this, dist, pos, level);
                return true;
            }
        } else {
            heatEndpoint.setConnectionInfo(this, dist, pos, level);
            endpoints.add(heatEndpoint);
            return true;
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int p1, Inventory p2, Player p3) {
        return new HeatStatContainer(p1, p3, this);
    }

    public boolean hasBounded() {
        return isBound;
    }

    public void bind(BlockEntity cur, Consumer<HeatConnector> con) {
        isBound = true;
        this.cur = cur;
        this.onConnect = con;
    }

    public void requestUpdate() {
        interval = 10;
    }

    public void requestSlowUpdate() {
        interval = 20;
    }

    public boolean isUpdateRequested() {
        return interval >= 0;
    }
    public void clearConnection(Level level) {
        // Reset pipes Connection
        for (BlockPos bp : propagated.keySet()) {
        	NetworkConnector hpte = FHUtils.getExistingTileEntity(level, bp, NetworkConnector.class);
            if (hpte != null) {
                hpte.setNetwork(null);
            }
        }
        // Clear pipes
        propagated.clear();
        // Reset endpoints
        for (HeatEndpoint bp : endpoints) {
            bp.clearConnection();
        }
        // Clear endpoints
        endpoints.clear();
    }
    /**
     * Tick.
     */
    public void tick(Level level) {
        // Do update if requested
        if (interval > 0) {
            interval--;
        } else if (interval == 0) {
        	clearConnection(level);
            // Connect all pipes and endpoints again
            if (onConnect != null)
                onConnect.accept(connect);
            interval = -1;
        }

        // Heat accumulated this tick!
        int tlevel = 1;

        // Retrieve heat from the endpoints
        float accumulated = 0;
        totalEndpointOutput = 0;
        for (HeatEndpoint endpoint : endpoints) {
            if (endpoint.canProvideHeat()) {
                // logic
                float provided = endpoint.provideHeat();
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
        for (HeatEndpoint endpoint : endpoints) {
            while (accumulated > 0 && endpoint.canReceiveHeat()) {
                // logic
                float received = endpoint.receiveHeat(endpoint.getMaxIntake(), tlevel);
                totalEndpointIntake += received;
                accumulated -= received;
                endpoint.intake = received;
            }
        }

        // Process data
        endpoints.forEach(HeatEndpoint::pushData);

    }

    public void invalidate(Level l) {
    	clearConnection(l);
        interval = -1;
    }

    @Override
    public Component getDisplayName() {
        return Lang.translateGui("heat_stat");
    }

    public static interface HeatConnector {
        void connect(Level l, BlockPos b, Direction d);
    }


}
