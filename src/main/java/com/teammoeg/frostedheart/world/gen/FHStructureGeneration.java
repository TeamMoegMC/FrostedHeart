package com.teammoeg.frostedheart.world.gen;


import com.teammoeg.frostedheart.world.FHStructureFeatures;
import com.teammoeg.frostedheart.world.FHStructures;
import net.minecraft.world.biome.Biome;

import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.world.biome.Biome.Category.*;

public class FHStructureGeneration {
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
