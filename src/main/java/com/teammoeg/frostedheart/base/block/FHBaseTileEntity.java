/*
 * Copyright (c) 2024 TeamMoeg
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
 *
 */

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
    public void markBlockForUpdate(BlockPos pos, BlockState newState) {
        BlockState state = level.getBlockState(pos);
        if (newState == null)
            newState = state;
        level.sendBlockUpdated(pos, state, newState, 3);
    }
}
