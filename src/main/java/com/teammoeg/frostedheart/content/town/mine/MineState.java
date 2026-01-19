package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class MineState extends WorkerState {
	@Getter
	@Setter
	private BlockPos connectedBase;
	public MineState() {
		
	}

	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		// TODO Auto-generated method stub
		super.writeNBT(tag, isNetwork);
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		// TODO Auto-generated method stub
		super.readNBT(tag, isNetwork);
	}

}
