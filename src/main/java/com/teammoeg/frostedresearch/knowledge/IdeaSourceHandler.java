/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Executable matcher behind a data-authored idea source. */
@FunctionalInterface
public interface IdeaSourceHandler {
    Optional<IdeaCandidate> match(ResourceLocation topicId, ResearchTopicDefinition topic,
            ResearchTopicDefinition.IdeaSource source, TeamKnowledgeData data, Set<UUID> pinnedEvidence);
}
