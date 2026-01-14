/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.world;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.WildRubberDandelionBlock;
import com.teammoeg.frostedheart.content.world.features.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.DiskFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FossilFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

@SuppressWarnings("unused")
public class FHFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, FHMain.MODID);

    public static final RegistryObject<ImprovedFreezeTopLayerFeature> FREEZE_TOP_LAYER = register("freeze_top_layer", ImprovedFreezeTopLayerFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<ImprovedIceSpikeFeature> ICE_SPIKES = register("ice_spikes", ImprovedIceSpikeFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<DiskFeature> DISK = register("disk", DiskFeature::new, DiskConfiguration.CODEC);
    public static final RegistryObject<LayeredDiskFeature> LAYERED_DISK = register("layered_disk", LayeredDiskFeature::new, LayeredDiskConfiguration.CODEC);
    public static final RegistryObject<SpacecraftFeature> SPACECRAFT = register("spacecraft", SpacecraftFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<FHFossilFeature> FH_FOSSIL = register("fossil", FHFossilFeature::new, FossilFeatureConfiguration.CODEC);
    public static final RegistryObject<FallenLogFeature> FALLEN_LOG = register("fallen_log", FallenLogFeature::new, FallenLogConfig.CODEC);

    private static <C extends FeatureConfiguration, F extends Feature<C>> RegistryObject<F> register(String name, Function<Codec<C>, F> feature, Codec<C> codec)
    {
        return FEATURES.register(name, () -> feature.apply(codec));
    }

    public static final class FHPlacedFeatures
    {
        public static final ResourceKey<PlacedFeature> FREEZE_TOP_LAYER = key("freeze_top_layer");
        public static final ResourceKey<PlacedFeature> ICE_SPIKES = key("ice_spikes");
        public static final ResourceKey<PlacedFeature> ICE_PATCH = key("ice_patch");
        public static final ResourceKey<PlacedFeature> SNOW_PATCH = key("snow_patch");
        public static final ResourceKey<PlacedFeature> POWDER_SNOW_PATCH = key("powder_snow_patch");
        public static final ResourceKey<PlacedFeature> CLAY_PERMAFROST_PATCH = key("clay_permafrost_patch");
        public static final ResourceKey<PlacedFeature> GRAVEL_PERMAFROST_PATCH = key("gravel_permafrost_patch");
        public static final ResourceKey<PlacedFeature> SPACECRAFT = key("spacecraft");

        public static final ResourceKey<PlacedFeature> FH_FOSSIL = key("fossil");
        // public static final ResourceKey<PlacedFeature> THIN_ICE_PATCH = key("thin_ice_patch");
        public static final ResourceKey<PlacedFeature> WILD_RUBBER_DANDELION = key("wild_rubber_dandelion");

        private static ResourceKey<PlacedFeature> key(String name)
        {
            return ResourceKey.create(Registries.PLACED_FEATURE, FHMain.rl(name));
        }
        
        public static void bootstrap(BootstapContext<PlacedFeature> ctx){
            HolderGetter<ConfiguredFeature<?, ?>> featureLookup = ctx.lookup(Registries.CONFIGURED_FEATURE);
            Holder<ConfiguredFeature<?,?>> wildRubberDandelion = featureLookup.getOrThrow(FHConfiguredFeatures.WILD_RUBBER_DANDELION);
            
            PlacementUtils.register(ctx,WILD_RUBBER_DANDELION,wildRubberDandelion,
                    RarityFilter.onAverageOnceEvery(64),
                    InSquarePlacement.spread(),
                    PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                    BiomeFilter.biome());
        }
    }
    
    public static final class FHConfiguredFeatures{
        public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_RUBBER_DANDELION = key("wild_rubber_dandelion");
        
        @SuppressWarnings("SameParameterValue")
        private static ResourceKey<ConfiguredFeature<?, ?>> key(String name){
            return ResourceKey.create(Registries.CONFIGURED_FEATURE, FHMain.rl(name));
        }
        
        public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> ctx){
            FeatureUtils.register(ctx, WILD_RUBBER_DANDELION,Feature.RANDOM_PATCH,
                    new RandomPatchConfiguration(64,12,4,
                            PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                                    new SimpleBlockConfiguration(
                                            new RandomizedIntStateProvider(
                                                    BlockStateProvider.simple(FHBlocks.WILD_RUBBER_DANDELION.get()),
                                                    WildRubberDandelionBlock.VARIANT,
                                                    UniformInt.of(0,2))))));
        }
    }

}
