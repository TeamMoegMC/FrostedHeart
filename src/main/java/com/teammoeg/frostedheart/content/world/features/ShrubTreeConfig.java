/*
 * Copyright (c) 2026 TeamMoeg
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
