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

import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.world.biome.Biome.Category.*;

public class FHGeneration {
    public static void generate_overworld_ores(final BiomeLoadingEvent event){
        // Generate gravel and clay disks
        Biome.Category category = event.getCategory();
        if (category == RIVER || category == BEACH) {
            for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_DISK)
                event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
        }
        // Generate rankine ores
        for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_ORES)
            event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
        // Generate clay and gravel deposit
        if (category != TAIGA && category != EXTREME_HILLS && category != OCEAN && category != DESERT && category != RIVER) {
            event.getGeneration().withFeature(GenerationStage.Decoration.LOCAL_MODIFICATIONS, FHFeatures.clay_deposit);
            event.getGeneration().withFeature(GenerationStage.Decoration.LOCAL_MODIFICATIONS, FHFeatures.gravel_deposit);
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

        Biome.Category category = event.getCategory();

        if (category == EXTREME_HILLS || category == TAIGA) {
            event.getGeneration().withStructure(FHStructureFeatures.OBSERVATORY_FEATURE);
        }

        if(category.name().equals("volcanic") || category == PLAINS || category == MESA || category == EXTREME_HILLS) {
            List<Supplier<StructureFeature<?, ?>>> structures = event.getGeneration().getStructures();
            structures.add(() -> FHStructures.DESTROYED_GENERATOR.get().withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG));
        }

    }
}
