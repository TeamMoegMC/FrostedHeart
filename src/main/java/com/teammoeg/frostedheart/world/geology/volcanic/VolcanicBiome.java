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

import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeAmbience;
import net.minecraft.world.biome.BiomeGenerationSettings;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.biome.MoodSoundAmbience;
import net.minecraft.world.biome.ParticleEffectAmbience;

public class VolcanicBiome {
    public final Biome build() {
        Biome.Builder biomeBuilder = new Biome.Builder();
        biomeBuilder.precipitation(Biome.RainType.RAIN)
                .category(Biome.Category.NONE)
                .depth(1F)
                .scale(0.8F)
                .temperature(1.0F)
                .downfall(0.1F)
                .setEffects((new BiomeAmbience.Builder()).setWaterColor(4159204).setWaterFogColor(329011).setFogColor(12638463).withSkyColor(calculateSkyColor(0.8F)).setMoodSound(MoodSoundAmbience.DEFAULT_CAVE).setParticle(new ParticleEffectAmbience(ParticleTypes.WHITE_ASH, 0.068093334F)).build());

        BiomeGenerationSettings.Builder biomeGenBuilder = new BiomeGenerationSettings.Builder();
        this.Generation(biomeGenBuilder);

        biomeBuilder.withGenerationSettings(biomeGenBuilder.build());

        MobSpawnInfo.Builder mobSpawnBuilder = new MobSpawnInfo.Builder();
        this.MobSpawn(mobSpawnBuilder);
        biomeBuilder.withMobSpawnSettings(mobSpawnBuilder.build());

        return biomeBuilder.build();
    }

    public int calculateSkyColor(float temperature) {
        float lvt_1_1_ = temperature / 3.0F;
        lvt_1_1_ = MathHelper.clamp(lvt_1_1_, -1.0F, 1.0F);
        return MathHelper.hsvToRGB(0.62222224F - lvt_1_1_ * 0.05F, 0.5F + lvt_1_1_ * 0.1F, 1.0F);
    }

    public void Generation(BiomeGenerationSettings.Builder builder) {
        builder.withSurfaceBuilder(FHSurfaceBuilder.VOLCANIC);
        DefaultBiomeFeatures.withLavaLakes(builder);
        DefaultBiomeFeatures.withBadlandsStructures(builder);
        DefaultBiomeFeatures.withDesertDeadBushes(builder);

        DefaultBiomeFeatures.withInfestedStone(builder);
        DefaultBiomeFeatures.withLavaAndWaterSprings(builder);
        DefaultBiomeFeatures.withCavesAndCanyons(builder);
//        builder.withStructure(FHStructureFeatures.VOLCANIC_VENT_FEATURE);
//        builder.withStructure(FHStructureFeatures.OBSERVATORY_FEATURE);
    }

    public void MobSpawn(MobSpawnInfo.Builder builder) {
        DefaultBiomeFeatures.withDesertMobs(builder);
    }
}
