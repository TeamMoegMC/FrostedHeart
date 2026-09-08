package com.teammoeg.frostedresearch.knowledge.link;

import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LinkMatcherTest {
    static final ResourceLocation IDEA = id("idea");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }

    static Observation observation(Map<String, ObservationValue> values) {
        return new Observation(UUID.randomUUID(), Observation.Type.ENTITY, id("bird"), values, Observation.Source.command(), Optional.empty());
    }

    static LinkRule.Slot observationSlot(String name, FieldCondition... conditions) {
        return new LinkRule.Slot(name, KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.ENTITY), List.of(conditions));
    }

    static FieldCondition field(String field, FieldCondition.Operator operator, ObservationValue value) {
        return new FieldCondition(field, operator, Optional.ofNullable(value), Optional.empty());
    }

    static LinkRule rule(List<LinkRule.Slot> inputs, LinkRule.CrossCondition... conditions) {
        return new LinkRule(inputs, List.of(conditions), IDEA);
    }

    static List<LinkMatcher.Match> match(List<KnowledgeElement> elements, LinkRule rule) {
        return LinkMatcher.match(elements, Map.of(id("rule"), rule), TagLookup.NONE);
    }

    @Test
    void matchesUnorderedCompleteSelectionAndReturnsEveryRule() {
        var a = KnowledgeElement.observation(observation(Map.of("temperature", ObservationValue.known(-30))));
        var b = KnowledgeElement.idea(id("foundation"));
        LinkRule r = rule(List.of(observationSlot("cold", field("temperature", FieldCondition.Operator.LT, ObservationValue.known(0))),
                new LinkRule.Slot("idea", KnowledgeKey.Kind.IDEA, Optional.of(id("foundation")), Optional.empty(), List.of())));
        assertEquals(match(List.of(a, b), r), match(List.of(b, a), r));
        assertEquals(a.key(), match(List.of(b, a), r).get(0).bindings().get("cold"));
        assertEquals(2, LinkMatcher.match(List.of(b, a), Map.of(id("one"), r, id("two"), r), TagLookup.NONE).size());
        assertTrue(match(List.of(a), r).isEmpty());
        assertTrue(match(List.of(a, b, KnowledgeElement.result(id("extra"))), r).isEmpty());
    }

    @Test
    void copiesCannotFillIndependentSlotsAndBacktrackingFindsViableBinding() {
        var unknown = KnowledgeElement.observation(observation(Map.of()));
        var known = KnowledgeElement.observation(observation(Map.of("temperature", ObservationValue.known(-5))));
        LinkRule repeated = rule(List.of(observationSlot("a"), observationSlot("b")));
        assertTrue(match(List.of(known, KnowledgeElement.observation(known.observation().orElseThrow())), repeated).isEmpty());
        assertEquals(1, match(List.of(unknown, known), repeated).size());
        LinkRule constrained = rule(List.of(observationSlot("any"), observationSlot("cold", field("temperature", FieldCondition.Operator.EXISTS, null))));
        assertEquals(known.key(), match(List.of(known, unknown), constrained).get(0).bindings().get("cold"));
    }

    @Test
    void crossSourceIsExplicitAndUnknownDoesNotMeanDifferent() {
        var a = observation(Map.of());
        var b = observation(Map.of());
        var cross = new LinkRule.CrossCondition(LinkRule.CrossCondition.Kind.COMPARE, "a", "original_observer", "b", "", FieldCondition.Operator.NE, Optional.empty());
        var r = rule(List.of(observationSlot("a"), observationSlot("b")), cross);
        assertTrue(match(List.of(KnowledgeElement.observation(a), KnowledgeElement.observation(b)), r).isEmpty());
        var player = new Observation.Source("player", Optional.of(UUID.randomUUID()), Optional.empty(), Optional.empty(), Optional.empty());
        var npc = new Observation.Source("npc", Optional.of(UUID.randomUUID()), Optional.empty(), Optional.empty(), Optional.empty());
        a = new Observation(a.recordId(), a.type(), a.object(), a.values(), player, Optional.empty());
        b = new Observation(b.recordId(), b.type(), b.object(), b.values(), npc, Optional.empty());
        assertEquals(1, match(List.of(KnowledgeElement.observation(a), KnowledgeElement.observation(b)), r).size());
    }

    @Test
    void distanceRequiresKnownCoordinatesInSameDimension() {
        var r = rule(List.of(observationSlot("a"), observationSlot("b")), new LinkRule.CrossCondition(LinkRule.CrossCondition.Kind.DISTANCE,
                "a", "", "b", "", FieldCondition.Operator.GTE, Optional.of(1000.0)));
        var origin = at("minecraft:overworld", 0);
        assertEquals(1, match(List.of(origin, at("minecraft:overworld", 1000)), r).size());
        assertTrue(match(List.of(origin, at("minecraft:overworld", 999)), r).isEmpty());
        assertTrue(match(List.of(origin, at("minecraft:the_nether", 1000)), r).isEmpty());
        assertTrue(match(List.of(origin, KnowledgeElement.observation(observation(Map.of()))), r).isEmpty());
    }

    private static KnowledgeElement at(String dimension, double x) {
        return KnowledgeElement.observation(observation(Map.of("dimension", ObservationValue.known(dimension), "x", ObservationValue.known(x), "y", ObservationValue.known(0), "z", ObservationValue.known(0))));
    }

    @Test
    void tagsAndExplicitMissingStatesHaveSeparateSemantics() {
        var o = observation(Map.of());
        assertTrue(field("temperature", FieldCondition.Operator.UNKNOWN, null).matches(o, TagLookup.NONE));
        assertFalse(field("temperature", FieldCondition.Operator.NE, ObservationValue.known(0)).matches(o, TagLookup.NONE));
        assertTrue(FieldCondition.compare(ObservationValue.known(-0.0), ObservationValue.known(0.0), FieldCondition.Operator.EQ));
        var item = new Observation(UUID.randomUUID(), Observation.Type.ITEM, id("bird"), Map.of(), Observation.Source.command(), Optional.empty());
        assertTrue(field("temperature", FieldCondition.Operator.NOT_APPLICABLE, null).matches(item, TagLookup.NONE));
        assertFalse(field("temperature", FieldCondition.Operator.UNKNOWN, null).matches(item, TagLookup.NONE));
        var tagged = new FieldCondition("object", FieldCondition.Operator.TAG, Optional.empty(), Optional.of(id("birds")));
        assertTrue(tagged.matches(o, (type, field, value, tag) -> type == Observation.Type.ENTITY && value.equals(id("bird")) && tag.equals(id("birds"))));
    }

    @Test
    void rejectsMistypedFieldsUnknownSlotsAndWrongCardinality() {
        assertFalse(rule(List.of(observationSlot("a"))).validate().isEmpty());
        assertFalse(rule(List.of(observationSlot("a"), observationSlot("a"))).validate().isEmpty());
        assertFalse(rule(List.of(observationSlot("a", field("temperature", FieldCondition.Operator.EQ, ObservationValue.known("cold"))), observationSlot("b"))).validate().isEmpty());
        assertFalse(rule(List.of(observationSlot("a", field("source_position.w", FieldCondition.Operator.EXISTS, null)), observationSlot("b"))).validate().isEmpty());
        assertFalse(rule(List.of(observationSlot("a"), observationSlot("b")), new LinkRule.CrossCondition(LinkRule.CrossCondition.Kind.COMPARE,
                "a", "source", "missing", "", FieldCondition.Operator.EQ, Optional.empty())).validate().isEmpty());
    }

    @Test
    void candidateSearchFiltersArchiveAndHonorsLimit() {
        var r = rule(List.of(observationSlot("a"), observationSlot("b")));
        var a = KnowledgeElement.observation(observation(Map.of()));
        var b = KnowledgeElement.observation(observation(Map.of()));
        var archive = List.of(KnowledgeElement.idea(id("unrelated")), a, a, b);
        assertEquals(1, LinkMatcher.candidates(archive, Map.of(id("one"), r, id("two"), r), 1, TagLookup.NONE).size());
        assertEquals(2, LinkMatcher.candidates(archive, Map.of(id("one"), r, id("two"), r), 3, TagLookup.NONE).size());
        assertTrue(LinkMatcher.candidates(List.of(a, a), Map.of(id("one"), r), 3, TagLookup.NONE).isEmpty());
    }
}
