/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.frostedresearch.knowledge.FieldComparisonArtifact;
import com.teammoeg.frostedresearch.knowledge.IdeaRecord;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Authoritative, team-owned identities acquired from V2 research results. */
public final class TeamKnowledgeData implements SpecialData {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(LinkedHashSet::new, TeamKnowledgeData::sorted);
    public static final Codec<TeamKnowledgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schemaVersion", 0).forGetter(data -> CURRENT_SCHEMA_VERSION),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredFindingIds", Set.of()).forGetter(TeamKnowledgeData::findingIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredDesignIds", Set.of()).forGetter(TeamKnowledgeData::designIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredConstructionIds", Set.of()).forGetter(TeamKnowledgeData::constructionIds),
            RESOURCE_SET_CODEC.optionalFieldOf("acquiredProcedureIds", Set.of()).forGetter(TeamKnowledgeData::procedureIds),
            KnowledgeRecord.CODEC.listOf().optionalFieldOf("observations", List.of()).forGetter(TeamKnowledgeData::observations),
            IdeaRecord.CODEC.listOf().optionalFieldOf("ideas", List.of()).forGetter(TeamKnowledgeData::ideas),
            FieldComparisonArtifact.CODEC.listOf().optionalFieldOf("comparisons", List.of()).forGetter(TeamKnowledgeData::comparisons)
    ).apply(instance, TeamKnowledgeData::new));
    /** Encoding path used by packets. Sealed observation facts are removed before serialization. */
    public static final Codec<TeamKnowledgeData> NETWORK_CODEC = CODEC.xmap(data -> data, TeamKnowledgeData::networkCopy);

    private final Set<ResourceLocation> findingIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> designIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> constructionIds = new LinkedHashSet<>();
    private final Set<ResourceLocation> procedureIds = new LinkedHashSet<>();
    private final Map<String, KnowledgeRecord> observations = new LinkedHashMap<>();
    private final Map<UUID, IdeaRecord> ideas = new LinkedHashMap<>();
    private final List<FieldComparisonArtifact> comparisons = new ArrayList<>();
    private long mutationRevision;

    public TeamKnowledgeData() {
    }

    public TeamKnowledgeData(int schemaVersion,
            Set<ResourceLocation> findingIds,
            Set<ResourceLocation> designIds,
            Set<ResourceLocation> constructionIds,
            Set<ResourceLocation> procedureIds) {
        this(schemaVersion, findingIds, designIds, constructionIds, procedureIds, List.of(), List.of(), List.of());
    }

    public TeamKnowledgeData(int schemaVersion,
            Set<ResourceLocation> findingIds,
            Set<ResourceLocation> designIds,
            Set<ResourceLocation> constructionIds,
            Set<ResourceLocation> procedureIds,
            List<KnowledgeRecord> observations,
            List<IdeaRecord> ideas,
            List<FieldComparisonArtifact> comparisons) {
        this.findingIds.addAll(findingIds);
        this.designIds.addAll(designIds);
        this.constructionIds.addAll(constructionIds);
        this.procedureIds.addAll(procedureIds);
        observations.forEach(record -> this.observations.put(record.semanticKey(), record));
        ideas.forEach(idea -> this.ideas.put(idea.id(), idea));
        this.comparisons.addAll(comparisons);
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

    public List<KnowledgeRecord> observations() {
        return List.copyOf(observations.values());
    }

    public List<IdeaRecord> ideas() {
        return List.copyOf(ideas.values());
    }

    public List<FieldComparisonArtifact> comparisons() {
        return List.copyOf(comparisons);
    }

    public Optional<KnowledgeRecord> observation(UUID id) {
        return observations.values().stream().filter(record -> record.id().equals(id)).findFirst();
    }

    public Optional<IdeaRecord> idea(ResourceLocation topicId, ResourceLocation ideaId) {
        return ideas.values().stream()
                .filter(idea -> idea.topicId().equals(topicId) && idea.ideaId().equals(ideaId)).findFirst();
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
                findingIds, designIds, constructionIds, procedureIds,
                observations(), ideas(), comparisons());
    }

    public TeamKnowledgeData networkCopy() {
        return new TeamKnowledgeData(CURRENT_SCHEMA_VERSION,
                findingIds, designIds, constructionIds, procedureIds,
                List.of(), List.of(), List.of());
    }

    public void replaceWith(TeamKnowledgeData replacement) {
        replace(findingIds, replacement.findingIds);
        replace(designIds, replacement.designIds);
        replace(constructionIds, replacement.constructionIds);
        replace(procedureIds, replacement.procedureIds);
        observations.clear();
        replacement.observations.values().forEach(record -> observations.put(record.semanticKey(), record));
        ideas.clear();
        ideas.putAll(replacement.ideas);
        comparisons.clear();
        comparisons.addAll(replacement.comparisons);
        mutationRevision++;
    }

    public boolean archiveObservation(KnowledgeRecord record) {
        KnowledgeRecord existing = observations.get(record.semanticKey());
        KnowledgeRecord replacement = existing == null ? record : existing.merge(record);
        if (replacement.equals(existing)) return false;
        observations.put(record.semanticKey(), replacement);
        mutationRevision++;
        return true;
    }

    public boolean recordIdea(IdeaRecord candidate) {
        IdeaRecord existing = ideas.get(candidate.id());
        IdeaRecord replacement = candidate;
        if (existing != null) {
            replacement = existing;
            for (String source : candidate.sources().stream().sorted().toList()) {
                replacement = replacement.merge(source, candidate.evidence(), candidate.lastUpdated());
            }
            if (existing.state() == IdeaRecord.State.ORPHAN
                    && candidate.state() != IdeaRecord.State.ORPHAN) {
                replacement = replacement.withState(candidate.state(), candidate.lastUpdated());
            }
        }
        if (replacement.equals(existing)) return false;
        ideas.put(replacement.id(), replacement);
        mutationRevision++;
        return true;
    }

    public boolean updateIdea(IdeaRecord replacement) {
        IdeaRecord existing = ideas.get(replacement.id());
        if (replacement.equals(existing)) return false;
        ideas.put(replacement.id(), replacement);
        mutationRevision++;
        return true;
    }

    public boolean appendComparison(FieldComparisonArtifact artifact) {
        if (comparisons.stream().anyMatch(existing -> existing.id().equals(artifact.id()))) return false;
        comparisons.add(artifact);
        mutationRevision++;
        return true;
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
