/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent;
import com.teammoeg.frostedresearch.knowledge.link.LinkMatcher;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

/**
 * 服务器统一知识入口。 / Server authority for learning, forgetting, linking and acquisition.
 */
public final class KnowledgeService {
    public enum Status {
        SUCCESS, LEARNABLE, ACTIVE, ALREADY_OWNED, ALREADY_RECEIVED, NO_NEW_DIFFERENCE, NO_USE,
        NOT_UNDERSTOOD, DEFINITION_UNAVAILABLE, CAPACITY, CANCELLED, NOT_FOUND, INVALID_INPUT, NO_MATCH, NO_HINT, DAILY_LIMIT
    }

    public enum Grant {NORMAL, INITIAL, COMMAND}

    public record OperationResult(Status status, KnowledgeKey key) {
        public boolean succeeded() {
            return status == Status.SUCCESS;
        }
    }

    public record Query(boolean inArchive, boolean inInbox, boolean active, Status status) {
    }

    private final TeamDataHolder team;
    private final KnowledgeState state;
    private final Optional<UUID> actor;
    private final long time;
    private final Runnable changed;
    private final int archiveLimit;
    private final int inboxLimit;
    private int batchDepth;
    private boolean syncPending;
    private long observationsRevision = -1;
    private List<Observation> observations = List.of();

    public KnowledgeService(UUID teamId, KnowledgeState state, long time, Runnable changed) {
        this(teamId, state, time, changed, 4096, 512);
    }

    public KnowledgeService(UUID teamId, KnowledgeState state, long time, Runnable changed, int archiveLimit, int inboxLimit) {
        this.team = null;
        this.testTeamId = teamId;
        this.state = state;
        this.actor = Optional.empty();
        this.time = time;
        this.changed = changed;
        this.archiveLimit = archiveLimit;
        this.inboxLimit = inboxLimit;
    }

    private UUID testTeamId;

    private KnowledgeService(TeamDataHolder team, Optional<UUID> actor, long time) {
        this.team = team;
        this.state = team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA);
        this.actor = actor;
        this.time = time;
        this.changed = () -> TeamResearchService.sync(team);
        this.archiveLimit = FRConfig.SERVER.knowledgeArchiveObservations.get();
        this.inboxLimit = FRConfig.SERVER.knowledgeInboxObservations.get();
    }

    public static KnowledgeService forPlayer(ServerPlayer player) {
        return new KnowledgeService(KnowledgeDataAPI.getData(player).team(), Optional.of(player.getUUID()), player.serverLevel().getGameTime());
    }

    public static KnowledgeService forTeam(TeamDataHolder team, long time) {
        return new KnowledgeService(team, Optional.empty(), time);
    }

    public KnowledgeState state() {
        return state;
    }

    public UUID teamId() {
        return team == null ? testTeamId : team.getId();
    }

    public CompoundTag snapshot() {
        return KnowledgeView.create(this);
    }

    public KnowledgeElement describe(KnowledgeKey key) {
        KnowledgeEntry entry = entry(key);
        return KnowledgeAvailability.describe(entry == null ? new KnowledgeElement(key, Optional.empty(), key.id(), "") : entry.element());
    }

    public KnowledgeEntry entry(KnowledgeKey key) {
        KnowledgeEntry entry = state.archive().entries().get(key);
        return entry == null ? state.inbox().entries().get(key) : entry;
    }

    public boolean isActive(KnowledgeKey key) {
        KnowledgeEntry entry = state.archive().entries().get(key);
        return entry != null && KnowledgeAvailability.available(entry.element());
    }

    public Query query(KnowledgeKey key) {
        KnowledgeEntry archive = state.archive().entries().get(key);
        if (archive != null) {
            boolean active = KnowledgeAvailability.available(archive.element());
            return new Query(true, state.inbox().contains(key), active, active ? Status.ACTIVE : unavailable(key));
        }
        KnowledgeEntry inbox = state.inbox().entries().get(key);
        return new Query(false, inbox != null, false, inbox == null ? Status.NOT_FOUND : learningStatus(inbox.element(), Grant.NORMAL));
    }

    private static Status unavailable(KnowledgeKey key) {
        return key.kind() == KnowledgeKey.Kind.OBSERVATION ? Status.NO_USE : Status.DEFINITION_UNAVAILABLE;
    }

    public Status learningStatus(KnowledgeElement element, Grant grant) {
        KnowledgeKey key = element.key();
        if (state.archive().contains(key)) return Status.ALREADY_OWNED;
        if (!KnowledgeAvailability.available(element)) return unavailable(key);
        if (grant == Grant.NORMAL && KnowledgeAvailability.understanding(key).stream().anyMatch(k -> !isActive(k)))
            return Status.NOT_UNDERSTOOD;
        if (key.kind() == KnowledgeKey.Kind.OBSERVATION) {
            if (state.archive().observationCount() >= archiveLimit) return Status.CAPACITY;
            if (observationsRevision != state.mutationRevision()) {
                observations = state.archive().entries().values().stream().flatMap(e -> e.element().observation().stream()).toList();
                observationsRevision = state.mutationRevision();
            }
            if (!KnowledgeDefinitions.current().addsObservationDifference(element.observation().orElseThrow(), observations))
                return Status.NO_NEW_DIFFERENCE;
        }
        return Status.LEARNABLE;
    }

    public OperationResult receive(KnowledgeElement element, AcquisitionSource source) {
        return receive(element, source, source);
    }

    public OperationResult receive(KnowledgeElement element, AcquisitionSource source, AcquisitionSource originalSource) {
        KnowledgeKey key = element.key();
        if (state.archive().contains(key)) return result(Status.ALREADY_OWNED, key);
        if (state.inbox().contains(key)) return result(Status.ALREADY_RECEIVED, key);
        Observation observation = element.observation().orElse(null);
        if (observation != null && observation.type() == Observation.Type.ITEM) {
            if (observation.object().equals(com.teammoeg.frostedresearch.FRMain.rl("research_note")))
                return result(Status.INVALID_INPUT, key);
            for (KnowledgeEntry existing : state.inbox().entries().values()) {
                if (existing.element().observation().filter(observation::sameItemContent).isPresent())
                    return result(Status.ALREADY_RECEIVED, existing.element().key());
            }
        }
        if (key.kind() == KnowledgeKey.Kind.OBSERVATION && state.inbox().observationCount() >= inboxLimit)
            return result(Status.CAPACITY, key);
        if (!check("receive", element, source)) return result(Status.CANCELLED, key);
        KnowledgeEntry entry = new KnowledgeEntry(KnowledgeAvailability.describe(element), source, -1, KnowledgeAvailability.resultType(key), originalSource);
        state.putInbox(entry);
        state.recordAcquisition(entry);
        committed("receive", entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    public OperationResult learnInbox(KnowledgeKey key) {
        KnowledgeEntry entry = state.inbox().entries().get(key);
        return entry == null ? result(state.archive().contains(key) ? Status.ALREADY_OWNED : Status.NOT_FOUND, key) : learn(entry.element(), entry.source(), Grant.NORMAL);
    }

    public List<OperationResult> learnInbox(Collection<KnowledgeKey> keys) {
        return batch(keys, this::learnInbox);
    }

    public List<OperationResult> deleteInbox(Collection<KnowledgeKey> keys) {
        return batch(keys, this::deleteInbox);
    }

    private List<OperationResult> batch(Collection<KnowledgeKey> keys, java.util.function.Function<KnowledgeKey, OperationResult> operation) {
        batchDepth++;
        try {
            return keys.stream().distinct().map(operation).toList();
        } finally {
            if (--batchDepth == 0 && syncPending) {
                syncPending = false;
                changed.run();
            }
        }
    }

    public OperationResult learn(KnowledgeElement element, AcquisitionSource source, Grant grant) {
        KnowledgeKey key = element.key();
        Status status = learningStatus(element, grant);
        if (status != Status.LEARNABLE) return result(status, key);
        String operation = grant == Grant.NORMAL ? "learn" : "learn_" + grant.name().toLowerCase(Locale.ROOT);
        if (!check(operation, element, source)) return result(Status.CANCELLED, key);
        status = learningStatus(element, grant);
        if (status != Status.LEARNABLE) return result(status, key);
        KnowledgeEntry pending = state.inbox().entries().get(key);
        KnowledgeEntry entry = pending == null ? new KnowledgeEntry(KnowledgeAvailability.describe(element), source, time, KnowledgeAvailability.resultType(key)) : pending.learned(time);
        state.putArchive(entry);
        state.removeInbox(key);
        if (pending == null) state.recordAcquisition(entry);
        committed(operation, entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    /**
     * Generated idea/results retain output in the inbox when normal learning cannot commit.
     */
    public OperationResult produce(KnowledgeElement element, AcquisitionSource source) {
        OperationResult learned = learn(element, source, Grant.NORMAL);
        if (learned.succeeded() || learned.status() == Status.ALREADY_OWNED) return learned;
        if (element.key().kind() == KnowledgeKey.Kind.OBSERVATION) {
            receive(element, source);
            return learned;
        }
        if (!state.inbox().contains(element.key())) {
            KnowledgeEntry retained = new KnowledgeEntry(KnowledgeAvailability.describe(element), source, -1, KnowledgeAvailability.resultType(element.key()));
            state.putInbox(retained);
            state.recordAcquisition(retained);
            committed("receive_produced", retained);
            notifyChanged();
        }
        return learned;
    }

    public OperationResult forget(KnowledgeKey key) {
        KnowledgeEntry entry = state.archive().entries().get(key);
        if (entry == null) return result(Status.NOT_FOUND, key);
        if (!check("forget", entry.element(), entry.source())) return result(Status.CANCELLED, key);
        if (!state.removeArchive(key)) return result(Status.NOT_FOUND, key);
        committed("forget", entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    public OperationResult deleteInbox(KnowledgeKey key) {
        KnowledgeEntry entry = state.inbox().entries().get(key);
        if (entry == null) return result(Status.NOT_FOUND, key);
        if (!check("delete_inbox", entry.element(), entry.source())) return result(Status.CANCELLED, key);
        if (!state.removeInbox(key)) return result(Status.NOT_FOUND, key);
        committed("delete_inbox", entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    public OperationResult canExport(KnowledgeKey key) {
        KnowledgeEntry entry = entry(key);
        if (entry == null) return result(Status.NOT_FOUND, key);
        if (entry.element().observation().map(o -> o.type() == Observation.Type.ITEM).orElse(false))
            return result(Status.INVALID_INPUT, key);
        return result(check("export", entry.element(), entry.source()) ? Status.SUCCESS : Status.CANCELLED, key);
    }

    public List<LinkMatcher.Match> previewLinks(List<KnowledgeKey> keys) {
        if (keys.size() < 2 || keys.size() > 5 || new HashSet<>(keys).size() != keys.size() || keys.stream().anyMatch(k -> !query(k).active()))
            return List.of();
        return LinkMatcher.match(keys.stream().map(k -> state.archive().entries().get(k).element()).toList(), KnowledgeDefinitions.current().links());
    }

    public List<LinkMatcher.Match> candidateLinks(int limit) {
        return LinkMatcher.candidates(state.archive().entries().values().stream().map(KnowledgeEntry::element).filter(e -> isActive(e.key())).toList(), KnowledgeDefinitions.current().links(), limit);
    }

    public OperationResult link(ResourceLocation ruleId, List<KnowledgeKey> keys) {
        var match = previewLinks(keys).stream().filter(m -> m.ruleId().equals(ruleId)).findFirst().orElse(null);
        if (match == null) return result(Status.NO_MATCH, null);
        KnowledgeKey output = KnowledgeKey.idea(match.rule().output());
        boolean discovered = state.discover(new LinkDiscovery(ruleId, output, match.bindings(), time));
        OperationResult produced = produce(KnowledgeElement.idea(match.rule().output()), new AcquisitionSource("link:" + ruleId, actor, time));
        if (discovered) notifyChanged();
        return produced;
    }

    public void initialize() {
        boolean reconciled = state.reconcileHints(KnowledgeDefinitions.current().links());
        if (state.initialized() || KnowledgeDefinitions.current().revision() == 0) {
            if (reconciled) notifyChanged();
            return;
        }
        state.initialize();
        for (KnowledgeKey key : KnowledgeDefinitions.current().initialKnowledge())
            learn(describe(key), new AcquisitionSource("initial", Optional.empty(), time), Grant.INITIAL);
        notifyChanged();
    }

    /**
     * Team system explicitly requests this migration; joining/leaving never calls it.
     */
    public void merge(KnowledgeState source) {
        state.merge(source);
        notifyChanged();
    }

    public Optional<UUID> startResearch(ResourceLocation project, KnowledgeKey idea) {
        if (idea.kind() != KnowledgeKey.Kind.IDEA || !query(idea).active()) return Optional.empty();
        var definition = KnowledgeDefinitions.current().ideas().get(new ResourceLocation(idea.id()));
        if (definition == null || !definition.projects().contains(project)) return Optional.empty();
        UUID id = UUID.randomUUID();
        state.recordResearch(new ResearchHistory(id, project, idea, time, false, List.of()));
        notifyChanged();
        return Optional.of(id);
    }

    public List<OperationResult> completeResearch(UUID run) {
        var record = state.research().get(run);
        if (record == null) return List.of();
        var definition = KnowledgeDefinitions.current().projects().get(record.project());
        return definition == null ? List.of() : completeResearch(run, definition.results());
    }

    public List<OperationResult> completeResearch(UUID run, List<ResourceLocation> results) {
        ResearchHistory history = state.research().get(run);
        if (history == null || history.completed() || results.isEmpty()) return List.of();
        var definition = KnowledgeDefinitions.current().projects().get(history.project());
        if (definition == null || !new HashSet<>(definition.results()).equals(new HashSet<>(results))) return List.of();
        batchDepth++;
        try {
            List<KnowledgeKey> keys = results.stream().distinct().map(KnowledgeKey::result).toList();
            state.recordResearch(new ResearchHistory(run, history.project(), history.idea(), history.time(), true, keys));
            List<OperationResult> outputs = results.stream().distinct().map(id -> produce(KnowledgeElement.result(id), new AcquisitionSource("research:" + run, actor, time))).toList();
            notifyChanged();
            return outputs;
        } finally {
            if (--batchDepth == 0 && syncPending) {
                syncPending = false;
                changed.run();
            }
        }
    }

    public OperationResult setDreamTopic(UUID player, KnowledgeKey key) {
        if (!query(key).active()) return result(Status.NOT_FOUND, key);
        state.dreamTopic(player, key);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    /**
     * One progressive hint; unknown outputs precede alternative paths to known ideas.
     */
    public OperationResult associate(KnowledgeKey topic, long day, String opportunity) {
        if (topic == null || !query(topic).active()) return result(Status.NOT_FOUND, topic);
        if (state.lastDay(opportunity) == day) return result(Status.DAILY_LIMIT, topic);
        KnowledgeElement element = state.archive().entries().get(topic).element();
        var candidates = KnowledgeDefinitions.current().links().entrySet().stream()
                .filter(e -> !e.getValue().hints().isEmpty())
                .filter(e -> e.getValue().inputs().stream().anyMatch(slot -> slot.matches(element, com.teammoeg.frostedresearch.knowledge.link.TagLookup.SERVER)))
                .sorted(Comparator.<Map.Entry<ResourceLocation, com.teammoeg.frostedresearch.knowledge.link.LinkRule>, Boolean>comparing(e -> query(KnowledgeKey.idea(e.getValue().output())).inArchive()).thenComparing(e -> e.getKey().toString())).toList();
        if (candidates.isEmpty()) return result(Status.NO_HINT, topic);
        var chosen = candidates.stream().filter(e -> e.getValue().hints().stream().anyMatch(h -> !state.hasHint(h)))
                .findFirst().orElse(null);
        if (chosen == null) {
            if (!opportunity.startsWith("dream:")) return result(Status.NO_HINT, topic);
            // A dream may revisit a known thought; it does not add another saved copy.
            state.markDay(opportunity, day);
            notifyChanged();
            return result(Status.SUCCESS, topic);
        }
        String hint = chosen.getValue().hints().stream().filter(h -> !state.hasHint(h)).findFirst().orElseThrow();
        state.addHint(chosen.getKey(), hint);
        state.markDay(opportunity, day);
        notifyChanged();
        return result(Status.SUCCESS, topic);
    }

    public OperationResult dream(UUID player, long day) {
        KnowledgeKey topic = state.dreamTopic(player);
        if (topic == null || !query(topic).active())
            topic = state.archive().entries().keySet().stream().filter(k -> query(k).active()).findFirst().orElse(null);
        return associate(topic, day, "dream:" + player);
    }

    public OperationResult discuss(UUID player, UUID partner, KnowledgeKey topic, long day) {
        return associate(topic, day, "discuss:" + player + ":" + partner);
    }

    private boolean check(String operation, KnowledgeElement element, AcquisitionSource source) {
        return !MinecraftForge.EVENT_BUS.post(new KnowledgeOperationEvent.Check(teamId(), actor, operation, element, source));
    }

    private void committed(String operation, KnowledgeEntry entry) {
        MinecraftForge.EVENT_BUS.post(new KnowledgeOperationEvent.Committed(teamId(), actor, operation, entry.element(), entry.source()));
    }

    private static OperationResult result(Status status, KnowledgeKey key) {
        return new OperationResult(status, key);
    }

    private void notifyChanged() {
        if (batchDepth > 0) syncPending = true;
        else changed.run();
    }
}
