package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;


public record ShrubTreeConfig(
        BlockStateProvider logProvider,
        BlockStateProvider foliageProvider,
        IntProvider branchCount,
        IntProvider branchLength
) implements FeatureConfiguration {

    public static final Codec<ShrubTreeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(ShrubTreeConfig::logProvider),
            BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(ShrubTreeConfig::foliageProvider),
            IntProvider.CODEC.fieldOf("branch_count").forGetter(ShrubTreeConfig::branchCount),
            IntProvider.CODEC.fieldOf("branch_length").forGetter(ShrubTreeConfig::branchLength)
    ).apply(instance, ShrubTreeConfig::new));
}
