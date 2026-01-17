package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record FallenLogConfig(BlockStateProvider logProvider, IntProvider length) implements FeatureConfiguration {
    public static final Codec<FallenLogConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(FallenLogConfig::logProvider),
            IntProvider.codec(0, 10).fieldOf("length").forGetter(FallenLogConfig::length)
    ).apply(instance, FallenLogConfig::new));
}
