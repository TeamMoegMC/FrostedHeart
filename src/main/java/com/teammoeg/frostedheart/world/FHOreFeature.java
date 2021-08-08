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

import java.util.BitSet;
import java.util.Random;

public class FHOreFeature extends Feature<FHOreFeatureConfig> {
    public FHOreFeature(Codec<FHOreFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, FHOreFeatureConfig config) {
        float f = rand.nextFloat() * (float) Math.PI;
        float f1 = (float) config.size / 8.0F;
        int i = MathHelper.ceil(((float) config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d0 = (double) pos.getX() + Math.sin((double) f) * (double) f1;
        double d1 = (double) pos.getX() - Math.sin((double) f) * (double) f1;
        double d2 = (double) pos.getZ() + Math.cos((double) f) * (double) f1;
        double d3 = (double) pos.getZ() - Math.cos((double) f) * (double) f1;
        int j = 2;
        double d4 = (double) (pos.getY() + rand.nextInt(3) - 2);
        double d5 = (double) (pos.getY() + rand.nextInt(3) - 2);
        int k = pos.getX() - MathHelper.ceil(f1) - i;
        int l = pos.getY() - 2 - i;
        int i1 = pos.getZ() - MathHelper.ceil(f1) - i;
        int j1 = 2 * (MathHelper.ceil(f1) + i);
        int k1 = 2 * (2 + i);

        for (int l1 = k; l1 <= k + j1; ++l1) {
            for (int i2 = i1; i2 <= i1 + j1; ++i2) {
                if (l <= reader.getHeight(Heightmap.Type.OCEAN_FLOOR_WG, l1, i2)) {
                    return this.func_207803_a(reader, rand, config, d0, d1, d2, d3, d4, d5, k, l, i1, j1, k1);
                }
            }
        }

        return false;
    }

    protected boolean func_207803_a(IWorld worldIn, Random random, FHOreFeatureConfig config, double p_207803_4_, double p_207803_6_, double p_207803_8_, double p_207803_10_, double p_207803_12_, double p_207803_14_, int p_207803_16_, int p_207803_17_, int p_207803_18_, int p_207803_19_, int p_207803_20_) {
        int i = 0;
        BitSet bitset = new BitSet(p_207803_19_ * p_207803_20_ * p_207803_19_);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();
        int j = config.size;
        double[] adouble = new double[j * 4];

        for (int k = 0; k < j; ++k) {
            float f = (float) k / (float) j;
            double d0 = MathHelper.lerp((double) f, p_207803_4_, p_207803_6_);
            double d2 = MathHelper.lerp((double) f, p_207803_12_, p_207803_14_);
            double d4 = MathHelper.lerp((double) f, p_207803_8_, p_207803_10_);
            double d6 = random.nextDouble() * (double) j / 16.0D;
            double d7 = ((double) (MathHelper.sin((float) Math.PI * f) + 1.0F) * d6 + 1.0D) / 2.0D;
            adouble[k * 4 + 0] = d0;
            adouble[k * 4 + 1] = d2;
            adouble[k * 4 + 2] = d4;
            adouble[k * 4 + 3] = d7;
        }

        for (int i3 = 0; i3 < j - 1; ++i3) {
            if (!(adouble[i3 * 4 + 3] <= 0.0D)) {
                for (int k3 = i3 + 1; k3 < j; ++k3) {
                    if (!(adouble[k3 * 4 + 3] <= 0.0D)) {
                        double d12 = adouble[i3 * 4 + 0] - adouble[k3 * 4 + 0];
                        double d13 = adouble[i3 * 4 + 1] - adouble[k3 * 4 + 1];
                        double d14 = adouble[i3 * 4 + 2] - adouble[k3 * 4 + 2];
                        double d15 = adouble[i3 * 4 + 3] - adouble[k3 * 4 + 3];
                        if (d15 * d15 > d12 * d12 + d13 * d13 + d14 * d14) {
                            if (d15 > 0.0D) {
                                adouble[k3 * 4 + 3] = -1.0D;
                            } else {
                                adouble[i3 * 4 + 3] = -1.0D;
                            }
                        }
                    }
                }
            }
        }

        for (int j3 = 0; j3 < j; ++j3) {
            double d11 = adouble[j3 * 4 + 3];
            if (!(d11 < 0.0D)) {
                double d1 = adouble[j3 * 4 + 0];
                double d3 = adouble[j3 * 4 + 1];
                double d5 = adouble[j3 * 4 + 2];
                int l = Math.max(MathHelper.floor(d1 - d11), p_207803_16_);
                int l3 = Math.max(MathHelper.floor(d3 - d11), p_207803_17_);
                int i1 = Math.max(MathHelper.floor(d5 - d11), p_207803_18_);
                int j1 = Math.max(MathHelper.floor(d1 + d11), l);
                int k1 = Math.max(MathHelper.floor(d3 + d11), l3);
                int l1 = Math.max(MathHelper.floor(d5 + d11), i1);

                for (int i2 = l; i2 <= j1; ++i2) {
                    double d8 = ((double) i2 + 0.5D - d1) / d11;
                    if (d8 * d8 < 1.0D) {
                        for (int j2 = l3; j2 <= k1; ++j2) {
                            double d9 = ((double) j2 + 0.5D - d3) / d11;
                            if (d8 * d8 + d9 * d9 < 1.0D) {
                                for (int k2 = i1; k2 <= l1; ++k2) {
                                    double d10 = ((double) k2 + 0.5D - d5) / d11;
                                    if (d8 * d8 + d9 * d9 + d10 * d10 < 1.0D) {
                                        int l2 = i2 - p_207803_16_ + (j2 - p_207803_17_) * p_207803_19_ + (k2 - p_207803_18_) * p_207803_19_ * p_207803_20_;
                                        if (!bitset.get(l2)) {
                                            bitset.set(l2);
                                            blockpos$mutable.setPos(i2, j2, k2);
                                            Block b = worldIn.getBlockState(blockpos$mutable).getBlock();
                                            ResourceLocation rs = b.getRegistryName();
                                            if (config.target.test(worldIn.getBlockState(blockpos$mutable), random)) {
                                                if (config.state.getBlock() instanceof RankineOreBlock) {
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
                                                        ++i;
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
                                                        ++i;
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
        }
        return i > 0;
    }
}