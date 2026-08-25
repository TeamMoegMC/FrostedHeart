/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Authoritative, team-owned identities acquired from V2 research results. */
public final class TeamKnowledgeData implements SpecialData {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(LinkedHashSet::new, TeamKnowledgeData::sorted);
    public static final Codec<TeamKnowledgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schemaVersion", 0).forGetter(data -> CURRENT_SCHEMA_VERSION),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredFindingIds", Set.of()).forGetter(TeamKnowledgeData::findingIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredDesignIds", Set.of()).forGetter(TeamKnowledgeData::designIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredConstructionIds", Set.of()).forGetter(TeamKnowledgeData::constructionIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredProcedureIds", Set.of()).forGetter(TeamKnowledgeData::procedureIds)
    ).apply(instance, TeamKnowledgeData::new));

    private final Set<ResourceLocation> findingIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> designIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> constructionIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> procedureIds = new LinkedHashSet<>();
    private long mutationRevision;

    public TeamKnowledgeData() {
    }

    public TeamKnowledgeData(int schemaVersion,
            Set<ResourceLocation> findingIds,
            Set<ResourceLocation> designIds,
            Set<ResourceLocation> constructionIds,
            Set<ResourceLocation> procedureIds) {
        this.findingIds.addAll(findingIds);
        this.designIds.addAll(designIds);
        this.constructionIds.addAll(constructionIds);
        this.procedureIds.addAll(procedureIds);
    }

    public Set<ResourceLocation> findingIds() {
        return Set.copyOf(findingIds);
    }

    public Set<ResourceLocation> designIds() {
        return Set.copyOf(designIds);
    }

    public Set<ResourceLocation> constructionIds() {
        return Set.copyOf(constructionIds);
    }

    public Set<ResourceLocation> procedureIds() {
        return Set.copyOf(procedureIds);
    }

    public long mutationRevision() {
        return mutationRevision;
    }

    public boolean hasFinding(ResourceLocation id) {
        return findingIds.contains(id);
    }

    public boolean hasDesign(ResourceLocation id) {
        return designIds.contains(id);
    }

    public boolean hasConstruction(ResourceLocation id) {
        return constructionIds.contains(id);
    }

    public boolean hasProcedure(ResourceLocation id) {
        return procedureIds.contains(id);
    }

    public TeamKnowledgeData copy() {
        return new TeamKnowledgeData(CURRENT_SCHEMA_VERSION,
                findingIds, designIds, constructionIds, procedureIds);
    }

    public void replaceWith(TeamKnowledgeData replacement) {
        replace(findingIds, replacement.findingIds);
        replace(designIds, replacement.designIds);
        replace(constructionIds, replacement.constructionIds);
        replace(procedureIds, replacement.procedureIds);
        mutationRevision++;
    }

    public boolean acquireFinding(ResourceLocation id) {
        return add(findingIds, id);
    }

    public boolean acquireDesign(ResourceLocation id) {
        return add(designIds, id);
    }

    public boolean acquireConstruction(ResourceLocation id) {
        return add(constructionIds, id);
    }

    public boolean acquireProcedure(ResourceLocation id) {
        return add(procedureIds, id);
    }

    public boolean revokeFinding(ResourceLocation id) {
        return remove(findingIds, id);
    }

    public boolean revokeDesign(ResourceLocation id) {
        return remove(designIds, id);
    }

    public boolean revokeConstruction(ResourceLocation id) {
        return remove(constructionIds, id);
    }

    public boolean revokeProcedure(ResourceLocation id) {
        return remove(procedureIds, id);
    }

    private boolean add(Set<ResourceLocation> target, ResourceLocation id) {
        boolean changed = target.add(id);
        if (changed) mutationRevision++;
        return changed;
    }

    private boolean remove(Set<ResourceLocation> target, ResourceLocation id) {
        boolean changed = target.remove(id);
        if (changed) mutationRevision++;
        return changed;
    }

    private static void replace(Set<ResourceLocation> target, Collection<ResourceLocation> values) {
        target.clear();
        target.addAll(values);
    }

    private static List<ResourceLocation> sorted(Set<ResourceLocation> values) {
        List<ResourceLocation> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        return sorted;
    }
}
