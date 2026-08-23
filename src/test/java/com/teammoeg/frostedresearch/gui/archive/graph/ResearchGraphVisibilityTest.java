/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.archive.graph;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchGraphVisibilityTest {
    @Test
    void editorSnapshotIncludesHiddenNodesAndTheirEdges() {
        Research hiddenRoot = research("hidden-root", true);
        Research child = research("child", false);
        child.setParents("hidden-root");

        ResearchGraphSnapshot normal = ResearchGraphSnapshot.fromResearches(List.of(hiddenRoot, child), 1, false);
        ResearchGraphSnapshot editor = ResearchGraphSnapshot.fromResearches(List.of(hiddenRoot, child), 2, true);

        assertTrue(normal.nodes().get("hidden-root").hidden());
        assertFalse(editor.nodes().get("hidden-root").hidden());
        assertEquals(List.of(new ResearchGraphEdge("hidden-root", "child")), editor.edges());
        assertFalse(ResearchGraphProjection.forResearchType(normal, "*").visibleNodeIds().contains("hidden-root"));
        assertTrue(ResearchGraphProjection.forResearchType(editor, "*").visibleNodeIds().contains("hidden-root"));
    }

    private static Research research(String id, boolean hidden) {
        return new Research(
                id, "", 1, 1000, CIcons.nop(), ResearchCategory.RESCUE,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                false, false, false, false, hidden, false);
    }
}
