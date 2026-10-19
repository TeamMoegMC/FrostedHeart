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

import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.link.FieldCondition;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.common.MinecraftForge;
import org.junit.jupiter.api.*;

import java.util.*;

import static com.teammoeg.frostedresearch.knowledge.KnowledgeService.Status.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Research runs: one open run per project, evidence from archive and inbox, calculations by level, gated completion.
 */
class ResearchRunTest {
    static final ResourceLocation ORIGIN = id("origin"), PROJECT = id("survey"), BARE = id("bare"), FINDING = id("survey_finding");
    static final AcquisitionSource SOURCE = new AcquisitionSource("note", Optional.empty(), 12);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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

    static LinkRule.Slot slot(String name, String object) {
        return new LinkRule.Slot(name, KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK),
                List.of(new FieldCondition("object", FieldCondition.Operator.EQ, Optional.of(ObservationValue.known(object)), Optional.empty())));
    }

    /**
     * Two rocks from different biomes, and two level-1 calculations.
     */
    static void install(String object, int level) {
        var evidence = ProjectRequirement.evidence("pair", "Two rocks", List.of(slot("first", object), slot("second", object)), List.of(),
                List.of(new ProjectRequirement.Distinct("biome", List.of("first", "second"))));
        var calculation = ProjectRequirement.calculation("calc", "", level, 2);
        var finding = new ResultDefinition(new ResearchResult.Finding(FINDING, List.of()), "Finding", "", List.of(), true);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,
                Map.of(ORIGIN, new IdeaDefinition("Origin", "", List.of(), List.of(PROJECT, BARE), true)),
                Map.of(FINDING, finding), Map.of(), Map.of(),
                Map.of(PROJECT, new ProjectDefinition("Survey", "", List.of(FINDING), List.of(evidence, calculation)),
                        BARE, new ProjectDefinition("Bare", List.of(FINDING))), List.of()));
    }

    static KnowledgeElement rock(String biome) {
        return KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("test", "rock"),
                Map.of("biome", ObservationValue.known(biome)), Observation.Source.command(), Optional.empty()));
    }

    static KnowledgeService service(KnowledgeState state) {
        var service = new KnowledgeService(UUID.randomUUID(), state, 100, () -> {
        });
        assertTrue(service.learn(KnowledgeElement.idea(ORIGIN), SOURCE, KnowledgeService.Grant.NORMAL).succeeded());
        return service;
    }

    @Test
    void oneOpenRunPerProjectAndACompletedProjectRestartsOnlyForAMissingResult() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        UUID first = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertEquals(ACTIVE, service.researchStartStatus(PROJECT, KnowledgeKey.idea(ORIGIN)));
        assertTrue(service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).isEmpty());

        assertEquals(SUCCESS, service.abandonResearch(first).status());
        assertTrue(state.research().get(first).abandoned());
        assertTrue(service.completeResearch(first).isEmpty(), "an abandoned run cannot be completed");
        UUID second = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();

        assertEquals(List.of(SUCCESS), service.completeResearch(second).stream().map(KnowledgeService.OperationResult::status).toList());
        assertEquals(ALREADY_OWNED, service.researchStartStatus(PROJECT, KnowledgeKey.idea(ORIGIN)));
        assertEquals(SUCCESS, service.forget(KnowledgeKey.result(FINDING)).status());
        assertEquals(LEARNABLE, service.researchStartStatus(PROJECT, KnowledgeKey.idea(ORIGIN)));
    }

    @Test
    void evidenceComesFromArchiveOrInboxAndSurvivesItsSources() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        var plains = rock("minecraft:plains");
        var otherPlains = rock("minecraft:plains");
        assertEquals(SUCCESS, service.learn(plains, SOURCE, KnowledgeService.Grant.NORMAL).status());
        assertEquals(SUCCESS, service.receive(otherPlains, SOURCE).status());
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertEquals(NO_MATCH, service.submitEvidence(run, "pair").status(), "both rocks come from the same biome");
        assertEquals(NO_MATCH, service.submitEvidence(run, "pair", List.of(plains.key(), otherPlains.key())).status());
        assertEquals(INVALID_INPUT, service.submitEvidence(run, "pair", List.of(plains.key(), KnowledgeKey.observation(UUID.randomUUID()))).status());

        var forest = rock("minecraft:forest");
        assertEquals(SUCCESS, service.receive(forest, SOURCE).status());
        assertEquals(SUCCESS, service.submitEvidence(run, "pair").status());
        var binding = state.research().get(run).evidence().get("pair");
        assertEquals(Set.of("minecraft:plains", "minecraft:forest"),
                Set.of(binding.get("first").value("biome").text().orElseThrow(), binding.get("second").value("biome").text().orElseThrow()));
        assertEquals(ALREADY_OWNED, service.submitEvidence(run, "pair").status());

        // Submitted copies stay valid after their inbox entry is removed.
        assertEquals(SUCCESS, service.deleteInbox(forest.key()).status());
        assertTrue(service.requirementMet(state.research().get(run), KnowledgeDefinitions.current().projects().get(PROJECT).requirement("pair").orElseThrow()));
        assertEquals(ALREADY_OWNED, service.submitEvidence(run, "pair", List.of(plains.key(), forest.key())).status());
    }

    @Test
    void calculationsCountWinsAtTheRequiredLevelUntilSatisfied() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertEquals(OptionalInt.of(1), service.calculationLevel(run, "calc"));
        assertTrue(service.calculationLevel(run, "pair").isEmpty(), "evidence is not a calculation");
        assertEquals(INVALID_INPUT, service.recordCalculation(run, "calc", 0).status());
        assertEquals(SUCCESS, service.recordCalculation(run, "calc", 1).status());
        assertEquals(SUCCESS, service.recordCalculation(run, "calc", 3).status());
        assertTrue(service.calculationLevel(run, "calc").isEmpty());
        assertEquals(ALREADY_OWNED, service.recordCalculation(run, "calc", 3).status());
        assertEquals(List.of(1, 3), state.research().get(run).calculations().get("calc"));
    }

    @Test
    void progressIsJudgedByTheCurrentDefinitions() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        service.learn(rock("minecraft:plains"), SOURCE, KnowledgeService.Grant.NORMAL);
        service.receive(rock("minecraft:forest"), SOURCE);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertEquals(SUCCESS, service.submitEvidence(run, "pair").status());
        service.recordCalculation(run, "calc", 1);
        service.recordCalculation(run, "calc", 1);
        var requirements = service.requirements(state.research().get(run));
        assertTrue(requirements.stream().allMatch(r -> service.requirementMet(state.research().get(run), r)));

        // A datapack update asks for another object and harder calculations: the old progress no longer counts.
        install("test:ice", 2);
        var changed = service.requirements(state.research().get(run));
        assertTrue(changed.stream().noneMatch(r -> service.requirementMet(state.research().get(run), r)));
        assertEquals(OptionalInt.of(2), service.calculationLevel(run, "calc"));
    }

    @Test
    void finishingNeedsEveryRequirementAndProjectsWithoutRequirementsStayCommandOnly() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        service.learn(rock("minecraft:plains"), SOURCE, KnowledgeService.Grant.NORMAL);
        service.receive(rock("minecraft:forest"), SOURCE);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertEquals(List.of(REQUIREMENTS_UNMET), statuses(service.finishResearch(run)));
        service.submitEvidence(run, "pair");
        service.recordCalculation(run, "calc", 1);
        assertEquals(List.of(REQUIREMENTS_UNMET), statuses(service.finishResearch(run)));
        service.recordCalculation(run, "calc", 2);
        assertTrue(service.requirementsMet(run));
        assertEquals(List.of(SUCCESS), statuses(service.finishResearch(run)));
        assertTrue(service.isActive(KnowledgeKey.result(FINDING)));
        assertTrue(state.research().get(run).completed());
        assertEquals(List.of(NOT_FOUND), statuses(service.finishResearch(run)));

        UUID bare = service.startResearch(BARE, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertFalse(service.requirementsMet(bare));
        assertEquals(List.of(REQUIREMENTS_UNMET), statuses(service.finishResearch(bare)));
        assertEquals(List.of(ALREADY_OWNED), statuses(service.completeResearch(bare)), "the command path still completes it");
    }

    @Test
    void abandoningDropsProgressAndRunsRoundTripWithTheirProgress() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        service.learn(rock("minecraft:plains"), SOURCE, KnowledgeService.Grant.NORMAL);
        service.receive(rock("minecraft:forest"), SOURCE);
        UUID kept = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        service.submitEvidence(kept, "pair");
        service.recordCalculation(kept, "calc", 1);
        assertEquals(state.research(), KnowledgeState.read(state.write()).research());

        assertEquals(SUCCESS, service.abandonResearch(kept).status());
        var abandoned = state.research().get(kept);
        assertFalse(abandoned.open());
        assertTrue(abandoned.evidence().isEmpty() && abandoned.calculations().isEmpty());
        assertEquals(NOT_FOUND, service.abandonResearch(kept).status());
        assertEquals(state.research(), KnowledgeState.read(state.write()).research());
        assertEquals("abandoned", service.snapshot().getList("research", Tag.TAG_COMPOUND).getCompound(0).getString("status"));
    }

    @Test
    void experimentsCountFinishedRunsAndRoundTrip() {
        var experiment = ProjectRequirement.experiment("load", "Load test", 2, 40, List.of(), ProjectRequirement.Space.NONE);
        var finding = new ResultDefinition(new ResearchResult.Finding(FINDING, List.of()), "Finding", "", List.of(), true);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,
                Map.of(ORIGIN, new IdeaDefinition("Origin", "", List.of(), List.of(PROJECT), true)), Map.of(FINDING, finding), Map.of(), Map.of(),
                Map.of(PROJECT, new ProjectDefinition("Survey", "", List.of(FINDING), List.of(experiment))), List.of()));
        var state = new KnowledgeState();
        var service = service(state);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        assertTrue(service.experimentRequirement(run, "load").isPresent());
        assertTrue(service.experimentRequirement(run, "missing").isEmpty());
        assertEquals(SUCCESS, service.recordExperiment(run, "load").status());
        assertFalse(service.requirementsMet(run));
        assertEquals(SUCCESS, service.recordExperiment(run, "load").status());
        assertTrue(service.requirementsMet(run));
        assertTrue(service.experimentRequirement(run, "load").isEmpty(), "a satisfied experiment needs no table");
        assertEquals(ALREADY_OWNED, service.recordExperiment(run, "load").status());
        assertEquals(Map.of("load", 2), state.research().get(run).experiments());
        assertEquals(state.research(), KnowledgeState.read(state.write()).research());
        var progress = service.snapshot().getList("research", Tag.TAG_COMPOUND).getCompound(0).getList("requirements", Tag.TAG_COMPOUND).getCompound(0);
        assertEquals(List.of("experiment", "2", "40"), List.of(progress.getString("type"), String.valueOf(progress.getInt("done")), String.valueOf(progress.getInt("duration"))));
        assertEquals(SUCCESS, service.abandonResearch(run).status());
        assertTrue(state.research().get(run).experiments().isEmpty());
    }

    @Test
    void openRunsShowProgressAndAvailableRecordsButNoSlotConditions() {
        install("test:rock", 1);
        var state = new KnowledgeState();
        var service = service(state);
        service.learn(rock("minecraft:plains"), SOURCE, KnowledgeService.Grant.NORMAL);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        var view = service.snapshot();
        var project = view.getList("projects", Tag.TAG_COMPOUND).stream().map(t -> (net.minecraft.nbt.CompoundTag) t)
                .filter(t -> t.getString("id").equals(PROJECT.toString())).findFirst().orElseThrow();
        assertEquals(run.toString(), project.getString("open_run"));
        assertEquals(2, project.getList("requirements", Tag.TAG_COMPOUND).size());
        var evidence = view.getList("research", Tag.TAG_COMPOUND).getCompound(0).getList("requirements", Tag.TAG_COMPOUND).getCompound(0);
        assertEquals("evidence", evidence.getString("type"));
        assertFalse(evidence.getBoolean("ready"), "a single biome cannot satisfy the distinct pair");
        assertEquals(List.of(1, 1), evidence.getList("slots", Tag.TAG_INT).stream().map(t -> ((net.minecraft.nbt.IntTag) t).getAsInt()).toList());
        for (var requirement : List.of(evidence, project.getList("requirements", Tag.TAG_COMPOUND).getCompound(0)))
            assertFalse(requirement.contains("inputs") || requirement.contains("conditions") || requirement.contains("distinct"),
                    "slot conditions stay on the server");
    }

    static List<KnowledgeService.Status> statuses(List<KnowledgeService.OperationResult> results) {
        return results.stream().map(KnowledgeService.OperationResult::status).toList();
    }
}
