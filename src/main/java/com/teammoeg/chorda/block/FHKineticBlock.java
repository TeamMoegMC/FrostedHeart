/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.chorda.block;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FHKineticBlock extends HorizontalKineticBlock {
    protected int lightOpacity;

    public FHKineticBlock(Properties blockProps) {
        super(blockProps.dynamicShape());
        lightOpacity = 15;

    }


    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        if (state.isSolidRender(worldIn, pos))
            return lightOpacity;
        else
            return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
    }

    public FHKineticBlock setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return this;
    }
}
