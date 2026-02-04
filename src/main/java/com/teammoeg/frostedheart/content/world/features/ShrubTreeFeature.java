/*
 * Copyright (c) 2024 TeamMoeg
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        if (!placeLog(level, origin, config, random)) {
            return false;
        }

        int numBranches = config.branchCount().sample(random);
        int branchLen = config.branchLength().sample(random);


        List<Direction> directions = new ArrayList<>(List.of(Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)));
        Collections.shuffle(directions);

        for (int i = 0; i < Math.min(numBranches, directions.size()); i++) {
            Direction mainDir = directions.get(i);
            generateVBranch(level, origin, mainDir, branchLen, config, random);
        }

        return true;
    }

    private void generateVBranch(WorldGenLevel level, BlockPos startPos, Direction mainDir, int length, ShrubTreeConfig config, RandomSource random) {
        BlockPos currentPos = startPos;

        for (int i = 0; i < length; i++) {
            BlockPos nextPos = currentPos.above().relative(mainDir);


            if (random.nextFloat() < 0.3f) {
                Direction skewDir = random.nextBoolean() ? mainDir.getClockWise() : mainDir.getCounterClockWise();
                nextPos = nextPos.relative(skewDir);
            }


            if (random.nextFloat() < 0.2f) {
                nextPos = currentPos.above();
            }

            currentPos = nextPos;


            if (!placeLog(level, currentPos, config, random)) {
                break;
            }


            if (random.nextFloat() < 0.3f) {
                Direction forkDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);

                BlockPos forkPos = currentPos.relative(forkDir);
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
}

