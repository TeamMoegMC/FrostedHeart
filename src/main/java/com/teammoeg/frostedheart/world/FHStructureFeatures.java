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

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

public class FHStructureFeatures {
    public static final ConfiguredStructureFeature<?, ?> OBSERVATORY_FEATURE = FHStructures.OBSERVATORY.configured(FeatureConfiguration.NONE);
//    public static final StructureFeature<?, ?> VOLCANIC_VENT_FEATURE = FHStructures.VOLCANIC_VENT.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);


    public static void registerStructureFeatures() {
        Registry<ConfiguredStructureFeature<?, ?>> registry = BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE;
        Registry.register(registry, new ResourceLocation(FHMain.MODID, "observatory"), OBSERVATORY_FEATURE);
//        Registry.register(registry, new ResourceLocation(FHMain.MODID,"volcanic_vent"), VOLCANIC_VENT_FEATURE);
    }
}
