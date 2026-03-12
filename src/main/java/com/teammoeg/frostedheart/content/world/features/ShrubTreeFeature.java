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

package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class ShrubTreeFeature extends Feature<ShrubTreeConfig> {
    public ShrubTreeFeature(Codec<ShrubTreeConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ShrubTreeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        ShrubTreeConfig config = context.config();

        if (level.isEmptyBlock(origin.below())) {
            return false;
        }

        int trunkHeight = config.trunkHeight().sample(random);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int topOffset = 0;

        for (int i = 0; i < trunkHeight; i++) {
            mutablePos.setWithOffset(origin, 0, i, 0);
            if (!placeLog(level, mutablePos, config, random)) {
                if (i == 0) return false;
                topOffset = i - 1;
                break;
            }
            topOffset = i;
        }

        BlockPos trunkTopPos = origin.above(topOffset);

        int numBranches = config.branchCount().sample(random);
        int branchLen = config.branchLength().sample(random);

        int branchCount = Math.min(numBranches, 4);
        int used = 0;
        for (int i = 0; i < branchCount; i++) {
            int idx;
            do {
                idx = random.nextInt(4);
            } while ((used & (1 << idx)) != 0);
            used |= (1 << idx);

            generateVBranch(level, trunkTopPos, SnowCache.HORIZONTAL_DIRS[idx], branchLen, config, random);
        }
        //固定方向吹风
        decorateWindEffects(level, origin, Direction.NORTH, random);
        return true;
    }

    private void generateVBranch(WorldGenLevel level, BlockPos startPos, Direction mainDir,
                                 int length, ShrubTreeConfig config, RandomSource random) {
        int cx = startPos.getX(), cy = startPos.getY(), cz = startPos.getZ();

        int mainDx = mainDir.getStepX(), mainDz = mainDir.getStepZ();
        Direction cw = mainDir.getClockWise();
        Direction ccw = mainDir.getCounterClockWise();
        int cwDx = cw.getStepX(), cwDz = cw.getStepZ();
        int ccwDx = ccw.getStepX(), ccwDz = ccw.getStepZ();

        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos forkPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < length; i++) {
            int nx = cx + mainDx;
            int ny = cy + 1;
            int nz = cz + mainDz;

            if (random.nextFloat() < 0.3f) {
                if (random.nextBoolean()) {
                    nx += cwDx;
                    nz += cwDz;
                } else {
                    nx += ccwDx;
                    nz += ccwDz;
                }
            }

            if (random.nextFloat() < 0.2f) {
                nx = cx;
                ny = cy + 1;
                nz = cz;
            }

            cx = nx;
            cy = ny;
            cz = nz;
            currentPos.set(cx, cy, cz);

            if (!placeLog(level, currentPos, config, random)) {
                break;
            }

            if (random.nextFloat() < 0.3f) {
                Direction forkDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                forkPos.setWithOffset(currentPos, forkDir);
                placeLog(level, forkPos, config, random);
            }
        }
    }

    private boolean placeLog(WorldGenLevel level, BlockPos pos, ShrubTreeConfig config, RandomSource random) {
        BlockState targetState = level.getBlockState(pos);
        if (targetState.canBeReplaced() || targetState.is(BlockTags.SNOW) || targetState.is(BlockTags.LEAVES)) {
            BlockState logState = config.logProvider().getState(random, pos);
            if (logState.hasProperty(RotatedPillarBlock.AXIS)) {
                logState = logState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            }
            level.setBlock(pos, logState, 2);
            return true;
        }
        return false;
    }

    private void decorateWindEffects(WorldGenLevel level, BlockPos treePos, Direction windDir, RandomSource random) {
        generateUpwindSnow(level, treePos, windDir, random);
        generateCrescentDrift(level, treePos, windDir, random);
    }

    private void generateUpwindSnow(WorldGenLevel level, BlockPos treePos, Direction windDir, RandomSource random) {
        Direction upwind = windDir.getOpposite();
        Direction right = windDir.getClockWise();

        final int upwindLength = 7;
        final int sideSpread = 5;
        final int maxLayers = 7;

        int upStepX = upwind.getStepX(), upStepZ = upwind.getStepZ();
        int rightStepX = right.getStepX(), rightStepZ = right.getStepZ();
        int baseX = treePos.getX(), baseY = treePos.getY(), baseZ = treePos.getZ();

        double invUpLen = 1.0 / upwindLength;
        double invSideSpread = 1.0 / sideSpread;

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        for (int up = 0; up <= upwindLength; up++) {
            double upRatio = up * invUpLen;
            double upComponent = upRatio * upRatio * 0.8;

            for (int side = -sideSpread; side <= sideSpread; side++) {
                if (up == 0 && side == 0) continue;

                double sideRatio = Math.abs(side) * invSideSpread;
                double distSq = upComponent + sideRatio * sideRatio;

                if (distSq > 1.0) continue;

                double dist = Math.sqrt(distSq);
                double factor = Math.sqrt(1.0 - dist);
                factor += (random.nextFloat() - 0.5) * 0.15;

                if (factor <= 0.05) continue;

                int layers = 1 + (int) Math.round((maxLayers - 1) * factor);
                layers = Math.max(1, Math.min(maxLayers, layers));

                targetPos.set(
                        baseX + up * upStepX + side * rightStepX,
                        baseY,
                        baseZ + up * upStepZ + side * rightStepZ
                );

                placeSnowOnly(level, targetPos, layers, searchPos);
            }
        }
    }

    private void placeSnowOnly(WorldGenLevel level, BlockPos pos, int layers, BlockPos.MutableBlockPos searchPos) {
        layers = Math.max(1, Math.min(7, layers));
        int px = pos.getX(), pz = pos.getZ();

        for (int dy = 1; dy >= -2; dy--) {
            int py = pos.getY() + dy;
            searchPos.set(px, py, pz);
            BlockState current = level.getBlockState(searchPos);

            if (!(current.isAir() || current.is(Blocks.SNOW) || current.canBeReplaced())) continue;

            searchPos.set(px, py - 1, pz);
            if (!level.getBlockState(searchPos).isSolid()) continue;

            if (current.is(Blocks.SNOW)) {
                layers = Math.min(7, Math.max(layers, current.getValue(SnowLayerBlock.LAYERS)));
            }

            searchPos.set(px, py, pz);
            level.setBlock(searchPos, SnowCache.SNOW_LAYER_STATES[layers], 2);
            return;
        }
    }

    /**
     * 背风面新月形雪堆
     */
    private void generateCrescentDrift(WorldGenLevel level, BlockPos treePos, Direction windDir, RandomSource random) {
        final int peakLayers = 32;
        final int armLength = 7;
        final int tailLength = 9;

        Direction right = windDir.getClockWise();

        int windStepX = windDir.getStepX(), windStepZ = windDir.getStepZ();
        int rightStepX = right.getStepX(), rightStepZ = right.getStepZ();
        int baseX = treePos.getX(), baseY = treePos.getY(), baseZ = treePos.getZ();

        double invArmLenSq = 1.0 / ((double) armLength * armLength);

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        for (int arm = -armLength; arm <= armLength; arm++) {
            double armFactor = 1.0 - (arm * arm) * invArmLenSq;
            if (armFactor <= 0.05) continue;

            int maxTail = Math.max(2, (int) Math.round(tailLength * Math.sqrt(armFactor)));
            double invMaxTailM1 = 1.0 / (maxTail - 1 + 0.001);

            for (int tail = 1; tail <= maxTail; tail++) {
                double tailRatio = (tail - 1) * invMaxTailM1;
                double tailFactor = 1.0 - Math.sqrt(tailRatio);

                double factor = armFactor * tailFactor;
                factor += (random.nextFloat() - 0.5) * 0.08;

                if (factor <= 0.0) continue;
                factor = Math.min(1.0, factor);

                int totalLayers = (int) Math.round(peakLayers * factor);
                if (totalLayers <= 0) continue;

                targetPos.set(
                        baseX + tail * windStepX + arm * rightStepX,
                        baseY,
                        baseZ + tail * windStepZ + arm * rightStepZ
                );

                placeSnowStack(level, targetPos, totalLayers, searchPos);
            }
        }
    }

    /**
     * 放雪堆
     */
    private void placeSnowStack(WorldGenLevel level, BlockPos pos, int totalLayers, BlockPos.MutableBlockPos searchPos) {
        if (totalLayers <= 0) return;

        int px = pos.getX(), pz = pos.getZ();

        int startY = Integer.MIN_VALUE;
        for (int dy = 1; dy >= -2; dy--) {
            int py = pos.getY() + dy;
            searchPos.set(px, py, pz);
            BlockState current = level.getBlockState(searchPos);
            if (!(current.isAir() || current.is(Blocks.SNOW) || current.canBeReplaced())) continue;

            searchPos.set(px, py - 1, pz);
            if (!level.getBlockState(searchPos).isSolid()) continue;

            startY = py;
            break;
        }

        if (startY == Integer.MIN_VALUE) return;

        int fullBlocks = totalLayers / 8;
        int topLayers = totalLayers % 8;

        if (topLayers == 0 && fullBlocks > 0) {
            fullBlocks--;
            topLayers = 7;
        }
        topLayers = Math.min(7, topLayers);

        int currentY = startY;


        for (int i = 0; i < fullBlocks; i++) {
            searchPos.set(px, currentY, pz);
            BlockState state = level.getBlockState(searchPos);
            if (!(state.isAir() || state.is(Blocks.SNOW) || state.canBeReplaced())) break;
            level.setBlock(searchPos, SnowCache.SNOW_BLOCK_STATE, 2);
            currentY++;
        }

        if (topLayers > 0) {
            searchPos.set(px, currentY, pz);
            BlockState state = level.getBlockState(searchPos);
            if (state.isAir() || state.is(Blocks.SNOW) || state.canBeReplaced()) {
                level.setBlock(searchPos, SnowCache.SNOW_LAYER_STATES[topLayers], 2);
            }
        }
    }

    private static final class SnowCache {
        static final Direction[] HORIZONTAL_DIRS =
                Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);

        static final BlockState[] SNOW_LAYER_STATES = Util.make(
                new BlockState[8],
                states -> {
                    BlockState base = Blocks.SNOW.defaultBlockState();
                    for (int i = 1; i <= 7; i++) {
                        states[i] = base.setValue(SnowLayerBlock.LAYERS, i);
                    }
                }
        );

        static final BlockState SNOW_BLOCK_STATE = Blocks.SNOW_BLOCK.defaultBlockState();
    }
}

