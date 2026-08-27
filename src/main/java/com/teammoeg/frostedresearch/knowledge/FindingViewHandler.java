/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Registered projection boundary through which a Finding may expose derived information. */
public interface FindingViewHandler {
    ResourceLocation id();

    /** Returns safe, client-visible annotations derived from this acquired Finding. */
    List<ResourceLocation> observationAnnotations(TeamKnowledgeData data, KnowledgeRecord record);
}
