package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import com.teammoeg.frostedheart.base.multiblock.components.OwnerState;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;

public interface OwnedMultiblockLogic<R extends OwnerState> {

	void onOwnerChange(IMultiblockContext<R> ctx);

}