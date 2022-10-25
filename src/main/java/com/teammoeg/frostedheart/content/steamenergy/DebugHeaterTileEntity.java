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

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements HeatProvider, INetworkConsumer, ITickableTileEntity {
    public DebugHeaterTileEntity() {
        super(FHTileTypes.DEBUGHEATER.get());
    }

    SteamEnergyNetwork network = new SteamEnergyNetwork(this);
    HeatProviderManager manager=new HeatProviderManager(this,c->{
    	for (Direction d : Direction.values()) {
    		c.accept(pos.offset(d), d.getOpposite());
        }
    });
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
    public float getTemperatureLevel() {
        return this.getBlockState().get(BlockStateProperties.LEVEL_1_8);
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return true;
    }

    @Override
    public boolean connect(Direction to, int distance) {
        return false;
    }


    @Override
    public void tick() {
    	manager.tick();
    }

    @Override
    public SteamNetworkHolder getHolder() {
        return null;
    }
}
