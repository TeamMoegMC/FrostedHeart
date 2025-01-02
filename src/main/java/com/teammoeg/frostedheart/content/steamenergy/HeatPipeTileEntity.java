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

import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;
import com.teammoeg.frostedheart.base.block.FluidPipeBlock;
import com.teammoeg.frostedheart.base.block.PipeTileEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class HeatPipeTileEntity extends PipeTileEntity implements FHTickableBlockEntity, EnergyNetworkProvider, INetworkConsumer {
    HeatEnergyNetwork ntwk;
    int cnt = 1;

    public HeatPipeTileEntity(BlockPos l, BlockState state) {
        super(FHBlockEntityTypes.HEATPIPE.get(), l, state);
    }

    @Override
    public boolean canConnectTo(Direction to) {
        return true;
    }

    public boolean connect(HeatEnergyNetwork network, Direction to, int ndist) {
        if (ntwk == null || ntwk.getNetworkSize() < network.getNetworkSize()) {
            ntwk = network;
        }
        if (ntwk.shouldPropagate(getBlockPos(), ndist)) {
            this.propagate(to, ntwk, ndist);
        }
        return true;
    }

    public void connectTo(Direction d, HeatEnergyNetwork network, int lengthx) {
        BlockPos n = this.getBlockPos().relative(d);

        d = d.getOpposite();
        HeatCapabilities.connect(network, getLevel(), n, d, lengthx + 1);

    }

    protected void propagate(Direction from, HeatEnergyNetwork network, int lengthx) {
        for (Direction d : Direction.values()) {
            if (from == d) continue;
            connectTo(d, network, lengthx);
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (descPacket) {
        }
    }

    @Override
    public void tick() {
        if (cnt > 0) {
            cnt--;
        } else {
            cnt = 10;
            BlockState bs = this.getBlockState();
            for (Direction dir : Direction.values()) {
                if (bs.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                    onFaceChange(dir, true);
                }
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (descPacket) {
        }
    }

    @Override
    public void onFaceChange(Direction dir, boolean isConnect) {
        if (ntwk == null) return;
        if (isConnect)
            ntwk.startPropagation(this, dir);
        else
            ntwk.requestUpdate();
    }

    @Override
    public HeatEnergyNetwork getNetwork() {
        return ntwk;
    }
}
