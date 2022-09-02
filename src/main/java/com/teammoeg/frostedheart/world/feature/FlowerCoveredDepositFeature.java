package com.teammoeg.frostedheart.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.BlockStateFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import java.util.Random;

public class FlowerCoveredDepositFeature extends Feature<BlockStateFeatureConfig> {
    public FlowerCoveredDepositFeature(Codec<BlockStateFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, BlockStateFeatureConfig config) {
        while(true) {
            moveDownUntilDirt: {
                // check whether the block beneath pos is dirt
                if (pos.getY() > 3) {
                    if (reader.isAirBlock(pos.down())) {
                        break moveDownUntilDirt;
                    }

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
                int flowerCount = 0;
                for(BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add(-xWidth, 0, -zWidth), pos.add(xWidth, 0, zWidth))) {
                    // randomly place flower
                    if (rand.nextInt(2) == 0  && blockpos.distanceSq(pos) <= (radius * radius)) {
                        BlockState flowerToPlace = Blocks.OXEYE_DAISY.getDefaultState();
                        // valid plant position and open to air and not on ice
                        if (flowerToPlace.isValidPosition(reader, blockpos) && reader.isAirBlock(pos.up())
                                && !reader.getBlockState(pos.down()).getBlock().matchesBlock(Blocks.ICE)
                        ) {
                            reader.setBlockState(blockpos, Blocks.OXEYE_DAISY.getDefaultState(), 4);
                            flowerCount++;
                        }
                    }
                    if (flowerCount == 5) {
                        break;
                    }
                }

                // move pos down by two blocks to hide clay
                pos = pos.down(2);
                for(BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add(-xWidth, -depth, -zWidth), pos.add(xWidth, 0, zWidth))) {
                    if (blockpos.distanceSq(pos.up(2)) <= (radius * radius)) {
                        if (
                                config.state.isValidPosition(reader, blockpos)
                                && !reader.isAirBlock(blockpos.up())  // not exposed in air or snow
                                && !reader.getBlockState(blockpos.up()).getBlock().matchesBlock(Blocks.SNOW)
                        )
                            reader.setBlockState(blockpos, config.state, 4);
                    }
                }

                return true;
            }

            // try next block beneath
            pos = pos.down();
        }
    }
}
