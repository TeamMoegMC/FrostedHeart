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
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 想法内容与产生路径相互独立。
 */
public record IdeaDefinition(String title, String body, List<KnowledgeKey> understanding,
                             List<ResourceLocation> projects, boolean enabled) {
    public static final Codec<IdeaDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("title").forGetter(IdeaDefinition::title),
            Codec.STRING.optionalFieldOf("body", "").forGetter(IdeaDefinition::body),
            KnowledgeKey.CODEC.listOf().optionalFieldOf("understanding", List.of()).forGetter(IdeaDefinition::understanding),
            ResourceLocation.CODEC.listOf().optionalFieldOf("projects", List.of()).forGetter(IdeaDefinition::projects),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(IdeaDefinition::enabled)
    ).apply(i, IdeaDefinition::new));

    public IdeaDefinition {
        understanding = List.copyOf(understanding);
        projects = List.copyOf(projects);
    }
}
