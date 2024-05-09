package com.teammoeg.frostedheart.world.gen;

import com.teammoeg.frostedheart.world.FHFeatures;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import static net.minecraft.world.biome.Biome.Category.*;

public class FHOreGeneration {
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
}
