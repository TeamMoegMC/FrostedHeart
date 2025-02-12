package com.teammoeg.chorda.block.entity;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockStateAccess {
	public BlockState getBlock();
	public void setBlock(BlockState state);
}
