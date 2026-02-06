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

package com.teammoeg.frostedheart.content.steamenergy.fountain;

import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FountainNozzleBlock extends CBlock {
    public static final IntegerProperty HEIGHT =
            IntegerProperty.create("height", 0, FountainTileEntity.MAX_HEIGHT);

    public FountainNozzleBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(HEIGHT, 0));
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource random) {
        super.animateTick(stateIn, worldIn, pos, random);
        final int height = stateIn.getValue(HEIGHT);
        if (height > 0) {
            double velocityY = -height - 0.5D;
            double y = pos.getY() + 0.5D;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                double velocityX = direction.getStepX() * (height * FountainTileEntity.RANGE_PER_NOZZLE - 0.5);
                double velocityZ = direction.getStepZ() * (height * FountainTileEntity.RANGE_PER_NOZZLE - 0.5);
                double x = pos.getX() + 0.5D + direction.getStepX() * 0.55D;
                double z = pos.getZ() + 0.5D + direction.getStepZ() * 0.55D;

                worldIn.addAlwaysVisibleParticle(FHParticleTypes.WET_STEAM.get(), true,
                        x + random.nextDouble() * 0.5 * direction.getStepX(),
                        y,
                        z - random.nextDouble() * 0.5 * direction.getStepZ(),
                        velocityX + random.nextDouble() * 0.3 * direction.getStepX(),
                        velocityY,
                        velocityZ + random.nextDouble() * 0.3 * direction.getStepY()
                );

                if (random.nextInt(height) != 0) {
                    worldIn.addAlwaysVisibleParticle(FHParticleTypes.WET_STEAM.get(), true,
                            x + random.nextDouble() * 0.5 * direction.getStepX(),
                            y,
                            z - random.nextDouble() * 0.5 * direction.getStepZ(),
                            velocityX,
                            velocityY,
                            velocityZ
                    );
                }

                if (random.nextInt(height) != 0) {
                    worldIn.addParticle(FHParticleTypes.WET_STEAM.get(),
                            x,
                            y,
                            z,
                            velocityX,
                            velocityY,
                            velocityZ
                    );
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HEIGHT) > 0;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        // Check if the nozzle is still valid
        int height = state.getValue(HEIGHT);
        if (height > 0) {
            boolean invalid;

            if (!(invalid = !world.getBlockState(pos.below()).hasProperty(HEIGHT)))
                invalid = world.getBlockState(pos.below()).getValue(HEIGHT) != height - 1;

            if (!invalid)
                invalid = world.getBlockState(pos.below(height)).getBlock() != FHBlocks.FOUNTAIN_BASE.get() &&
                        world.getBlockState(pos.below(height)).getBlock() != FHBlocks.FOUNTAIN_NOZZLE.get();

            if (invalid) {
                world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 0));
                return;
            }

            // Spawn water
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
            BlockPos base = pos.below(height);
            BlockPos target = base.relative(direction, height);

            if (world.getBlockState(target).isAir()) {
                world.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos pos2, boolean p_220069_6_) {
        super.neighborChanged(state, world, pos, block, pos2, p_220069_6_);

        int height = state.getValue(HEIGHT);
        BlockState below = world.getBlockState(pos.below());

        if (height > 0) {
            if (below.getBlock() == FHBlocks.FOUNTAIN_BASE.get()) {
                if (height != 1) world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 1));
            } else if (below.getBlock() == FHBlocks.FOUNTAIN_NOZZLE.get()) {
                if (below.getValue(HEIGHT) != height - 1)
                    world.setBlockAndUpdate(pos, state.setValue(HEIGHT, below.getValue(HEIGHT) + 1));
            } else {
                world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 0));
            }
        }
    }

}
