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
package com.teammoeg.frostedresearch.gui.archive.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
