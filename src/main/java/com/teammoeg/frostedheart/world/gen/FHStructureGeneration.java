package com.teammoeg.frostedheart.world.gen;

import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.FHStructureFeatures;
import com.teammoeg.frostedheart.world.FHStructures;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.datafix.fixes.BiomeName;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Level;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.world.FHSurfaceBuilder.VOLCANIC;
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
