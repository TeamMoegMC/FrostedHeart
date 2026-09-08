/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;

import java.util.List;

/**
 * 成果独立定义，复用已有五类 ResearchResult 效果。
 */
public record ResultDefinition(ResearchResult result, String title, String body, List<KnowledgeKey> understanding,
                               boolean enabled) {
    public static final Codec<ResultDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResearchResult.CODEC.fieldOf("result").forGetter(ResultDefinition::result),
            Codec.STRING.fieldOf("title").forGetter(ResultDefinition::title),
            Codec.STRING.optionalFieldOf("body", "").forGetter(ResultDefinition::body),
            KnowledgeKey.CODEC.listOf().optionalFieldOf("understanding", List.of()).forGetter(ResultDefinition::understanding),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ResultDefinition::enabled)
    ).apply(i, ResultDefinition::new));

    public ResultDefinition {
        understanding = List.copyOf(understanding);
    }
}
