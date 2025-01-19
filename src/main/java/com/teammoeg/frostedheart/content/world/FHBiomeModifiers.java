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

package com.teammoeg.frostedheart.content.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, FHMain.MODID);
    public static final RegistryObject<Codec<? extends Instance>> CODEC =
            BIOME_MODIFIERS.register("instance", () -> {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(PlacedFeature.LIST_CODEC.fieldOf("surface_structures").forGetter((c) -> {
                return c.surfaceStructures;
            }), PlacedFeature.LIST_CODEC.fieldOf("top_layer_modification").forGetter((c) -> {
                return c.topLayerModification;
            })).apply(instance,Instance::new);
        });
    });

    record Instance(HolderSet<PlacedFeature> surfaceStructures, HolderSet<PlacedFeature> topLayerModification) implements BiomeModifier
    {
        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
        {
            if (biome.unwrapKey().map(k -> !FHConfig.isWinterBiome(k.location())).orElse(true) || phase != Phase.MODIFY)
            {
                return;
            }

            final ClimateSettingsBuilder climate = builder.getClimateSettings();
            climate.setHasPrecipitation(true);
            climate.setTemperature(-0.5f);
            climate.setTemperatureModifier(Biome.TemperatureModifier.NONE);

            // TODO: check if we keep this
            builder.getSpecialEffects()
                    .waterColor(0x3938C9)
                    .waterFogColor(0x050533);

            final BiomeGenerationSettingsBuilder settings = builder.getGenerationSettings();
            for (Holder<PlacedFeature> feature : surfaceStructures)
            {
                settings.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, feature);
            }

            settings.getFeatures(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
                    .removeIf(holder -> holder.unwrapKey().map(key -> key == MiscOverworldPlacements.FREEZE_TOP_LAYER).orElse(false));
            for (Holder<PlacedFeature> feature : topLayerModification)
            {
                settings.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, feature);
            }

            final MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();

            // TODO: spawn rates
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.POLAR_BEAR, 5, 1, 3));
            // spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.STRAY, 20, 1, 3));
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(FHEntityTypes.WANDERING_REFUGEE.get(), 1, 1, 5));

        }

        @Override
        public Codec<? extends BiomeModifier> codec()
        {
            return CODEC.get();
        }
    }
}
