/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.multiblock.components.OwnerState;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class LogisticState extends OwnerState {
	LogisticNetwork ln;
	StoredCapability<LogisticNetwork> cap;
	Level level;
	BlockPos worldPosition;
	CompoundTag pendingNetworkData;
	LazyTickWorker ticker=new LazyTickWorker(20,()->{
		
		ChunkPos cp=new ChunkPos(worldPosition);
		for(int i=cp.x-1;i<=cp.x+1;i++)
			for(int j=cp.z-1;j<=cp.z+1;j++) {
				if(level.hasChunk(i, j)) {
					FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(
					level.getChunk(i, j)
					).ifPresent(chunk->chunk.register(worldPosition));
				}
			}
		
	});
	public LogisticState() {
	}

	void initialize(Level level,BlockPos worldPosition,Runnable markDirty) {
		this.level=level;
		this.worldPosition=worldPosition;
		if(ln==null) {
			ln=new LogisticNetwork(level,worldPosition,markDirty);
			if(pendingNetworkData!=null) {
				ln.load(pendingNetworkData);
				pendingNetworkData=null;
			}
			cap=new StoredCapability<>(ln);
			ticker.enqueue();
		}
	}

	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
		CompoundTag networkData=new CompoundTag();
		if(ln!=null)
			ln.save(networkData);
		else if(pendingNetworkData!=null)
			networkData=pendingNetworkData.copy();
		nbt.put("logisticNetwork",networkData);
	}

	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
		CompoundTag networkData=nbt.contains("logisticNetwork",Tag.TAG_COMPOUND)?nbt.getCompound("logisticNetwork"):nbt;
		if(ln!=null)
			ln.load(networkData);
		else
			pendingNetworkData=networkData.copy();
	}

	void shutdown() {
		if(ln!=null)
			ln.shutdown();
		releaseRegistrations();
	}

	private void releaseRegistrations() {
		if(level==null||worldPosition==null)
			return;
		ChunkPos cp=new ChunkPos(worldPosition);
		for(int x=cp.x-1;x<=cp.x+1;x++)
			for(int z=cp.z-1;z<=cp.z+1;z++)
				if(level.hasChunk(x,z))
					FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(level.getChunk(x,z))
						.ifPresent(chunk->chunk.release(worldPosition));
	}

}
