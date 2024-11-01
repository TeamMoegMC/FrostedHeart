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

package com.teammoeg.frostedheart.world;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.features.ImprovedFreezeTopLayerFeature;
import com.teammoeg.frostedheart.world.features.ImprovedIceSpikeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.DiskFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class FHFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, FHMain.MODID);

    public static final RegistryObject<ImprovedFreezeTopLayerFeature> FREEZE_TOP_LAYER = register("freeze_top_layer", ImprovedFreezeTopLayerFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<ImprovedIceSpikeFeature> ICE_SPIKES = register("ice_spikes", ImprovedIceSpikeFeature::new, NoneFeatureConfiguration.CODEC);
    public static final RegistryObject<DiskFeature> DISK = register("disk", DiskFeature::new, DiskConfiguration.CODEC);

    private static <C extends FeatureConfiguration, F extends Feature<C>> RegistryObject<F> register(String name, Function<Codec<C>, F> feature, Codec<C> codec)
    {
        return FEATURES.register(name, () -> feature.apply(codec));
    }

    public static final class Keys
    {
        public static final ResourceKey<PlacedFeature> FREEZE_TOP_LAYER = key("freeze_top_layer");
        public static final ResourceKey<PlacedFeature> ICE_SPIKES = key("ice_spikes");
        public static final ResourceKey<PlacedFeature> ICE_PATCH = key("ice_patch");
        public static final ResourceKey<PlacedFeature> SNOW_PATCH = key("snow_patch");
        public static final ResourceKey<PlacedFeature> POWDER_SNOW_PATCH = key("powder_snow_patch");

        private static ResourceKey<PlacedFeature> key(String name)
        {
            return ResourceKey.create(Registries.PLACED_FEATURE, FHMain.rl(name));
        }
    }

}
