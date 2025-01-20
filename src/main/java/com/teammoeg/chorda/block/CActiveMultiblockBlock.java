package com.teammoeg.chorda.block;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.NonMirrorableWithActiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class CActiveMultiblockBlock<S extends IMultiblockState> extends NonMirrorableWithActiveBlock<S> {

	public CActiveMultiblockBlock(Properties properties, MultiblockRegistration<S> multiblock) {
		super(properties, multiblock);
	}

	@Override
	public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return 0.8f;
	}

}
