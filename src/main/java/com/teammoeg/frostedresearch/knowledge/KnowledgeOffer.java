/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.resources.ResourceLocation;

/** Validated direct-person knowledge offered to a team. */
public record KnowledgeOffer(ResourceLocation provider, ResourceLocation topicId,
        ResourceLocation ideaId, String source) {
}
