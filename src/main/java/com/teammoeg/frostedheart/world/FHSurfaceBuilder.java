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

    private static <SC extends ISurfaceBuilderConfig> ConfiguredSurfaceBuilder<SC> register(String name, ConfiguredSurfaceBuilder<SC> configuredSurfaceBuilder) {
        return WorldGenRegistries.register(WorldGenRegistries.CONFIGURED_SURFACE_BUILDER, name, configuredSurfaceBuilder);
    }
}
