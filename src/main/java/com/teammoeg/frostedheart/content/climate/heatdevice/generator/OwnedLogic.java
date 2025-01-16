package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import com.teammoeg.chorda.multiblock.components.OwnerState;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;

public interface OwnedLogic<R extends OwnerState> {

    void onOwnerChange(IMultiblockContext<R> ctx);

}