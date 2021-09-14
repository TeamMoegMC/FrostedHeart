package com.teammoeg.frostedheart.steamenergy;

import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3i;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements HeatProvider,IConnectable {
	public DebugHeaterTileEntity() {
		super(FHTileTypes.DEBUGHEATER.get());
	}

	SteamEnergyNetwork network=new SteamEnergyNetwork(this);
	@Override
	public SteamEnergyNetwork getNetwork() {
		return network;
	}

	@Override
	public float getMaxHeat() {
		return Float.MAX_VALUE;
	}

	@Override
	public float drainHeat(float value) {
		return value;
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}

	@Override
	public void disconnectAt(Direction to) {
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof IConnectable&&!(te instanceof DebugHeaterTileEntity)) {
			((IConnectable) te).disconnectAt(to.getOpposite());
		}
	}

	@Override
	public void connectAt(Direction to) {
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof IConnectable&&!(te instanceof DebugHeaterTileEntity)) {
			((IConnectable) te).connectAt(to.getOpposite());
		}
	}

}
