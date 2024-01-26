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

package com.teammoeg.frostedheart.world.feature;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.BlockStateFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

public class FlowerCoveredDepositFeature extends Feature<BlockStateFeatureConfig> {
    public FlowerCoveredDepositFeature(Codec<BlockStateFeatureConfig> codec) {
        super(codec);
    }
    
    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, BlockStateFeatureConfig config) {
        while (true) {
            moveDownUntilDirt:
            {
                // check whether the block beneath pos is dirt
                if (reader.getBlockState(pos).isIn(BlockTags.ICE))//do not generate in ice or
                    return false;
                if (pos.getY() > 3) {
                    if (reader.isAirBlock(pos.down())) {
                        break moveDownUntilDirt;
                    }
                    if (!reader.getFluidState(pos).isEmpty())
                        return false;

                    Block block = reader.getBlockState(pos.down()).getBlock();
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
                /*int flowerCount = 0;
                for (BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add(-xWidth, 0, -zWidth), pos.add(xWidth, 0, zWidth))) {
                    // randomly place flower
                    if (rand.nextInt(5) == 0 && blockpos.distanceSq(pos) <= (radius * radius)) {
                        BlockState flowerToPlace = Blocks.OXEYE_DAISY.getDefaultState();
                        // valid plant position and open to air and not on ice
                        if (flowerToPlace.isValidPosition(reader, blockpos) && reader.isAirBlock(pos.up())
                                && !reader.getBlockState(pos.down()).getBlock().matchesBlock(Blocks.ICE)
                        ) {
                            reader.setBlockState(blockpos, Blocks.OXEYE_DAISY.getDefaultState(), 4);
                            flowerCount++;
                        }
                    }
                    if (flowerCount == 3) {
                        break;
                    }
                }*/

                // move pos down by two blocks to hide clay
                BlockPos opos=pos;
                pos = pos.down(2);
                //pos=pos.up();
                Set<BlockPos> empties=new HashSet<>();
                Set<BlockPos> filled=new HashSet<>();
                for (BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add(-xWidth, -depth, -zWidth), pos.add(xWidth, 0, zWidth))) {
                    if (blockpos.distanceSq(pos) <= (radius * radius)) {
                        if (
                                config.state.isValidPosition(reader, blockpos)
                                && !reader.getBlockState(blockpos).getBlock().matchesBlock(Blocks.SNOW)
                                && !reader.getBlockState(blockpos).getBlock().isIn(BlockTags.ICE)
                                && !reader.getBlockState(blockpos).getBlock().matchesBlock(Blocks.AIR)
                                        //&& !reader.isAirBlock(blockpos.up())  // not exposed in air or snow
                                        //&& !reader.getBlockState(blockpos.up()).getBlock().matchesBlock(Blocks.SNOW)
                        ) {
                        	BlockPos upos=blockpos.up(2);
                        	for(Direction d:Direction.Plane.HORIZONTAL) {
                        		empties.add(upos.offset(d));
                        	}
                        	filled.add(upos);
                            reader.setBlockState(upos, config.state, 4);
                        }
                    }
                }
                empties.removeAll(filled);
                for(BlockPos bp:empties) {
                	if(reader.getBlockState(bp).isAir()) {
                		reader.setBlockState(bp, Blocks.SNOW.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 8),4);
                	}
                }

                return true;
            }

            // try next block beneath
            pos = pos.down();
        }
    }
}
