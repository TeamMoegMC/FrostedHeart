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
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatEnergyNetwork  implements INamedContainerProvider,NBTSerializable{
    private transient int interval = 0;
    private transient Consumer<BiConsumer<BlockPos, Direction>> onConnect;
    transient World world;
    transient TileEntity cur;

    transient PriorityQueue<HeatEndpoint> endpoints=new PriorityQueue<>(Comparator.comparingInt(HeatEndpoint::getPriority).reversed().thenComparing(HeatEndpoint::getDistance));
    public Map<HeatEndpoint,EndPointData> data=new HashMap<>();
    public Set<EndPointData> epdataset=new HashSet<>();
    Map<BlockPos,Integer> propagated=new HashMap<>();
    boolean dataModified=true;
	private boolean isValid = true;
	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {
		nbt.put("pipes",
		SerializeUtil.toNBTList(propagated.entrySet(),k->{
			 CompoundNBT cnbt=new CompoundNBT();
			 cnbt.putLong("pos",k.getKey().toLong());
			 cnbt.putInt("len", k.getValue());
			return cnbt;
		}));
		nbt.put("endpoints",
		SerializeUtil.toNBTList(data.entrySet(),k->{
			 CompoundNBT cnbt=new CompoundNBT();
			 cnbt.putLong("pos",k.getValue().pos.toLong());
			 cnbt.putString("blk", RegistryUtils.getRegistryName(k.getValue().blk).toString());
			return cnbt;
		}));
	}

	@Override
	public void load(CompoundNBT nbt, boolean isPacket) {
		propagated.clear();
		ListNBT cn=nbt.getList("pipes", Constants.NBT.TAG_COMPOUND);
		for(INBT ccn:cn) {
			CompoundNBT ccnbt=((CompoundNBT)ccn);
			propagated.put(BlockPos.fromLong(ccnbt.getLong("pos")), ccnbt.getInt("len"));
		}
		epdataset.clear();
		ListNBT cn2=nbt.getList("endpoints", Constants.NBT.TAG_COMPOUND);
		for(INBT ccn:cn2) {
			CompoundNBT ccnbt=((CompoundNBT)ccn);
			epdataset.add(new EndPointData(RegistryUtils.getBlock(new ResourceLocation(ccnbt.getString("blk"))),
					BlockPos.fromLong(ccnbt.getLong("pos"))));
		}
	}
    private BiConsumer<BlockPos, Direction> connect = (pos, d) -> {
    	if(getWorld()!=null) {
	        TileEntity te = Utils.getExistingTileEntity(getWorld(), pos);
	        if (te instanceof INetworkConsumer)
	            ((INetworkConsumer) te).tryConnectAt(this,d, 1);
	        else if(te!=null)
	        	te.getCapability(FHCapabilities.HEAT_EP.capability(),d).ifPresent(t->t.reciveConnection(getWorld(),pos,this,d,1));
	        if(cur instanceof INetworkConsumer) {
	        	((INetworkConsumer) cur).tryConnectAt(this, d.getOpposite(), 0);
	        }else if(cur!=null) {
	        	cur.getCapability(FHCapabilities.HEAT_EP.capability(),d.getOpposite()).ifPresent(t->t.reciveConnection(getWorld(),cur.getPos(),this,d.getOpposite(),0));
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
    	hpte.connectTo(dir, this, propagated.getOrDefault(hpte.getPos(),-1));
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
    public Container createMenu(int p1, PlayerInventory p2, PlayerEntity p3) {
        return new HeatStatContainer(p1,p3,this);
    }
    /**
     * Instantiates a new HeatProviderManager.<br>
     *
     * @param cur the current tile entity<br>
     * @param con the function that called when refresh is required. Should provide connect direction and location when called.<br>
     */
    public HeatEnergyNetwork(TileEntity cur, Consumer<BiConsumer<BlockPos, Direction>> con) {
    	this.cur=cur;
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
	public ITextComponent getDisplayName() {
		return TranslateUtils.translateGui("heat_stat");
	}


}
