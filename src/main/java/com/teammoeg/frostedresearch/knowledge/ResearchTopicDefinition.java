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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Phase-one slice of a future complete research topic definition. */
public record ResearchTopicDefinition(
        int format,
        Presentation presentation,
        List<ResearchResult> results,
        List<ItemReward> rewards) {
    public static final int CURRENT_FORMAT = 3;
    public static final Codec<ResearchTopicDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("format").forGetter(ResearchTopicDefinition::format),
            Presentation.CODEC.optionalFieldOf("presentation", Presentation.EMPTY).forGetter(ResearchTopicDefinition::presentation),
            ResearchResult.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(ResearchTopicDefinition::results),
            ItemReward.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(ResearchTopicDefinition::rewards)
    ).apply(instance, ResearchTopicDefinition::new));

    public ResearchTopicDefinition {
        results = List.copyOf(results);
        rewards = List.copyOf(rewards);
    }

    public record Presentation(Optional<CIcons.CIcon> icon) {
        public static final Presentation EMPTY = new Presentation(Optional.empty());
        public static final Codec<Presentation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CIcons.CODEC.optionalFieldOf("icon").forGetter(Presentation::icon)
        ).apply(instance, Presentation::new));
    }

    public record ItemReward(ResourceLocation item, int count) {
        public static final Codec<ItemReward> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(ItemReward::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ItemReward::count)
        ).apply(instance, ItemReward::new));
    }
}
