package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class HeatCapabilities {
    @CapabilityInject(HeatEndpoint.class)
    public static Capability<HeatEndpoint> ENDPOINT_CAPABILITY = null;
    /**
     * Check can recive connect from direction.<br>
     *
     * @param to the to<br>
     * @return true, if can recive connect from direction
     */
    public static boolean canConnectAt(IWorld world,BlockPos pos,Direction to) {
    	return FHUtils.getExistingTileEntity(world, pos,INetworkConsumer.class)!=null||FHUtils.getCapability(world, pos, to, ENDPOINT_CAPABILITY)!=null;
    	
    };


    /**
     * Connects to endpoint.<br>
     *
     * @param d        the direction connection from<br>
     * @param distance the distance<br>
     * @return true, if connected
     */
    public static boolean connect(HeatEnergyNetwork network,World w,BlockPos pos,Direction d, int distance) {
    	HeatEndpoint ep=FHUtils.getCapability(w, pos, d, ENDPOINT_CAPABILITY);
    	if(ep!=null)
    		return ep.reciveConnection(w, pos, network, d, distance);
    	INetworkConsumer inc=FHUtils.getExistingTileEntity(w, pos,INetworkConsumer.class);
    	return inc!=null?inc.tryConnectAt(network, d, distance):false;
    }


}
