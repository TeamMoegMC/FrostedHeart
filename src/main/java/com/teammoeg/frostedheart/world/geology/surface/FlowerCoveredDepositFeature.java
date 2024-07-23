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

package com.teammoeg.frostedheart.world.geology.surface;

import java.util.Random;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.Feature;

public class FlowerCoveredDepositFeature extends Feature<BlockStateConfiguration> {
    public FlowerCoveredDepositFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(WorldGenLevel reader, ChunkGenerator generator, Random rand, BlockPos pos, BlockStateConfiguration config) {
        while (true) {
            moveDownUntilDirt:
            {
                // check whether the block beneath pos is dirt
                if (reader.getBlockState(pos).is(BlockTags.ICE))//do not generate in ice or
                    return false;
                if (pos.getY() > 3) {
                    if (reader.isEmptyBlock(pos.below())) {
                        break moveDownUntilDirt;
                    }
                    if (!reader.getFluidState(pos).isEmpty())
                        return false;

                    Block block = reader.getBlockState(pos.below()).getBlock();
                    if (!isDirt(block)) {
                        break moveDownUntilDirt;
                    }
                }

                if (pos.getY() <= 3) {
                    return false;
                }


                // now pos is located at level above dirt, generate flower
                int xWidth = 3 + rand.nextInt(2);
                int zWidth = 3 + rand.nextInt(2);
                int depth = 1 + rand.nextInt(3);
                double radius = (xWidth + zWidth + depth) * 0.333F + 0.5D;
                int flowerCount = 0;
                for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-xWidth, 0, -zWidth), pos.offset(xWidth, 0, zWidth))) {
                    // randomly place flower
                    if (rand.nextInt(5) == 0 && blockpos.distSqr(pos) <= (radius * radius)) {
                        BlockState flowerToPlace = Blocks.OXEYE_DAISY.defaultBlockState();
                        // valid plant position and open to air and not on ice
                        if (flowerToPlace.canSurvive(reader, blockpos) && reader.isEmptyBlock(pos.above())
                                && !reader.getBlockState(pos.below()).getBlock().is(Blocks.ICE)
                        ) {
                            reader.setBlock(blockpos, Blocks.OXEYE_DAISY.defaultBlockState(), 4);
                            flowerCount++;
                        }
                    }
                    if (flowerCount == 3) {
                        break;
                    }
                }

                // move pos down by two blocks to hide clay
                pos = pos.below(2);
                for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-xWidth, -depth, -zWidth), pos.offset(xWidth, 0, zWidth))) {
                    if (blockpos.distSqr(pos.above(2)) <= (radius * radius)) {
                        if (
                                config.state.canSurvive(reader, blockpos)
                                        && !reader.isEmptyBlock(blockpos.above())  // not exposed in air or snow
                                        && !reader.getBlockState(blockpos.above()).getBlock().is(Blocks.SNOW)
                        )
                            reader.setBlock(blockpos, config.state, 4);
                    }
                }

                return true;
            }

            // try next block beneath
            pos = pos.below();
        }
    }
}
