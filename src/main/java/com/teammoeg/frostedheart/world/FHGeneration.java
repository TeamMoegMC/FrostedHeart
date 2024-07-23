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

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import java.util.List;
import java.util.function.Supplier;

public class FHGeneration {
    public static void generate_overworld_ores(final BiomeLoadingEvent event){
        // Generate gravel and clay disks
        Biome.BiomeCategory category = event.getCategory();
        if (category == Biome.BiomeCategory.RIVER || category == Biome.BiomeCategory.BEACH) {
            for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_DISK)
                event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature);
        }
        // Generate rankine ores
        for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_ORES)
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature);
        // Generate clay and gravel deposit
        if (category != Biome.BiomeCategory.TAIGA && category != Biome.BiomeCategory.EXTREME_HILLS && category != Biome.BiomeCategory.OCEAN && category != Biome.BiomeCategory.DESERT && category != Biome.BiomeCategory.RIVER) {
            event.getGeneration().addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, FHFeatures.clay_deposit);
            event.getGeneration().addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, FHFeatures.gravel_deposit);
        }
    }
    public static void generate_nether_ores(final BiomeLoadingEvent event){

    }
    public static void generate_end_ores(final BiomeLoadingEvent event){

    }

    public static void generate_overworld_structures(final BiomeLoadingEvent event){
        if(event.getName() == null){
            return;
        }

        Biome.BiomeCategory category = event.getCategory();

        if (category == Biome.BiomeCategory.EXTREME_HILLS || category == Biome.BiomeCategory.TAIGA) {
            event.getGeneration().addStructureStart(FHStructureFeatures.OBSERVATORY_FEATURE);
        }

        if(category.name().equals("volcanic") || category == Biome.BiomeCategory.PLAINS || category == Biome.BiomeCategory.MESA || category == Biome.BiomeCategory.EXTREME_HILLS) {
            List<Supplier<ConfiguredStructureFeature<?, ?>>> structures = event.getGeneration().getStructures();
            structures.add(() -> FHStructures.DESTROYED_GENERATOR.get().configured(FeatureConfiguration.NONE));
        }

    }
}
