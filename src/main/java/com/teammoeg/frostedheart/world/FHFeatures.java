/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import java.util.ArrayList;

import com.alcatrazescapee.primalwinter.common.ModBlocks;
import com.cannolicatfish.rankine.init.RankineBlocks;
import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.world.geology.ore.FHOreFeature;
import com.teammoeg.frostedheart.world.geology.ore.FHOreFeatureConfig;
import com.teammoeg.frostedheart.world.geology.surface.FlowerCoveredDepositFeature;
import com.teammoeg.frostedheart.world.civilization.orbit.spacecraft.SpacecraftFeature;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.BlockStateFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureSpread;
import net.minecraft.world.gen.feature.Features;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.SphereReplaceConfig;

public class FHFeatures {

    // FH
    public static final BlockState COPPER_GRAVEL = FHBlocks.copper_gravel.get().getDefaultState();

    // vanilla
    protected static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    protected static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.getDefaultState();
    protected static final BlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    protected static final BlockState SAND = Blocks.SAND.getDefaultState();
    protected static final BlockState CLAY = Blocks.CLAY.getDefaultState();
    protected static final BlockState GRASS_BLOCK = Blocks.GRASS_BLOCK.getDefaultState();

    // primal winter
    protected static final BlockState SNOWY_DIRT = ModBlocks.SNOWY_DIRT.get().getDefaultState();
    protected static final BlockState SNOWY_COARSE_DIRT = ModBlocks.SNOWY_COARSE_DIRT.get().getDefaultState();
    protected static final BlockState SNOWY_SAND = ModBlocks.SNOWY_SAND.get().getDefaultState();

    // rankine has 9 types of soil, coarse soil, mud, grass block
    // all soil
    public static final BlockState LOAM = RankineBlocks.LOAM.get().getDefaultState();
    public static final BlockState SILTY_LOAM = RankineBlocks.SILTY_LOAM.get().getDefaultState();
    public static final BlockState LOAMY_SAND = RankineBlocks.LOAMY_SAND.get().getDefaultState();
    public static final BlockState SANDY_LOAM = RankineBlocks.SANDY_LOAM.get().getDefaultState();
    public static final BlockState CLAY_LOAM = RankineBlocks.CLAY_LOAM.get().getDefaultState();
    public static final BlockState SANDY_CLAY_LOAM = RankineBlocks.SANDY_CLAY_LOAM.get().getDefaultState();
    public static final BlockState SILTY_CLAY_LOAM = RankineBlocks.SILTY_CLAY_LOAM.get().getDefaultState();
    public static final BlockState SANDY_CLAY = RankineBlocks.SANDY_CLAY.get().getDefaultState();
    public static final BlockState SILTY_CLAY = RankineBlocks.SILTY_CLAY.get().getDefaultState();

    // all mud
    public static final BlockState LOAM_MUD = RankineBlocks.LOAM_MUD.get().getDefaultState();
    public static final BlockState SILTY_LOAM_MUD = RankineBlocks.SILTY_LOAM_MUD.get().getDefaultState();
    public static final BlockState LOAMY_SAND_MUD = RankineBlocks.LOAMY_SAND_MUD.get().getDefaultState();
    public static final BlockState SANDY_LOAM_MUD = RankineBlocks.SANDY_LOAM_MUD.get().getDefaultState();
    public static final BlockState CLAY_LOAM_MUD = RankineBlocks.CLAY_LOAM_MUD.get().getDefaultState();
    public static final BlockState SANDY_CLAY_LOAM_MUD = RankineBlocks.SANDY_CLAY_LOAM_MUD.get().getDefaultState();
    public static final BlockState SILTY_CLAY_LOAM_MUD = RankineBlocks.SILTY_CLAY_LOAM_MUD.get().getDefaultState();
    public static final BlockState SANDY_CLAY_MUD = RankineBlocks.SANDY_CLAY_MUD.get().getDefaultState();
    public static final BlockState SILTY_CLAY_MUD = RankineBlocks.SILTY_CLAY_MUD.get().getDefaultState();

    // all
    public static final BlockState LOAM_GRASS_BLOCK = RankineBlocks.LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SILTY_LOAM_GRASS_BLOCK = RankineBlocks.SILTY_LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState LOAMY_SAND_GRASS_BLOCK = RankineBlocks.LOAMY_SAND_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SANDY_LOAM_GRASS_BLOCK = RankineBlocks.SANDY_LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState CLAY_LOAM_GRASS_BLOCK = RankineBlocks.CLAY_LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SANDY_CLAY_LOAM_GRASS_BLOCK = RankineBlocks.SANDY_CLAY_LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SILTY_CLAY_LOAM_GRASS_BLOCK = RankineBlocks.SILTY_CLAY_LOAM_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SANDY_CLAY_GRASS_BLOCK = RankineBlocks.SANDY_CLAY_GRASS_BLOCK.get().getDefaultState();
    public static final BlockState SILTY_CLAY_GRASS_BLOCK = RankineBlocks.SILTY_CLAY_GRASS_BLOCK.get().getDefaultState();


    protected static final ImmutableList<BlockState> clay_target = ImmutableList.of(
            // vanilla
            DIRT, COARSE_DIRT, GRASS_BLOCK, SAND,
            // all clay soil
            SILTY_CLAY, SANDY_CLAY, CLAY_LOAM, SANDY_CLAY_LOAM, SILTY_CLAY_LOAM,
            // all clay mud
            SILTY_CLAY_MUD, SANDY_CLAY_MUD, CLAY_LOAM_MUD, SANDY_CLAY_LOAM_MUD, SILTY_CLAY_LOAM_MUD,
            // all clay grass blocks
            SILTY_CLAY_GRASS_BLOCK, SANDY_CLAY_GRASS_BLOCK, CLAY_LOAM_GRASS_BLOCK, SANDY_CLAY_LOAM_GRASS_BLOCK, SILTY_CLAY_LOAM_GRASS_BLOCK
    );
    protected static final ImmutableList<BlockState> disk_target = ImmutableList.of(
            // vanilla
            DIRT, COARSE_DIRT, GRASS_BLOCK, SAND,
            // primal winter
            SNOWY_DIRT, SNOWY_SAND, SNOWY_COARSE_DIRT,
            // all sandy soil (beach)
            SANDY_CLAY, SANDY_LOAM, LOAMY_SAND, SANDY_CLAY_LOAM,
            // all sandy grass block (beach)
            SANDY_CLAY_GRASS_BLOCK, SANDY_LOAM_GRASS_BLOCK, LOAMY_SAND_GRASS_BLOCK, SANDY_CLAY_LOAM_GRASS_BLOCK,
            // all mud (underneath water/beach)
            LOAM_MUD, SILTY_LOAM_MUD, LOAMY_SAND_MUD, SANDY_CLAY_MUD, SILTY_CLAY_MUD,
            SANDY_LOAM_MUD, CLAY_LOAM_MUD, SANDY_CLAY_LOAM_MUD, SILTY_CLAY_LOAM_MUD
    );

    // Features, registered at CommonRegistryEvents#onFeatureRegistry
    public static final Feature<FHOreFeatureConfig> FHORE = new FHOreFeature(FHOreFeatureConfig.CODEC);
    public static final SpacecraftFeature SPACECRAFT = new SpacecraftFeature(NoFeatureConfig.CODEC);
    public static final Feature<BlockStateFeatureConfig> FLOWER_COVERED_DEPOSIT_FEATURE = new FlowerCoveredDepositFeature(BlockStateFeatureConfig.CODEC);

    // Configured features, generate at CommonEvents#addOreGenFeatures
    public static final ConfiguredFeature<?, ?> spacecraft_feature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "spacecraft", SPACECRAFT.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG));
    public static final ConfiguredFeature<?, ?> clay_deposit = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "clay_deposit", FLOWER_COVERED_DEPOSIT_FEATURE.withConfiguration(new BlockStateFeatureConfig(FHFeatures.CLAY)).withPlacement(Features.Placements.HEIGHTMAP_PLACEMENT).chance(4));
    public static final ConfiguredFeature<?, ?> gravel_deposit = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "gravel_deposit", FLOWER_COVERED_DEPOSIT_FEATURE.withConfiguration(new BlockStateFeatureConfig(FHFeatures.GRAVEL)).withPlacement(Features.Placements.HEIGHTMAP_PLACEMENT).chance(4));

    public static ArrayList<ConfiguredFeature<?, ?>> FH_ORES = new ArrayList<>();
    public static ArrayList<ConfiguredFeature<?, ?>> FH_DISK = new ArrayList<>();

    public static void initFeatures() {
        registerFHOre("ore_magnetite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnetite, RankineBlocks.MAGNETITE_ORE.get().getDefaultState(), 45)).range(64).square().chance(4));
        registerFHOre("ore_pyrite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.PYRITE_ORE.get().getDefaultState(), 45)).range(35).square().chance(5));
        registerFHOre("ore_hematite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.hematite, RankineBlocks.HEMATITE_ORE.get().getDefaultState(), 45)).range(35).square().chance(5));
        registerFHOre("ore_chalcocite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.chalcocite, RankineBlocks.CHALCOCITE_ORE.get().getDefaultState(), 45)).range(65).square()).chance(3);
        registerFHOre("ore_malachite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.malachite, RankineBlocks.MALACHITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(2));
        registerFHOre("ore_pentlandite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pentlandite, RankineBlocks.PENTLANDITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(5));
        registerFHOre("ore_native_tin", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.native_tin, RankineBlocks.NATIVE_TIN_ORE.get().getDefaultState(), 45)).range(65).square().chance(3));
        registerFHOre("ore_cassiterite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.cassiterite, RankineBlocks.CASSITERITE_ORE.get().getDefaultState(), 45)).range(65).square().chance(3));
        registerFHOre("ore_bituminous", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bituminous, RankineBlocks.BITUMINOUS_ORE.get().getDefaultState(), 55)).range(80).square().chance(10));
        registerFHOre("ore_lignite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.lignite, RankineBlocks.LIGNITE_ORE.get().getDefaultState(), 50)).range(80).square().chance(2));
        registerFHOre("ore_bauxite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, RankineBlocks.BAUXITE_ORE.get().getDefaultState(), 50)).range(60).square().chance(3));
        registerFHOre("ore_stibnite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.stibnite, RankineBlocks.STIBNITE_ORE.get().getDefaultState(), 35)).range(65).square().chance(12));
        registerFHOre("ore_cinnabar", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.CINNABAR_ORE.get().getDefaultState(), 40)).range(30).square().chance(6));
        registerFHOre("ore_magnesite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.magnesite, RankineBlocks.MAGNESITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(10));
        registerFHOre("ore_galena", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.GALENA_ORE.get().getDefaultState(), 40)).range(40).square().chance(7));
        registerFHOre("ore_fluorite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, FHBlocks.fluorite_ore.get().getDefaultState(), 35)).range(65).square().chance(10));
        registerFHOre("ore_silver", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_SILVER_ORE.get().getDefaultState(), 35)).range(30).square().chance(10));
        registerFHOre("ore_gold", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.gold, RankineBlocks.NATIVE_GOLD_ORE.get().getDefaultState(), 35)).range(30).square().chance(10));
        registerFHOre("ore_sphalerite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.pyrite, RankineBlocks.SPHALERITE_ORE.get().getDefaultState(), 40)).range(65).square().chance(6));
        registerFHOre("ore_anthracite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.anthracite, RankineBlocks.ANTHRACITE_ORE.get().getDefaultState(), 50)).range(48).chance(14));
        registerFHOre("ore_graphite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.graphite, RankineBlocks.PLUMBAGO_ORE.get().getDefaultState(), 35)).range(50).chance(10));
        registerFHOre("ore_halite", FHORE.withConfiguration(new FHOreFeatureConfig(FHOreFeatureConfig.FillerBlockType.bauxite, FHBlocks.halite_ore.get().getDefaultState(), 40)).range(65).square().chance(7));

        registerFHDisk("copper_gravel", Feature.DISK.withConfiguration(new SphereReplaceConfig(COPPER_GRAVEL, FeatureSpread.create(1, 2), 1, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).chance(2));
        registerFHDisk("fh_disk_clay", Feature.DISK.withConfiguration(new SphereReplaceConfig(CLAY, FeatureSpread.create(2, 3), 1, clay_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).chance(2));
        registerFHDisk("fh_disk_gravel", Feature.DISK.withConfiguration(new SphereReplaceConfig(GRAVEL, FeatureSpread.create(2, 3), 2, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT));
        registerFHDisk("fh_disk_sand", Feature.DISK.withConfiguration(new SphereReplaceConfig(SAND, FeatureSpread.create(2, 4), 2, disk_target)).withPlacement(Features.Placements.SEAGRASS_DISK_PLACEMENT).count(3));
    }

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> registerFHDisk(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_DISK.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }

    private static <FC extends IFeatureConfig> ConfiguredFeature<FC, ?> registerFHOre(String key, ConfiguredFeature<FC, ?> configuredFeature) {
        FH_ORES.add(configuredFeature);
        return Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, key, configuredFeature);
    }
}
