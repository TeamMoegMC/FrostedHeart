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

package com.teammoeg.frostedresearch.gui.archive.graph;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/** Stable graph-definition diagnostic suitable for an author layout tool. */
public record ResearchGraphDiagnostic(Type type, List<String> nodeIds, @Nullable String referenceId) {
    public ResearchGraphDiagnostic {
        nodeIds = List.copyOf(new TreeSet<>(nodeIds));
    }

    public static ResearchGraphDiagnostic missingParent(String nodeId, String parentId) {
        return new ResearchGraphDiagnostic(Type.MISSING_PARENT, List.of(nodeId), parentId);
    }

    public static ResearchGraphDiagnostic cycle(Collection<String> nodeIds) {
        return new ResearchGraphDiagnostic(Type.CYCLE, List.copyOf(nodeIds), null);
    }

    public static ResearchGraphDiagnostic manualOverlap(String firstId, String secondId) {
        return new ResearchGraphDiagnostic(Type.MANUAL_OVERLAP, List.of(firstId, secondId), null);
    }

    public enum Type {
        MISSING_PARENT,
        CYCLE,
        MANUAL_OVERLAP
    }
}
