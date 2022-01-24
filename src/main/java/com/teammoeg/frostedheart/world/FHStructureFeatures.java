package com.teammoeg.frostedheart.world;

import com.google.common.collect.ImmutableMap;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;

import java.util.HashMap;
import java.util.Map;

public class FHStructureFeatures {
    public static StructureFeature<?, ?> Observatory_Feature = FHStructures.Observatory.get().withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);
    public static void registerStructureFeatures() {
        Structure.NAME_STRUCTURE_BIMAP.put(FHStructures.Observatory.get().getRegistryName().toString(), FHStructures.Observatory.get());
        Registry<StructureFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE;
        Registry.register(registry, new ResourceLocation(FHMain.MODID,"observatory"), Observatory_Feature);
        StructureSeparationSettings structureConfig = new StructureSeparationSettings(8,4,123456789);

        DimensionStructuresSettings.field_236191_b_  = ImmutableMap.<Structure<?>, StructureSeparationSettings>builder()
                        .putAll(DimensionStructuresSettings.field_236191_b_)
                        .put(FHStructures.Observatory.get(), structureConfig)
                        .build();

        WorldGenRegistries.NOISE_SETTINGS.forEach(settings -> {
            Map<Structure<?>, StructureSeparationSettings> structureMap = settings.getStructures().func_236195_a_();

            if(structureMap instanceof ImmutableMap){
                Map<Structure<?>, StructureSeparationSettings> tempMap = new HashMap<>(structureMap);
                tempMap.put(FHStructures.Observatory.get(), structureConfig);
                settings.getStructures().field_236193_d_ = tempMap;
            }
            else{
                structureMap.put(FHStructures.Observatory.get(), structureConfig);
            }
        });
    }
}
