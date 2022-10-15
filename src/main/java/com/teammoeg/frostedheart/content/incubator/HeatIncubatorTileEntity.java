package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

public class HeatIncubatorTileEntity extends IncubatorTileEntity implements INetworkConsumer {
	NetworkHolder network = new NetworkHolder();
	public HeatIncubatorTileEntity() {
		super(FHTileTypes.INCUBATOR2.get());
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
		return to == this.getBlockState().get(IncubatorBlock.HORIZONTAL_FACING).getOpposite();
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
		fuelMax=1600;
		if(fuel<=1200) {
			if(network.tryDrainHeat(10)) {
				fuel+=400;
				return true;
			}
		}
		return false;
	}

	@Override
	protected float getMaxEfficiency() {
		return 2f;
	}


	@Override
	public boolean isStackValid(int i, ItemStack itemStack) {
		if(i==0)return false;
		return super.isStackValid(i, itemStack);
	}


	@Override
	protected int fuelMin() {
		return 1200;
	}

}
