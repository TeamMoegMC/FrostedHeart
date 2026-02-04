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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class LayeredDiskFeature extends Feature<LayeredDiskConfiguration> {
    public LayeredDiskFeature(Codec<LayeredDiskConfiguration> codec) {
        super(codec);
    }

    public boolean place(FeaturePlaceContext<LayeredDiskConfiguration> context) {
        LayeredDiskConfiguration config = context.config();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        boolean flag = false;
        int baseY = origin.getY();
        int maxY = baseY + config.verticalSpan(); // top layer
        int minY = baseY - config.verticalSpan() - 1; // bottom layer

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Iterate over a horizontal disk
        int radius = config.radius().sample(random);
        for(BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, 0, -radius), origin.offset(radius, 0, radius))) {
            int dx = pos.getX() - origin.getX();
            int dz = pos.getZ() - origin.getZ();
            if (dx * dx + dz * dz <= radius * radius) {
                // Place the layered column at each horizontal position
                flag |= this.placeLayeredColumn(config, level, random, maxY, minY, mutablePos.set(pos));
            }
        }
        return flag;
    }

    protected boolean placeLayeredColumn(LayeredDiskConfiguration config, WorldGenLevel level, RandomSource random, int maxY, int minY, BlockPos.MutableBlockPos pos) {
        boolean placedTop = false;
        boolean placedBottom = false;
        boolean flag = false;

        // Traverse vertically from the top down
        for (int y = maxY; y > minY; y--) {
            pos.setY(y);
            if (config.target().test(level, pos)) {
                // If the top layer hasn't been placed, place it
                if (!placedTop) {
                    BlockState topState = config.topStateProvider().getState(level, random, pos);
                    level.setBlock(pos, topState, 2);
                    this.markAboveForPostProcessing(level, pos);
                    placedTop = true;
                    flag = true;
                }
                // Otherwise, if the top is placed and bottom is not, place the bottom layer
                else if (!placedBottom) {
                    BlockState bottomState = config.bottomStateProvider().getState(level, random, pos);
                    level.setBlock(pos, bottomState, 2);
                    this.markAboveForPostProcessing(level, pos);
                    placedBottom = true;
                    flag = true;
                } else {
                    // If both layers have been placed, we can exit early for this column
                    break;
                }
            }
        }
        return flag;
    }
}

