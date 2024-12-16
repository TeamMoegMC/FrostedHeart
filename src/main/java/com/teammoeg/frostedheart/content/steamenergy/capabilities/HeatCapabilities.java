package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

public class HeatCapabilities {
    /**
     * Check can recive connect from direction.<br>
     *
     * @param to the to<br>
     * @return true, if can recive connect from direction
     */
    public static boolean canConnectAt(LevelAccessor world,BlockPos pos,Direction to) {
    	return FHUtils.getExistingTileEntity(world, pos,INetworkConsumer.class)!=null||FHUtils.getCapability(world, pos, to, FHCapabilities.HEAT_EP.capability())!=null;
    	
    }


    /**
     * Connects to endpoint.<br>
     *
     * @param d        the direction connection from<br>
     * @param distance the distance<br>
     * @return true, if connected
     */
    public static boolean connect(HeatEnergyNetwork network,Level w,BlockPos pos,Direction d, int distance) {
    	BlockEntity te=FHUtils.getExistingTileEntity(w, pos);
    	if(te!=null) {
    		LazyOptional<HeatEndpoint> ep=te.getCapability(FHCapabilities.HEAT_EP.capability(), d);
    		if(ep.isPresent())
        		return ep.orElse(null).reciveConnection(w, pos, network, d, distance);
    		if(te instanceof INetworkConsumer)
    			return ((INetworkConsumer) te).tryConnectAt(network, d, distance);
    	}
    	return false;
    }


}
