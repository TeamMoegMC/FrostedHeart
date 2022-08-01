package com.teammoeg.frostedheart.world.biome;

import com.teammoeg.frostedheart.world.FHSurfaceBuilder;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.*;

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

    public void MobSpawn(MobSpawnInfo.Builder builder) {
        DefaultBiomeFeatures.withDesertMobs(builder);
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

    public int calculateSkyColor(float temperature) {
        float lvt_1_1_ = temperature / 3.0F;
        lvt_1_1_ = MathHelper.clamp(lvt_1_1_, -1.0F, 1.0F);
        return MathHelper.hsvToRGB(0.62222224F - lvt_1_1_ * 0.05F, 0.5F + lvt_1_1_ * 0.1F, 1.0F);
    }
}
