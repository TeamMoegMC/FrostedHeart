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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Category projection that preserves prerequisite context without changing layout coordinates. */
public record ResearchGraphProjection(
        Set<String> primaryNodeIds,
        Set<String> contextNodeIds,
        Set<String> visibleNodeIds,
        List<ResearchGraphEdge> edges) {

    public ResearchGraphProjection {
        primaryNodeIds = immutableOrdered(primaryNodeIds);
        contextNodeIds = immutableOrdered(contextNodeIds);
        visibleNodeIds = immutableOrdered(visibleNodeIds);
        edges = List.copyOf(edges);
    }

    public static ResearchGraphProjection forResearchType(
            ResearchGraphSnapshot snapshot,
            String researchTypeId) {
        String normalizedType = ResearchTypeIdNormalizer.normalize(researchTypeId);
        TreeSet<String> primary = new TreeSet<>();
        for (ResearchGraphNode node : snapshot.nodes().values()) {
            if (node.hidden()) {
                continue;
            }
            if (ResearchTypeIdNormalizer.ALL_TYPES.equals(normalizedType)
                    || node.researchTypeId().equals(normalizedType)) {
                primary.add(node.id());
            }
        }

        TreeSet<String> context = new TreeSet<>();
        if (!ResearchTypeIdNormalizer.ALL_TYPES.equals(normalizedType)) {
            ArrayDeque<String> pending = new ArrayDeque<>(primary);
            while (!pending.isEmpty()) {
                ResearchGraphNode node = snapshot.nodes().get(pending.removeFirst());
                if (node == null) {
                    continue;
                }
                for (String parentId : node.parentIds()) {
                    ResearchGraphNode parent = snapshot.nodes().get(parentId);
                    if (parent != null && !parent.hidden()
                            && !primary.contains(parentId) && context.add(parentId)) {
                        pending.addLast(parentId);
                    }
                }
            }
        }

        TreeSet<String> visible = new TreeSet<>(primary);
        visible.addAll(context);
        List<ResearchGraphEdge> edges = snapshot.edges().stream()
                .filter(edge -> visible.contains(edge.parentId()) && visible.contains(edge.childId()))
                .toList();
        return new ResearchGraphProjection(primary, context, visible, edges);
    }

    private static Set<String> immutableOrdered(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(new TreeSet<>(values)));
    }
}
