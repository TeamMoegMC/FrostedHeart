package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.multiblock.components.OwnerState;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class LogisticState extends OwnerState {
	LogisticNetwork ln;
	StoredCapability<LogisticNetwork> cap;
	Level level;
	BlockPos worldPosition;
	LazyTickWorker ticker=new LazyTickWorker(20,()->{
		
		ChunkPos cp=new ChunkPos(worldPosition);
		for(int i=cp.x-1;i<=cp.x+1;i++)
			for(int j=cp.z-1;j<=cp.z+1;j++) {
				if(level.hasChunk(i, j)) {
					FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(
					level.getChunk(i, j)
					).resolve().get().register(worldPosition);
				}
			}
		
	});
	public LogisticState() {
	}

	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
	}

	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
	}

}
