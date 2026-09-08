/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * 研究系统报告的实际经历；不实现研究任务或进度。 / Actual research provenance, not project execution.
 */
public record ResearchHistory(UUID id, ResourceLocation project, KnowledgeKey idea, long time, boolean completed,
                              List<KnowledgeKey> results) {
    public static final Codec<ResearchHistory> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(ResearchHistory::id),
            ResourceLocation.CODEC.fieldOf("project").forGetter(ResearchHistory::project),
            KnowledgeKey.CODEC.fieldOf("idea").forGetter(ResearchHistory::idea),
            Codec.LONG.fieldOf("time").forGetter(ResearchHistory::time),
            Codec.BOOL.fieldOf("completed").forGetter(ResearchHistory::completed),
            KnowledgeKey.CODEC.listOf().fieldOf("results").forGetter(ResearchHistory::results)
    ).apply(i, ResearchHistory::new));

    public ResearchHistory {
        results = List.copyOf(results);
    }
}
