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

package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ThinIceBlock extends IceBlock {
    public ThinIceBlock(Properties pProperties) {
        super(pProperties);
    }

    // no random tick
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adj, Direction side) {
        if (adj.is(this)) {
                return true;
        }
        if (adj.is(FHBlocks.LAYERED_THIN_ICE.get()) && adj.hasProperty(LayeredThinIceBlock.LAYERS)) {
            return adj.getValue(LayeredThinIceBlock.LAYERS).equals(LayeredThinIceBlock.MAX_HEIGHT);
        }
        return super.skipRendering(state, adj, side);
    }
}
