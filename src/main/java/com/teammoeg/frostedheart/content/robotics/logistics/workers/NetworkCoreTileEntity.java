/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class NetworkCoreTileEntity extends CBlockEntity implements CTickableBlockEntity{
	LogisticNetwork ln;
	LazyOptional<LogisticNetwork> cap;
	public NetworkCoreTileEntity( BlockPos pos, BlockState state) {
		super(FHBlockEntityTypes.NETWORK_CORE.get(),pos, state);
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		// TODO Auto-generated method stub
		
	}
	LazyTickWorker ticker=new LazyTickWorker(20,()->{
		
			ChunkPos cp=new ChunkPos(worldPosition);
			for(int i=cp.x-1;i<=cp.x+1;i++)
				for(int j=cp.z-1;j<=cp.z+1;j++) {
					//FHMain.LOGGER.info(i+","+j);
					if(level.hasChunk(i, j)) {
						//FHMain.LOGGER.info("has chunk");
						FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(
						level.getChunk(i, j)
						).resolve().get().register(worldPosition);
						
					}
				}
		
	});
	@Override
	public void tick() {
		if(!this.level.isClientSide) {
			if(ln==null) {
				ln=new LogisticNetwork(level,worldPosition);
				cap=LazyOptional.of(()->ln);
				ticker.enqueue();
			}
	
			ticker.tick();
			ln.tick();
		}
		
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { 
		if(cap==FHCapabilities.LOGISTIC.capability()) {
			return this.cap.cast();
		}
		return super.getCapability(cap, side);
	}
	@Override
	public void onRemoved() {
		super.onRemoved();
		if(cap!=null)
		cap.invalidate();
	}
}
