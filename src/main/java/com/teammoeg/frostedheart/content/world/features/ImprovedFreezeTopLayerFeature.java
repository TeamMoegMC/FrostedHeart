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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * @author alcatrazEscapee
 * @license MIT
 */
public class ImprovedFreezeTopLayerFeature extends Feature<NoneFeatureConfiguration>
{
    public ImprovedFreezeTopLayerFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        final WorldGenLevel level = context.level();
        final BlockPos pos = context.origin();
        final int originX = pos.getX();
        final int originZ = pos.getZ();
        final RandomSource random = context.random();
        final boolean enableAccumulation =
                FHConfig.SERVER.WORLDGEN.enableSnowAccumulationDuringWorldgen.get();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();

        // First, find the highest exposed y pos in the chunk
        int maxY = 0;
        for (int idx = 0; idx < 256; idx++)
        {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    originX + (idx & 15), originZ + (idx >> 4));
            if (maxY < y)
            {
                maxY = y;
            }
        }

        // Then, step downwards, tracking the exposure to sky at each step
        int[] skyLights = new int[256], prevSkyLights = new int[256];
        BlockState[] stateCache = new BlockState[256];
        Arrays.fill(prevSkyLights, 7);
        for (int y = maxY; y >= 0; y--)
        {
            // Propagate skylight vertically and cache block states
            for (int idx = 0; idx < 256; idx++)
            {
                cursor.set(originX + (idx & 15), y, originZ + (idx >> 4));
                final BlockState state = level.getBlockState(cursor);
                stateCache[idx] = state;
                if (state.isAir())
                {
                    // Continue skylight downwards
                    skyLights[idx] = prevSkyLights[idx];
                }
            }

            // Extend skylight horizontally (zero-allocation, replaces per-block BFS)
            propagateSkyLights(skyLights);

            // Place snow and ice where skylight reaches
            for (int idx = 0; idx < 256; idx++)
            {
                final int skyLight = prevSkyLights[idx];
                if (skyLight > 0)
                {
                    cursor.set(originX + (idx & 15), y, originZ + (idx >> 4));
                    placeSnowAndIce(level, cursor, stateCache[idx], tempPos,
                            random, skyLight, enableAccumulation);
                }
            }

            // Break early if all possible sky light is gone
            boolean hasSkyLight = false;
            for (int i = 0; i < 256; i++)
            {
                if (skyLights[i] > 0)
                {
                    hasSkyLight = true;
                    break; // exit checking loop, continue with y loop
                }
            }
            if (!hasSkyLight)
            {
                break; // exit y loop
            }

            // Copy sky lights into previous and reset current sky lights
            System.arraycopy(skyLights, 0, prevSkyLights, 0, skyLights.length);
            Arrays.fill(skyLights, 0);
        }
        return true;
    }

    /**
     * Zero-allocation skylight horizontal propagation.
     * Sweeps from highest light level downward, spreading to adjacent cells.
     * Replaces the original per-block BFS (ArrayList + HashSet + Vec3i allocations).
     */
    private static void propagateSkyLights(int[] skyLights)
    {
        for (int light = 7; light >= 2; light--)
        {
            final int spread = light - 1;
            for (int idx = 0; idx < 256; idx++)
            {
                if (skyLights[idx] == light)
                {
                    final int x = idx & 15;
                    final int z = idx >> 4;
                    if (x > 0  && skyLights[idx - 1]  < spread) skyLights[idx - 1]  = spread;
                    if (x < 15 && skyLights[idx + 1]  < spread) skyLights[idx + 1]  = spread;
                    if (z > 0  && skyLights[idx - 16] < spread) skyLights[idx - 16] = spread;
                    if (z < 15 && skyLights[idx + 16] < spread) skyLights[idx + 16] = spread;
                }
            }
        }
    }

    private void placeSnowAndIce(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                 BlockState state, BlockPos.MutableBlockPos tempPos,
                                 RandomSource random, int skyLight,
                                 boolean enableAccumulation)
    {
        // Biome check removed - all biomes are cold in this modpack

        final FluidState fluidState = level.getFluidState(pos);

        // Then, try and place snow layers / ice at the current location
        if (fluidState.getType() == Fluids.WATER
                && (state.getBlock() instanceof LiquidBlock || state.canBeReplaced()))
        {
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
            if (!(state.getBlock() instanceof LiquidBlock))
            {
                level.scheduleTick(pos, Blocks.ICE, 0);
            }
        }
        else if (fluidState.getType() == Fluids.LAVA
                && state.getBlock() instanceof LiquidBlock)
        {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 2);
        }
        else if (state.canBeReplaced()
                && Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
        {
            // Protect existing snow layers: only increase, never decrease
            if (state.is(Blocks.SNOW))
            {
                if (enableAccumulation)
                {
                    int existing = state.getValue(BlockStateProperties.LAYERS);
                    int newLayers = calcLayers(level, pos, tempPos, random, skyLight);
                    if (newLayers > existing)
                    {
                        level.setBlock(pos, state.setValue(
                                BlockStateProperties.LAYERS, newLayers), 3);
                    }
                }
                return;
            }

            // Special exceptions
            if (state.getBlock() instanceof DoublePlantBlock)
            {
                tempPos.set(pos).move(Direction.UP);
                if (level.getBlockState(tempPos).is(state.getBlock()))
                {
                    // Remove the above plant
                    level.removeBlock(tempPos, false);
                }
            }

            int layers;
            if (enableAccumulation)
            {
                layers = calcLayers(level, pos, tempPos, random, skyLight);
            }
            else
            {
                layers = 1;
            }
            level.setBlock(pos, Blocks.SNOW.defaultBlockState()
                    .setValue(BlockStateProperties.LAYERS, layers), 3);
        }
    }

    private int calcLayers(WorldGenLevel level, BlockPos pos,
                           BlockPos.MutableBlockPos tempPos,
                           RandomSource random, int skyLight)
    {
        return Mth.clamp(skyLight - random.nextInt(3)
                - countExposedFaces(level, pos, tempPos), 1, 7);
    }

    private static int countExposedFaces(WorldGenLevel level, BlockPos pos,
                                         BlockPos.MutableBlockPos tempPos)
    {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            tempPos.set(pos).move(direction);
            if (!level.getBlockState(tempPos).isFaceSturdy(
                    level, tempPos, direction.getOpposite()))
            {
                count++;
            }
        }
        return count;
    }
}
