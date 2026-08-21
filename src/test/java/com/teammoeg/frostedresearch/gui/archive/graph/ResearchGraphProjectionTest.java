/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchGraphProjectionTest {
    @Test
    void typeAliasesShareOneProjectionAndKeepVisiblePrerequisitesAsContext() {
        ResearchGraphSnapshot snapshot = ResearchGraphSnapshot.of(List.of(
                node("root", "frostedresearch:production", false),
                node("child", "frostedheart:living", false, "root"),
                node("hidden", "frostedresearch:living", true, "root"),
                node("behind-hidden", "frostedresearch:living", false, "hidden")), 1L);

        ResearchGraphProjection projection =
                ResearchGraphProjection.forResearchType(snapshot, "frostedheart:living");

        assertEquals(Set.of("child", "behind-hidden"), projection.primaryNodeIds());
        assertEquals(Set.of("root"), projection.contextNodeIds());
        assertFalse(projection.visibleNodeIds().contains("hidden"));
        assertTrue(projection.edges().contains(new ResearchGraphEdge("root", "child")));
        assertFalse(projection.edges().contains(new ResearchGraphEdge("hidden", "behind-hidden")));
    }

    @Test
    void allProjectionNeverLeaksHiddenNodes() {
        ResearchGraphSnapshot snapshot = ResearchGraphSnapshot.of(List.of(
                node("visible", "rescue", false),
                node("hidden", "rescue", true, "visible")), 1L);

        ResearchGraphProjection projection = ResearchGraphProjection.forResearchType(snapshot, "*");

        assertEquals(Set.of("visible"), projection.visibleNodeIds());
        assertTrue(projection.edges().isEmpty());
    }

    private static ResearchGraphNode node(String id, String type, boolean hidden, String... parents) {
        return ResearchGraphNode.automatic(id, type, List.of(parents), hidden);
    }
}
