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
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatEndpoint;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatEnergyNetwork  implements MenuProvider,NBTSerializable{
    private transient int interval = 0;
    private transient Consumer<BiConsumer<BlockPos, Direction>> onConnect;
    transient Level world;
    transient BlockEntity cur;

    transient PriorityQueue<HeatEndpoint> endpoints=new PriorityQueue<>(Comparator.comparingInt(HeatEndpoint::getPriority).reversed().thenComparing(HeatEndpoint::getDistance));
    public Map<HeatEndpoint,EndPointData> data=new HashMap<>();
    public Set<EndPointData> epdataset=new HashSet<>();
    Map<BlockPos,Integer> propagated=new HashMap<>();
    boolean dataModified=true;
	private boolean isValid = true;
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.put("pipes",
		SerializeUtil.toNBTList(propagated.entrySet(),(t,p)->p.compound().putLong("pos",t.getKey().asLong()).putInt("len", t.getValue())));
		nbt.put("endpoints",
		SerializeUtil.toNBTList(data.entrySet(),(t,p)->p.compound().putLong("pos",t.getValue().pos.asLong()).putString("blk", RegistryUtils.getRegistryName(t.getValue().blk).toString())));
	}

	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
		propagated.clear();
		ListTag cn=nbt.getList("pipes", Tag.TAG_COMPOUND);
		for(Tag ccn:cn) {
			CompoundTag ccnbt=((CompoundTag)ccn);
			propagated.put(BlockPos.of(ccnbt.getLong("pos")), ccnbt.getInt("len"));
		}
		epdataset.clear();
		ListTag cn2=nbt.getList("endpoints", Tag.TAG_COMPOUND);
		for(Tag ccn:cn2) {
			CompoundTag ccnbt=((CompoundTag)ccn);
			epdataset.add(new EndPointData(RegistryUtils.getBlock(new ResourceLocation(ccnbt.getString("blk"))),
					BlockPos.of(ccnbt.getLong("pos"))));
		}
	}
    private BiConsumer<BlockPos, Direction> connect = (pos, d) -> {
    	if(getWorld()!=null) {
	        BlockEntity te = Utils.getExistingTileEntity(getWorld(), pos);
	        if (te instanceof INetworkConsumer)
	            ((INetworkConsumer) te).tryConnectAt(this,d, 1);
	        else if(te!=null)
	        	te.getCapability(FHCapabilities.HEAT_EP.capability(),d).ifPresent(t->t.reciveConnection(getWorld(),pos,this,d,1));
	        if(cur instanceof INetworkConsumer) {
	        	((INetworkConsumer) cur).tryConnectAt(this, d.getOpposite(), 0);
	        }else if(cur!=null) {
	        	cur.getCapability(FHCapabilities.HEAT_EP.capability(),d.getOpposite()).ifPresent(t->t.reciveConnection(getWorld(),cur.getBlockPos(),this,d.getOpposite(),0));
	        }
    	}
    };
    @Override
	public String toString() {
		return "HeatEnergyNetwork [endpoints=" + endpoints + "]";
	}

    public boolean shouldPropagate(BlockPos pos,int dist) {
    	int odist=propagated.getOrDefault(pos,-1);
    	if(odist>dist||odist==-1) {
    		propagated.put(pos, dist);
    		return true;
    	}
    	return false;
    }
    public void startPropagation(HeatPipeTileEntity hpte,Direction dir) {
    	hpte.connectTo(dir, this, propagated.getOrDefault(hpte.getBlockPos(),-1));
    }
    public int getNetworkSize() {
    	return data.size()+propagated.size();
    }
    public boolean addEndpoint(BlockPos pos,HeatEndpoint heatEndpoint,int dist) {
    	if(data.containsKey(heatEndpoint)) {
    		if(dist<heatEndpoint.getDistance()) {
    			heatEndpoint.connect(this, dist);
    			dataModified=true;
    			return true;
    		}
    	}else {
    		heatEndpoint.connect(this, dist);
    		data.put(heatEndpoint, new EndPointData(getWorld().getBlockState(pos).getBlock(),pos));
    		dataModified=true;
    		return true;
    	}
    	return false;
    }
    public boolean removeEndpoint(BlockPos pos,HeatEndpoint heatEndpoint) {
    	Object dat= data.remove(heatEndpoint);
    	if(dat!=null) {
    		dataModified=true;
    		return true;
    	}
    	return false;
    	
    }
    @Override
    public AbstractContainerMenu createMenu(int p1, Inventory p2, Player p3) {
        return new HeatStatContainer(p1,p3,this);
    }
    /**
     * Instantiates a new HeatProviderManager.<br>
     *
     * @param cur the current tile entity<br>
     * @param con the function that called when refresh is required. Should provide connect direction and location when called.<br>
     */
    public HeatEnergyNetwork(BlockEntity cur, Consumer<BiConsumer<BlockPos, Direction>> con) {
    	this.cur=cur;
        this.onConnect = con;
    }
    public Level getWorld() {
    	if(world==null && cur != null)
            this.world = cur.getLevel();
    	return world;
    }
    public void requestUpdate() {
    	interval=10;
    }
    public void requestSlowUpdate() {
    	interval=20;
    }
    public boolean isUpdateRequested() {
    	return interval>=0;
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
        	for(HeatEndpoint bp:data.keySet()) {
        		bp.clearConnection();
        	}
        	data.clear();
            onConnect.accept(connect);
            dataModified=true;
            interval = -1;
        }
        float value=0;
        int tlevel=1;
        if(dataModified) {
        	dataModified=false;
        	endpoints.clear();
        	endpoints.addAll(data.keySet());
        }
        for(HeatEndpoint endpoint:endpoints) {
        	if(endpoint.canProvideHeat()) {
        		float provided=endpoint.provideHeat();
        		data.get(endpoint).intake=provided;
        		value+=provided;
        		tlevel=Math.max(endpoint.getTemperatureLevel(), tlevel);
        	}
        }
        boolean shouldFill=true;
    	while(shouldFill) {
    		shouldFill=false;
	    	for(HeatEndpoint endpoint:endpoints) {
	    		if(endpoint.canSendHeat()) {
	    			float oldval=value;
		    		value=endpoint.sendHeat(value,tlevel);
		    		data.get(endpoint).applyOutput(oldval-value,endpoint.getMaxIntake());
		    		
		    		shouldFill|=endpoint.canSendHeat();
	    		}
	    		
	    	}
	    	if(value<=0)break;
    	}
    	data.values().forEach(EndPointData::pushData);
    }

    public void invalidate() {
    	for(BlockPos bp:propagated.keySet()) {
    		HeatPipeTileEntity hpte=FHUtils.getExistingTileEntity(getWorld(), bp, HeatPipeTileEntity.class);
    		if(hpte!=null) {
    			hpte.ntwk=null;
    		}
    	}
    	propagated.clear();
    	for(HeatEndpoint bp:data.keySet()) {
    		bp.clearConnection();
    	}
    	data.clear();
    	interval=-1;
    	isValid=false;
    	dataModified=true;
    }
	@Override
	public Component getDisplayName() {
		return Lang.translateGui("heat_stat");
	}


}
