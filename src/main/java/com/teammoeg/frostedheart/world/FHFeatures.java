/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.world;

import java.util.ArrayList;

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHContent;

import com.teammoeg.frostedheart.world.feature.FHOreFeature;
import com.teammoeg.frostedheart.world.feature.FHOreFeatureConfig;
import com.teammoeg.frostedheart.world.feature.SpacecraftFeature;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.*;

public class FHFeatures {
    public static final Feature<FHOreFeatureConfig> FHORE = new FHOreFeature(FHOreFeatureConfig.CODEC);
    public static final SpacecraftFeature SPACECRAFT = new SpacecraftFeature(NoFeatureConfig.CODEC);
    public static ArrayList<ConfiguredFeature<?, ?>> FH_ORES = new ArrayList<>();
    public static final ConfiguredFeature<?, ?> ore_magnetite = register("ore_magnetite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnetite, RankineBlocks.MAGNETITE_ORE.get().getDefaultState(), 40)).range(64).square().chance(4));
    public static final ConfiguredFeature<?, ?> ore_pyrite = register("ore_pyrite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.PYRITE_ORE.get().getDefaultState(), 40)).range(35).square().chance(4));
    //public static final ConfiguredFeature<?, ?> ore_native_copper = register("ore_native_copper", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_copper, RankineBlocks.NATIVE_COPPER_ORE.get().getDefaultState(), 40)).range(65).square()).chance(2);
    public static final ConfiguredFeature<?, ?> ore_malachite = register("ore_malachite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.malachite, RankineBlocks.MALACHITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_pentlandite = register("ore_pentlandite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pentlandite, RankineBlocks.PENTLANDITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(5));
    public static final ConfiguredFeature<?, ?> ore_native_tin = register("ore_native_tin", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_tin, RankineBlocks.NATIVE_TIN_ORE.get().getDefaultState(), 40)).range(65).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_cassiterite = register("ore_cassiterite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.cassiterite, RankineBlocks.CASSITERITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_bituminous = register("ore_bituminous", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bituminous, RankineBlocks.BITUMINOUS_ORE.get().getDefaultState(), 55)).range(80).square().chance(12));
    public static final ConfiguredFeature<?, ?> ore_lignite = register("ore_lignite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.lignite, RankineBlocks.LIGNITE_ORE.get().getDefaultState(), 50)).range(80).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_bauxite = register("ore_bauxite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, RankineBlocks.BAUXITE_ORE.get().getDefaultState(), 50)).range(60).square().chance(2));
    public static final ConfiguredFeature<?, ?> ore_stibnite = register("ore_stibnite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.stibnite, RankineBlocks.STIBNITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(12));
    public static final ConfiguredFeature<?, ?> ore_cinnabar = register("ore_cinnabar", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.CINNABAR_ORE.get().getDefaultState(), 40)).range(30).square().chance(6));
    public static final ConfiguredFeature<?, ?> ore_magnesite = register("ore_magnesite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnesite, RankineBlocks.MAGNESITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(10));
    public static final ConfiguredFeature<?, ?> ore_galena = register("ore_galena", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.GALENA_ORE.get().getDefaultState(), 40)).range(40).square().chance(7));
    //public static final ConfiguredFeature<?, ?> ore_halite = register("ore_halite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, RankineBlocks.HALITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(7));
    //public static final ConfiguredFeature<?, ?> ore_fluorite = register("ore_fluorite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.FLUORITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(10));
    public static final ConfiguredFeature<?, ?> ore_silver = register("ore_silver", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_SILVER_ORE.get().getDefaultState(), 35)).range(30).square().chance(12));
    public static final ConfiguredFeature<?, ?> ore_gold = register("ore_gold", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_GOLD_ORE.get().getDefaultState(), 35)).range(30).square().chance(12));
    public static final ConfiguredFeature<?, ?> ore_sphalerite = register("ore_sphalerite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.SPHALERITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(4));
    public static final ConfiguredFeature<?, ?> ore_anthracite = register("ore_anthracite",FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.anthracite, RankineBlocks.ANTHRACITE_ORE.get().getDefaultState(), 50)).range(48).chance(15));
    public static final ConfiguredFeature<?, ?> ore_graphite = register("ore_graphite",FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.graphite, RankineBlocks.PLUMBAGO_ORE.get().getDefaultState(), 35)).range(50).chance(12));

    public static final ConfiguredFeature<?, ?> copper_gravel = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "copper_gravel" ,Feature.DISK.withConfiguration(new SphereReplaceConfig(FHContent.FHBlocks.copper_gravel.getDefaultState(), FeatureSpread.create(1, 1), 1, ImmutableList.of(Blocks.GRAVEL.getDefaultState(),Blocks.SAND.getDefaultState(),Blocks.DIRT.getDefaultState()))).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).chance(1));
    public static final ConfiguredFeature<?,?> spacecraft_feature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,"spacecraft",SPACECRAFT.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG));
    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> register(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_ORES.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }

}
