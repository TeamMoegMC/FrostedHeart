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

package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.knowledge.link.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeDefinitionsTest {
    static final ResourceLocation IDEA = new ResourceLocation("test", "idea");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void reset() {
        KnowledgeDefinitions.clearForTests();
    }

    static Observation observation(double x) {
        return new Observation(UUID.randomUUID(), Observation.Type.ENTITY, new ResourceLocation("test", "bird"),
                Map.of("dimension", ObservationValue.known("minecraft:overworld"), "x", ObservationValue.known(x), "y", ObservationValue.known(0), "z", ObservationValue.known(0)), Observation.Source.command(), Optional.empty());
    }

    static LinkRule rule(boolean distance) {
        var a = new LinkRule.Slot("a", KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ENTITY), List.of());
        var b = new LinkRule.Slot("b", KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ENTITY), List.of());
        return new LinkRule(List.of(a, b), distance ? List.of(new LinkRule.CrossCondition(LinkRule.CrossCondition.Kind.DISTANCE,
                "a", "", "b", "", FieldCondition.Operator.GTE, Optional.of(1000.0))) : List.of(), IDEA);
    }

    static KnowledgeDefinitions.Snapshot snapshot(Map<ResourceLocation, LinkRule> links) {
        return new KnowledgeDefinitions.Snapshot(1, Map.of(IDEA, new IdeaDefinition("Birds", "", List.of(), List.of(), true)), Map.of(), links, Map.of(), Map.of(), List.of());
    }

    @Test
    void multiplicityAndCrossFieldsArePreservedByObservationEquivalence() {
        var repeated = snapshot(Map.of(IDEA, rule(false)));
        var one = observation(0);
        var two = observation(0);
        var three = observation(0);
        assertTrue(repeated.hasObservationUse(one, TagLookup.NONE));
        assertTrue(repeated.addsObservationDifference(two, List.of(one), TagLookup.NONE));
        assertFalse(repeated.addsObservationDifference(three, List.of(one, two), TagLookup.NONE));
        assertFalse(repeated.addsObservationDifference(one, List.of(one), TagLookup.NONE));
        var spaced = snapshot(Map.of(IDEA, rule(true)));
        assertTrue(spaced.addsObservationDifference(observation(1000), List.of(one, two), TagLookup.NONE));
    }

    @Test
    void unknownExtensionEquivalencePreservesRecordsAndRegisteredCountIsRespected() {
        KnowledgeDefinitions.install(snapshot(Map.of()));
        KnowledgeDefinitions.registerObservationUse(IDEA, o -> true);
        var one = observation(0);
        var two = observation(0);
        var three = observation(0);
        assertTrue(KnowledgeDefinitions.current().addsObservationDifference(three, List.of(one, two)));
        KnowledgeDefinitions.registerObservationUse(IDEA, new ObservationUse() {
            public boolean matches(Observation o) {
                return true;
            }

            public Optional<Boolean> equivalent(Observation a, Observation b) {
                return Optional.of(true);
            }

            public int independentRecords() {
                return 3;
            }
        });
        assertTrue(KnowledgeDefinitions.current().addsObservationDifference(three, List.of(one, two)));
        assertFalse(KnowledgeDefinitions.current().addsObservationDifference(observation(0), List.of(one, two, three)));
    }

    @Test
    void observationAndCopiedItemNbtRoundTripWithoutAliasing() {
        CompoundTag data = new CompoundTag();
        data.putString("material", "iron");
        Observation o = new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft", "iron_ingot"),
                Map.of("item.material", ObservationValue.known("iron")), Observation.Source.command(), Optional.of(data));
        data.putString("material", "gold");
        o.itemData().orElseThrow().putString("material", "copper");
        assertEquals("iron", o.itemData().orElseThrow().getString("material"));
        var element = KnowledgeElement.observation(o);
        var json = KnowledgeElement.CODEC.encodeStart(JsonOps.INSTANCE, element).getOrThrow(false, m -> fail(m));
        assertEquals(element, KnowledgeElement.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, m -> fail(m)));
        assertEquals(ObservationValue.State.NOT_APPLICABLE, o.value("temperature").state());
        assertFalse(o.canExportNote());
    }

    @Test
    void removingProducingPathLeavesDefinitionValid() {
        assertTrue(KnowledgeDefinitions.validate(snapshot(Map.of())).isEmpty());
        var bad = snapshot(Map.of(IDEA, new LinkRule(rule(false).inputs(), List.of(), new ResourceLocation("test", "missing"))));
        assertTrue(KnowledgeDefinitions.validate(bad).stream().anyMatch(s -> s.contains("missing output idea")));
    }

    static LinkRule.Slot rockSlot(String name) {
        return new LinkRule.Slot(name, KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK),
                List.of(new FieldCondition("object", FieldCondition.Operator.EQ, Optional.of(ObservationValue.known("test:rock")), Optional.empty())));
    }

    static KnowledgeDefinitions.Snapshot project(List<ProjectRequirement> requirements) {
        ResourceLocation project = new ResourceLocation("test", "project"), finding = new ResourceLocation("test", "finding");
        var result = new ResultDefinition(new com.teammoeg.frostedresearch.knowledge.ResearchResult.Finding(finding, List.of()), "Finding", "", List.of(), true);
        return new KnowledgeDefinitions.Snapshot(1, Map.of(IDEA, new IdeaDefinition("Birds", "", List.of(), List.of(project), true)),
                Map.of(finding, result), Map.of(), Map.of(), Map.of(project, new ProjectDefinition("Project", "", List.of(finding), requirements)), List.of());
    }

    @Test
    void projectRequirementsValidateTheirShape() {
        var pair = ProjectRequirement.evidence("pair", "Two rocks", List.of(rockSlot("a"), rockSlot("b")), List.of(),
                List.of(new ProjectRequirement.Distinct("biome", List.of("a", "b"))));
        assertEquals(List.of(), KnowledgeDefinitions.validate(project(List.of(pair, ProjectRequirement.calculation("calc", "", 3, 2)))));
        Map<String, ProjectRequirement> invalid = new LinkedHashMap<>();
        invalid.put("duplicate requirement id", ProjectRequirement.calculation("pair", "", 0, 1));
        invalid.put("calculation level", ProjectRequirement.calculation("deep", "", 4, 1));
        invalid.put("calculation count", ProjectRequirement.calculation("none", "", 0, 0));
        invalid.put("author-written title", ProjectRequirement.evidence("untitled", "", List.of(rockSlot("a")), List.of(), List.of()));
        invalid.put("must be observations", ProjectRequirement.evidence("idea", "Idea", List.of(new LinkRule.Slot("i", KnowledgeKey.Kind.IDEA,
                Optional.of(IDEA), Optional.empty(), List.of())), List.of(), List.of()));
        invalid.put("unknown slot", ProjectRequirement.evidence("lost", "Lost", List.of(rockSlot("a"), rockSlot("b")), List.of(),
                List.of(new ProjectRequirement.Distinct("biome", List.of("a", "c")))));
        invalid.put("two different slots", ProjectRequirement.evidence("single", "Single", List.of(rockSlot("a")), List.of(),
                List.of(new ProjectRequirement.Distinct("biome", List.of("a")))));
        invalid.put("1 to 8 slots", ProjectRequirement.evidence("crowd", "Crowd", java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> rockSlot("s" + i)).toList(), List.of(), List.of()));
        invalid.forEach((message, requirement) -> assertTrue(KnowledgeDefinitions.validate(project(List.of(pair, requirement))).stream()
                .anyMatch(error -> error.contains(message)), message));
    }

    @Test
    void evidenceSlotsAreObservationUsesAndRepeatedSlotsKeepIndependentRecords() {
        var pair = ProjectRequirement.evidence("pair", "Two rocks", List.of(rockSlot("a"), rockSlot("b")), List.of(), List.of());
        KnowledgeDefinitions.install(project(List.of(pair)));
        var snapshot = KnowledgeDefinitions.current();
        var first = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("test", "rock"), Map.of(), Observation.Source.command(), Optional.empty());
        var second = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("test", "rock"), Map.of(), Observation.Source.command(), Optional.empty());
        var third = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("test", "rock"), Map.of(), Observation.Source.command(), Optional.empty());
        assertTrue(snapshot.hasObservationUse(first, TagLookup.NONE));
        assertTrue(snapshot.addsObservationDifference(second, List.of(first), TagLookup.NONE), "two slots need two records");
        assertFalse(snapshot.addsObservationDifference(third, List.of(first, second), TagLookup.NONE));
    }

    @Test
    void documentedExampleDatapackLoadsAndItsItemCombinationProducesTheSharedIdea() {
        Path directory = Path.of("docs/knowledge/example-datapack");
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(new PathPackResources("knowledge-example", directory, false)))) {
            var result = KnowledgeDefinitions.reload(resources);
            assertTrue(result.applied(), result.diagnostics().toString());
            var loaded = KnowledgeDefinitions.current();
            assertEquals(9, loaded.ideas().size());
            assertEquals(12, loaded.links().size());
            assertEquals(17, loaded.results().size());
            var paper = new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft", "paper"), Map.of(), Observation.Source.command(), Optional.empty());
            var stick = new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft", "stick"), Map.of(), Observation.Source.command(), Optional.empty());
            var matches = LinkMatcher.match(List.of(KnowledgeElement.observation(stick), KnowledgeElement.observation(paper)), loaded.links(), TagLookup.NONE);
            assertEquals(1, matches.size());
            assertEquals(new ResourceLocation("knowledge_example", "materials"), matches.get(0).rule().output());
            assertTrue(loaded.hasObservationUse(paper, TagLookup.NONE));
            assertTrue(loaded.hasObservationUse(stick, TagLookup.NONE));
            var record = loaded.projects().get(new ResourceLocation("knowledge_example", "material_record"));
            assertEquals(List.of("log", "tabulate"), record.requirements().stream().map(ProjectRequirement::id).toList());
            var birch = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("minecraft", "birch_log"), Map.of(), Observation.Source.command(), Optional.empty());
            TagLookup logs = (type, field, value, tag) -> type == Observation.Type.BLOCK && tag.equals(new ResourceLocation("minecraft", "logs"));
            assertTrue(loaded.hasObservationUse(birch, logs), "evidence for a project makes an otherwise unused log useful");
        }
    }

    @Test
    void reloadCommitsWholeVersionAndRetainsLastValidSnapshotOnErrors(@TempDir Path directory) throws Exception {
        Path ideas = directory.resolve("data/test/frostedresearch/knowledge/ideas");
        Files.createDirectories(ideas);
        Files.writeString(ideas.resolve("idea.json"), "{\"title\":\"Birds\"}");
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(new PathPackResources("test", directory, false)))) {
            assertTrue(KnowledgeDefinitions.reload(resources).applied());
            var installed = KnowledgeDefinitions.current();
            Files.writeString(ideas.resolve("idea.json"), "{\"title\":\"Birds\",\"understanding\":[{\"kind\":\"idea\",\"id\":\"test:missing\"}]}");
            assertFalse(KnowledgeDefinitions.reload(resources).applied());
            assertSame(installed, KnowledgeDefinitions.current());
            Files.delete(ideas.resolve("idea.json"));
            assertTrue(KnowledgeDefinitions.reload(resources).applied());
            assertTrue(KnowledgeDefinitions.current().ideas().isEmpty());
            assertTrue(KnowledgeDefinitions.current().revision() > installed.revision());
        }
    }
}
