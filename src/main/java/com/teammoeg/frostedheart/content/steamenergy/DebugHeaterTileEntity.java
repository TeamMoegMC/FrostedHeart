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
import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements INetworkConsumer,EnergyNetworkProvider, ITickableTileEntity {

    HeatEnergyNetwork manager = new HeatEnergyNetwork(this, c -> {
        for (Direction d : Direction.values()) {
            c.accept(pos.offset(d), d.getOpposite());
        }
    });
    public DebugHeaterTileEntity() {
        super(FHTileTypes.DEBUGHEATER.get());
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return true;
    }

    @Override
    public boolean connect(HeatEnergyNetwork manager,Direction to, int distance) {
        return false;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
    }

    @Override
    public void tick() {
        manager.tick();
        manager.fillHeat(Integer.MAX_VALUE);
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
    }

	@Override
	public HeatEnergyNetwork getNetwork() {
		return manager;
	}
}
