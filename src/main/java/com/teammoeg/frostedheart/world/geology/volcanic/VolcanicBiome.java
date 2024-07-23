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

package com.teammoeg.frostedheart.world.geology.volcanic;

import com.teammoeg.frostedheart.world.FHSurfaceBuilder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;

public class VolcanicBiome {
    public final Biome build() {
        Biome.BiomeBuilder biomeBuilder = new Biome.BiomeBuilder();
        biomeBuilder.precipitation(Biome.Precipitation.RAIN)
                .biomeCategory(Biome.BiomeCategory.NONE)
                .depth(1F)
                .scale(0.8F)
                .temperature(1.0F)
                .downfall(0.1F)
                .specialEffects((new BiomeSpecialEffects.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.068093334F)).build());

        BiomeGenerationSettings.Builder biomeGenBuilder = new BiomeGenerationSettings.Builder();
        this.Generation(biomeGenBuilder);

        biomeBuilder.generationSettings(biomeGenBuilder.build());

        MobSpawnSettings.Builder mobSpawnBuilder = new MobSpawnSettings.Builder();
        this.MobSpawn(mobSpawnBuilder);
        biomeBuilder.mobSpawnSettings(mobSpawnBuilder.build());

        return biomeBuilder.build();
    }

    public int calculateSkyColor(float temperature) {
        float lvt_1_1_ = temperature / 3.0F;
        lvt_1_1_ = Mth.clamp(lvt_1_1_, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - lvt_1_1_ * 0.05F, 0.5F + lvt_1_1_ * 0.1F, 1.0F);
    }

    public void Generation(BiomeGenerationSettings.Builder builder) {
        builder.surfaceBuilder(FHSurfaceBuilder.VOLCANIC);
        BiomeDefaultFeatures.addDesertLakes(builder);
        BiomeDefaultFeatures.addDefaultOverworldLandMesaStructures(builder);
        BiomeDefaultFeatures.addDesertVegetation(builder);

        BiomeDefaultFeatures.addInfestedStone(builder);
        BiomeDefaultFeatures.addDefaultSprings(builder);
        BiomeDefaultFeatures.addDefaultCarvers(builder);
//        builder.withStructure(FHStructureFeatures.VOLCANIC_VENT_FEATURE);
//        builder.withStructure(FHStructureFeatures.OBSERVATORY_FEATURE);
    }

    public void MobSpawn(MobSpawnSettings.Builder builder) {
        BiomeDefaultFeatures.desertSpawns(builder);
    }
}
