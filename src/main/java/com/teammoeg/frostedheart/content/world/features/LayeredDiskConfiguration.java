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
