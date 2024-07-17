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

package com.teammoeg.frostedheart.world;

import com.cannolicatfish.rankine.init.RankineBlocks;

import net.minecraft.block.Blocks;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.surfacebuilders.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilders.ISurfaceBuilderConfig;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilderConfig;

public class FHSurfaceBuilder {
    public static final ConfiguredSurfaceBuilder<SurfaceBuilderConfig> VOLCANIC = register("volcanic",
            SurfaceBuilder.DEFAULT.func_242929_a(new SurfaceBuilderConfig(RankineBlocks.BASALTIC_TUFF.get().getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.GRAVEL.getDefaultState())));

    public static final ConfiguredSurfaceBuilder<SurfaceBuilderConfig> FROZEN_FOREST = register("frozen_forest",
            SurfaceBuilder.DEFAULT.func_242929_a(
                    new SurfaceBuilderConfig(
                            Blocks.GRASS_BLOCK.getDefaultState(),//表层方块
                            Blocks.STONE.getDefaultState(),//表层下方块
                            Blocks.SAND.getDefaultState()//水下方块
                    )
            )
    );

    private static <SC extends ISurfaceBuilderConfig> ConfiguredSurfaceBuilder<SC> register(String name, ConfiguredSurfaceBuilder<SC> configuredSurfaceBuilder) {
        return WorldGenRegistries.register(WorldGenRegistries.CONFIGURED_SURFACE_BUILDER, name, configuredSurfaceBuilder);
    }
}
