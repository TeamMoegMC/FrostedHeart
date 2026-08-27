/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read model for acquired findings. Orphan result IDs are intentionally absent. */
public final class KnowledgeProjection {
    public static final Codec<KnowledgeProjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FindingEntry.CODEC.listOf().optionalFieldOf("findings", List.of()).forGetter(value -> new ArrayList<>(value.findings.values())),
            ObservationSummary.CODEC.listOf().optionalFieldOf("observations", List.of()).forGetter(KnowledgeProjection::observations),
            IdeaSummary.CODEC.listOf().optionalFieldOf("ideas", List.of()).forGetter(KnowledgeProjection::ideas),
            ComparisonSummary.CODEC.listOf().optionalFieldOf("comparisons", List.of()).forGetter(KnowledgeProjection::comparisons),
            ActionCard.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(KnowledgeProjection::actions)
    ).apply(instance, KnowledgeProjection::new));
    public static final KnowledgeProjection EMPTY = new KnowledgeProjection(List.of(), List.of(), List.of(), List.of(), List.of());

    private final Map<ResourceLocation, FindingEntry> findings;
    private final List<ObservationSummary> observations;
    private final List<IdeaSummary> ideas;
    private final List<ComparisonSummary> comparisons;
    private final List<ActionCard> actions;

    public KnowledgeProjection(List<FindingEntry> findings) {
        this(findings, List.of(), List.of(), List.of(), List.of());
    }

    public KnowledgeProjection(List<FindingEntry> findings, List<ObservationSummary> observations,
            List<IdeaSummary> ideas, List<ComparisonSummary> comparisons, List<ActionCard> actions) {
        Map<ResourceLocation, FindingEntry> byId = new LinkedHashMap<>();
        findings.forEach(entry -> byId.put(entry.findingId(), entry));
        this.findings = Collections.unmodifiableMap(byId);
        this.observations = List.copyOf(observations);
        this.ideas = List.copyOf(ideas);
        this.comparisons = List.copyOf(comparisons);
        this.actions = List.copyOf(actions.stream().limit(3).toList());
    }

    public boolean hasFinding(ResourceLocation id) {
        return findings.containsKey(id);
    }

    public FindingEntry finding(ResourceLocation id) {
        return findings.get(id);
    }

    public Map<ResourceLocation, FindingEntry> findings() {
        return findings;
    }

    public List<ObservationSummary> observations() { return observations; }
    public List<IdeaSummary> ideas() { return ideas; }
    public List<ComparisonSummary> comparisons() { return comparisons; }
    public List<ActionCard> actions() { return actions; }

    public record FindingEntry(ResourceLocation findingId, List<ResourceLocation> views,
            AccessSource.ResultSource source) {
        public static final Codec<FindingEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("finding").forGetter(FindingEntry::findingId),
                ResourceLocation.CODEC.listOf().fieldOf("views").forGetter(FindingEntry::views),
                AccessSource.ResultSource.CODEC.fieldOf("source").forGetter(FindingEntry::source)
        ).apply(instance, FindingEntry::new));

        public FindingEntry {
            views = List.copyOf(views);
        }
    }

    public record ObservationSummary(UUID id, ResourceLocation kindId, ResourceLocation dimension,
            net.minecraft.core.BlockPos position, ResourceLocation subject,
            Map<String, String> stateProperties, Map<String, String> contextFacts,
            Set<ResourceLocation> publicFacets,
            Set<ResourceLocation> channels, long lastObserved, List<ResourceLocation> annotations) {
        public static final Codec<ObservationSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.UUIDUtil.CODEC.fieldOf("id").forGetter(ObservationSummary::id),
                ResourceLocation.CODEC.fieldOf("kind").forGetter(ObservationSummary::kindId),
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(ObservationSummary::dimension),
                net.minecraft.core.BlockPos.CODEC.fieldOf("position").forGetter(ObservationSummary::position),
                ResourceLocation.CODEC.fieldOf("subject").forGetter(ObservationSummary::subject),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("state", Map.of())
                        .forGetter(ObservationSummary::stateProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("context", Map.of())
                        .forGetter(ObservationSummary::contextFacts),
                ResourceLocation.CODEC.listOf().optionalFieldOf("facets", List.of())
                        .forGetter(value -> List.copyOf(value.publicFacets())),
                ResourceLocation.CODEC.listOf().optionalFieldOf("channels", List.of())
                        .forGetter(value -> List.copyOf(value.channels())),
                Codec.LONG.fieldOf("last_observed").forGetter(ObservationSummary::lastObserved),
                ResourceLocation.CODEC.listOf().optionalFieldOf("annotations", List.of())
                        .forGetter(ObservationSummary::annotations)
        ).apply(instance, (id, kind, dimension, position, subject, state, context, facets, channels, observed, annotations) ->
                new ObservationSummary(id, kind, dimension, position, subject, state, context,
                        Set.copyOf(facets), Set.copyOf(channels), observed, annotations)));

        public ObservationSummary {
            stateProperties = Map.copyOf(stateProperties);
            contextFacts = Map.copyOf(contextFacts);
            publicFacets = Set.copyOf(publicFacets);
            channels = Set.copyOf(channels);
            annotations = List.copyOf(annotations);
        }
    }

    public record IdeaSummary(UUID id, ResourceLocation topicId, ResourceLocation ideaId,
            IdeaRecord.State state, int sourceCount, int evidenceCount) {
        public static final Codec<IdeaSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.UUIDUtil.CODEC.fieldOf("id").forGetter(IdeaSummary::id),
                ResourceLocation.CODEC.fieldOf("topic").forGetter(IdeaSummary::topicId),
                ResourceLocation.CODEC.fieldOf("idea").forGetter(IdeaSummary::ideaId),
                IdeaRecord.STATE_CODEC.fieldOf("state").forGetter(IdeaSummary::state),
                Codec.INT.fieldOf("source_count").forGetter(IdeaSummary::sourceCount),
                Codec.INT.fieldOf("evidence_count").forGetter(IdeaSummary::evidenceCount)
        ).apply(instance, IdeaSummary::new));
    }

    public record ComparisonSummary(UUID id, ResourceLocation topicId,
            FieldComparisonArtifact.Outcome outcome, long createdAt) {
        public static final Codec<ComparisonSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.UUIDUtil.CODEC.fieldOf("id").forGetter(ComparisonSummary::id),
                ResourceLocation.CODEC.fieldOf("topic").forGetter(ComparisonSummary::topicId),
                FieldComparisonArtifact.OUTCOME_CODEC.fieldOf("outcome").forGetter(ComparisonSummary::outcome),
                Codec.LONG.fieldOf("created_at").forGetter(ComparisonSummary::createdAt)
        ).apply(instance, ComparisonSummary::new));
    }
}
