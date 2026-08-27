/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;

/** Executable acceptance rule referenced by a topic. */
@FunctionalInterface
public interface ResolutionHandler {
    boolean canResolve(ResourceLocation topicId, ResearchTopicDefinition topic,
            ResearchTopicDefinition.Resolution resolution, TeamKnowledgeData data, IdeaRecord idea);
}
