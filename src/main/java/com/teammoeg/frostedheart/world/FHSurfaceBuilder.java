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

import net.minecraft.world.level.block.Blocks;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;

public class FHSurfaceBuilder {
    public static final ConfiguredSurfaceBuilder<SurfaceBuilderBaseConfiguration> VOLCANIC = register("volcanic",
            SurfaceBuilder.DEFAULT.configured(new SurfaceBuilderBaseConfiguration(RankineBlocks.BASALTIC_TUFF.get().defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.GRAVEL.defaultBlockState())));

    public static final ConfiguredSurfaceBuilder<SurfaceBuilderBaseConfiguration> FROZEN_FOREST = register("frozen_forest",
            SurfaceBuilder.DEFAULT.configured(
                    new SurfaceBuilderBaseConfiguration(
                            Blocks.GRASS_BLOCK.defaultBlockState(),//表层方块
                            Blocks.STONE.defaultBlockState(),//表层下方块
                            Blocks.SAND.defaultBlockState()//水下方块
                    )
            )
    );

    private static <SC extends SurfaceBuilderConfiguration> ConfiguredSurfaceBuilder<SC> register(String name, ConfiguredSurfaceBuilder<SC> configuredSurfaceBuilder) {
        return BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_SURFACE_BUILDER, name, configuredSurfaceBuilder);
    }
}
