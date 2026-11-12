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

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent;
import com.teammoeg.frostedresearch.knowledge.link.LinkMatcher;
import com.teammoeg.frostedresearch.knowledge.link.TagLookup;
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
        NOT_UNDERSTOOD, DEFINITION_UNAVAILABLE, CAPACITY, CANCELLED, NOT_FOUND, INVALID_INPUT, NO_MATCH, NO_HINT, DAILY_LIMIT,
        REQUIREMENTS_UNMET, LEGACY_COMPLETED
    }

    /**
     * Candidate tries allowed when evidence is matched against a team's whole archive and inbox.
     */
    public static final int EVIDENCE_SEARCH_STEPS = 20_000;

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

    public Optional<UUID> actor() { return actor; }

    public long time() { return time; }

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
        if (keys.size() > 5 || new HashSet<>(keys).size() != keys.size() || keys.stream().anyMatch(k -> !query(k).active()))
            return List.of();
        return LinkMatcher.match(keys.stream().map(k -> state.archive().entries().get(k).element()).toList(), KnowledgeDefinitions.current().links()).stream().filter(this::linkAllowed).toList();
    }

    public List<LinkMatcher.Match> candidateLinks(int limit) {
        if (limit <= 0) return List.of();
        return LinkMatcher.candidates(state.archive().entries().values().stream().map(KnowledgeEntry::element).filter(e -> isActive(e.key())).toList(), KnowledgeDefinitions.current().links(), Integer.MAX_VALUE).stream().filter(this::linkAllowed).limit(limit).toList();
    }

    private boolean linkAllowed(LinkMatcher.Match match) {
        return IdeaAcquisitionService.requiredMet(this, match.rule().output());
    }

    /** Check one selected rule without enumerating candidates, used by transient waits. */
    public Optional<LinkMatcher.Match> matchLink(ResourceLocation ruleId, List<KnowledgeKey> keys) {
        var rule = KnowledgeDefinitions.current().links().get(ruleId);
        if (rule == null || keys.size() > 5 || new HashSet<>(keys).size() != keys.size() || keys.stream().anyMatch(k -> !isActive(k))) return Optional.empty();
        return LinkMatcher.bindExact(keys.stream().map(k -> state.archive().entries().get(k).element()).toList(), rule.pattern(), TagLookup.SERVER)
            .map(binding -> {
                Map<String, KnowledgeKey> witnesses = new LinkedHashMap<>();
                binding.forEach((name, element) -> witnesses.put(name, element.key()));
                return new LinkMatcher.Match(ruleId, rule, witnesses);
            }).filter(this::linkAllowed);
    }

    public OperationResult link(ResourceLocation ruleId, List<KnowledgeKey> keys) {
        var match = matchLink(ruleId, keys).orElse(null);
        if (match == null) return result(Status.NO_MATCH, null);
        KnowledgeKey output = KnowledgeKey.idea(match.rule().output());
        boolean discovered = state.discover(new LinkDiscovery(ruleId, output, match.bindings(), time));
        // A valid manual deduction can add a new path to an already known or received idea.
        // Keep that history independent of acquisition; do not relearn/regrant an existing entity.
        Query owned = query(output);
        OperationResult produced = owned.inArchive() ? result(Status.ALREADY_OWNED, output)
            : owned.inInbox() ? result(Status.ALREADY_RECEIVED, output)
            : produce(KnowledgeElement.idea(match.rule().output()), new AcquisitionSource("link:" + ruleId, actor, time));
        if (discovered) notifyChanged();
        return produced;
    }

    public void initialize() {
        for (var run : List.copyOf(state.research().values())) if (run.open()) {
            var prepared=ResearchStages.update(this,run);
            if(!prepared.equals(run)) {
                var definition=KnowledgeDefinitions.current().projects().get(run.project());
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
        clearSupersededProofs();
        notifyChanged();
    }

    /**
     * Whether the team can begin this project from its current knowledge: {@code LEARNABLE}, or the reason it cannot. A team
     * has at most one open run per project, and a completed project starts again only while one of its results is
     * missing from the archive.
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
        Status status = definition == null ? Status.DEFINITION_UNAVAILABLE : legacyCompleted(project) ? Status.LEGACY_COMPLETED : openRun(project).isPresent() ? Status.ACTIVE
            : state.research().values().stream().anyMatch(run -> run.completed() && run.project().equals(project))
                && definition.results().stream().allMatch(result -> state.archive().contains(KnowledgeKey.result(result))) ? Status.ALREADY_OWNED
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
        UUID id = UUID.randomUUID();
        state.recordResearch(ResearchStages.update(this, new ResearchHistory(id, project, origins, time, false, List.of())));
        notifyChanged();
        return Optional.of(id);
    }

    public Optional<ResearchHistory> openRun(ResourceLocation project) {
        return state.research().values().stream().filter(run -> run.open() && run.project().equals(project)).findFirst();
    }

    /**
     * Requirements of the run's project under the current definitions.
     */
    public List<ProjectRequirement> requirements(ResearchHistory run) {
        return ResearchStages.tasks(ResearchStages.prepare(this,run)).stream().map(com.teammoeg.frostedresearch.knowledge.definition.StageDefinition.Task::requirement).toList();
    }

    /**
     * Stored progress counts only while it satisfies the current definition, so changed requirements may need new work.
     */
    public boolean requirementMet(ResearchHistory run, ProjectRequirement requirement) {
        return switch (requirement.type()) {
            case MINIGAME, OBSERVATION -> run.stageState().completedTasks().contains(requirement.id());
            case SAMPLE -> requirement.sample().filter(value -> value.equals(run.samples().get(requirement.id()))).isPresent();
            case CALCULATION -> calculationsDone(run, requirement) >= requirement.count();
            case EXPERIMENT -> experimentsDone(run, requirement) >= requirement.count();
            case EVIDENCE -> {
                var binding = run.evidence().get(requirement.id());
                yield binding != null && LinkMatcher.bindExact(binding.values().stream().map(KnowledgeElement::observation).toList(),
                        requirement.pattern(), TagLookup.SERVER).isPresent();
            }
        };
    }

    /**
     * Won games at or above the requirement's current level.
     */
    public List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> sampleTargets(ResourceLocation experiment) {
        List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> targets = new ArrayList<>();
        state.research().values().stream().filter(run -> run.open() && !legacyCompleted(run.project())).sorted(Comparator.comparing(r -> r.id().toString())).forEach(run -> {
            for (var requirement : requirements(run)) if (requirement.type() == ProjectRequirement.Type.SAMPLE && !requirementMet(run, requirement)
                    && requirement.sample().filter(sample -> sample.experiment().equals(experiment)).isPresent())
                targets.add(new com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding(teamId(), run.id(), requirement.id(), experiment));
        });
        return List.copyOf(targets);
    }

    /** Consumes exactly one real bound sample, only after every current task/run/team check has passed. */
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
                .filter(level -> level >= requirement.level()).count();
    }

    public static int experimentsDone(ResearchHistory run, ProjectRequirement requirement) {
        return run.experiments().getOrDefault(requirement.id(), 0);
    }

    /**
     * The experiment requirement while it still needs runs, for an experiment table to work toward.
     */
    public Optional<ProjectRequirement> experimentRequirement(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EXPERIMENT);
        return requirement == null || requirementMet(run, requirement) ? Optional.empty() : Optional.of(requirement);
    }

    public OperationResult recordExperiment(UUID runId, String requirementId) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.EXPERIMENT);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (requirementMet(run, requirement)) return result(Status.ALREADY_OWNED, null);
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
        if (run == null || !run.open() || legacyCompleted(run.project())) return false;
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
        return requirement == null || requirementMet(run, requirement) ? OptionalInt.empty() : OptionalInt.of(requirement.level());
    }

    public OperationResult recordCalculation(UUID runId, String requirementId, int level) {
        ResearchHistory run = state.research().get(runId);
        ProjectRequirement requirement = openRequirement(run, requirementId, ProjectRequirement.Type.CALCULATION);
        if (requirement == null) return result(Status.NOT_FOUND, null);
        if (requirementMet(run, requirement)) return result(Status.ALREADY_OWNED, null);
        if (level < requirement.level()) return result(Status.INVALID_INPUT, null);
        state.recordResearch(ResearchStages.update(this, run.withCalculation(requirement.id(), level)));
        notifyChanged();
        return result(Status.SUCCESS, null);
    }

    public List<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding> observationTargets(ResourceLocation experiment) {
        var targets=new ArrayList<com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding>();
        state.research().values().stream().filter(run -> run.open() && !legacyCompleted(run.project())).sorted(Comparator.comparing(r -> r.id().toString())).forEach(run -> {
            for(var requirement:requirements(run)) if(requirement.type()==ProjectRequirement.Type.OBSERVATION && !requirementMet(run,requirement)
                && requirement.observation().filter(o -> o.teamCurrentRun() && o.experiment().filter(experiment::equals).isPresent()).isPresent())
                targets.add(new com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding(teamId(),run.id(),requirement.id(),experiment));
        });return List.copyOf(targets);
    }
    /** Fresh sampling only. Received notes/archive entries never call this world-action endpoint. */
    public void observeResearchTarget(ServerPlayer player,Observation observation,net.minecraft.core.BlockPos liveBlock,net.minecraft.world.item.ItemStack liveStack) {
        if(!KnowledgeDataAPI.getData(player).team().getId().equals(teamId())) return;
        for(var run:List.copyOf(state.research().values())) {
            if(!run.open() || legacyCompleted(run.project())) continue;
            for(var requirement:requirements(run)) {
                if(requirement.type()!=ProjectRequirement.Type.OBSERVATION || requirementMet(run,requirement)) continue;
                var task=requirement.observation().orElseThrow();
                if(!task.condition().matches(KnowledgeElement.observation(observation))) continue;
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
        if(run==null || !run.open() || legacyCompleted(run.project())) return result(Status.NOT_FOUND,null);
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
        if (run == null || !run.open() || legacyCompleted(run.project())) return null;
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
        ResearchHistory history = state.research().get(run);
        if (history == null || !history.open() || legacyCompleted(history.project()) || results.isEmpty()) return List.of();
        var definition = KnowledgeDefinitions.current().projects().get(history.project());
        if (definition == null || !new HashSet<>(definition.results()).equals(new HashSet<>(results))) return List.of();
        List<KnowledgeKey> keys = results.stream().distinct().map(KnowledgeKey::result).toList();
        state.recordResearch(history.completedWith(keys));
        com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs.removeRun(run);
        List<OperationResult> outputs = results.stream().distinct().map(id -> produce(KnowledgeElement.result(id), new AcquisitionSource("research:" + run, actor, time))).toList();
        notifyChanged();
        return outputs;
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
