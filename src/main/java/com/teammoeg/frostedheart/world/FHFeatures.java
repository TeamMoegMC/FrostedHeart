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

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.world.feature.FHOreFeature;
import com.teammoeg.frostedheart.world.feature.FHOreFeatureConfig;
import com.teammoeg.frostedheart.world.feature.SpacecraftFeature;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.*;

import java.util.ArrayList;

public class FHFeatures {
    public static final Feature<FHOreFeatureConfig> FHORE = new FHOreFeature(FHOreFeatureConfig.CODEC);
    public static final SpacecraftFeature SPACECRAFT = new SpacecraftFeature(NoFeatureConfig.CODEC);
    public static final ConfiguredFeature<?, ?> spacecraft_feature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "spacecraft", SPACECRAFT.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG));
    public static ArrayList<ConfiguredFeature<?, ?>> FH_ORES = new ArrayList<>();
    public static ArrayList<ConfiguredFeature<?, ?>> FH_DISK = new ArrayList<>();

    public static void initFeatures() {
        registerFHOre("ore_magnetite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnetite, RankineBlocks.MAGNETITE_ORE.get().getDefaultState(), 40)).range(64).square().chance(4));
        registerFHOre("ore_pyrite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.PYRITE_ORE.get().getDefaultState(), 40)).range(35).square().chance(5));
        registerFHOre("ore_hematite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.hematite, RankineBlocks.HEMATITE_ORE.get().getDefaultState(), 40)).range(35).square().chance(5));
        registerFHOre("ore_chalcocite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.chalcocite, RankineBlocks.CHALCOCITE_ORE.get().getDefaultState(), 40)).range(65).square()).chance(2);
        registerFHOre("ore_malachite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.malachite, RankineBlocks.MALACHITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(2));
        registerFHOre("ore_pentlandite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pentlandite, RankineBlocks.PENTLANDITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(5));
        registerFHOre("ore_native_tin", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_tin, RankineBlocks.NATIVE_TIN_ORE.get().getDefaultState(), 40)).range(65).square().chance(2));
        registerFHOre("ore_cassiterite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.cassiterite, RankineBlocks.CASSITERITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(2));
        registerFHOre("ore_bituminous", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bituminous, RankineBlocks.BITUMINOUS_ORE.get().getDefaultState(), 55)).range(80).square().chance(12));
        registerFHOre("ore_lignite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.lignite, RankineBlocks.LIGNITE_ORE.get().getDefaultState(), 50)).range(80).square().chance(2));
        registerFHOre("ore_bauxite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, RankineBlocks.BAUXITE_ORE.get().getDefaultState(), 50)).range(60).square().chance(2));
        registerFHOre("ore_stibnite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.stibnite, RankineBlocks.STIBNITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(12));
        registerFHOre("ore_cinnabar", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.CINNABAR_ORE.get().getDefaultState(), 40)).range(30).square().chance(6));
        registerFHOre("ore_magnesite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnesite, RankineBlocks.MAGNESITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(10));
        registerFHOre("ore_galena", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.GALENA_ORE.get().getDefaultState(), 40)).range(40).square().chance(7));
        registerFHOre("ore_fluorite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, FHBlocks.fluorite_ore.getDefaultState(), 35)).range(65).square().chance(10));
        registerFHOre("ore_silver", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_SILVER_ORE.get().getDefaultState(), 35)).range(30).square().chance(12));
        registerFHOre("ore_gold", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_GOLD_ORE.get().getDefaultState(), 35)).range(30).square().chance(12));
        registerFHOre("ore_sphalerite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.SPHALERITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(4));
        registerFHOre("ore_anthracite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.anthracite, RankineBlocks.ANTHRACITE_ORE.get().getDefaultState(), 50)).range(48).chance(15));
        registerFHOre("ore_graphite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.graphite, RankineBlocks.PLUMBAGO_ORE.get().getDefaultState(), 35)).range(50).chance(12));
        ImmutableList<BlockState> clay_target = ImmutableList.of(RankineBlocks.SILTY_CLAY.get().getDefaultState(), RankineBlocks.SILTY_CLAY_MUD.get().getDefaultState(), RankineBlocks.SANDY_CLAY.get().getDefaultState(),Blocks.DIRT.getDefaultState(),RankineBlocks.SANDY_CLAY_MUD.get().getDefaultState(),RankineBlocks.CLAY_LOAM_MUD.get().getDefaultState());
        ImmutableList<BlockState> disk_target = ImmutableList.of(RankineBlocks.SANDY_CLAY_LOAM.get().getDefaultState(),RankineBlocks.SILTY_CLAY_LOAM.get().getDefaultState(),Blocks.GRAVEL.getDefaultState(),Blocks.SAND.getDefaultState(),Blocks.DIRT.getDefaultState(),RankineBlocks.CLAY_LOAM.get().getDefaultState(),RankineBlocks.SILTY_CLAY.get().getDefaultState(), RankineBlocks.SILTY_CLAY_MUD.get().getDefaultState(), RankineBlocks.SANDY_CLAY.get().getDefaultState(),RankineBlocks.SANDY_CLAY_MUD.get().getDefaultState());

        registerFHDisk("copper_gravel", Feature.DISK.withConfiguration(new SphereReplaceConfig(FHBlocks.copper_gravel.getDefaultState(), FeatureSpread.create(1, 2), 1, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).chance(2));
        registerFHDisk("fh_disk_clay", Feature.DISK.withConfiguration(new SphereReplaceConfig(Blocks.CLAY.getDefaultState(), FeatureSpread.create(2, 3), 1, clay_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).chance(2));
        registerFHDisk("fh_disk_gravel", Feature.DISK.withConfiguration(new SphereReplaceConfig(Blocks.GRAVEL.getDefaultState(), FeatureSpread.create(2, 3), 2, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT));
        registerFHDisk("fh_disk_sand", Feature.DISK.withConfiguration(new SphereReplaceConfig(Blocks.SAND.getDefaultState(), FeatureSpread.create(2, 4), 2, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).count(3));
    }

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> registerFHOre(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_ORES.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> registerFHDisk(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_DISK.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }
}
