package com.teammoeg.frostedheart.world;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;

import java.util.ArrayList;

public class FHFeatures {
    public static final Feature<FHOreFeatureConfig> FHORE = new FHOreFeature(FHOreFeatureConfig.CODEC);
    public static ArrayList<ConfiguredFeature> FH_ORES = new ArrayList();
//    public static final ConfiguredFeature<?, ?> ore_magnetite = register("ore_magnetite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnetite, RankineBlocks.MAGNETITE_ORE.get().getStateContainer().getBaseState(), 40)).range(64).square().count(20));
//    public static final ConfiguredFeature<?, ?> ore_pyrite = register("ore_pyrite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.PYRITE_ORE.get().getStateContainer().getBaseState(), 40)).range(64).square().count(20));

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> register(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_ORES.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }

}
