package com.teammoeg.chorda.block.entity;

import net.minecraft.core.BlockPos;

/**
 * Interface for any connected blocks
 * */
public interface ConnectedBlockEntity {
	public BlockPos getMasterPos();
}
