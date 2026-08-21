/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
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
