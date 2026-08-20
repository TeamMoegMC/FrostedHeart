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

import com.teammoeg.frostedresearch.research.Research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/** Stable, read-only copy of research dependency definitions. */
public final class ResearchGraphSnapshot {
    private final Map<String, ResearchGraphNode> nodes;
    private final List<ResearchGraphEdge> edges;
    private final long localRevision;

    private ResearchGraphSnapshot(
            Map<String, ResearchGraphNode> nodes,
            List<ResearchGraphEdge> edges,
            long localRevision) {
        this.nodes = Collections.unmodifiableMap(nodes);
        this.edges = List.copyOf(edges);
        this.localRevision = localRevision;
    }

    public static ResearchGraphSnapshot fromResearches(Collection<Research> researches, long localRevision) {
        List<ResearchGraphNode> nodes = new ArrayList<>(researches.size());
        for (Research research : researches) {
            if (research != null && research.getId() != null) {
                nodes.add(ResearchGraphNode.automatic(
                        research.getId(),
                        ResearchTypeIdNormalizer.normalize(research.getCategory()),
                        research.getParentIds(),
                        research.isHidden()));
            }
        }
        return of(nodes, localRevision);
    }

    public static ResearchGraphSnapshot of(Collection<ResearchGraphNode> inputNodes, long localRevision) {
        TreeMap<String, ResearchGraphNode> byId = new TreeMap<>();
        for (ResearchGraphNode node : inputNodes) {
            ResearchGraphNode previous = byId.put(node.id(), Objects.requireNonNull(node, "node"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate research id: " + node.id());
            }
        }

        Map<String, TreeSet<String>> childrenById = new HashMap<>();
        byId.keySet().forEach(id -> childrenById.put(id, new TreeSet<>()));
        List<ResearchGraphEdge> edges = new ArrayList<>();
        for (ResearchGraphNode node : byId.values()) {
            for (String parentId : node.parentIds()) {
                if (byId.containsKey(parentId)) {
                    childrenById.get(parentId).add(node.id());
                    edges.add(new ResearchGraphEdge(parentId, node.id()));
                }
            }
        }
        Collections.sort(edges);

        Map<String, ResearchGraphNode> completedNodes = new LinkedHashMap<>();
        for (ResearchGraphNode node : byId.values()) {
            completedNodes.put(node.id(), new ResearchGraphNode(
                    node.id(),
                    node.researchTypeId(),
                    node.parentIds(),
                    List.copyOf(childrenById.get(node.id())),
                    node.hidden(),
                    node.layoutHint()));
        }
        return new ResearchGraphSnapshot(completedNodes, edges, localRevision);
    }

    public Map<String, ResearchGraphNode> nodes() {
        return nodes;
    }

    public List<ResearchGraphEdge> edges() {
        return edges;
    }

    public long localRevision() {
        return localRevision;
    }
}
