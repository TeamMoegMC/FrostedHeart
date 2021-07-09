package com.teammoeg.frostedheart;

import com.cannolicatfish.rankine.init.RankineBlocks;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;

public class Features {

    public static final ConfiguredFeature<?, ?> ORE_magnetite = register("ore_magnetite", (new FHOreFeatureConfig(OreFeatureConfig.FillerBlockType.BASE_STONE_OVERWORLD, RankineBlocks.MAGNETITE_ORE.get().getStateContainer().getBaseState(), 40)).range(64).square().count(20));

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> register(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }
}
