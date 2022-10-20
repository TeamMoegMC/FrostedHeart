package com.teammoeg.frostedheart.content.steamenergy;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

// TODO: Auto-generated Javadoc
/**
 * Class HeatProviderManager.
 *
 * Integrated manager for heat providers
 */
public class HeatProviderManager {
	private int interval=0;
	private TileEntity cur;
	private Consumer<BiConsumer<BlockPos,Direction>> onConnect;
	private BiConsumer<BlockPos,Direction> connect= (pos,d)->{
		TileEntity te = Utils.getExistingTileEntity(cur.getWorld(), pos);
        if (te instanceof INetworkConsumer)
                ((INetworkConsumer) te).tryConnectAt(d, 0);
	};
	
	/**
	 * Instantiates a new HeatProviderManager.<br>
	 *
	 * @param cur the current tile entity<br>
	 * @param con the function that called when refresh is required. Should provide connect direction and location when called.<br>
	 */
	public HeatProviderManager(TileEntity cur,Consumer<BiConsumer<BlockPos,Direction>> con) {
		this.cur=cur;
		this.onConnect=con;
	}
	
	/**
	 * Tick.
	 */
	public void tick() {
		if(interval>0) 
			interval--;
		else {
			onConnect.accept(connect);
			interval=5;
		}
	}

}
