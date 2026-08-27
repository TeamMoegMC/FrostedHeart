/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Executable workflow handlers shared by data-authored research topics. */
public final class ResearchWorkflowRegistry {
    private static final Map<ResourceLocation, IdeaSourceHandler> IDEA_HANDLERS = new LinkedHashMap<>();
    private static final Set<ResourceLocation> INSPIRATION_HANDLERS = new LinkedHashSet<>();
    private static final Map<ResourceLocation, ProtocolHandler> PROTOCOL_HANDLERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResolutionHandler> RESOLUTION_HANDLERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, FindingViewHandler> FINDING_HANDLERS = new LinkedHashMap<>();

    public static final Set<String> COMPARISON_OUTCOMES = Set.of("match", "no_match", "insufficient");

    static {
        registerCoreHandlers();
    }

    private ResearchWorkflowRegistry() {
    }

    public static void registerIdeaSource(ResourceLocation id, IdeaSourceHandler handler) {
        IDEA_HANDLERS.put(id, handler);
    }

    public static void registerInspirationProvider(ResourceLocation id) {
        INSPIRATION_HANDLERS.add(id);
    }

    public static void registerProtocol(ResourceLocation id, ProtocolHandler handler) {
        PROTOCOL_HANDLERS.put(id, handler);
    }

    public static void registerResolution(ResourceLocation id, ResolutionHandler handler) {
        RESOLUTION_HANDLERS.put(id, handler);
    }

    public static void registerFindingView(FindingViewHandler handler) {
        FINDING_HANDLERS.put(handler.id(), handler);
    }

    public static boolean hasIdeaSource(ResourceLocation id) {
        return IDEA_HANDLERS.containsKey(id);
    }

    public static boolean hasInspirationProvider(ResourceLocation id) {
        return INSPIRATION_HANDLERS.contains(id);
    }

    public static boolean hasProtocol(ResourceLocation id) {
        return PROTOCOL_HANDLERS.containsKey(id);
    }

    public static boolean hasResolution(ResourceLocation id) {
        return RESOLUTION_HANDLERS.containsKey(id);
    }

    public static boolean hasFindingView(ResourceLocation id) {
        return FINDING_HANDLERS.containsKey(id);
    }

    public static FindingViewHandler findingView(ResourceLocation id) {
        return FINDING_HANDLERS.get(id);
    }

    public static ProtocolHandler protocol(ResourceLocation id) {
        return PROTOCOL_HANDLERS.get(id);
    }

    public static ResolutionHandler resolution(ResourceLocation id) {
        return RESOLUTION_HANDLERS.get(id);
    }

    /** Projects acquired Finding views onto one observation without exposing sealed facts. */
    public static List<ResourceLocation> observationAnnotations(
            TeamKnowledgeData data, KnowledgeRecord record) {
        Set<ResourceLocation> annotations = new LinkedHashSet<>();
        for (ResourceLocation findingId : data.findingIds()) {
            ResearchResultCatalog.ResultEntry entry = ResearchResultCatalog.current().result(findingId);
            if (entry == null || !(entry.result() instanceof ResearchResult.Finding finding)) continue;
            for (ResourceLocation viewId : finding.views()) {
                FindingViewHandler handler = findingView(viewId);
                if (handler != null) annotations.addAll(handler.observationAnnotations(data, record));
            }
        }
        return List.copyOf(annotations);
    }

    /** Matches every loaded topic without revealing unmatched topic metadata to the caller. */
    public static List<IdeaCandidate> findCandidates(TeamKnowledgeData data, Set<UUID> pinnedEvidence) {
        Map<String, IdeaCandidate> candidates = new LinkedHashMap<>();
        ResearchResultCatalog.current().topics().forEach((topicId, topic) -> {
            if (topic.inspiration().isEmpty()
                    || !hasInspirationProvider(topic.inspiration().get().provider())) return;
            for (ResearchTopicDefinition.IdeaSource source : topic.ideaSources()) {
                IdeaSourceHandler handler = IDEA_HANDLERS.get(source.provider());
                if (handler == null) continue;
                handler.match(topicId, topic, source, data, pinnedEvidence).ifPresent(candidate ->
                        candidates.putIfAbsent(candidate.semanticKey(), candidate));
                if (candidates.size() >= 3) break;
            }
        });
        return List.copyOf(candidates.values()).stream().limit(3).toList();
    }

    /** Compiles post-Idea actions only. Before an Idea exists this method necessarily returns empty. */
    public static List<ActionCard> actionCards(TeamKnowledgeData data) {
        List<ActionCard> actions = new ArrayList<>();
        for (IdeaRecord idea : data.ideas()) {
            if (idea.state() == IdeaRecord.State.RESOLVED || idea.state() == IdeaRecord.State.ORPHAN) continue;
            ResearchTopicDefinition topic = ResearchResultCatalog.current().topics().get(idea.topicId());
            if (topic == null) continue;
            for (ResearchTopicDefinition.Protocol protocol : topic.protocols()) {
                ProtocolHandler handler = PROTOCOL_HANDLERS.get(protocol.resolver());
                if (handler != null) actions.addAll(handler.actions(idea.topicId(), topic, protocol, data, idea));
                if (actions.size() >= 3) return actions.stream().distinct().limit(3).toList();
            }
        }
        return actions.stream().distinct().limit(3).toList();
    }

    private static void registerCoreHandlers() {
        ResourceLocation recordPair = id("record_pair");
        ResourceLocation compareRecords = id("compare_records");
        ResourceLocation comparisonResolution = id("comparison_resolution");
        registerInspirationProvider(id("drawing_desk"));
        registerInspirationProvider(new ResourceLocation("frostedheart", "drawing_desk"));
        registerIdeaSource(recordPair, (topicId, topic, source, data, pinned) -> {
            List<KnowledgeRecord> records = data.observations().stream()
                    .filter(record -> pinned.contains(record.id())).limit(2).toList();
            if (records.size() < 2) return Optional.empty();
            Set<UUID> evidence = new LinkedHashSet<>();
            records.forEach(record -> evidence.add(record.id()));
            return Optional.of(new IdeaCandidate(topicId, source.idea(), evidence, "drawing_desk"));
        });
        registerProtocol(compareRecords, new ProtocolHandler() {
            private final ResourceLocation add = id("add_observation");
            private final ResourceLocation compare = id("compare_records");
            private final ResourceLocation review = id("review_result");

            @Override
            public List<ActionCard> actions(ResourceLocation topicId, ResearchTopicDefinition topic,
                    ResearchTopicDefinition.Protocol protocol, TeamKnowledgeData data, IdeaRecord idea) {
                if (idea.state() == IdeaRecord.State.READY) {
                    return List.of(new ActionCard(topicId, protocol.id(), review));
                }
                return idea.evidence().size() < 2
                        ? List.of(new ActionCard(topicId, protocol.id(), add))
                        : List.of(new ActionCard(topicId, protocol.id(), compare, true));
            }

            @Override
            public Optional<Execution> execute(net.minecraft.server.level.ServerPlayer player,
                    ResourceLocation topicId, ResearchTopicDefinition topic,
                    ResearchTopicDefinition.Protocol protocol, TeamKnowledgeData data, IdeaRecord idea) {
                List<KnowledgeRecord> records = idea.evidence().stream()
                        .map(data::observation).flatMap(Optional::stream).limit(2).toList();
                if (records.size() < 2) return Optional.empty();
                FieldComparisonArtifact.Outcome outcome = records.get(0).subject().equals(records.get(1).subject())
                        ? FieldComparisonArtifact.Outcome.MATCH : FieldComparisonArtifact.Outcome.NO_MATCH;
                String key = idea.id() + "|" + records.get(0).id() + "|" + records.get(1).id();
                UUID artifactId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
                FieldComparisonArtifact artifact = new FieldComparisonArtifact(artifactId, topicId, idea.id(),
                        records.get(0).id(), records.get(1).id(), outcome,
                        player.serverLevel().getGameTime());
                return Optional.of(new Execution(artifact, outcome == FieldComparisonArtifact.Outcome.MATCH));
            }
        });
        registerResolution(comparisonResolution, (topicId, topic, resolution, data, idea) ->
                idea.state() == IdeaRecord.State.READY && data.comparisons().stream().anyMatch(report ->
                        report.topicId().equals(topicId)
                                && report.ideaId().equals(idea.id())
                                && report.outcome() == FieldComparisonArtifact.Outcome.MATCH));
        registerFindingView(new FindingViewHandler() {
            private final ResourceLocation id = ResearchWorkflowRegistry.id("archive");
            @Override public ResourceLocation id() { return id; }
            @Override public List<ResourceLocation> observationAnnotations(
                    TeamKnowledgeData data, KnowledgeRecord record) { return List.of(); }
        });
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("frostedresearch", path);
    }
}
