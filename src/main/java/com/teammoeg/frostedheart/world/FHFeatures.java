package com.teammoeg.frostedheart.world;

import com.cannolicatfish.rankine.init.RankineBlocks;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;

import java.util.ArrayList;

public class FHFeatures {
    public static final Feature<FHOreFeatureConfig> FHORE = new FHOreFeature(FHOreFeatureConfig.CODEC);
    public static ArrayList<ConfiguredFeature> FH_ORES = new ArrayList();
    public static final ConfiguredFeature<?, ?> ore_magnetite = register("ore_magnetite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnetite, RankineBlocks.MAGNETITE_ORE.get().getStateContainer().getBaseState(), 40)).range(64).square().chance(1));
    public static final ConfiguredFeature<?, ?> ore_pyrite = register("ore_pyrite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.PYRITE_ORE.get().getStateContainer().getBaseState(), 40)).range(35).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_native_copper = register("ore_native_copper", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_copper, RankineBlocks.NATIVE_COPPER_ORE.get().getStateContainer().getBaseState(), 40)).range(65).square()).chance(0);
    public static final ConfiguredFeature<?, ?> ore_malachite = register("ore_malachite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.malachite, RankineBlocks.MALACHITE_ORE.get().getStateContainer().getBaseState(), 45)).range(65).square().chance(0));
    public static final ConfiguredFeature<?, ?> ore_pentlandite = register("ore_pentlandite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pentlandite, RankineBlocks.PENTLANDITE_ORE.get().getStateContainer().getBaseState(), 40)).range(65).square().chance(5));
    public static final ConfiguredFeature<?, ?> ore_native_tin = register("ore_native_tin", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_tin, RankineBlocks.NATIVE_TIN_ORE.get().getStateContainer().getBaseState(), 40)).range(65).square().chance(0));
    public static final ConfiguredFeature<?, ?> ore_cassiterite = register("ore_cassiterite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.cassiterite, RankineBlocks.CASSITERITE_ORE.get().getStateContainer().getBaseState(), 45)).range(65).square().chance(0));
    public static final ConfiguredFeature<?, ?> ore_bituminous = register("ore_bituminous", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bituminous, RankineBlocks.BITUMINOUS_ORE.get().getStateContainer().getBaseState(), 55)).range(80).square().chance(10));
    public static final ConfiguredFeature<?, ?> ore_lignite = register("ore_lignite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.lignite, RankineBlocks.LIGNITE_ORE.get().getStateContainer().getBaseState(), 40)).range(80).square().chance(0));
    public static final ConfiguredFeature<?, ?> ore_bauxite = register("ore_bauxite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, RankineBlocks.BAUXITE_ORE.get().getStateContainer().getBaseState(), 50)).range(60).square().chance(0));
    public static final ConfiguredFeature<?, ?> ore_stibnite = register("ore_stibnite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.stibnite, RankineBlocks.STIBNITE_ORE.get().getStateContainer().getBaseState(), 35)).range(65).square().chance(10));
    public static final ConfiguredFeature<?, ?> ore_cinnabar = register("ore_cinnabar", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.CINNABAR_ORE.get().getStateContainer().getBaseState(), 40)).range(30).square().chance(4));

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> register(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_ORES.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }

}
