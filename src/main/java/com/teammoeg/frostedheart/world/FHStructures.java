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

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.world.civilization.alliance.DestroyedGeneratorStructure;
import com.teammoeg.frostedheart.world.civilization.orbit.observatory.ObservatoryPiece;
import com.teammoeg.frostedheart.world.civilization.orbit.observatory.ObservatoryStructure;

import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHStructures {
    //For now... (Added by Mixxs... please help)
    public static DeferredRegister<StructureFeature<?>> STRUCTURES = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, FHMain.MODID);

    public static final RegistryObject<StructureFeature<NoneFeatureConfiguration>> DESTROYED_GENERATOR =
            STRUCTURES.register("destroyed_generator", DestroyedGeneratorStructure::new);

    public static void setupStructures(){
        setupMapSpacingAndLand(DESTROYED_GENERATOR.get(),
                new StructureFeatureConfiguration(100,50, 841515441),
                true);
    }

    public static <F extends StructureFeature<?>> void setupMapSpacingAndLand(F structure, StructureFeatureConfiguration structureSeparationSettings,
                                                                       boolean transformSurroundingLand) {
        StructureFeature.STRUCTURES_REGISTRY.put(structure.getRegistryName().toString(), structure);

        if (transformSurroundingLand) {
            StructureFeature.NOISE_AFFECTING_FEATURES = ImmutableList.<StructureFeature<?>>builder()
                    .addAll(StructureFeature.NOISE_AFFECTING_FEATURES)
                    .add(structure)
                    .build();
        }

        StructureSettings.DEFAULTS =
                ImmutableMap.<StructureFeature<?>, StructureFeatureConfiguration>builder()
                        .putAll(StructureSettings.DEFAULTS)
                        .put(structure, structureSeparationSettings)
                        .build();



        BuiltinRegistries.NOISE_GENERATOR_SETTINGS.entrySet().forEach(settings -> {
            Map<StructureFeature<?>, StructureFeatureConfiguration> structureMap = settings.getValue().structureSettings().structureConfig();

            if (structureMap instanceof ImmutableMap) {
                Map<StructureFeature<?>, StructureFeatureConfiguration> tempMap = new HashMap<>(structureMap);
                tempMap.put(structure, structureSeparationSettings);
                settings.getValue().structureSettings().structureConfig();

            } else {
                structureMap.put(structure, structureSeparationSettings);
            }
        });

    }

    public static void register(IEventBus eventBus){
        STRUCTURES.register(eventBus);
    }



    // Before




    public static final StructurePieceType OBSERVATORY_PIECE = registerPiece(ObservatoryPiece::new, "observatory");
//    public static final IStructurePieceType VOLCANIC_VENT_PIECE = registerPiece(VolcanicVentPiece::new, "volcanic_vent");

    public static final StructureFeature<NoneFeatureConfiguration> OBSERVATORY = new ObservatoryStructure(NoneFeatureConfiguration.CODEC);
//    public static final Structure<NoFeatureConfig> VOLCANIC_VENT = new VolcanicVentStructure(NoFeatureConfig.CODEC);


    private static StructurePieceType registerPiece(StructurePieceType type, String key) {
        return Registry.register(Registry.STRUCTURE_PIECE, key, type);
    }

    public static void registerStructureGenerate() {
        StructureFeature.STRUCTURES_REGISTRY.put(RegistryUtils.getRegistryName(FHStructures.OBSERVATORY).toString(), FHStructures.OBSERVATORY);
//        Structure.NAME_STRUCTURE_BIMAP.put(FHStructures.VOLCANIC_VENT.getRegistryName().toString(), FHStructures.VOLCANIC_VENT);

        HashMap<StructureFeature<?>, StructureFeatureConfiguration> StructureSettingMap = new HashMap<>();
        StructureSettingMap.put(OBSERVATORY, new StructureFeatureConfiguration(30, 15, 545465463));
//        StructureSettingMap.put(VOLCANIC_VENT,new StructureSeparationSettings(12,8,123456));


        StructureSettings.DEFAULTS = ImmutableMap.<StructureFeature<?>, StructureFeatureConfiguration>builder()
                .putAll(StructureSettings.DEFAULTS)
                .putAll(StructureSettingMap)
                .build();
        StructureFeature.NOISE_AFFECTING_FEATURES = ImmutableList.<StructureFeature<?>>builder()
                .addAll(StructureFeature.NOISE_AFFECTING_FEATURES)
                .add(FHStructures.OBSERVATORY.getStructure())
                .build();
        BuiltinRegistries.NOISE_GENERATOR_SETTINGS.forEach(settings -> {
            Map<StructureFeature<?>, StructureFeatureConfiguration> structureMap = settings.structureSettings().structureConfig();
            if (structureMap instanceof ImmutableMap) {
                Map<StructureFeature<?>, StructureFeatureConfiguration> tempMap = new HashMap<>(structureMap);
                tempMap.putAll(StructureSettingMap);
                settings.structureSettings().structureConfig = tempMap;
            } else structureMap.putAll(StructureSettingMap);
        });
    }
}
