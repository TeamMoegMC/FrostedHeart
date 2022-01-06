/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.world;

import java.util.BitSet;
import java.util.Random;

import com.cannolicatfish.rankine.blocks.RankineOreBlock;
import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;

public class FHOreFeature extends Feature<FHOreFeatureConfig> {
    public FHOreFeature(Codec<FHOreFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, FHOreFeatureConfig config) {
        float f = rand.nextFloat() * (float) Math.PI;
        float f1 = config.size / 8.0F;
        int i = MathHelper.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double maxX = pos.getX() + Math.sin(f) * f1;
        double minX = pos.getX() - Math.sin(f) * f1;
        double maxZ = pos.getZ() + Math.cos(f) * f1;
        double minZ = pos.getZ() - Math.cos(f) * f1;
        int j = 2;
        double maxY = pos.getY() + rand.nextInt(3) - 2;
        double minY = pos.getY() + rand.nextInt(3) - 2;
        int startX = pos.getX() - MathHelper.ceil(f1) - i;
        int startY = pos.getY() - 2 - i;
        int startZ = pos.getZ() - MathHelper.ceil(f1) - i;
        int maxSizeXZ = 2 * (MathHelper.ceil(f1) + i);
        int maxSizeY = 2 * (2 + i);

        for (int l1 = startX; l1 <= startX + maxSizeXZ; ++l1) {
            for (int i2 = startZ; i2 <= startZ + maxSizeXZ; ++i2) {
                if (startY <= reader.getHeight(Heightmap.Type.OCEAN_FLOOR_WG, l1, i2)) {
                    return this.generate(reader, rand, config, maxX, minX, maxZ, minZ, maxY, minY, startX, startY, startZ, maxSizeXZ, maxSizeY);
                }
            }
        }

        return false;
    }

    protected boolean generate(IWorld worldIn, Random random, FHOreFeatureConfig config, double maxX, double minX,
    		double maxZ, double minZ, double maxY, double minY, int startX, int startY, int startZ, int maxSizeXZ, int maxSizeY) {
        int totalGenerated = 0;
        BitSet generated = new BitSet(maxSizeXZ * maxSizeY * maxSizeXZ);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();
        int size = config.size;
        double[] boxes = new double[size * 4];

        for (int k = 0; k < size; ++k) {
            float sizeRatio = (float) k / (float) size;
            double rX = MathHelper.lerp(sizeRatio, maxX, minX);
            double rY = MathHelper.lerp(sizeRatio, maxY, minY);
            double rZ = MathHelper.lerp(sizeRatio, maxZ, minZ);
            double genSize = random.nextDouble() * size / 16.0D;
            double genAmount = ((MathHelper.sin((float) Math.PI * sizeRatio) + 1.0F) * genSize + 1.0D) / 2.0D;
            boxes[k * 4 + 0] = rX;
            boxes[k * 4 + 1] = rY;
            boxes[k * 4 + 2] = rZ;
            boxes[k * 4 + 3] = genAmount;
        }

        for (int i = 0; i < size - 1; ++i) {
            if (boxes[i * 4 + 3] > 0.0D) {//has generation amount
                for (int j = i + 1; j < size; ++j) {
                    if (boxes[j * 4 + 3] > 0.0D) {//has generation amount
                        double d12 = boxes[i * 4 + 0] - boxes[j * 4 + 0];//delta X
                        double d13 = boxes[i * 4 + 1] - boxes[j * 4 + 1];//delta Y
                        double d14 = boxes[i * 4 + 2] - boxes[j * 4 + 2];//delta Z
                        double d15 = boxes[i * 4 + 3] - boxes[j * 4 + 3];//delta gen amount
                        if (d15 * d15 > d12 * d12 + d13 * d13 + d14 * d14) {//generation difference too large
                            if (d15 > 0.0D) {//outer slice too much
                                boxes[j * 4 + 3] = -1D;//inner slice no gen
                            } else {//inner slice too much
                                boxes[i * 4 + 3] = -1D;//outer slice no gen
                            }
                        }
                    }
                }
            }
        }

        for (int iSlice = 0; iSlice < size; ++iSlice) {
            double genAmount = boxes[iSlice * 4 + 3];//amount
            if (genAmount >= 0D) {
                double sX = boxes[iSlice * 4 + 0];
                double sY = boxes[iSlice * 4 + 1];
                double sZ = boxes[iSlice * 4 + 2];
                int genBeginX = Math.max(MathHelper.floor(sX - genAmount), startX);
                int genBeginY = Math.max(MathHelper.floor(sY - genAmount), startY);
                int genBeginZ = Math.max(MathHelper.floor(sZ - genAmount), startZ);
                int genEndX = Math.max(MathHelper.floor(sX + genAmount), genBeginX);
                int genEndY = Math.max(MathHelper.floor(sY + genAmount), genBeginY);
                int genEndZ = Math.max(MathHelper.floor(sZ + genAmount), genBeginZ);

                for (int dx = genBeginX; dx <= genEndX; ++dx) {
                    double xAmount = (dx + 0.5D - sX) / genAmount;
                    if (xAmount * xAmount < 1.0D) {
                        for (int dy = genBeginY; dy <= genEndY; ++dy) {
                            double yAmount = (dy + 0.5D - sY) / genAmount;
                            if (xAmount * xAmount + yAmount * yAmount < 1.0D) {
                                for (int dz = genBeginZ; dz <= genEndZ; ++dz) {
                                    double zAmount = (dz + 0.5D - sZ) / genAmount;
                                    if (xAmount * xAmount + yAmount * yAmount + zAmount * zAmount < 1.0D) {
                                        int packedLocation = dx - startX + (dy - startY) * maxSizeXZ + (dz - startZ) * maxSizeXZ * maxSizeY;
                                        if (!generated.get(packedLocation)) {//do not overwrite
                                            generated.set(packedLocation);
                                            blockpos$mutable.setPos(dx, dy, dz);
                                            Block b = worldIn.getBlockState(blockpos$mutable).getBlock();
                                            ResourceLocation rs = b.getRegistryName();
                                            if (config.target.test(worldIn.getBlockState(blockpos$mutable), random)) {
                                                    if (rs.getNamespace().equals("rankine")) {
                                                        switch (rs.getPath()) {
                                                            case "gray_granite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 11), 2);
                                                                break;
                                                            case "granodiorite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 12), 2);
                                                                break;
                                                            case "hornblende_andesite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 13), 2);
                                                                break;
                                                            case "tholeiitic_basalt":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 14), 2);
                                                                break;
                                                            case "pyroxene_gabbro":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 15), 2);
                                                                break;
                                                            case "anorthosite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 16), 2);
                                                                break;
                                                            case "rhyolite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 17), 2);
                                                                break;
                                                            case "comendite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 18), 2);
                                                                break;
                                                            case "black_dacite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 19), 2);
                                                                break;
                                                            case "red_dacite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 20), 2);
                                                                break;
                                                            case "red_porphyry":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 21), 2);
                                                                break;
                                                            case "purple_porphyry":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 22), 2);
                                                                break;
                                                            case "pegmatite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 23), 2);
                                                                break;
                                                            case "peridotite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 24), 2);
                                                                break;
                                                            case "troctolite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 25), 2);
                                                                break;
                                                            case "kimberlite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 26), 2);
                                                                break;
                                                            case "komatiite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 27), 2);
                                                                break;
                                                            case "pumice":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 28), 2);
                                                                break;
                                                            case "scoria":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 29), 2);
                                                                break;
                                                            case "white_marble":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 30), 2);
                                                                break;
                                                            case "black_marble":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 31), 2);
                                                                break;
                                                            case "gneiss":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 32), 2);
                                                                break;
                                                            case "mica_schist":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 33), 2);
                                                                break;
                                                            case "phyllite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 34), 2);
                                                                break;
                                                            case "slate":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 35), 2);
                                                                break;
                                                            case "quartzite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 36), 2);
                                                                break;
                                                            case "mariposite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 37), 2);
                                                                break;
                                                            case "skarn":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 38), 2);
                                                                break;
                                                            case "ringwoodite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 39), 2);
                                                                break;
                                                            case "wadsleyite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 40), 2);
                                                                break;
                                                            case "bridgmanite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 41), 2);
                                                                break;
                                                            case "ferropericlase":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 42), 2);
                                                                break;
                                                            case "perovskite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 43), 2);
                                                                break;
                                                            case "tufa_limestone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 44), 2);
                                                                break;
                                                            case "dolostone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 45), 2);
                                                                break;
                                                            case "chalk":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 46), 2);
                                                                break;
                                                            case "carbonaceous_shale":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 47), 2);
                                                                break;
                                                            case "siltstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 48), 2);
                                                                break;
                                                            case "quartz_sandstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 49), 2);
                                                                break;
                                                            case "arkose_sandstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 50), 2);
                                                                break;
                                                            case "mudstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 51), 2);
                                                                break;
                                                            case "breccia":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 52), 2);
                                                                break;
                                                            case "meteorite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 53), 2);
                                                                break;
                                                            case "entstatite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 54), 2);
                                                                break;
                                                            default:
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 0), 2);
                                                        }
                                                        ++totalGenerated;
                                                    } else if (rs.getNamespace().equals("minecraft")) {
                                                        switch (rs.getPath()) {
                                                            case "granite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 1), 2);
                                                                break;
                                                            case "diorite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 2), 2);
                                                                break;
                                                            case "andesite":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 3), 2);
                                                                break;
                                                            case "sandstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 4), 2);
                                                                break;
                                                            case "red_sandstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 5), 2);
                                                                break;
                                                            case "netherrack":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 6), 2);
                                                                break;
                                                            case "blackstone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 7), 2);
                                                                break;
                                                            case "basalt":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 8), 2);
                                                                break;
                                                            case "end_stone":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 9), 2);
                                                                break;
                                                            case "obsidian":
                                                                worldIn.setBlockState(blockpos$mutable, config.state.with(RankineOreBlock.TYPE, 10), 2);
                                                                break;
                                                        }
                                                        ++totalGenerated;
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return totalGenerated > 0;
    }
}