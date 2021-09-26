/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.multiblock;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

public abstract class FHStoneMultiblock extends IETemplateMultiblock {
    public FHStoneMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, Supplier<BlockState> baseState) {
        super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
    }

    @Override
    public boolean canBeMirrored() {
        return false;
    }

    @Override
    public Direction transformDirection(Direction original) {
        return original.getOpposite();
    }

    @Override
    public Direction untransformDirection(Direction transformed) {
        return transformed.getOpposite();
    }

    @Override
    public BlockPos multiblockToModelPos(BlockPos posInMultiblock) {
        return super.multiblockToModelPos(new BlockPos(
                getSize(null).getX() - posInMultiblock.getX() - 1,
                posInMultiblock.getY(),
                getSize(null).getZ() - posInMultiblock.getZ() - 1
        ));
    }
}
