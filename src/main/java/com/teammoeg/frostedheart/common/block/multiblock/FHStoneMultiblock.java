package com.teammoeg.frostedheart.common.block.multiblock;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

public abstract class FHStoneMultiblock extends IETemplateMultiblock {
    public FHStoneMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, Supplier<BlockState> baseState)
    {
        super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
    }

    @Override
    public boolean canBeMirrored()
    {
        return false;
    }

    @Override
    public Direction transformDirection(Direction original)
    {
        return original.getOpposite();
    }

    @Override
    public Direction untransformDirection(Direction transformed)
    {
        return transformed.getOpposite();
    }

    @Override
    public BlockPos multiblockToModelPos(BlockPos posInMultiblock)
    {
        return super.multiblockToModelPos(new BlockPos(
                getSize(null).getX()-posInMultiblock.getX()-1,
                posInMultiblock.getY(),
                getSize(null).getZ()-posInMultiblock.getZ()-1
        ));
    }
}
