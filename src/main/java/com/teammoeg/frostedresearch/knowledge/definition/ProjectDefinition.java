/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 知识图谱使用的项目及成果关联；项目执行玩法由研究系统负责。
 */
public record ProjectDefinition(String title, List<ResourceLocation> results) {
    public static final Codec<ProjectDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("title", "").forGetter(ProjectDefinition::title),
            ResourceLocation.CODEC.listOf().fieldOf("results").forGetter(ProjectDefinition::results)
    ).apply(i, ProjectDefinition::new));

    public ProjectDefinition {
        results = List.copyOf(results);
    }
}
