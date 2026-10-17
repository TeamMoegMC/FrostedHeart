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
