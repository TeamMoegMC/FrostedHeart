/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Linear-time dependency layout. Stable cycle breaks leave explicit return edges.
 */
public final class KnowledgeGraphModel {
    public enum Kind {OBSERVATION, IDEA, RESULT, PROJECT, LINK}

    public record Node(String id, Kind kind, Component title, ItemStack icon, KnowledgeKey key,
                       KnowledgeJournalData.Entry entry, KnowledgeJournalData.Project project,
                       KnowledgeJournalData.Relation link, boolean active, boolean completed,
                       int x, int y, int width, int height) {
        public int centerX() {
            return x + width / 2;
        }

        public int centerY() {
            return y + height / 2;
        }
    }

    public record Edge(String from, String to, boolean planned) {
    }

    private record Origin(String project, KnowledgeKey idea) {
    }

    private record Seed(String id, Kind kind, Component title, ItemStack icon, KnowledgeKey key,
                        KnowledgeJournalData.Entry entry, KnowledgeJournalData.Project project,
                        KnowledgeJournalData.Relation link, boolean active, boolean completed) {
    }

    public final List<Node> nodes;
    public final List<Edge> edges;
    public final Map<String, Node> byId;
    public final int width, height;

    public KnowledgeGraphModel(KnowledgeJournalData data, boolean history) {
        Map<String, Seed> seeds = new LinkedHashMap<>();
        Set<Edge> edges = new LinkedHashSet<>();
        Map<String, Set<String>> flow = new LinkedHashMap<>();
        Set<Origin> actualOrigins = new HashSet<>();
        for (var relation : data.relations)
            if (relation.research())
                for (var input : relation.inputs()) actualOrigins.add(new Origin(relation.project(), input));
        for (var entry : data.archive) if (history || entry.active()) addKnowledge(seeds, data, entry.key(), history);
        for (var project : data.projects) {
            if (!history && !project.active()) continue;
            String id = projectId(project.id());
            seeds.put(id, new Seed(id, Kind.PROJECT, project.title(), new ItemStack(Items.WRITABLE_BOOK), null,
                    null, project, null, project.active(), project.completed()));
            for (KnowledgeKey origin : project.ideas()) {
                String from = addKnowledge(seeds, data, origin, history);
                if (from == null) continue;
                boolean actual = actualOrigins.contains(new Origin(project.id(), origin));
                edges.add(new Edge(from, id, !actual));
                connect(flow, from, id);
            }
        }
        for (var relation : data.relations) {
            if (!history && !relation.active()) continue;
            String joint = relation.research() ? projectId(relation.project().isBlank() ? relation.identity() : relation.project()) : "link:" + relation.identity();
            seeds.putIfAbsent(joint, new Seed(joint, relation.research() ? Kind.PROJECT : Kind.LINK,
                    relation.research() ? relation.title() : KnowledgePresentation.t("graph.connection"),
                    new ItemStack(relation.research() ? Items.WRITABLE_BOOK : Items.FEATHER), null, null, null,
                    relation, relation.active(), relation.research() && !relation.outputs().isEmpty()));
            for (KnowledgeKey input : relation.inputs()) {
                String from = addKnowledge(seeds, data, input, history);
                if (from == null) continue;
                edges.remove(new Edge(from, joint, true));
                edges.add(new Edge(from, joint, false));
                if (relation.research()) connect(flow, from, joint);
                else for (KnowledgeKey output : relation.outputs()) connect(flow, from, knowledgeId(output));
            }
            for (KnowledgeKey output : relation.outputs()) {
                String to = addKnowledge(seeds, data, output, history);
                if (to == null) continue;
                edges.add(new Edge(joint, to, false));
                if (relation.research()) connect(flow, joint, to);
            }
        }
        // Only ordinary nodes occupy columns; a link joint sits just before its resulting idea.
        List<String> ordinary = seeds.values().stream().filter(n -> n.kind != Kind.LINK).map(Seed::id).toList();
        Map<String, Integer> incoming = new HashMap<>();
        for (String id : ordinary) incoming.put(id, 0);
        flow.values().forEach(targets -> targets.forEach(id -> {
            if (incoming.containsKey(id)) incoming.merge(id, 1, Integer::sum);
        }));
        Map<String, Integer> depth = new HashMap<>();
        Set<String> placed = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (String id : ordinary) if (incoming.get(id) == 0) queue.add(id);
        expand(queue, flow, incoming, depth, placed);
        // A closed loop has no zero-indegree entry. Break it at its earliest stored node;
        // later edges back to already placed nodes remain visible return paths.
        for (String id : ordinary)
            if (!placed.contains(id)) {
                queue.add(id);
                expand(queue, flow, incoming, depth, placed);
            }
        Map<Integer, Integer> row = new HashMap<>();
        Map<String, Node> positioned = new LinkedHashMap<>();
        int maxX = 136, maxY = 48;
        for (String id : ordinary) {
            Seed seed = seeds.get(id);
            int column = depth.getOrDefault(id, 0), ordinal = row.getOrDefault(column, 0);
            row.put(column, ordinal + 1);
            Node node = node(seed, column * 188, ordinal * 82, 136, 48);
            positioned.put(id, node);
            maxX = Math.max(maxX, node.x + node.width);
            maxY = Math.max(maxY, node.y + node.height);
        }
        Map<String, Integer> alternatives = new HashMap<>();
        for (Seed seed : seeds.values())
            if (seed.kind == Kind.LINK && seed.link != null) {
                Node output = seed.link.outputs().isEmpty() ? null : positioned.get(knowledgeId(seed.link.outputs().get(0)));
                if (output == null) continue;
                int alternate = alternatives.getOrDefault(output.id, 0);
                alternatives.put(output.id, alternate + 1);
                Node joint = node(seed, output.x - 31, output.centerY() - 6 + alternate * 17, 12, 12);
                positioned.put(seed.id, joint);
                maxY = Math.max(maxY, joint.y + joint.height);
            }
        this.nodes = List.copyOf(positioned.values());
        this.byId = Map.copyOf(positioned);
        this.edges = edges.stream().filter(e -> positioned.containsKey(e.from) && positioned.containsKey(e.to)).toList();
        width = maxX;
        height = maxY + 20;
    }

    private static void expand(ArrayDeque<String> queue, Map<String, Set<String>> flow,
                               Map<String, Integer> incoming, Map<String, Integer> depth, Set<String> placed) {
        while (!queue.isEmpty()) {
            String from = queue.removeFirst();
            if (!placed.add(from)) continue;
            for (String to : flow.getOrDefault(from, Set.of())) {
                if (!incoming.containsKey(to) || placed.contains(to)) continue;
                depth.merge(to, depth.getOrDefault(from, 0) + 1, Math::max);
                if (incoming.merge(to, -1, Integer::sum) == 0) queue.addLast(to);
            }
        }
    }

    private static void connect(Map<String, Set<String>> flow, String from, String to) {
        flow.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
    }

    private static Node node(Seed s, int x, int y, int width, int height) {
        return new Node(s.id, s.kind, s.title, s.icon, s.key, s.entry, s.project, s.link, s.active, s.completed, x, y, width, height);
    }

    private static String addKnowledge(Map<String, Seed> seeds, KnowledgeJournalData data, KnowledgeKey key, boolean history) {
        var entry = data.entry(key);
        if (!history && (entry == null || !entry.active())) return null;
        String id = knowledgeId(key);
        seeds.putIfAbsent(id, new Seed(id, Kind.valueOf(key.kind().name()), data.title(key), entry == null ? new ItemStack(Items.PAPER) : entry.icon(), key, entry, null, null, entry != null && entry.active(), false));
        return id;
    }

    public static String knowledgeId(KnowledgeKey key) {
        return "knowledge:" + key.kind() + ":" + key.id();
    }

    public static String projectId(String id) {
        return "project:" + id;
    }
}
