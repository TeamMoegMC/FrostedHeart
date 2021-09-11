package com.teammoeg.frostedheart.steamenergy;

import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements HeatProvider {
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
	public boolean drainHeat(float value) {
		return true;
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}

}
