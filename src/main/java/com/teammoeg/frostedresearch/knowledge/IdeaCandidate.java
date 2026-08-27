/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/** A server-derived, not-yet-recorded question revealed by an inspiration session. */
public record IdeaCandidate(ResourceLocation topicId, ResourceLocation ideaId,
        Set<UUID> evidence, String source) {
    public IdeaCandidate {
        evidence = Set.copyOf(evidence);
    }

    public String semanticKey() {
        return topicId + "|" + ideaId;
    }
}
