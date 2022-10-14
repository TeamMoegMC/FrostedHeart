package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

public class HeatIncubatorTileEntity extends IncubatorTileEntity implements INetworkConsumer {
	NetworkHolder network = new NetworkHolder();
	public HeatIncubatorTileEntity() {
	}

	public HeatIncubatorTileEntity(TileEntityType<?> type) {
		super(type);
	}
	@Override
	public boolean connect(Direction to, int dist) {
		TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
		if (te instanceof EnergyNetworkProvider) {
			network.connect(((EnergyNetworkProvider) te).getNetwork(), dist);
			return true;
		}
		return false;
	}

	@Override
	public boolean canConnectAt(Direction to) {
		return to == this.getBlockState().get(IncubatorBlock.HORIZONTAL_FACING);
	}

	@Override
	public NetworkHolder getHolder() {
		return network;
	}

	@Override
	public void tick() {
		super.tick();
		network.tick();
	}

	@Override
	protected boolean fetchFuel() {
		
		if(network.tryDrainHeat(4)) {
			fuel=fuelMax=160;
			return true;
		}
		return false;
	}

}
