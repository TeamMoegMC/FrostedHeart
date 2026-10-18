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

/** Directed dependency edge from a prerequisite to its child research. */
public record ResearchGraphEdge(String parentId, String childId) implements Comparable<ResearchGraphEdge> {
    @Override
    public int compareTo(ResearchGraphEdge other) {
        int parentOrder = parentId.compareTo(other.parentId);
        return parentOrder != 0 ? parentOrder : childId.compareTo(other.childId);
    }
}
