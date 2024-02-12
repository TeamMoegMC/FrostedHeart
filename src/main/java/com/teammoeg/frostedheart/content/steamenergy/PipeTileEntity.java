package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.base.block.FluidPipeBlock;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

public abstract class PipeTileEntity extends IEBaseTileEntity implements ITickableTileEntity{

	public PipeTileEntity(TileEntityType<? extends TileEntity> type) {
		super(type);
		// TODO Auto-generated constructor stub
	}
    @Override
    public void tick() {
    	/*BlockState bs=this.getBlockState();
    	BlockState obs=bs;
    	FluidPipeBlock block=(FluidPipeBlock) bs.getBlock();
        for (Direction d : Direction.values())
            bs=bs.with(FluidPipeBlock.RIM_PROPERTY_MAP.get(d), block.shouldDrawRim(this.getWorld(), pos, bs, d));
        bs=bs.with(FluidPipeBlock.CASING, block.shouldDrawCasing(this.getWorld(), pos, bs));*/
        //if(obs!=bs)
        //	this.getWorld().setBlockState(getPos(), bs);
    }

}
