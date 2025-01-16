package com.teammoeg.chorda.multiblock.components;

import java.util.UUID;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;

public interface IOwnerState<T> {

	UUID getOwner();

	void setOwner(UUID owner);

	void onOwnerChange(IMultiblockContext<? extends T> ctx);
}