package com.teammoeg.frostedheart.base.block;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

public abstract class PipeTileEntity extends IEBaseTileEntity implements ITickableTileEntity{

	public PipeTileEntity(TileEntityType<? extends TileEntity> type) {
		super(type);

	}
    @Override
    public void tick() {

    }
    public abstract void onFaceChange(Direction dir,boolean isConnect);

}
