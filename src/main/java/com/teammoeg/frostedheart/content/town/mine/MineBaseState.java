package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class MineBaseState extends WorkerState {
	ResourceLocation biomePath;
	public MineBaseState() {
		
	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
		if(biomePath!=null)
		tag.putString("biome",biomePath.toString());
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
		if(tag.contains("biome"))
			biomePath=new ResourceLocation(tag.getString("biome"));
	}

}
