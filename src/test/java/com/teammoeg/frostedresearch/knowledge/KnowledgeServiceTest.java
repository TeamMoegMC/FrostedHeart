package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static com.teammoeg.frostedresearch.knowledge.KnowledgeService.Status.*;

class KnowledgeServiceTest {
    static final ResourceLocation BASE = id("base"), DERIVED = id("derived"), OTHER = id("other"), RULE = id("link");
    static final AcquisitionSource SOURCE = new AcquisitionSource("note", Optional.empty(), 12);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // MinecraftForge creates this bus with startShutdown(); normal FML startup starts it.
        MinecraftForge.EVENT_BUS.start();
    }

    @AfterEach
    void clear() {
        KnowledgeDefinitions.clearForTests();
        ResearchResultCatalog.clearForTests();
    }

    static ResourceLocation id(String name) {
        return new ResourceLocation("test", name);
    }

    static IdeaDefinition idea(List<KnowledgeKey> understanding) {
        return new IdeaDefinition("Title", "Body", understanding, List.of(), true);
    }

    static KnowledgeDefinitions.Snapshot definitions(Map<ResourceLocation, LinkRule> links) {
        return new KnowledgeDefinitions.Snapshot(0, Map.of(BASE, idea(List.of()), DERIVED, idea(List.of(KnowledgeKey.idea(BASE))), OTHER, idea(List.of())),
                Map.of(), links, Map.of(), Map.of(), List.of());
    }

    static KnowledgeService service(KnowledgeState state, AtomicInteger syncs) {
        return new KnowledgeService(UUID.randomUUID(), state, 100, syncs::incrementAndGet);
    }

    static KnowledgeElement observation() {
        return KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.BLOCK, id("rock"), Map.of(), Observation.Source.command(), Optional.empty()));
    }

    @Test
    void inboxDoesNotGrantKnowledgeAndUnderstandingIsCheckedOnlyWhenLearning() {
        KnowledgeDefinitions.install(definitions(Map.of()));
        var state = new KnowledgeState();
        var syncs = new AtomicInteger();
        var service = service(state, syncs);
        var derived = KnowledgeElement.idea(DERIVED);
        var base = KnowledgeElement.idea(BASE);
        assertEquals(SUCCESS, service.receive(derived, SOURCE).status());
        assertEquals(NOT_UNDERSTOOD, service.query(derived.key()).status());
        assertFalse(service.query(derived.key()).active());
        assertEquals(NOT_UNDERSTOOD, service.learnInbox(derived.key()).status());
        assertTrue(state.inbox().contains(derived.key()));
        assertEquals(SUCCESS, service.learn(base, SOURCE, KnowledgeService.Grant.NORMAL).status());
        assertEquals(SUCCESS, service.learnInbox(derived.key()).status());
        assertFalse(state.inbox().contains(derived.key()));
        assertEquals(SUCCESS, service.forget(base.key()).status());
        assertTrue(service.query(derived.key()).active());
        assertEquals(ALREADY_OWNED, service.learnInbox(derived.key()).status());
        assertEquals(2, state.acquisitions().size());
    }

    @Test
    void cancellationAndDuplicateLearningNeverCommitOrSync() {
        KnowledgeDefinitions.install(definitions(Map.of()));
        var state = new KnowledgeState();
        var syncs = new AtomicInteger();
        var service = service(state, syncs);
        var base = KnowledgeElement.idea(BASE);
        service.receive(base, SOURCE);
        List<String> committed = new ArrayList<>();
        Consumer<KnowledgeOperationEvent.Committed> notifications = event -> {
            if (event.team().equals(service.teamId())) committed.add(event.operation());
        };
        Consumer<KnowledgeOperationEvent.Check> cancel = event -> {
            if (event.team().equals(service.teamId()) && event.operation().equals("learn")) event.setCanceled(true);
        };
        // JUnit does not run Forge's transformer that supplies event no-argument constructors.
        // Compute the ordinary listener lists through valid event instances before class registration.
        new KnowledgeOperationEvent.Committed(service.teamId(), Optional.empty(), "learn", base, SOURCE).getListenerList();
        new KnowledgeOperationEvent.Check(service.teamId(), Optional.empty(), "learn", base, SOURCE).getListenerList();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, KnowledgeOperationEvent.Committed.class, notifications);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, KnowledgeOperationEvent.Check.class, cancel);
        try {
            long revision = state.mutationRevision();
            int sent = syncs.get();
            assertEquals(CANCELLED, service.learnInbox(base.key()).status());
            assertEquals(revision, state.mutationRevision());
            assertEquals(sent, syncs.get());
            assertTrue(committed.isEmpty());
            assertTrue(state.inbox().contains(base.key()));
            assertFalse(state.archive().contains(base.key()));
            MinecraftForge.EVENT_BUS.unregister(cancel);
            assertEquals(SUCCESS, service.learnInbox(base.key()).status());
            assertEquals(List.of("learn"), committed);
            revision = state.mutationRevision();
            sent = syncs.get();
            assertEquals(ALREADY_OWNED, service.learn(base, SOURCE, KnowledgeService.Grant.NORMAL).status());
            assertEquals(revision, state.mutationRevision());
            assertEquals(sent, syncs.get());
            assertEquals(1, committed.size());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
            MinecraftForge.EVENT_BUS.unregister(notifications);
        }
    }

    @Test
    void missingDefinitionIsDormantAndRestoresWithoutRelearning() {
        var definitions = definitions(Map.of());
        KnowledgeDefinitions.install(definitions);
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        var key = KnowledgeKey.idea(BASE);
        assertEquals(SUCCESS, service.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL).status());
        KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty());
        assertTrue(service.query(key).inArchive());
        assertFalse(service.query(key).active());
        assertEquals(DEFINITION_UNAVAILABLE, service.query(key).status());
        assertEquals(1, state.acquisitions().size());
        KnowledgeDefinitions.install(definitions);
        assertTrue(service.query(key).active());
        assertEquals(1, state.acquisitions().size());
    }

    @Test
    void generatedOutputRetainsUnunderstoodKnowledgeAndLinkHistorySurvivesPathRemoval() {
        var left = new LinkRule.Slot("a", KnowledgeKey.Kind.IDEA, Optional.of(OTHER), Optional.empty(), List.of());
        var right = new LinkRule.Slot("b", KnowledgeKey.Kind.RESULT, Optional.of(id("finding")), Optional.empty(), List.of());
        LinkRule rule = new LinkRule(List.of(left, right), List.of(), DERIVED, List.of("hint"));
        var defs = definitions(Map.of(RULE, rule));
        var finding = new ResultDefinition(new ResearchResult.Finding(id("finding"), List.of()), "Finding", "", List.of(), true);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0, defs.ideas(), Map.of(id("finding"), finding), defs.links(), Map.of(), Map.of(), List.of()));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        service.learn(KnowledgeElement.idea(OTHER), SOURCE, KnowledgeService.Grant.NORMAL);
        service.learn(KnowledgeElement.result(id("finding")), SOURCE, KnowledgeService.Grant.NORMAL);
        assertEquals(NOT_UNDERSTOOD, service.link(RULE, List.of(KnowledgeKey.result(id("finding")), KnowledgeKey.idea(OTHER))).status());
        assertTrue(state.inbox().contains(KnowledgeKey.idea(DERIVED)));
        assertEquals(1, state.links().size());
        KnowledgeState restored = KnowledgeState.read(state.write());
        assertEquals(state.links(), restored.links());
        assertEquals(state.acquisitions(), restored.acquisitions());
        KnowledgeDefinitions.install(definitions(Map.of()));
        assertEquals(1, restored.links().size());
        var later = service(restored, new AtomicInteger());
        later.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL);
        assertEquals(SUCCESS, later.learnInbox(KnowledgeKey.idea(DERIVED)).status());
        assertTrue(later.isActive(KnowledgeKey.idea(DERIVED)));
    }

    @Test
    void initialGrantBypassesUnderstandingOnceAndCommandsRemainExplicit() {
        var defs = definitions(Map.of());
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0, defs.ideas(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(KnowledgeKey.idea(DERIVED))));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        service.initialize();
        assertTrue(service.isActive(KnowledgeKey.idea(DERIVED)));
        assertFalse(service.isActive(KnowledgeKey.idea(BASE)));
        int historySize = state.acquisitions().size();
        service.initialize();
        assertEquals(historySize, state.acquisitions().size());
        service.forget(KnowledgeKey.idea(DERIVED));
        service.initialize();
        assertFalse(service.isActive(KnowledgeKey.idea(DERIVED)));
        assertEquals(SUCCESS, service.learn(KnowledgeElement.idea(DERIVED), SOURCE, KnowledgeService.Grant.COMMAND).status());
    }

    @Test
    void observationCapacityKeepsRejectedInboxAndMergePreservesOverflow() {
        KnowledgeDefinitions.registerObservationUse(id("evidence"), o -> true);
        var state = new KnowledgeState();
        var service = new KnowledgeService(UUID.randomUUID(), state, 100, () -> {
        }, 1, 1);
        var one = observation();
        var two = observation();
        var three = observation();
        assertEquals(SUCCESS, service.receive(one, SOURCE).status());
        assertEquals(CAPACITY, service.receive(two, SOURCE).status());
        assertEquals(SUCCESS, service.learnInbox(one.key()).status());
        assertEquals(SUCCESS, service.receive(two, SOURCE).status());
        long revision = state.mutationRevision();
        assertEquals(CAPACITY, service.learnInbox(two.key()).status());
        assertEquals(revision, state.mutationRevision());
        assertTrue(state.inbox().contains(two.key()));
        var source = new KnowledgeState();
        source.putArchive(new KnowledgeEntry(three, SOURCE, 100, ""));
        service.merge(source);
        assertEquals(2, state.archive().observationCount());
        assertTrue(state.archive().contains(three.key()));
    }

    @Test
    void teamSnapshotExposesOnlyEncounteredKnowledgeAndActualBindings() {
        var left = new LinkRule.Slot("a", KnowledgeKey.Kind.IDEA, Optional.of(BASE), Optional.empty(), List.of());
        var right = new LinkRule.Slot("b", KnowledgeKey.Kind.IDEA, Optional.of(OTHER), Optional.empty(), List.of());
        var rule = new LinkRule(List.of(left, right), List.of(), DERIVED, List.of("First hint", "Unrevealed hint"));
        KnowledgeDefinitions.install(definitions(Map.of(RULE, rule)));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        service.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL);
        var snapshot = service.snapshot();
        assertTrue(snapshot.getList("links", Tag.TAG_COMPOUND).isEmpty());
        assertFalse(snapshot.toString().contains(RULE.toString()));
        assertFalse(snapshot.toString().contains(DERIVED.toString()));
        assertEquals(SUCCESS, service.associate(KnowledgeKey.idea(BASE), 1, "test").status());
        snapshot = service.snapshot();
        assertTrue(snapshot.toString().contains("First hint"));
        assertFalse(snapshot.toString().contains("Unrevealed hint"));
        assertTrue(snapshot.getList("links", Tag.TAG_COMPOUND).isEmpty());
        service.learn(KnowledgeElement.idea(OTHER), SOURCE, KnowledgeService.Grant.NORMAL);
        assertEquals(SUCCESS, service.link(RULE, List.of(KnowledgeKey.idea(OTHER), KnowledgeKey.idea(BASE))).status());
        assertEquals(1, service.snapshot().getList("links", Tag.TAG_COMPOUND).size());
        assertFalse(service.snapshot().toString().contains("conditions"));
    }

    @Test
    void discussionPartnersAndDreamsHaveIndependentDailyOpportunities() {
        var inputs = List.of(new LinkRule.Slot("a", KnowledgeKey.Kind.IDEA, Optional.of(BASE), Optional.empty(), List.of()),
                new LinkRule.Slot("b", KnowledgeKey.Kind.IDEA, Optional.of(OTHER), Optional.empty(), List.of()));
        KnowledgeDefinitions.install(definitions(Map.of(RULE, new LinkRule(inputs, List.of(), DERIVED, List.of("one", "two", "three", "four")))));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        service.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL);
        UUID player = UUID.randomUUID(), firstPartner = UUID.randomUUID(), secondPartner = UUID.randomUUID();
        assertEquals(SUCCESS, service.discuss(player, firstPartner, KnowledgeKey.idea(BASE), 8).status());
        assertEquals(DAILY_LIMIT, service.discuss(player, firstPartner, KnowledgeKey.idea(BASE), 8).status());
        assertEquals(SUCCESS, service.discuss(player, secondPartner, KnowledgeKey.idea(BASE), 8).status());
        assertEquals(List.of("one", "two"), state.hints().get(RULE));
        assertEquals(SUCCESS, service.setDreamTopic(player, KnowledgeKey.idea(BASE)).status());
        assertEquals(SUCCESS, service.dream(player, 8).status());
        assertEquals(DAILY_LIMIT, service.dream(player, 8).status());
        assertEquals(SUCCESS, service.discuss(player, firstPartner, KnowledgeKey.idea(BASE), 9).status());
        assertEquals(List.of("one", "two", "three", "four"), state.hints().get(RULE));
    }

    @Test
    void hintsPreferUnknownOutputsAndReloadNeverRepeatsAlreadyRevealedText() {
        var inputs = List.of(new LinkRule.Slot("a", KnowledgeKey.Kind.IDEA, Optional.of(BASE), Optional.empty(), List.of()),
                new LinkRule.Slot("b", KnowledgeKey.Kind.IDEA, Optional.of(DERIVED), Optional.empty(), List.of()));
        ResourceLocation knownRule = id("a_known"), unknownRule = id("z_unknown");
        var known = new LinkRule(inputs, List.of(), DERIVED, List.of("known-path"));
        var unknown = new LinkRule(inputs, List.of(), OTHER, List.of("unknown-first", "unknown-second"));
        KnowledgeDefinitions.install(definitions(Map.of(knownRule, known, unknownRule, unknown)));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        service.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL);
        service.learn(KnowledgeElement.idea(DERIVED), SOURCE, KnowledgeService.Grant.NORMAL);
        assertEquals(SUCCESS, service.associate(KnowledgeKey.idea(BASE), 1, "first").status());
        assertEquals(List.of("unknown-first"), state.hints().get(unknownRule));
        assertFalse(state.hints().containsKey(knownRule));
        var reordered = new LinkRule(inputs, List.of(), OTHER, List.of("unknown-first", "unknown-first", "unknown-second"));
        KnowledgeDefinitions.install(definitions(Map.of(knownRule, known, unknownRule, reordered)));
        var restored = KnowledgeState.read(state.write());
        var later = service(restored, new AtomicInteger());
        assertEquals(SUCCESS, later.associate(KnowledgeKey.idea(BASE), 1, "second").status());
        assertEquals(List.of("unknown-first", "unknown-second"), restored.hints().get(unknownRule));
        assertEquals(SUCCESS, later.associate(KnowledgeKey.idea(BASE), 1, "third").status());
        assertEquals(List.of("known-path"), restored.hints().get(knownRule));
        long revision = restored.mutationRevision();
        assertEquals(NO_HINT, later.associate(KnowledgeKey.idea(BASE), 2, "fourth").status());
        assertEquals(revision, restored.mutationRevision());
    }

    @Test
    void researchRequiresDeclaredOriginAndCompletesOnlyItsExactOutputsOnce() {
        ResourceLocation project = id("project"), findingId = id("project_finding"), designId = id("project_design");
        var ideas = new HashMap<>(definitions(Map.of()).ideas());
        ideas.put(BASE, new IdeaDefinition("Origin", "", List.of(), List.of(project), true));
        var finding = new ResultDefinition(new ResearchResult.Finding(findingId, List.of()), "Finding", "", List.of(), true);
        var design = new ResultDefinition(new ResearchResult.Design(designId, List.of(new ResourceLocation("minecraft", "oak_button"))),
                "Design", "", List.of(KnowledgeKey.idea(DERIVED)), true);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0, ideas, Map.of(findingId, finding, designId, design), Map.of(), Map.of(),
                Map.of(project, new ProjectDefinition("Project", List.of(findingId, designId))), List.of()));
        var state = new KnowledgeState();
        var service = service(state, new AtomicInteger());
        assertTrue(service.startResearch(project, KnowledgeKey.idea(BASE)).isEmpty());
        service.learn(KnowledgeElement.idea(BASE), SOURCE, KnowledgeService.Grant.NORMAL);
        service.learn(KnowledgeElement.idea(OTHER), SOURCE, KnowledgeService.Grant.NORMAL);
        assertTrue(service.startResearch(project, KnowledgeKey.idea(OTHER)).isEmpty());
        assertTrue(service.startResearch(id("undeclared"), KnowledgeKey.idea(BASE)).isEmpty());
        UUID run = service.startResearch(project, KnowledgeKey.idea(BASE)).orElseThrow();
        long revision = state.mutationRevision();
        assertTrue(service.completeResearch(run, List.of(findingId)).isEmpty());
        assertTrue(service.completeResearch(run, List.of(findingId, id("arbitrary"))).isEmpty());
        assertEquals(revision, state.mutationRevision());
        assertFalse(state.research().get(run).completed());
        var outcomes = service.completeResearch(run);
        assertEquals(List.of(SUCCESS, NOT_UNDERSTOOD), outcomes.stream().map(KnowledgeService.OperationResult::status).toList());
        assertTrue(service.isActive(KnowledgeKey.result(findingId)));
        assertTrue(state.inbox().contains(KnowledgeKey.result(designId)));
        assertEquals("research:" + run, service.entry(KnowledgeKey.result(findingId)).source().mechanism());
        assertEquals("research:" + run, service.entry(KnowledgeKey.result(designId)).source().mechanism());
        assertEquals(KnowledgeKey.idea(BASE), state.research().get(run).idea());
        assertEquals(List.of(KnowledgeKey.result(findingId), KnowledgeKey.result(designId)), state.research().get(run).results());
        int acquisitions = state.acquisitions().size();
        revision = state.mutationRevision();
        assertTrue(service.completeResearch(run).isEmpty());
        assertEquals(acquisitions, state.acquisitions().size());
        assertEquals(revision, state.mutationRevision());
        assertEquals(state.research(), KnowledgeState.read(state.write()).research());
    }
}
