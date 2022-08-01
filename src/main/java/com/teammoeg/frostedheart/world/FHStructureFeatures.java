package com.teammoeg.frostedheart.world;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class FHStructureFeatures {
    public static final StructureFeature<?, ?> OBSERVATORY_FEATURE = FHStructures.OBSERVATORY.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);
//    public static final StructureFeature<?, ?> VOLCANIC_VENT_FEATURE = FHStructures.VOLCANIC_VENT.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);


    public static void registerStructureFeatures() {
        Registry<StructureFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE;
        Registry.register(registry, new ResourceLocation(FHMain.MODID, "observatory"), OBSERVATORY_FEATURE);
//        Registry.register(registry, new ResourceLocation(FHMain.MODID,"volcanic_vent"), VOLCANIC_VENT_FEATURE);
    }
}
