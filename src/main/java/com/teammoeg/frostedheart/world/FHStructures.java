package com.teammoeg.frostedheart.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.teammoeg.frostedheart.world.structure.ObservatoryPiece;
import com.teammoeg.frostedheart.world.structure.ObservatoryStructure;
import com.teammoeg.frostedheart.world.structure.VolcanicVentPiece;
import com.teammoeg.frostedheart.world.structure.VolcanicVentStructure;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;

import java.util.HashMap;
import java.util.Map;

public class FHStructures {
    
    public static final IStructurePieceType OBSERVATORY_PIECE = registerPiece(ObservatoryPiece::new, "observatory");
//    public static final IStructurePieceType VOLCANIC_VENT_PIECE = registerPiece(VolcanicVentPiece::new, "volcanic_vent");


    public static final Structure<NoFeatureConfig> OBSERVATORY = new ObservatoryStructure(NoFeatureConfig.CODEC);
//    public static final Structure<NoFeatureConfig> VOLCANIC_VENT = new VolcanicVentStructure(NoFeatureConfig.CODEC);


    public static void registerStructureGenerate() {
        Structure.NAME_STRUCTURE_BIMAP.put(FHStructures.OBSERVATORY.getRegistryName().toString(), FHStructures.OBSERVATORY);
//        Structure.NAME_STRUCTURE_BIMAP.put(FHStructures.VOLCANIC_VENT.getRegistryName().toString(), FHStructures.VOLCANIC_VENT);

        HashMap<Structure<?>, StructureSeparationSettings> StructureSettingMap = new HashMap<>();
        StructureSettingMap.put(OBSERVATORY,new StructureSeparationSettings(30,15,545465463));
//        StructureSettingMap.put(VOLCANIC_VENT,new StructureSeparationSettings(12,8,123456));


        DimensionStructuresSettings.field_236191_b_=ImmutableMap.<Structure<?>, StructureSeparationSettings>builder()
                .putAll(DimensionStructuresSettings.field_236191_b_)
                .putAll(StructureSettingMap)
                .build();
        Structure.field_236384_t_=ImmutableList.<Structure<?>>builder()
                .addAll(Structure.field_236384_t_)
                .add(FHStructures.OBSERVATORY.getStructure())
                .build();
        WorldGenRegistries.NOISE_SETTINGS.forEach(settings -> {
            Map<Structure<?>, StructureSeparationSettings> structureMap = settings.getStructures().func_236195_a_();
            if(structureMap instanceof ImmutableMap){
                Map<Structure<?>, StructureSeparationSettings> tempMap = new HashMap<>(structureMap);
                tempMap.putAll(StructureSettingMap);
                settings.getStructures().field_236193_d_ = tempMap;
            }
            else structureMap.putAll(StructureSettingMap);
        });
    }

    private static IStructurePieceType registerPiece(IStructurePieceType type, String key) {
        return Registry.register(Registry.STRUCTURE_PIECE,  key, type);
    }
}
