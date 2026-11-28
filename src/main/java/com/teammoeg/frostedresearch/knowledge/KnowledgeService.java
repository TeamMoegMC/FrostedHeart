/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.gui.drawdesk.game.GenerateInfo;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeCondition;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent;
import com.teammoeg.frostedresearch.knowledge.labor.LaborReport;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchLaborPool;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchSupplies;
import com.teammoeg.frostedresearch.knowledge.link.LinkMatcher;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.link.TagLookup;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.stream.Stream;

/**
 * 服务器统一知识入口。 / Server authority for learning, forgetting, linking and acquisition.
 */
public final class KnowledgeService {
    public enum Status {
        SUCCESS, LEARNABLE, ACTIVE, ALREADY_OWNED, ALREADY_RECEIVED, NO_NEW_DIFFERENCE, NO_USE,
        NOT_UNDERSTOOD, DEFINITION_UNAVAILABLE, CAPACITY, CANCELLED, NOT_FOUND, INVALID_INPUT, NO_MATCH, NO_HINT, DAILY_LIMIT,
        REQUIREMENTS_UNMET, LEGACY_COMPLETED
    }

    /**
     * Candidate tries allowed when evidence is matched against a team's whole archive and inbox.
     */
    public static final int EVIDENCE_SEARCH_STEPS = 20_000;
    /**
     * Candidate tries allowed per link rule when institute researchers look for a new idea in the archive.
     */
    public static final int CANDIDATE_SEARCH_STEPS = 20_000;
    /** Link rules institute researchers consider on one day. */
    public static final int CANDIDATE_LIMIT = 64;
    /** Topics a day's association tries before giving up on a hint. */
    public static final int ASSOCIATION_TOPIC_TRIES = 8;
    /** Tolerance for labor arithmetic, in worker-days. */
    public static final double LABOR_EPSILON = 1e-6;

    public enum Grant {NORMAL, INITIAL, COMMAND, CREATIVE}

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
    private final int archiveBase;
    private final int inboxBase;
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
        this.archiveBase = archiveLimit;
        this.inboxBase = inboxLimit;
    }

    private UUID testTeamId;

    private KnowledgeService(TeamDataHolder team, Optional<UUID> actor, long time) {
        this.team = team;
        this.state = team.getData(FRSpecialDataTypes.KNOWLEDGE_DATA);
        this.actor = actor;
        this.time = time;
        this.changed = () -> TeamResearchService.sync(team);
        // Other systems, such as town research institutes, may grant capacity beyond the configured base.
        KnowledgeCapacity.Limits limits = KnowledgeCapacity.limits(team);
        this.archiveLimit = limits.archive();
        this.inboxLimit = limits.inbox();
        this.archiveBase = limits.archiveBase();
        this.inboxBase = limits.inboxBase();
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

    public Optional<UUID> actor() { return actor; }

    public long time() { return time; }

    /** Archived observation records the team may hold, including granted capacity. */
    public int archiveLimit() {
        return archiveLimit;
    }

    /** Inbox observation records the team may hold, including granted capacity. */
    public int inboxLimit() {
        return inboxLimit;
    }

    /** Configured archive capacity before any granted capacity. */
    public int archiveBase() {
        return archiveBase;
    }

    /** Configured inbox capacity before any granted capacity. */
    public int inboxBase() {
        return inboxBase;
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

    public boolean canUseToolAction(String action) {
        boolean managed = false;
        for (var definition : KnowledgeDefinitions.current().results().values()) {
            if (!definition.enabled() || !(definition.result() instanceof ResearchResult.Procedure procedure)
                    || !procedure.toolActions().contains(action)) continue;
            if (isActive(KnowledgeKey.result(procedure.id()))) return true;
            managed = true;
        }
        return !managed;
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
                Observation previous = existing.element().observation().orElse(null);
                if (previous != null && observation.sameItemContent(previous)
                        && KnowledgeDefinitions.current().equivalentForResearch(observation, previous))
                    return result(Status.ALREADY_RECEIVED, existing.element().key());
            }
        }
        if (key.kind() == KnowledgeKey.Kind.OBSERVATION && state.inbox().observationCount() >= inboxLimit)
            return result(Status.CAPACITY, key);
        if (!check("receive", element, source)) return result(Status.CANCELLED, key);
        KnowledgeEntry entry = new KnowledgeEntry(KnowledgeAvailability.describe(element), source, -1, KnowledgeAvailability.resultType(key), originalSource);
        state.putInbox(entry);
        state.recordAcquisition(entry);
        if (observation != null) refreshRecordedObservations();
        committed("receive", entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    public OperationResult learnInbox(KnowledgeKey key) {
        KnowledgeEntry entry = state.inbox().entries().get(key);
        return entry == null ? result(state.archive().contains(key) ? Status.ALREADY_OWNED : Status.NOT_FOUND, key) : learn(entry.element(), entry.source(), Grant.NORMAL);
    }

    public List<OperationResult> learnInbox(Collection<KnowledgeKey> keys) {
        return keys.stream().distinct().map(this::learnInbox).toList();
    }

    public List<OperationResult> deleteInbox(Collection<KnowledgeKey> keys) {
        return keys.stream().distinct().map(this::deleteInbox).toList();
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
        if (pending == null && key.kind() == KnowledgeKey.Kind.OBSERVATION) refreshRecordedObservations();
        committed(operation, entry);
        notifyChanged();
        return result(Status.SUCCESS, key);
    }

    /**
     * Generated idea/results retain output in the inbox when normal learning cannot commit.
     */
    public OperationResult produce(KnowledgeElement element, AcquisitionSource source) {
        return produce(element, source, Grant.NORMAL);
    }

    private OperationResult produce(KnowledgeElement element, AcquisitionSource source, Grant grant) {
        OperationResult learned = learn(element, source, grant);
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
        if (keys.size() > 5 || new HashSet<>(keys).size() != keys.size() || keys.stream().anyMatch(k -> !query(k).active()))
            return List.of();
        var selected = keys.stream().map(k -> state.archive().entries().get(k).element()).toList();
        return KnowledgeDefinitions.current().links().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .flatMap(entry -> bindLink(entry.getKey(), entry.getValue(), selected).stream())
                .filter(this::linkCandidate).toList();
    }

    public List<LinkMatcher.Match> candidateLinks(int limit) {
        return candidateLinks(KnowledgeDefinitions.current().links(), limit);
    }

    /**
     * Combinations of active archive knowledge that satisfy these rules, one per rule. Each rule's search is bounded
     * by {@link #CANDIDATE_SEARCH_STEPS}; a rule whose budget runs out yields nothing.
     */
    public List<LinkMatcher.Match> candidateLinks(Map<ResourceLocation, LinkRule> rules, int limit) {
        if (limit <= 0) return List.of();
        return LinkMatcher.candidates(state.archive().entries().values().stream().map(KnowledgeEntry::element).filter(e -> isActive(e.key())).toList(),
                rules, rules.size(), TagLookup.SERVER, CANDIDATE_SEARCH_STEPS).stream()
                .filter(this::linkCandidate).limit(limit).toList();
    }

    private boolean linkCandidate(LinkMatcher.Match match) {
        KnowledgeKey output = KnowledgeKey.idea(match.rule().output());
        return !state.archive().contains(output) && !state.inbox().contains(output) && linkAllowed(match);
    }

    private boolean linkAllowed(LinkMatcher.Match match) {
        var idea = KnowledgeDefinitions.current().ideas().get(match.rule().output());
        return idea != null && idea.enabled() && match.rule().requiredKnowledge().orElse(idea.acquisition().required())
            .stream().allMatch(c -> matchKnowledge(c).isPresent());
    }

    public List<KnowledgeCondition> linkOptionalKnowledge(LinkRule rule) {
        var idea = KnowledgeDefinitions.current().ideas().get(rule.output());
        return rule.optionalKnowledge().orElseGet(() -> idea == null ? List.of() : idea.acquisition().optional());
    }

    /** Check one selected rule without enumerating candidates, used by transient link puzzles. */
    public Optional<LinkMatcher.Match> matchLink(ResourceLocation ruleId, List<KnowledgeKey> keys) {
        var rule = KnowledgeDefinitions.current().links().get(ruleId);
        if (rule == null || keys.size() > 5 || new HashSet<>(keys).size() != keys.size() || keys.stream().anyMatch(k -> !isActive(k))) return Optional.empty();
        return bindLink(ruleId, rule, keys.stream().map(k -> state.archive().entries().get(k).element()).toList())
            .filter(this::linkAllowed);
    }

    private Optional<LinkMatcher.Match> bindLink(ResourceLocation ruleId, LinkRule rule, List<KnowledgeElement> selected) {
        return LinkMatcher.bindWithOptional(selected, rule.pattern(), linkOptionalKnowledge(rule), TagLookup.SERVER)
            .map(binding -> {
                Map<String, KnowledgeKey> witnesses = new LinkedHashMap<>();
                binding.forEach((name, element) -> witnesses.put(name, element.key()));
                return new LinkMatcher.Match(ruleId, rule, witnesses);
            });
    }

    public OperationResult link(ResourceLocation ruleId, List<KnowledgeKey> keys) {
        return link(ruleId, keys, "link:" + ruleId);
    }

    /**
     * Links with an explicit acquisition mechanism, such as {@code institute:link:<rule>} for town researchers.
     */
    public OperationResult link(ResourceLocation ruleId, List<KnowledgeKey> keys, String mechanism) {
        var match = matchLink(ruleId, keys).orElse(null);
        if (match == null) return result(Status.NO_MATCH, null);
        KnowledgeKey output = KnowledgeKey.idea(match.rule().output());
        boolean discovered = state.discover(new LinkDiscovery(ruleId, output, match.bindings(), time));
        // A valid manual deduction can add a new path to an already known or received idea.
        // Keep that history independent of acquisition; do not relearn/regrant an existing entity.
        Query owned = query(output);
        OperationResult produced = owned.inArchive() ? result(Status.ALREADY_OWNED, output)
            : owned.inInbox() ? result(Status.ALREADY_RECEIVED, output)
            : produce(KnowledgeElement.idea(match.rule().output()), new AcquisitionSource(mechanism, actor, time));
        if (discovered) notifyChanged();
        return produced;
    }

    public void initialize() {
        for (var run : ResearchRuns.sorted(state)) if (runActive(run)) {
            var definition=KnowledgeDefinitions.current().projects().get(run.project());
            var saved = definition == null ? run : com.teammoeg.frostedresearch.knowledge.experiment.TrialKilnSamples.adoptDrawingStage(run, definition);
            var prepared=ResearchStages.prepare(this,saved);
            if (definition != null) prepared=com.teammoeg.frostedresearch.knowledge.experiment.TrialKilnSamples.reconcile(prepared,definition);
            prepared=ResearchStages.update(this,prepared);
            if(!prepared.equals(run)) {
                if(run.stageState().visits().isEmpty() && definition!=null && !definition.stages().isEmpty()
                    && (!run.calculations().isEmpty() || !run.evidence().isEmpty() || !run.experiments().isEmpty() || !run.samples().isEmpty()))
                    com.teammoeg.frostedresearch.FRMain.LOGGER.info("Research {} adopts its first ordered stage; only matching task IDs retain progress",run.id());
                state.recordResearch(prepared); notifyChanged();
            }
        }
        if (team != null) LegacyResearchBridge.reconcile(team, this);
        clearSupersededProofs();
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
    private void clearSupersededProofs() {
        state.research().values().stream().filter(run -> run.open() && legacyCompleted(run.project()))
            .forEach(run -> com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.removeRun(run.id()));
    }
    public boolean legacyCompleted(ResourceLocation project) {
        return team != null && LegacyResearchBridge.completedCredit(team.getData(FRSpecialDataTypes.RESEARCH_DATA), project);
    }
    public net.minecraft.nbt.ListTag legacyProjection() {
        return team == null ? new net.minecraft.nbt.ListTag() : LegacyResearchBridge.projection(team);
    }
    public void merge(TeamDataHolder source) {
        if (team != null) LegacyResearchBridge.mergeReceipts(team.getData(FRSpecialDataTypes.RESEARCH_DATA), source.getData(FRSpecialDataTypes.RESEARCH_DATA));
        merge(source.getData(FRSpecialDataTypes.KNOWLEDGE_DATA));
    }
    public void merge(KnowledgeState source) {
        state.merge(source);
        refreshRecordedObservations();
        clearSupersededProofs();
        notifyChanged();
    }

    /**
     * Whether the team can begin this project from its current knowledge: {@code LEARNABLE}, or the reason it cannot. A team
     * starts at most one run through the normal action; an explicit merge may retain several. A real completed
     * project takes priority while all current results are valid learned knowledge; other open history is standby.
     */
    public Optional<KnowledgeKey> matchKnowledge(com.teammoeg.frostedresearch.knowledge.definition.KnowledgeCondition condition) {
        if (condition.kind() != KnowledgeKey.Kind.OBSERVATION) {
            return condition.definition().map(id -> new KnowledgeKey(condition.kind(), id.toString())).filter(this::isActive);
        }
        return state.archive().entries().values().stream().map(KnowledgeEntry::element)
            .filter(e -> e.key().kind() == KnowledgeKey.Kind.OBSERVATION && condition.matches(e) && isActive(e.key())).map(KnowledgeElement::key)
            .sorted(Comparator.comparing(KnowledgeKey::toString)).findFirst();
    }

    public record ProjectAccessEvaluation(boolean discovered, Map<String, KnowledgeKey> witnesses,
            Set<String> revealedGroups, Set<String> revealedConditions, List<KnowledgeKey> sources,
            List<KnowledgeKey> origins, Status startStatus) {}

    public ProjectAccessEvaluation projectAccess(ResourceLocation project) {
        var definition = KnowledgeDefinitions.current().projects().get(project);
        Map<String, KnowledgeKey> witnesses = new LinkedHashMap<>();
        Set<String> groups = new HashSet<>(), revealed = new HashSet<>();
        List<KnowledgeKey> sources = new ArrayList<>(), origins = new ArrayList<>();
        boolean discovered = false, met = false;
        if (definition != null && definition.access().isPresent()) {
            var access = definition.access().get();
            access.conditions().forEach((name, condition) -> matchKnowledge(condition).ifPresent(key -> witnesses.put(name, key)));
            for (String name : access.discovery()) if (witnesses.containsKey(name)) sources.add(witnesses.get(name));
            discovered = !sources.isEmpty();
            met = access.startAll().stream().allMatch(witnesses::containsKey);
            for (var group : access.groups()) if (group.revealAny().stream().anyMatch(witnesses::containsKey)) {
                groups.add(group.id()); revealed.addAll(group.conditions());
            }
            sources.removeIf(key -> revealed.stream().noneMatch(n -> key.equals(witnesses.get(n))));
            origins.addAll(access.discovery().stream().map(witnesses::get).filter(Objects::nonNull).toList());
            origins.addAll(access.startAll().stream().map(witnesses::get).filter(Objects::nonNull).toList());
        } else if (definition != null) {
            KnowledgeDefinitions.current().ideas().entrySet().stream().filter(e -> e.getValue().projects().contains(project))
                .map(e -> KnowledgeKey.idea(e.getKey())).filter(this::isActive).sorted(Comparator.comparing(KnowledgeKey::id)).forEach(sources::add);
            discovered = !sources.isEmpty(); met = discovered;
            if (discovered) origins.add(sources.get(0));
        }
        Status status = definition == null ? Status.DEFINITION_UNAVAILABLE : legacyCompleted(project) ? Status.LEGACY_COMPLETED : ResearchRuns.completed(state,project) ? Status.ALREADY_OWNED
            : openRun(project).isPresent() ? Status.ACTIVE
            : !discovered ? Status.NOT_FOUND : !met ? Status.NOT_UNDERSTOOD : Status.LEARNABLE;
        return new ProjectAccessEvaluation(discovered || legacyCompleted(project), Map.copyOf(witnesses), Set.copyOf(groups), Set.copyOf(revealed),
            sources.stream().distinct().toList(), origins.stream().distinct().sorted(Comparator.comparing(KnowledgeKey::toString)).toList(), status);
    }

    public Status researchStartStatus(ResourceLocation project) { return projectAccess(project).startStatus(); }

    public Status researchStartStatus(ResourceLocation project, KnowledgeKey idea) {
        var definition = KnowledgeDefinitions.current().projects().get(project);
        if (definition != null && definition.access().isEmpty() && idea != null) {
            if (idea.kind() != KnowledgeKey.Kind.IDEA || !isActive(idea)) return Status.NOT_UNDERSTOOD;
            var origin = KnowledgeDefinitions.current().ideas().get(idea.definitionId());
            if (origin == null || !origin.projects().contains(project)) return Status.INVALID_INPUT;
        }
        return researchStartStatus(project);
    }

    public Optional<UUID> startResearch(ResourceLocation project) { return startResearch(project, null); }

    public Optional<UUID> startResearch(ResourceLocation project, KnowledgeKey idea) {
        if (researchStartStatus(project, idea) != Status.LEARNABLE) return Optional.empty();
        var evaluation = projectAccess(project);
        var definition = KnowledgeDefinitions.current().projects().get(project);
        List<KnowledgeKey> origins = definition.access().isEmpty() && idea != null ? List.of(idea) : evaluation.origins();
        return Optional.of(createRun(project, origins));
    }

    /** Resolves a creative catalogue selection against current team progress. */
    public Optional<UUID> creativeResearchRun(ResourceLocation project) {
        var access = projectAccess(project);
        return switch (access.startStatus()) {
            case ACTIVE -> openRun(project).map(ResearchHistory::id);
            case NOT_FOUND, NOT_UNDERSTOOD, LEARNABLE -> Optional.of(createRun(project, access.origins()));
            default -> Optional.empty();
        };
    }

    private UUID createRun(ResourceLocation project, List<KnowledgeKey> origins) {
        UUID id = UUID.randomUUID();
        state.recordResearch(ResearchStages.update(this, new ResearchHistory(id, project, origins, time, false, List.of())));
        notifyChanged();
        return id;
    }

    public Optional<ResearchHistory> openRun(ResourceLocation project) {
        return openRuns(project).stream().findFirst();
    }

    public List<ResearchHistory> openRuns(ResourceLocation project) {
        return ResearchRuns.project(state,project).stream().filter(this::runActive).toList();
    }
    public boolean standby(UUID run) { return ResearchRuns.standby(state,state.research().get(run)); }
    public boolean runActive(ResearchHistory run) {
        return run!=null && run.open() && !legacyCompleted(run.project()) && !ResearchRuns.standby(state,run);
    }
    /** Human-facing device target; ordinal derives from the same history order as the journal. */
    public net.minecraft.network.chat.Component targetLabel(com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding binding) {
        var run=state.research().get(binding.run());
        if(run==null) return net.minecraft.network.chat.Component.empty();
        var definition=KnowledgeDefinitions.current().projects().get(run.project());
        if(definition==null) return net.minecraft.network.chat.Component.empty();
        var title=net.minecraft.network.chat.Component.translatableWithFallback(definition.title(),definition.title());
        if(ResearchRuns.project(state,run.project()).size()>1)
            title.append(" · ").append(net.minecraft.network.chat.Component.translatable("gui.frostedresearch.journal.research.run_number",ResearchRuns.number(state,run)));
        var prepared=ResearchStages.prepare(this,run);
        ResearchStages.current(prepared).filter(stage -> !stage.title().isBlank()).ifPresent(stage ->
            title.append(" · ").append(net.minecraft.network.chat.Component.translatableWithFallback(stage.title(),stage.title())));
        definition.requirement(binding.requirement()).filter(r -> !r.title().isBlank()).ifPresent(r ->
            title.append(" · ").append(net.minecraft.network.chat.Component.translatableWithFallback(r.title(),r.title())));
        return title;
    }

    /**
     * Requirements of the run's project under the current definitions.
     */
    public List<ProjectRequirement> requirements(ResearchHistory run) {
        return ResearchStages.tasks(ResearchStages.prepare(this,run)).stream().map(com.teammoeg.frostedresearch.knowledge.definition.StageDefinition.Task::requirement).toList();
    }

    /**
     * Stored progress counts only while it satisfies the current definition, so changed requirements may need new work.
     * A calculation or experiment is met once the player's games or experiments, together with any labor shares
     * standing in for them, reach its count, and any required labor is finished.
     */
    public boolean requirementMet(ResearchHistory run, ProjectRequirement requirement) {
        return switch (requirement.type()) {
            case MINIGAME -> run.stageState().completedTasks().contains(requirement.id());
            case OBSERVATION -> run.stageState().completedTasks().contains(requirement.id())
                || requirement.observation().filter(ProjectRequirement.ObservationTask::acceptRecorded)
                    .filter(task -> Stream.concat(state.archive().entries().values().stream(),
                            state.inbox().entries().values().stream())
                        .anyMatch(entry -> task.condition().matches(entry.element()))).isPresent();
            case SAMPLE -> requirement.sample().filter(value -> value.equals(run.samples().get(requirement.id()))).isPresent();
            case CALCULATION, EXPERIMENT -> playerRemaining(run, requirement) == 0
                    && (!requirement.laborRequired() || laborRemaining(run, requirement) <= LABOR_EPSILON);
            case EVIDENCE -> {
                var binding = run.evidence().get(requirement.id());
                yield binding != null && LinkMatcher.bindExact(binding.values().stream().map(KnowledgeElement::observation).toList(),
                        requirement.pattern(), TagLookup.SERVER).isPresent();
            }
        };
    }

    private void refreshRecordedObservations() {
        for (var run : ResearchRuns.sorted(state)) {
            if (!runActive(run) || requirements(run).stream().noneMatch(requirement ->
                    !run.stageState().completedTasks().contains(requirement.id())
                        && requirement.observation().filter(ProjectRequirement.ObservationTask::acceptRecorded).isPresent()))
                continue;
            var updated = ResearchStages.update(this, run);
            if (!updated.equals(run)) state.recordResearch(updated);
        }
    }

    /**
     * Won games at or above the requirement's current level.
     */
    public List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> sampleTargets(ResourceLocation experiment) {
        List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> targets = new ArrayList<>();
        ResearchRuns.sorted(state).stream().filter(this::runActive).forEach(run -> {
            for (var requirement : requirements(run)) if (requirement.type() == ProjectRequirement.Type.SAMPLE && !requirementMet(run, requirement)
                    && requirement.sample().filter(sample -> sample.experiment().equals(experiment)).isPresent())
                targets.add(new com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding(teamId(), run.id(), requirement.id(), experiment));
        });
        return List.copyOf(targets);
    }

    /** Consumes exactly one real bound sample, only after every current task/run/team check has passed. */
    public OperationResult submitSample(UUID runId, String requirementId, net.minecraft.world.item.ItemStack deskStack,
                                        Iterable<net.minecraft.world.item.ItemStack> inventory) {
        OperationResult result = submitSample(runId, requirementId, deskStack);
        if (result.status() != Status.INVALID_INPUT) return result;
        for (var stack : inventory) {
            if (stack.isEmpty()) continue;
            result = submitSample(runId, requirementId, stack);
            if (result.status() != Status.INVALID_INPUT) return result;
        }
        return result;
    }

    /** Direct-stack API retained for devices and existing callers. */
    public OperationResult submitSample(UUID runId, String requirementId, net.minecraft.world.item.ItemStack stack) {
        var run = state.research().get(runId);
        var requirement = openRequirement(run, requirementId, ProjectRequirement.Type.SAMPLE);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (requirementMet(run, requirement)) return result(Status.ALREADY_OWNED, null);
        var sample = requirement.sample().orElse(null);
        var binding = com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding.read(stack).orElse(null);
        if (sample == null || stack.isEmpty() || !net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(sample.item()) || binding == null
                || !binding.team().equals(teamId()) || !binding.run().equals(runId) || !binding.requirement().equals(requirementId)
                || !binding.experiment().equals(sample.experiment())) return result(Status.INVALID_INPUT, null);
        stack.shrink(1);
        state.recordResearch(ResearchStages.update(this, run.withSample(requirementId, sample)));
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    public static int calculationsDone(ResearchHistory run, ProjectRequirement requirement) {
        return (int) run.calculations().getOrDefault(requirement.id(), List.of()).stream()
                .filter(level -> GenerateInfo.atLeast(level, requirement.level())).count();
    }

    public static int experimentsDone(ResearchHistory run, ProjectRequirement requirement) {
        return run.experiments().getOrDefault(requirement.id(), 0);
    }

    /** Worker-days town researchers have put into this requirement. */
    public static double laborDone(ResearchHistory run, ProjectRequirement requirement) {
        return requirement.labor().isEmpty() ? 0 : run.labor().getOrDefault(requirement.id(), 0.0);
    }

    /**
     * Finished shares of optional labor, each standing in for one of the player's games or experiments. Required labor
     * stands in for nothing.
     */
    public static int laborShares(ResearchHistory run, ProjectRequirement requirement) {
        var labor = requirement.labor().filter(value -> !value.required()).orElse(null);
        if (labor == null) return 0;
        return (int) Math.floor((laborDone(run, requirement) + LABOR_EPSILON) / labor.share());
    }

    /** Games or experiments the player has finished for this requirement. */
    public static int playerDone(ResearchHistory run, ProjectRequirement requirement) {
        return switch (requirement.type()) {
            case CALCULATION -> calculationsDone(run, requirement);
            case EXPERIMENT -> experimentsDone(run, requirement);
            case EVIDENCE, SAMPLE, MINIGAME, OBSERVATION -> 0;
        };
    }

    /**
     * Games or experiments still to be done at a drawing desk or experiment table, after optional labor shares.
     */
    public static int playerRemaining(ResearchHistory run, ProjectRequirement requirement) {
        if (requirement.type() == ProjectRequirement.Type.EVIDENCE) return 0;
        return Math.max(0, requirement.count() - playerDone(run, requirement) - laborShares(run, requirement));
    }

    /**
     * Worker-days of labor that still advance this requirement: what required labor lacks, or what optional labor
     * needs to cover the games or experiments the player has not done.
     */
    public static double laborRemaining(ResearchHistory run, ProjectRequirement requirement) {
        var labor = requirement.labor().orElse(null);
        if (labor == null) return 0;
        double done = laborDone(run, requirement);
        if (labor.required()) return Math.max(0, labor.share() - done);
        int open = Math.max(0, requirement.count() - playerDone(run, requirement));
        return Math.max(0, open * labor.share() - done);
    }

    /**
     * The experiment requirement while it still needs runs, for an experiment table to work toward.
     */
    public Optional<ProjectRequirement> experimentRequirement(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EXPERIMENT);
        return requirement == null || playerRemaining(run, requirement) == 0 ? Optional.empty() : Optional.of(requirement);
    }

    public OperationResult recordExperiment(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EXPERIMENT);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (playerRemaining(run, requirement) == 0) return result(Status.ALREADY_OWNED, null);
        state.recordResearch(ResearchStages.update(this, run.withExperiment(requirement.id())));
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    /**
     * True when every requirement holds. A project without requirements is only completed by the research system or a
     * command.
     */
    public boolean requirementsMet(UUID runId) {
        ResearchHistory run = state.research().get(runId);
        if (!runActive(run)) return false;
        run=ResearchStages.update(this,run);
        if(!ResearchStages.available(run) || !ResearchStages.last(run)) return false;
        var project=KnowledgeDefinitions.current().projects().get(run.project());
        if(project.stages().isEmpty()) {
            var current=run;var requirements=requirements(run);
            return !requirements.isEmpty() && requirements.stream().allMatch(r -> requirementMet(current,r));
        }
        return run.stageState().visits().get(run.stageState().current()).ready();
    }

    /**
     * Observations a team may submit as evidence: its archive and its inbox.
     */
    public List<KnowledgeElement> evidencePool() {
        List<KnowledgeElement> pool = new ArrayList<>();
        for (var entries : List.of(state.archive().entries().values(), state.inbox().entries().values()))
            for (KnowledgeEntry entry : entries) if (entry.element().observation().isPresent()) pool.add(entry.element());
        return pool;
    }

    /**
     * Finds observations in the archive or inbox that satisfy the requirement and records copies of them.
     */
    public OperationResult submitEvidence(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EVIDENCE);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (requirementMet(run, requirement)) return result(Status.ALREADY_OWNED, null);
        return LinkMatcher.find(evidencePool(), requirement.pattern(), TagLookup.SERVER, EVIDENCE_SEARCH_STEPS)
                .map(binding -> recordEvidence(run, requirement, binding)).orElseGet(() -> result(Status.NO_MATCH, null));
    }

    /**
     * Submits exactly these observations, one per slot.
     */
    public OperationResult submitEvidence(UUID runId, String requirementId, List<KnowledgeKey> keys) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EVIDENCE);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (requirementMet(run, requirement)) return result(Status.ALREADY_OWNED, null);
        List<KnowledgeElement> records = new ArrayList<>();
        for (KnowledgeKey key : keys) {
            KnowledgeEntry entry = entry(key);
            if (entry == null || entry.element().observation().isEmpty()) return result(Status.INVALID_INPUT, key);
            records.add(entry.element());
        }
        return LinkMatcher.bindExact(records, requirement.pattern(), TagLookup.SERVER)
                .map(binding -> recordEvidence(run, requirement, binding)).orElseGet(() -> result(Status.NO_MATCH, null));
    }

    private OperationResult recordEvidence(ResearchHistory run, ProjectRequirement requirement, Map<String, KnowledgeElement> binding) {
        Map<String, Observation> copies = new HashMap<>();
        binding.forEach((slot, element) -> copies.put(slot, element.observation().orElseThrow()));
        state.recordResearch(ResearchStages.update(this, run.withEvidence(requirement.id(), copies)));
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    /**
     * Level of the next calculation game this requirement still needs, if it needs one.
     */
    public OptionalInt calculationLevel(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.CALCULATION);
        return requirement == null || playerRemaining(run, requirement) == 0 ? OptionalInt.empty() : OptionalInt.of(requirement.level());
    }

    public OperationResult recordCalculation(UUID runId, String requirementId, int level) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.CALCULATION);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (playerRemaining(run, requirement) == 0) return result(Status.ALREADY_OWNED, null);
        if (!GenerateInfo.atLeast(level, requirement.level())) return result(Status.INVALID_INPUT, null);
        state.recordResearch(ResearchStages.update(this, run.withCalculation(requirement.id(), level)));
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    public List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> observationTargets(ResourceLocation experiment) {
        return observationTargets(java.util.Set.of(experiment));
    }
    public List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> observationTargets(java.util.Set<ResourceLocation> experiments) {
        var targets=new ArrayList<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding>();
        ResearchRuns.sorted(state).stream().filter(this::runActive).forEach(run -> {
            for(var requirement:requirements(run)) if(requirement.type()==ProjectRequirement.Type.OBSERVATION && !requirementMet(run,requirement)
                && requirement.observation().filter(o -> o.teamCurrentRun() && o.experiment().filter(experiments::contains).isPresent()).isPresent())
                targets.add(new com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding(teamId(),run.id(),requirement.id(),requirement.observation().orElseThrow().experiment().orElseThrow()));
        });return List.copyOf(targets);
    }
    /** Fresh sampling only. Received notes/archive entries never call this world-action endpoint. */
    public void observeResearchTarget(ServerPlayer player,Observation observation,net.minecraft.core.BlockPos liveBlock,net.minecraft.world.item.ItemStack liveStack) {
        if(!KnowledgeDataAPI.getData(player).team().getId().equals(teamId())) return;
        for(var run:ResearchRuns.sorted(state)) {
            if(!runActive(run)) continue;
            for(var requirement:requirements(run)) {
                if(requirement.type()!=ProjectRequirement.Type.OBSERVATION || requirementMet(run,requirement)) continue;
                var task=requirement.observation().orElseThrow();
                if(task.acceptRecorded() || !task.condition().matches(KnowledgeElement.observation(observation))) continue;
                com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding binding=null;
                if(task.teamCurrentRun()) {
                    if(observation.type()==Observation.Type.BLOCK && liveBlock!=null && player.serverLevel().hasChunkAt(liveBlock)
                        && net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(player.serverLevel().getBlockState(liveBlock).getBlock()).equals(observation.object()))
                        binding=com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.get(player.serverLevel()).at(liveBlock).orElse(null);
                    else if(observation.type()==Observation.Type.ITEM && !liveStack.isEmpty()
                        && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(liveStack.getItem()).equals(observation.object()))
                        binding=com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding.read(liveStack).orElse(null);
                    if(binding==null || !binding.team().equals(teamId()) || !binding.run().equals(run.id()) || !binding.requirement().equals(requirement.id())
                        || task.experiment().filter(binding.experiment()::equals).isEmpty()) continue;
                }
                if(recordObservationTask(run.id(),requirement.id()).succeeded() && binding!=null)
                    com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.removeBinding(binding);
            }
        }
    }

    public Optional<ResourceLocation> minigameId(UUID run, String task) {
        var requirement = openRequirement(state.research().get(run), task, ProjectRequirement.Type.MINIGAME);
        return requirement == null ? Optional.empty() : requirement.game();
    }

    public OptionalInt minigameLevel(UUID run, String task, ResourceLocation game) {
        var requirement = openRequirement(state.research().get(run), task, ProjectRequirement.Type.MINIGAME);
        return requirement != null && requirement.game().filter(game::equals).isPresent()
                ? OptionalInt.of(requirement.level()) : OptionalInt.empty();
    }

    public boolean taskAvailable(UUID runId,String task,ProjectRequirement.Type type) {
        return openRequirement(state.research().get(runId),task,type)!=null;
    }
    public OperationResult recordMinigameWin(UUID runId,String task,ResourceLocation game) {
        var run=state.research().get(runId);var requirement=openRequirement(run,task,ProjectRequirement.Type.MINIGAME);
        if(requirement==null || requirement.game().filter(game::equals).isEmpty()) return result(Status.NOT_FOUND,null);
        if(requirementMet(run,requirement)) return result(Status.ALREADY_OWNED,null);
        state.recordResearch(ResearchStages.update(this,run.withStages(ResearchStages.prepare(this,run).stageState().success(task))));
        notifyChanged();return result(Status.SUCCESS,null);
    }
    public OperationResult recordObservationTask(UUID runId,String task) {
        var run=state.research().get(runId);var requirement=openRequirement(run,task,ProjectRequirement.Type.OBSERVATION);
        if(requirement==null) return result(Status.NOT_FOUND,null);
        if(requirementMet(run,requirement)) return result(Status.ALREADY_OWNED,null);
        state.recordResearch(ResearchStages.update(this,run.withStages(ResearchStages.prepare(this,run).stageState().success(task))));
        notifyChanged();return result(Status.SUCCESS,null);
    }
    public OperationResult advanceStage(UUID runId,String expectedStage) {
        var run=state.research().get(runId);
        if(!runActive(run)) return result(Status.NOT_FOUND,null);
        run=ResearchStages.update(this,run);
        if(!run.stageState().current().equals(expectedStage) || !ResearchStages.available(run) || ResearchStages.last(run)
            || !run.stageState().visits().get(expectedStage).ready()) return result(Status.REQUIREMENTS_UNMET,null);
        int next=run.stageState().order().indexOf(expectedStage)+1;
        var definition=KnowledgeDefinitions.current().projects().get(run.project()).stage(run.stageState().order().get(next)).orElse(null);
        if(definition==null) return result(Status.DEFINITION_UNAVAILABLE,null);
        run=run.withStages(ResearchStages.enter(this,run,definition));
        state.recordResearch(ResearchStages.update(this,run));notifyChanged();return result(Status.SUCCESS,null);
    }

    private ProjectRequirement openRequirement(ResearchHistory run, String requirementId, ProjectRequirement.Type type) {
        if (!runActive(run)) return null;
        return requirements(run).stream().filter(r -> r.id().equals(requirementId) && r.type() == type).findFirst().orElse(null);
    }

    /**
     * Player path: completes an open run once all of its requirements hold.
     */
    public List<OperationResult> finishResearch(UUID runId) {
        ResearchHistory run = state.research().get(runId);
        if (run == null || !run.open()) return List.of(result(Status.NOT_FOUND, null));
        if (!requirementsMet(runId)) return List.of(result(Status.REQUIREMENTS_UNMET, null));
        return completeResearch(runId);
    }

    /**
     * Closes an open run. Its submitted progress is dropped; the record stays as history.
     */
    public OperationResult abandonResearch(UUID runId) {
        ResearchHistory run = state.research().get(runId);
        if (run == null || !run.open()) return result(Status.NOT_FOUND, null);
        state.recordResearch(run.abandon());
        com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.removeRun(runId);
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    public List<OperationResult> completeResearch(UUID run) {
        var record = state.research().get(run);
        if (record == null) return List.of();
        var definition = KnowledgeDefinitions.current().projects().get(record.project());
        return definition == null ? List.of() : completeResearch(run, definition.results());
    }

    public List<OperationResult> completeResearch(UUID run, List<ResourceLocation> results) {
        return completeResearch(run, results, Grant.NORMAL);
    }

    public List<OperationResult> completeCreativeResearch(ResourceLocation project) {
        var run = creativeResearchRun(project);
        return run.isEmpty() ? List.of() : completeResearch(run.get(),
            KnowledgeDefinitions.current().projects().get(project).results(), Grant.CREATIVE);
    }

    private List<OperationResult> completeResearch(UUID run, List<ResourceLocation> results, Grant grant) {
        ResearchHistory history = state.research().get(run);
        if (!runActive(history) || results.isEmpty()) return List.of();
        var definition = KnowledgeDefinitions.current().projects().get(history.project());
        if (definition == null || !new HashSet<>(definition.results()).equals(new HashSet<>(results))) return List.of();
        List<KnowledgeKey> keys = results.stream().distinct().map(KnowledgeKey::result).toList();
        state.recordResearch(history.completedWith(keys));
        com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.removeRun(run);
        List<OperationResult> outputs = results.stream().distinct().map(id -> produce(KnowledgeElement.result(id), new AcquisitionSource("research:" + run, actor, time), grant)).toList();
        notifyChanged();
        return outputs;
    }

    /**
     * 城镇研究所的一天：把人力投入各进行中启动的人力协作需求，并按投入比例从 {@code supplies} 扣除每天的物品。
     * 需求按所需教育等级从高到低分配（同级按启动时间与需求顺序），每项先用满足条件的最低等级人力。
     * {@code pool} 被就地扣减，剩下的就是当天的空闲人力。
     * <p>
     * One day of town research: labor goes to the labor of every open run's requirements, and the daily items are drawn
     * from {@code supplies} in proportion. Requirements asking for more education are served first (then by run start
     * and requirement order), each from the lowest qualifying level. {@code pool} is reduced in place; what remains is
     * the day's idle labor.
     */
    public List<LaborReport.Allocation> workLabor(ResearchLaborPool pool, ResearchSupplies supplies) {
        record Demand(UUID run, ProjectRequirement requirement, ProjectRequirement.Labor labor, double wanted) {
        }
        List<Demand> demands = new ArrayList<>();
        List<ResearchHistory> runs = state.research().values().stream().filter(ResearchHistory::open)
                .sorted(Comparator.comparingLong(ResearchHistory::time).thenComparing(run -> run.id().toString())).toList();
        for (ResearchHistory run : runs)
            for (ProjectRequirement requirement : requirements(run)) {
                var labor = requirement.labor().orElse(null);
                if (labor == null) continue;
                double remaining = laborRemaining(run, requirement);
                if (remaining > LABOR_EPSILON) demands.add(new Demand(run.id(), requirement, labor, Math.min(labor.workers(), remaining)));
            }
        // Stable: equal education keeps run and requirement order.
        demands.sort(Comparator.comparingInt((Demand demand) -> demand.labor().education()).reversed());
        List<LaborReport.Allocation> allocations = new ArrayList<>();
        boolean changed = false;
        for (Demand demand : demands) {
            double given = Math.min(demand.wanted(), pool.available(demand.labor().education()));
            boolean itemsShort = false;
            List<ProjectRequirement.Material> items = demand.labor().items();
            if (given > LABOR_EPSILON && !items.isEmpty()) {
                double share = Math.max(0, Math.min(1, supplies.share(items, dailyItems(demand.labor(), given))));
                if (share < 1 - LABOR_EPSILON) {
                    itemsShort = true;
                    given *= share;
                }
                // Overlapping item tags can make a joint take fail where each item alone was in stock; try less.
                boolean taken = false;
                for (int attempt = 0; attempt < 4 && given > LABOR_EPSILON; attempt++) {
                    if (supplies.take(items, dailyItems(demand.labor(), given))) {
                        taken = true;
                        break;
                    }
                    itemsShort = true;
                    given /= 2;
                }
                if (!taken) given = 0;
            }
            if (given > LABOR_EPSILON) {
                pool.take(demand.labor().education(), given);
                ResearchHistory current = state.research().get(demand.run());
                state.recordResearch(ResearchStages.update(this, current.withLabor(demand.requirement().id(), given)));
                changed = true;
            } else given = 0;
            allocations.add(new LaborReport.Allocation(demand.run(), demand.requirement().id(), demand.wanted(), given, itemsShort));
        }
        if (changed) notifyChanged();
        return List.copyOf(allocations);
    }

    private static List<Double> dailyItems(ProjectRequirement.Labor labor, double given) {
        double share = given / labor.workers();
        return labor.items().stream().map(item -> item.count() * share).toList();
    }

    /**
     * 研究所居民的联想：先在当前有效档案中寻找能完整绑定、输出想法还不在档案和来件中的联络规则，随机选一条，按正常联络流程产出想法；
     * 没有时以一项随机的有效知识为主题给出一条暗示。调用方负责判定当天是否联想成功。
     * <p>
     * Institute researchers associate: among link rules that the active archive fully satisfies and whose idea is in
     * neither the archive nor the inbox, one chosen at random links as a player's link would; failing that, a random
     * active topic receives a hint. The caller decides whether the day's association happens at all.
     *
     * @param day 城镇结算日，暗示每天至多一次 / town day; one institute hint per day
     * @return the outcome, naming the idea produced or the hint's topic; labor and chance are left for the caller
     */
    /** The day-map key under which the town day an institute found {@code idea} is kept. */
    public static String instituteFoundKey(KnowledgeKey idea) {
        return "institute_found:" + idea.kind().name().toLowerCase(java.util.Locale.ROOT) + "/" + idea.id();
    }

    public LaborReport.Association instituteAssociation(RandomSource random, long day) {
        Map<ResourceLocation, LinkRule> fresh = new LinkedHashMap<>();
        KnowledgeDefinitions.current().links().forEach((id, rule) -> {
            KnowledgeKey idea = KnowledgeKey.idea(rule.output());
            // Only ideas the team has not met, and whose definitions are usable now.
            if (!state.archive().contains(idea) && !state.inbox().contains(idea)
                    && KnowledgeAvailability.available(KnowledgeElement.idea(rule.output()))) fresh.put(id, rule);
        });
        List<LinkMatcher.Match> candidates = fresh.isEmpty() ? List.of() : candidateLinks(fresh, CANDIDATE_LIMIT);
        if (!candidates.isEmpty()) {
            LinkMatcher.Match chosen = candidates.get(random.nextInt(candidates.size()));
            List<KnowledgeKey> keys = chosen.rule().inputs().stream().map(slot -> chosen.bindings().get(slot.name())).toList();
            link(chosen.ruleId(), keys, "institute:link:" + chosen.ruleId());
            KnowledgeKey found = KnowledgeKey.idea(chosen.rule().output());
            // The town day it was found on, so the institute's list can say when.
            state.markDay(instituteFoundKey(found), day);
            return new LaborReport.Association(LaborReport.Outcome.IDEA, 0, 0, Optional.of(found));
        }
        List<KnowledgeKey> topics = new ArrayList<>(state.archive().entries().keySet().stream().filter(this::isActive)
                .sorted(Comparator.comparing((KnowledgeKey key) -> key.kind()).thenComparing(KnowledgeKey::id)).toList());
        for (int i = topics.size() - 1; i > 0; i--) Collections.swap(topics, i, random.nextInt(i + 1));
        for (KnowledgeKey topic : topics.subList(0, Math.min(ASSOCIATION_TOPIC_TRIES, topics.size()))) {
            if (associate(topic, day, "institute").succeeded())
                return new LaborReport.Association(LaborReport.Outcome.HINT, 0, 0, Optional.of(topic));
        }
        return new LaborReport.Association(LaborReport.Outcome.NOTHING, 0, 0, Optional.empty());
    }

    /**
     * Keeps this day's institute report for the team's members to read.
     */
    public void recordLaborReport(LaborReport report) {
        state.laborReport(report);
        notifyChanged();
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
                .filter(e -> e.getValue().inputs().stream().anyMatch(slot -> slot.matches(element, TagLookup.SERVER)))
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
        if (operation.equals("learn") || operation.startsWith("learn_"))
            IdeaAcquisitionService.fire(this, List.of(new IdeaAcquisitionService.Signal(IdeaAcquisitionService.KNOWLEDGE_LEARNED, entry.element())), java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
    }

    private static OperationResult result(Status status, KnowledgeKey key) {
        return new OperationResult(status, key);
    }

    /** Team services only mark the team; TeamResearchService coalesces every mark of a tick into one snapshot. */
    private void notifyChanged() {
        changed.run();
    }
}
