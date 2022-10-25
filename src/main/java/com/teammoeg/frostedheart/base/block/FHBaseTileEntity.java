package com.teammoeg.frostedheart.base.block;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;

public abstract class FHBaseTileEntity extends IEBaseTileEntity {

	public FHBaseTileEntity(TileEntityType<? extends TileEntity> type) {
		super(type);
	}
    @Override
    public void markBlockForUpdate(BlockPos pos, BlockState newState)
	{
		BlockState state = world.getBlockState(pos);
		if(newState==null)
			newState = state;
		world.notifyBlockUpdate(pos, state, newState, 3);
	}
}
