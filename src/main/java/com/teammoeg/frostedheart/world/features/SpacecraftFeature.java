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

package com.teammoeg.frostedheart.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Set;

public class SpacecraftFeature extends Feature<NoneFeatureConfiguration> {
    public SpacecraftFeature(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource rand = level.getRandom();

        // Set starting position and rotation for the spacecraft
        BlockPos start = new BlockPos(pos.getX() - 9, pos.getY() - 1, pos.getZ() - 7);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotationPivot(new BlockPos(9, 2, 7))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        settings.keepLiquids = false;

        // Load the structure template
        StructureTemplate template = level.getLevel().getStructureManager().get(new ResourceLocation(FHMain.MODID, "relic/spacecraft")).orElse(null);
        if (template == null) {
            return false;
        }

        // Get bounding box for the spacecraft placement and use it to define the impact region
        BoundingBox boundingBox = template.getBoundingBox(settings, start);
        createImpactRegion(level, boundingBox);

        return template.placeInWorld(level, start, boundingBox.getCenter(), settings, level.getRandom(), 2);
    }

    /**
     * Clears blocks in a natural impact region, creating a gradient crater around the spacecraft.
     */
//    private void createImpactRegion(WorldGenLevel level, BoundingBox boundingBox) {
//        int centerX = boundingBox.getCenter().getX();
//        int centerZ = boundingBox.getCenter().getZ();
//
//        // Define dimensions for the impact area
//        int maxXBackward = centerX - 48;
//        int maxXForward = centerX + 15;
//        int minZ = centerZ - 12;
//        int maxZ = centerZ + 12;
//        int minY = boundingBox.minY();
//        int maxY = boundingBox.maxY();
//        int depth = maxY - minY;
//
//        RandomSource rand = level.getRandom();
//
//        for (int x = maxXBackward; x <= maxXForward; x++) {
//            for (int z = minZ; z <= maxZ; z++) {
//                // Calculate distance from the center for the gradient effect
//                int dx = Math.abs(centerX - x);
//                int dz = Math.abs(centerZ - z);
//                double distance = Math.sqrt(dx * dx + dz * dz);
//
//                // Adjust depth of clearing based on distance from center, creating a gradient
//                int maxClearDepth = (int) (distance / 48) * depth; // Slope down based on distance
//                maxClearDepth = Math.max(depth, maxClearDepth); // Ensure we don't clear below ground level
//
//                // Add randomness to make the edges feel more rugged
//                int randomOffset = rand.nextInt(3) - 1; // Offset between -1 and +1
//                if (rand.nextInt(10) == 0)
//                    maxClearDepth += randomOffset;
//
//                // Clear blocks in the calculated range
//                for (int y = maxY - maxClearDepth; y <= maxY; y++) {
//                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
//                }
//            }
//        }
//
//        // clear sky above the spacecraft
//        for (int y = maxY; y < maxY + 50; y++) {
//            for (int x = centerX - 48; x <= centerX + 15; x++) {
//                for (int z = centerZ - 12; z <= centerZ + 12; z++) {
//                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
//                }
//            }
//        }
//    }

    private void createImpactRegion(WorldGenLevel level, BoundingBox boundingBox) {
        int centerX = boundingBox.getCenter().getX();
        int centerZ = boundingBox.getCenter().getZ();
        int minY = boundingBox.minY();
        int maxY = boundingBox.maxY();
        int depth = maxY - minY;

        RandomSource rand = level.getRandom();

        // 1. Create a circular crater around the center of the ship
        int craterRadius = 18;
        for (int x = centerX - craterRadius; x <= centerX + craterRadius; x++) {
            for (int z = centerZ - craterRadius; z <= centerZ + craterRadius; z++) {
                int dx = centerX - x;
                int dz = centerZ - z;
                if (dx * dx + dz * dz <= craterRadius * craterRadius) {
                    // Calculate gradient depth for a sloped crater
                    int offset = 10;
                    double distance = Math.sqrt(dx * dx + dz * dz) - offset;
                    int maxClearDepth = (int) ((1 - (distance / craterRadius)) * depth);
                    maxClearDepth = Mth.clamp(maxClearDepth, 0, depth);

                    // Add randomness to trench depth for ruggedness
                    if (rand.nextInt(3) == 0) {
                        maxClearDepth += rand.nextInt(3) - 1;
                    }

                    // Clear blocks down to maxClearDepth
                    for (int y = maxY - maxClearDepth; y <= maxY; y++) {
                        if (y == maxY - maxClearDepth && rand.nextInt(3) == 0) {
                            // Add fire
                            level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
                        }
                        level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 2. Create a rectangular trench extending backward from the crater
        int trenchWidth = 34;
        int trenchLength = 48;
        int trenchStartX = centerX - trenchLength;
        for (int x = trenchStartX; x <= centerX; x++) {
            for (int z = centerZ - trenchWidth / 2; z <= centerZ + trenchWidth / 2; z++) {
                // Calculate gradient depth to slope down the trench edges
                int edgeDistance = Math.min(Math.abs(centerZ - z), trenchWidth / 2);
                int maxClearDepth = (int) ((1 - (edgeDistance / (double)(trenchWidth / 2))) * depth);
                maxClearDepth = Math.max(0, maxClearDepth);

                // Add randomness to trench depth for ruggedness
                if (rand.nextInt(3) == 0) {
                    maxClearDepth += rand.nextInt(3) - 1;
                }

                // Clear blocks down to maxClearDepth
                for (int y = maxY - maxClearDepth; y <= maxY; y++) {
                    if (y == maxY - maxClearDepth && rand.nextInt(3) == 0) {
                        // Add fire
                        level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
                    }
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // Clear sky above the crater and trench for open air
        for (int y = maxY; y < maxY + 50; y++) {
            for (int x = trenchStartX; x <= centerX + craterRadius; x++) {
                for (int z = centerZ - trenchWidth / 2; z <= centerZ + trenchWidth / 2; z++) {
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }



}
