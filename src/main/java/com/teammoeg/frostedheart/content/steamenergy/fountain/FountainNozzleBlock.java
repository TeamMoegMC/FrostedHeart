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

package com.teammoeg.frostedheart.content.steamenergy.fountain;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.FHParticleTypes;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import dev.ftb.mods.ftbteams.property.IntProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import net.minecraft.block.AbstractBlock.Properties;

public class FountainNozzleBlock extends FHBaseBlock {
    public static final IntegerProperty HEIGHT =
            IntegerProperty.create("height", 0, FountainTileEntity.MAX_HEIGHT);

    public FountainNozzleBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(HEIGHT, 0));
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random random) {
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEIGHT);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HEIGHT) > 0;
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        // Check if the nozzle is still valid
        int height = state.getValue(HEIGHT);
        if (height > 0) {
            boolean invalid;

            if (!(invalid = !world.getBlockState(pos.below()).hasProperty(HEIGHT)))
                invalid = world.getBlockState(pos.below()).getValue(HEIGHT) != height - 1;

            if (!invalid)
                invalid = world.getBlockState(pos.below(height)).getBlock() != FHBlocks.fountain.get() &&
                          world.getBlockState(pos.below(height)).getBlock() != FHBlocks.fountain_nozzle.get();

            if (invalid) {
                world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 0));
                return;
            }

            // Spawn water
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
            BlockPos base = pos.below(height);
            BlockPos target = base.relative(direction, height);

            if (world.getBlockState(target).isAir(world, target)) {
                world.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos pos2, boolean p_220069_6_) {
        super.neighborChanged(state, world, pos, block, pos2, p_220069_6_);

        int height = state.getValue(HEIGHT);
        BlockState below = world.getBlockState(pos.below());

        if (height > 0) {
            if (below.getBlock() == FHBlocks.fountain.get()) {
                if (height != 1) world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 1));
            } else if (below.getBlock() == FHBlocks.fountain_nozzle.get()) {
                if (below.getValue(HEIGHT) != height - 1)
                    world.setBlockAndUpdate(pos, state.setValue(HEIGHT, below.getValue(HEIGHT) + 1));
            } else {
                world.setBlockAndUpdate(pos, state.setValue(HEIGHT, 0));
            }
        }
    }

}
