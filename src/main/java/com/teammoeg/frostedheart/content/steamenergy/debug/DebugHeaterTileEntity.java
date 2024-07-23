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

package com.teammoeg.frostedheart.content.steamenergy.debug;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatProviderEndPoint;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements  TickableBlockEntity {

    HeatEnergyNetwork manager = new HeatEnergyNetwork(this, c -> {
        for (Direction d : Direction.values()) {
            c.accept(worldPosition.relative(d), d.getOpposite());
        }
    });
    HeatProviderEndPoint endpoint=new HeatProviderEndPoint(-1, Integer.MAX_VALUE, Integer.MAX_VALUE);
    public DebugHeaterTileEntity() {
        super(FHTileTypes.DEBUGHEATER.get());
    }


    @Override      
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
    }

    @Override
    public void tick() {
        if((!endpoint.hasValidNetwork()||manager.data.size()<=1)&&!manager.isUpdateRequested()) {
        	manager.requestSlowUpdate();
        }
        endpoint.addHeat(Integer.MAX_VALUE);
    	manager.tick();
        
    }
    LazyOptional<HeatProviderEndPoint> heatcap=LazyOptional.of(()->endpoint);
    @Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==FHCapabilities.HEAT_EP.capability())
			return heatcap.cast();
		return super.getCapability(cap, side);
	}

	@Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
    }

}
