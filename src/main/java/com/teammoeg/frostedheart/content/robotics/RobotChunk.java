package com.teammoeg.frostedheart.content.robotics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class RobotChunk implements ICapabilitySerializable<CompoundNBT> {
	List<BlockPos> poss=new ArrayList<>();
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		return null;
	}
	long hashCode(BlockPos bp) {
		return bp.getY()<<56+bp.getX()<<28+bp.getZ();
	}
	public void addContent() {
		
	}
	@Override
	public CompoundNBT serializeNBT() {
		return null;
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {
	}

}
