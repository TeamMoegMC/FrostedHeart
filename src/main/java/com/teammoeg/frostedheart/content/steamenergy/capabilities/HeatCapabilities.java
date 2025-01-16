package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.NetworkConnector;
import com.teammoeg.chorda.util.CUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

public class HeatCapabilities {
    /**
     * Check can recive connect from direction.<br>
     *
     * @param to the to<br>
     * @return true, if can recive connect from direction
     */
    public static boolean canConnectAt(LevelAccessor world, BlockPos pos, Direction to) {
        return CUtils.getExistingTileEntity(world, pos, NetworkConnector.class) != null || CUtils.getCapability(world, pos, to, FHCapabilities.HEAT_EP.capability()) != null;

    }


    /**
     * Connects to endpoint.<br>
     *
     * @param d        the direction connection from<br>
     * @param distance the distance<br>
     * @return true, if connected
     */
    public static boolean connect(HeatNetwork network, Level w, BlockPos pos, Direction d, int distance) {
        BlockEntity te = CUtils.getExistingTileEntity(w, pos);
        if (te != null) {
            
            if (te instanceof NetworkConnector) {
            	if(((NetworkConnector) te).canConnectTo(d)) {
            		((NetworkConnector) te).setNetwork(network);
            		return true;
            	}
            	return false;
            }
            
            LazyOptional<HeatEndpoint> ep = te.getCapability(FHCapabilities.HEAT_EP.capability(), d);
            if (ep.isPresent())
                if(ep.orElse(null).reciveConnection(w, pos, network, d, distance)) {
                	network.addEndpoint(ep, distance, w, pos);
                	return true;
                }

        }
        return false;
    }

}
