/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Existing component address, backed by the unified archive rather than separate entitlement sets.
 */
public final class TeamKnowledgeData extends KnowledgeState {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final Codec<TeamKnowledgeData> CODEC = KnowledgeState.CODEC.xmap(TeamKnowledgeData::new, data -> data);

    public TeamKnowledgeData() {
    }

    public TeamKnowledgeData(KnowledgeState state) {
        replaceState(state);
    }

    public TeamKnowledgeData(int schema, Set<ResourceLocation> findings, Set<ResourceLocation> designs, Set<ResourceLocation> constructions, Set<ResourceLocation> procedures) {
        findings.forEach(this::acquireFinding);
        designs.forEach(this::acquireDesign);
        constructions.forEach(this::acquireConstruction);
        procedures.forEach(this::acquireProcedure);
    }

    public TeamKnowledgeData copy() {
        return new TeamKnowledgeData(this);
    }

    public void replaceWith(TeamKnowledgeData replacement) {
        replaceState(replacement);
    }

    public Set<ResourceLocation> findingIds() {
        return ids("finding");
    }

    public Set<ResourceLocation> designIds() {
        return ids("design");
    }

    public Set<ResourceLocation> constructionIds() {
        return ids("construction");
    }

    public Set<ResourceLocation> procedureIds() {
        return ids("procedure");
    }

    public boolean hasFinding(ResourceLocation id) {
        return has(id, "finding");
    }

    public boolean hasDesign(ResourceLocation id) {
        return has(id, "design");
    }

    public boolean hasConstruction(ResourceLocation id) {
        return has(id, "construction");
    }

    public boolean hasProcedure(ResourceLocation id) {
        return has(id, "procedure");
    }

    // Kept for existing direct data callers. Gameplay calls KnowledgeService for checks/events/sync.
    public boolean acquireFinding(ResourceLocation id) {
        return add(id, "finding");
    }

    public boolean acquireDesign(ResourceLocation id) {
        return add(id, "design");
    }

    public boolean acquireConstruction(ResourceLocation id) {
        return add(id, "construction");
    }

    public boolean acquireProcedure(ResourceLocation id) {
        return add(id, "procedure");
    }

    public boolean revokeFinding(ResourceLocation id) {
        return remove(id, "finding");
    }

    public boolean revokeDesign(ResourceLocation id) {
        return remove(id, "design");
    }

    public boolean revokeConstruction(ResourceLocation id) {
        return remove(id, "construction");
    }

    public boolean revokeProcedure(ResourceLocation id) {
        return remove(id, "procedure");
    }

    private Set<ResourceLocation> ids(String type) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        archive().entries().values().stream().filter(e -> e.resultType().equals(type)).forEach(e -> ids.add(new ResourceLocation(e.element().key().id())));
        return Set.copyOf(ids);
    }

    private boolean has(ResourceLocation id, String type) {
        var entry = archive().entries().get(KnowledgeKey.result(id));
        return entry != null && entry.resultType().equals(type);
    }

    private boolean add(ResourceLocation id, String type) {
        if (archive().contains(KnowledgeKey.result(id))) return false;
        putArchive(new KnowledgeEntry(KnowledgeElement.result(id), new AcquisitionSource("direct_api", Optional.empty(), 0), 0, type));
        return true;
    }

    private boolean remove(ResourceLocation id, String type) {
        return has(id, type) && removeArchive(KnowledgeKey.result(id)); }
}
