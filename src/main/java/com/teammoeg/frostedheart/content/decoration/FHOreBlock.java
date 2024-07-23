/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.decoration;

import com.cannolicatfish.rankine.blocks.RankineOreBlock;
import com.cannolicatfish.rankine.util.WorldgenUtils;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public class FHOreBlock extends FHBaseBlock {
    public FHOreBlock(Properties blockProps) {
        super(blockProps);
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(RankineOreBlock.TYPE);
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getLevel();
        BlockState target = world.getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
        if (target.getBlock() instanceof RankineOreBlock) {
            return this.defaultBlockState().setValue(RankineOreBlock.TYPE, target.getValue(RankineOreBlock.TYPE));
        } else {
            return WorldgenUtils.ORE_STONES.contains(target.getBlock()) ? this.defaultBlockState().setValue(RankineOreBlock.TYPE, WorldgenUtils.ORE_STONES.indexOf(target.getBlock())) : this.defaultBlockState().setValue(RankineOreBlock.TYPE, 0);
        }
    }
}
