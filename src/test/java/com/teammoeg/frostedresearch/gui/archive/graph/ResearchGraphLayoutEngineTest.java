/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchGraphLayoutEngineTest {
    private final ResearchGraphLayoutEngine engine = new ResearchGraphLayoutEngine();

    @Test
    void layoutIsIndependentOfRegistryIterationOrder() {
        List<ResearchGraphNode> nodes = List.of(
                node("root", "production"),
                node("a", "production", "root"),
                node("b", "living", "root"),
                node("merge", "living", "a", "b"),
                node("isolated", "rescue"));
        List<ResearchGraphNode> reversed = new ArrayList<>(nodes);
        Collections.reverse(reversed);

        ResearchGraphLayout first = engine.layout(ResearchGraphSnapshot.of(nodes, 1L));
        ResearchGraphLayout second = engine.layout(ResearchGraphSnapshot.of(reversed, 2L));

        assertEquals(first.positions(), second.positions());
        assertEquals(first.worldBounds(), second.worldBounds());
        assertEquals(0, first.positions().get("root").rank());
        assertEquals(2, first.positions().get("merge").rank());
    }

    @Test
    void cyclesAndMissingParentsProduceDiagnosticsWithoutBlockingLayout() {
        List<ResearchGraphNode> nodes = List.of(
                node("a", "production", "c"),
                node("b", "production", "a"),
                node("c", "production", "b"),
                node("self", "rescue", "self"),
                node("missing", "living", "unknown"));

        ResearchGraphLayout layout = engine.layout(ResearchGraphSnapshot.of(nodes, 1L));

        assertEquals(5, layout.positions().size());
        assertEquals(2, count(layout, ResearchGraphDiagnostic.Type.CYCLE));
        assertEquals(1, count(layout, ResearchGraphDiagnostic.Type.MISSING_PARENT));
        assertEquals("unknown", layout.diagnostics().stream()
                .filter(diagnostic -> diagnostic.type() == ResearchGraphDiagnostic.Type.MISSING_PARENT)
                .findFirst().orElseThrow().referenceId());
    }

    @Test
    void manualAnchorsRemainFixedAndAutomaticNodesMoveOutOfTheirBounds() {
        ResearchGraphNode manual = new ResearchGraphNode(
                "manual", "production", List.of(), List.of(), false, ResearchLayoutHint.manual(10.0D, 20.0D));
        ResearchGraphNode automatic = node("automatic", "production");

        ResearchGraphLayout layout = engine.layout(ResearchGraphSnapshot.of(List.of(manual, automatic), 1L));

        assertEquals(10.0D, layout.positions().get("manual").x());
        assertEquals(20.0D, layout.positions().get("manual").y());
        assertTrue(layout.positions().get("manual").manual());
        assertFalse(layout.positions().get("automatic").manual());
        assertNotEquals(0.0D, layout.positions().get("automatic").y());
    }

    @Test
    void conflictingManualAnchorsAreDiagnosedButNeverMoved() {
        ResearchGraphNode first = new ResearchGraphNode(
                "first", "production", List.of(), List.of(), false, ResearchLayoutHint.manual(10.0D, 20.0D));
        ResearchGraphNode second = new ResearchGraphNode(
                "second", "production", List.of(), List.of(), false, ResearchLayoutHint.manual(20.0D, 30.0D));

        ResearchGraphLayout layout = engine.layout(ResearchGraphSnapshot.of(List.of(second, first), 1L));

        assertEquals(1, count(layout, ResearchGraphDiagnostic.Type.MANUAL_OVERLAP));
        assertEquals(10.0D, layout.positions().get("first").x());
        assertEquals(20.0D, layout.positions().get("second").x());
    }

    private static long count(ResearchGraphLayout layout, ResearchGraphDiagnostic.Type type) {
        return layout.diagnostics().stream().filter(diagnostic -> diagnostic.type() == type).count();
    }

    private static ResearchGraphNode node(String id, String type, String... parents) {
        return ResearchGraphNode.automatic(id, type, List.of(parents), false);
    }
}
