package com.teammoeg.frostedheart.content.town.mine;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class MineBaseState extends WorkerState {
	@Getter
	private List<BlockPos> linkedMines=new ArrayList<>();
	public MineBaseState() {
		
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
