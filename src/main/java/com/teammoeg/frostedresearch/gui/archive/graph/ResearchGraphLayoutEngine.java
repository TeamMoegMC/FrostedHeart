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

import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout.Bounds;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout.NodePosition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic layered layout with SCC cycle handling and display-only manual anchors. */
public final class ResearchGraphLayoutEngine {
    public static final double NODE_WIDTH = 144.0D;
    public static final double NODE_HEIGHT = 52.0D;
    public static final double RANK_SPACING = 176.0D;
    public static final double LANE_SPACING = 72.0D;
    public static final double BOUNDS_PADDING = 24.0D;
    private static final int BARYCENTRIC_SWEEPS = 4;

    public ResearchGraphLayout layout(ResearchGraphSnapshot snapshot) {
        TreeMap<String, ResearchGraphNode> nodes = new TreeMap<>();
        for (ResearchGraphNode node : snapshot.nodes().values()) {
            if (!node.hidden()) {
                nodes.put(node.id(), node);
            }
        }
        if (nodes.isEmpty()) {
            return new ResearchGraphLayout(Map.of(), Bounds.EMPTY, List.of());
        }

        List<ResearchGraphDiagnostic> diagnostics = new ArrayList<>();
        Map<String, TreeSet<String>> outgoing = new TreeMap<>();
        Map<String, TreeSet<String>> incoming = new TreeMap<>();
        nodes.keySet().forEach(id -> {
            outgoing.put(id, new TreeSet<>());
            incoming.put(id, new TreeSet<>());
        });

        for (ResearchGraphNode node : nodes.values()) {
            for (String parentId : node.parentIds()) {
                ResearchGraphNode parent = nodes.get(parentId);
                if (parent == null) {
                    if (!snapshot.nodes().containsKey(parentId)) {
                        diagnostics.add(ResearchGraphDiagnostic.missingParent(node.id(), parentId));
                    }
                    continue;
                }
                outgoing.get(parentId).add(node.id());
                incoming.get(node.id()).add(parentId);
            }
        }

        List<Component> components = stronglyConnectedComponents(outgoing);
        Map<String, Component> componentByNode = new HashMap<>();
        for (Component component : components) {
            component.nodeIds().forEach(id -> componentByNode.put(id, component));
            if (component.nodeIds().size() > 1
                    || outgoing.get(component.nodeIds().get(0)).contains(component.nodeIds().get(0))) {
                diagnostics.add(ResearchGraphDiagnostic.cycle(component.nodeIds()));
            }
        }

        Map<Component, TreeSet<Component>> componentOutgoing = new HashMap<>();
        Map<Component, TreeSet<Component>> componentIncoming = new HashMap<>();
        Comparator<Component> componentOrder = Comparator.comparing(Component::key);
        for (Component component : components) {
            componentOutgoing.put(component, new TreeSet<>(componentOrder));
            componentIncoming.put(component, new TreeSet<>(componentOrder));
        }
        for (Map.Entry<String, TreeSet<String>> entry : outgoing.entrySet()) {
            Component parentComponent = componentByNode.get(entry.getKey());
            for (String childId : entry.getValue()) {
                Component childComponent = componentByNode.get(childId);
                if (parentComponent != childComponent) {
                    componentOutgoing.get(parentComponent).add(childComponent);
                    componentIncoming.get(childComponent).add(parentComponent);
                }
            }
        }

        Map<Component, Integer> ranks = assignRanks(
                components,
                componentOutgoing,
                componentIncoming,
                componentOrder);
        Map<Integer, List<Component>> orderedRanks = createInitialOrder(nodes, components, ranks);
        reduceCrossings(orderedRanks, ranks, componentIncoming, componentOutgoing);

        Map<String, CandidatePosition> candidates = createCandidatePositions(orderedRanks, ranks);
        Map<String, NodePosition> positions = applyManualAnchorsAndAvoidCollisions(
                nodes,
                candidates,
                diagnostics);
        Bounds bounds = calculateBounds(positions.values());
        return new ResearchGraphLayout(positions, bounds, diagnostics);
    }

    private static Map<Component, Integer> assignRanks(
            List<Component> components,
            Map<Component, TreeSet<Component>> outgoing,
            Map<Component, TreeSet<Component>> incoming,
            Comparator<Component> componentOrder) {
        Map<Component, Integer> indegrees = new HashMap<>();
        Map<Component, Integer> ranks = new HashMap<>();
        PriorityQueue<Component> ready = new PriorityQueue<>(componentOrder);
        for (Component component : components) {
            int indegree = incoming.get(component).size();
            indegrees.put(component, indegree);
            ranks.put(component, 0);
            if (indegree == 0) {
                ready.add(component);
            }
        }
        while (!ready.isEmpty()) {
            Component component = ready.remove();
            int nextRank = ranks.get(component) + 1;
            for (Component child : outgoing.get(component)) {
                ranks.put(child, Math.max(ranks.get(child), nextRank));
                int remaining = indegrees.merge(child, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(child);
                }
            }
        }
        return ranks;
    }

    private static Map<Integer, List<Component>> createInitialOrder(
            Map<String, ResearchGraphNode> nodes,
            List<Component> components,
            Map<Component, Integer> ranks) {
        Map<Integer, List<Component>> byRank = new TreeMap<>();
        Comparator<Component> initialOrder = Comparator
                .comparing((Component component) -> component.nodeIds().stream()
                        .map(id -> nodes.get(id).researchTypeId())
                        .min(String::compareTo)
                        .orElse(""))
                .thenComparing(Component::key);
        for (Component component : components) {
            byRank.computeIfAbsent(ranks.get(component), ignored -> new ArrayList<>()).add(component);
        }
        byRank.values().forEach(componentsAtRank -> componentsAtRank.sort(initialOrder));
        return byRank;
    }

    private static void reduceCrossings(
            Map<Integer, List<Component>> orderedRanks,
            Map<Component, Integer> ranks,
            Map<Component, TreeSet<Component>> incoming,
            Map<Component, TreeSet<Component>> outgoing) {
        if (orderedRanks.size() < 2) {
            return;
        }
        List<Integer> rankIds = new ArrayList<>(orderedRanks.keySet());
        for (int sweep = 0; sweep < BARYCENTRIC_SWEEPS; sweep++) {
            Map<Component, Integer> indexes = indexes(orderedRanks);
            for (int i = 1; i < rankIds.size(); i++) {
                int rank = rankIds.get(i);
                sortByBarycenter(orderedRanks.get(rank), incoming, ranks, rank, indexes);
                indexes = indexes(orderedRanks);
            }
            indexes = indexes(orderedRanks);
            for (int i = rankIds.size() - 2; i >= 0; i--) {
                int rank = rankIds.get(i);
                sortByBarycenter(orderedRanks.get(rank), outgoing, ranks, rank, indexes);
                indexes = indexes(orderedRanks);
            }
        }
    }

    private static void sortByBarycenter(
            List<Component> components,
            Map<Component, TreeSet<Component>> neighbors,
            Map<Component, Integer> ranks,
            int currentRank,
            Map<Component, Integer> indexes) {
        Map<Component, Double> barycenters = new HashMap<>();
        Map<Component, Integer> previousIndexes = new HashMap<>();
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            previousIndexes.put(component, i);
            double total = 0.0D;
            int count = 0;
            for (Component neighbor : neighbors.get(component)) {
                if (ranks.get(neighbor) != currentRank) {
                    total += indexes.getOrDefault(neighbor, 0);
                    count++;
                }
            }
            barycenters.put(component, count == 0 ? i : total / count);
        }
        components.sort(Comparator
                .comparingDouble((Component component) -> barycenters.get(component))
                .thenComparingInt(previousIndexes::get)
                .thenComparing(Component::key));
    }

    private static Map<Component, Integer> indexes(Map<Integer, List<Component>> orderedRanks) {
        Map<Component, Integer> indexes = new HashMap<>();
        for (List<Component> components : orderedRanks.values()) {
            for (int i = 0; i < components.size(); i++) {
                indexes.put(components.get(i), i);
            }
        }
        return indexes;
    }

    private static Map<String, CandidatePosition> createCandidatePositions(
            Map<Integer, List<Component>> orderedRanks,
            Map<Component, Integer> ranks) {
        Map<String, CandidatePosition> candidates = new HashMap<>();
        for (Map.Entry<Integer, List<Component>> rankEntry : orderedRanks.entrySet()) {
            int lane = 0;
            for (Component component : rankEntry.getValue()) {
                for (String nodeId : component.nodeIds()) {
                    candidates.put(nodeId, new CandidatePosition(
                            rankEntry.getKey() * RANK_SPACING,
                            lane++ * LANE_SPACING,
                            ranks.get(component)));
                }
            }
        }
        return candidates;
    }

    private static Map<String, NodePosition> applyManualAnchorsAndAvoidCollisions(
            Map<String, ResearchGraphNode> nodes,
            Map<String, CandidatePosition> candidates,
            List<ResearchGraphDiagnostic> diagnostics) {
        List<PlacedNode> manualNodes = new ArrayList<>();
        for (ResearchGraphNode node : nodes.values()) {
            if (node.layoutHint().isManual()) {
                manualNodes.add(new PlacedNode(node.id(), node.layoutHint().x(), node.layoutHint().y()));
            }
        }
        for (int i = 0; i < manualNodes.size(); i++) {
            for (int j = i + 1; j < manualNodes.size(); j++) {
                if (overlaps(manualNodes.get(i), manualNodes.get(j))) {
                    diagnostics.add(ResearchGraphDiagnostic.manualOverlap(
                            manualNodes.get(i).id(),
                            manualNodes.get(j).id()));
                }
            }
        }

        Map<String, NodePosition> positions = new LinkedHashMap<>();
        List<PlacedNode> occupied = new ArrayList<>(manualNodes);
        for (PlacedNode manual : manualNodes) {
            CandidatePosition candidate = candidates.get(manual.id());
            positions.put(manual.id(), new NodePosition(
                    manual.x(),
                    manual.y(),
                    candidate.rank(),
                    true));
        }

        List<String> automaticIds = nodes.values().stream()
                .filter(node -> !node.layoutHint().isManual())
                .map(ResearchGraphNode::id)
                .sorted(Comparator
                        .comparingInt((String id) -> candidates.get(id).rank())
                        .thenComparingDouble(id -> candidates.get(id).y())
                        .thenComparing(id -> id))
                .toList();
        for (String id : automaticIds) {
            CandidatePosition candidate = candidates.get(id);
            PlacedNode placed = new PlacedNode(id, candidate.x(), candidate.y());
            while (collidesWithAny(placed, occupied)) {
                placed = new PlacedNode(id, placed.x(), placed.y() + LANE_SPACING);
            }
            occupied.add(placed);
            positions.put(id, new NodePosition(placed.x(), placed.y(), candidate.rank(), false));
        }

        LinkedHashMap<String, NodePosition> sortedPositions = new LinkedHashMap<>();
        nodes.keySet().forEach(id -> sortedPositions.put(id, positions.get(id)));
        return sortedPositions;
    }

    private static boolean collidesWithAny(PlacedNode node, List<PlacedNode> occupied) {
        for (PlacedNode other : occupied) {
            if (overlaps(node, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(PlacedNode first, PlacedNode second) {
        return Math.abs(first.x() - second.x()) < NODE_WIDTH
                && Math.abs(first.y() - second.y()) < NODE_HEIGHT;
    }

    private static Bounds calculateBounds(Collection<NodePosition> positions) {
        if (positions.isEmpty()) {
            return Bounds.EMPTY;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (NodePosition position : positions) {
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            maxX = Math.max(maxX, position.x() + NODE_WIDTH);
            maxY = Math.max(maxY, position.y() + NODE_HEIGHT);
        }
        return new Bounds(
                minX - BOUNDS_PADDING,
                minY - BOUNDS_PADDING,
                maxX + BOUNDS_PADDING,
                maxY + BOUNDS_PADDING);
    }

    private static List<Component> stronglyConnectedComponents(Map<String, TreeSet<String>> outgoing) {
        Tarjan tarjan = new Tarjan(outgoing);
        for (String id : outgoing.keySet()) {
            if (!tarjan.indexes.containsKey(id)) {
                tarjan.visit(id);
            }
        }
        tarjan.components.sort(Comparator.comparing(Component::key));
        return tarjan.components;
    }

    private record Component(String key, List<String> nodeIds) {
        private Component(List<String> nodeIds) {
            this(nodeIds.get(0), List.copyOf(nodeIds));
        }
    }

    private record CandidatePosition(double x, double y, int rank) {
    }

    private record PlacedNode(String id, double x, double y) {
    }

    private static final class Tarjan {
        private final Map<String, TreeSet<String>> outgoing;
        private final Map<String, Integer> indexes = new HashMap<>();
        private final Map<String, Integer> lowLinks = new HashMap<>();
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> onStack = new HashSet<>();
        private final List<Component> components = new ArrayList<>();
        private int nextIndex;

        private Tarjan(Map<String, TreeSet<String>> outgoing) {
            this.outgoing = outgoing;
        }

        private void visit(String nodeId) {
            indexes.put(nodeId, nextIndex);
            lowLinks.put(nodeId, nextIndex);
            nextIndex++;
            stack.push(nodeId);
            onStack.add(nodeId);

            for (String childId : outgoing.get(nodeId)) {
                if (!indexes.containsKey(childId)) {
                    visit(childId);
                    lowLinks.put(nodeId, Math.min(lowLinks.get(nodeId), lowLinks.get(childId)));
                } else if (onStack.contains(childId)) {
                    lowLinks.put(nodeId, Math.min(lowLinks.get(nodeId), indexes.get(childId)));
                }
            }

            if (lowLinks.get(nodeId).equals(indexes.get(nodeId))) {
                TreeSet<String> component = new TreeSet<>();
                String member;
                do {
                    member = stack.pop();
                    onStack.remove(member);
                    component.add(member);
                } while (!nodeId.equals(member));
                components.add(new Component(List.copyOf(component)));
            }
        }
    }
}
