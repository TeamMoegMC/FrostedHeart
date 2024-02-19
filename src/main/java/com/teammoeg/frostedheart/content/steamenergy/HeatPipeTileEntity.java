/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.block.PipeTileEntity;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class HeatPipeTileEntity extends PipeTileEntity implements ITickableTileEntity,EnergyNetworkProvider, FHBlockInterfaces.IActiveState, INetworkConsumer {
	HeatEnergyNetwork ntwk;
    public HeatPipeTileEntity() {
        super(FHTileTypes.HEATPIPE.get());
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return true;
    }

    public boolean connect(HeatEnergyNetwork network,Direction to, int ndist) {
        if (!network.shouldPropagate(getPos(),ndist)) return true;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
        	HeatEnergyNetwork newNetwork = ((EnergyNetworkProvider) te).getNetwork();
        	ntwk=newNetwork;
            this.propagate(to, newNetwork, ndist);
            return true;
        }
        return false;
    }
    public void connectTo(Direction d, HeatEnergyNetwork network, int lengthx) {
    	BlockPos n = this.getPos().offset(d);
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), n);
        if (te instanceof INetworkConsumer) {
            ((INetworkConsumer) te).tryConnectAt(network,d.getOpposite(), lengthx + 1);
        }
    }
    protected void propagate(Direction from, HeatEnergyNetwork network, int lengthx) {
        //System.out.println(from);
        for (Direction d : Direction.values()) {
            if (from == d) continue;
            connectTo(d,network,lengthx);
        }
        return;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
    }

    @Override
    public void tick() {
        super.tick();
        setActive(false);
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
    }

	@Override
	public void onFaceChange(Direction dir, boolean isConnect) {
		if(isConnect)
			ntwk.startPropagation(this, dir);
		else
			ntwk.requestUpdate();
	}

	@Override
	public HeatEnergyNetwork getNetwork() {
		return ntwk;
	}
}
