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

import com.teammoeg.frostedheart.FHContent;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements HeatProvider, IConnectable {
    public DebugHeaterTileEntity() {
        super(FHContent.FHTileTypes.DEBUGHEATER.get());
    }

    SteamEnergyNetwork network = new SteamEnergyNetwork(this);

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
    public boolean disconnectAt(Direction to) {
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof IConnectable && !(te instanceof HeatProvider)) {
            ((IConnectable) te).disconnectAt(to.getOpposite());
        }
        return true;
    }

    @Override
    public boolean connectAt(Direction to) {
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof IConnectable && !(te instanceof HeatProvider)) {
            ((IConnectable) te).connectAt(to.getOpposite());
        }
        return true;
    }


    @Override
    public int getTemperatureLevel() {
        return 3;
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return true;
    }

}
