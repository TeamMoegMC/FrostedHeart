package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;

public record LayeredDiskConfiguration(
        RuleBasedBlockStateProvider topStateProvider,
        RuleBasedBlockStateProvider bottomStateProvider,
        BlockPredicate target,
        IntProvider radius,
        int verticalSpan // total vertical span or separate values for each layer if desired
) implements FeatureConfiguration {
    public static final Codec<LayeredDiskConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(
                RuleBasedBlockStateProvider.CODEC.fieldOf("top_state_provider").forGetter(LayeredDiskConfiguration::topStateProvider),
                RuleBasedBlockStateProvider.CODEC.fieldOf("bottom_state_provider").forGetter(LayeredDiskConfiguration::bottomStateProvider),
                BlockPredicate.CODEC.fieldOf("target").forGetter(LayeredDiskConfiguration::target),
                IntProvider.codec(0, 8).fieldOf("radius").forGetter(LayeredDiskConfiguration::radius),
                Codec.intRange(0, 2).fieldOf("vertical_span").forGetter(LayeredDiskConfiguration::verticalSpan)
        ).apply(instance, LayeredDiskConfiguration::new);
    });
}
