/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

public class CooledMagmaBlock extends Block {
    private static final int HEATING_CHECK_INTERVAL = 24000; // Ticks between heating checks
    private static final float HEATING_CHANCE = 1F / 2F; // On average it needs to heat 2 days

    public CooledMagmaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        BubbleColumnBlock.updateColumn(level, pos.above(), state);
        level.scheduleTick(pos, this, HEATING_CHECK_INTERVAL);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BubbleColumnBlock.updateColumn(level, pos.above(), state);

        // Schedule another tick for cooling check
        level.scheduleTick(pos, this, HEATING_CHECK_INTERVAL);

        // Check for cooling condition
        if (frostedHeart$isTouchingLava(level, pos)) {
            // Check temperature condition (you may want additional conditions)
            if (random.nextFloat() < HEATING_CHANCE) {
                // Transform to cooled magma block
                BlockState heated = Blocks.MAGMA_BLOCK.defaultBlockState();
                level.setBlockAndUpdate(pos, heated);
            }
        }
    }

    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pFacing == Direction.UP && pFacingState.is(Blocks.WATER)) {
            pLevel.scheduleTick(pCurrentPos, this, 20);
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    /**
     * Helper method to check if a block is touching lava on any side
     */
    private boolean frostedHeart$isTouchingLava(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
