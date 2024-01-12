/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class HeatPipeTileEntity extends IEBaseTileEntity implements EnergyNetworkProvider, ITickableTileEntity, FHBlockInterfaces.IActiveState, INetworkConsumer {
    private SteamNetworkHolder network = new SteamNetworkHolder();
    private boolean isPathFinding;
    private boolean justPropagated;

    public HeatPipeTileEntity() {
        super(FHTileTypes.HEATPIPE.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
    }

    @Override
    public SteamEnergyNetwork getNetwork() {
        return network.getNetwork();
    }

    protected boolean hasNetwork() {
        return network != null && network.isValid();
    }

    protected void propagate(Direction from, SteamEnergyNetwork newNetwork, int lengthx) {
        if (isPathFinding) return;
        //System.out.println(from);
        try {
            isPathFinding = true;
            network.connect(newNetwork, lengthx);
            for (Direction d : Direction.values()) {
                if (from == d) continue;
                BlockPos n = this.getPos().offset(d);
                TileEntity te = Utils.getExistingTileEntity(this.getWorld(), n);
                if (te instanceof INetworkConsumer) {
                    ((INetworkConsumer) te).tryConnectAt(d.getOpposite(), lengthx + 1);
                }
            }
            return;
        } finally {
            isPathFinding = false;
        }
    }

    public boolean connect(Direction to, int ndist) {
        if (justPropagated) return true;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
            SteamEnergyNetwork newNetwork = ((EnergyNetworkProvider) te).getNetwork();
            justPropagated = true;
            this.propagate(to, newNetwork, ndist);
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        justPropagated = false;
        if (network.isValid()) {
            network.tick();
            if (network.drainHeat(network.getTemperatureLevel() * 0.15F) >= 0.15) {
                setActive(true);
                return;
            }
        }
        setActive(false);
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return true;
    }

    @Override
    public SteamNetworkHolder getHolder() {
        return network;
    }
}
