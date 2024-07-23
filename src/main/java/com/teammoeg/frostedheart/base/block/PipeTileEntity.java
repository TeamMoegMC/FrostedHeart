package com.teammoeg.frostedheart.base.block;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;

public abstract class PipeTileEntity extends IEBaseTileEntity implements TickableBlockEntity{

	public PipeTileEntity(BlockEntityType<? extends BlockEntity> type) {
		super(type);

	}
    @Override
    public void tick() {

    }
    public abstract void onFaceChange(Direction dir,boolean isConnect);

}
