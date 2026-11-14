package com.teammoeg.frostedheart.content.town.citizen.nav;

import java.util.*;
import com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity;
import net.minecraft.core.BlockPos;

/** Immutable building-node graph; every entrance of a building is the same logical node. */
public final class TownRoadNetwork {
    public record Waypoint(BlockPos pos, boolean buildingTransfer) {}
    private record Edge(BlockPos to, List<BlockPos> steps) {}
    private record Pair(BlockPos from, BlockPos to) {}
    private record Location(BlockPos node, List<BlockPos> prefix) {}
    private final Map<BlockPos, List<Edge>> edges = new HashMap<>();
    private final Map<BlockPos, BlockPos> components = new HashMap<>();
    private final Map<BlockPos, List<Location>> locations = new HashMap<>();
    private final Map<Pair, List<Waypoint>> cache = new LinkedHashMap<>(64, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Pair, List<Waypoint>> entry) { return size() > 256; }
    };
    private final Set<BlockPos> working;

    public TownRoadNetwork(TownConnectivity.Result result) {
        working = result.working();
        working.forEach(pos -> edges.put(pos, new ArrayList<>()));
        for (var route : result.routes()) {
            if (route.steps().isEmpty() || (!working.contains(route.from()) && !working.contains(route.to()))) continue;
            add(route.from(), route.to(), route.steps());
            List<BlockPos> reversed = new ArrayList<>(route.steps()); Collections.reverse(reversed);
            add(route.to(), route.from(), List.copyOf(reversed));
        }
        for (BlockPos node : edges.keySet().stream().sorted().toList()) {
            if (components.containsKey(node)) continue;
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(node); components.put(node, node);
            while (!queue.isEmpty()) for (Edge edge : edges.get(queue.remove())) {
                if (components.putIfAbsent(edge.to(), node) == null) queue.add(edge.to());
            }
        }
    }
    private void add(BlockPos from, BlockPos to, List<BlockPos> steps) {
        edges.computeIfAbsent(from, key -> new ArrayList<>()).add(new Edge(to, steps));
        edges.computeIfAbsent(to, key -> new ArrayList<>());
        // Index points once; prefixes are built lazily, not copied for each resident or point.
        for (int i = 0; i < steps.size(); i++) {
            locations.computeIfAbsent(steps.get(i), key -> new ArrayList<>())
                    .add(new Location(to, steps.subList(i, steps.size())));
        }
    }
    public BlockPos entrance(BlockPos node, BlockPos fallback) {
        var list = edges.get(node);
        if (list == null || list.isEmpty()) return fallback;
        var steps = list.get(0).steps();
        return steps.get(Math.min(1, steps.size() - 1));
    }
    public boolean working(BlockPos node) { return working.contains(node); }
    public BlockPos nodeAt(int x, int z, BlockPos fallback) {
        return working.stream().filter(pos -> pos.getX() == x && pos.getZ() == z).sorted().findFirst().orElse(fallback);
    }
    public BlockPos wanderTarget(BlockPos home, int seed) {
        List<BlockPos> candidates = edges.keySet().stream()
                .filter(pos -> Objects.equals(components.get(home), components.get(pos)) && !pos.equals(home))
                .sorted().toList();
        return candidates.isEmpty() ? home : candidates.get(Math.floorMod(seed, candidates.size()));
    }
    public List<Waypoint> between(BlockPos from, BlockPos to) {
        if (!edges.containsKey(from) || !edges.containsKey(to)
                || !Objects.equals(components.get(from), components.get(to))) return List.of();
        Pair key = new Pair(from, to);
        return cache.computeIfAbsent(key, ignored -> {
            if (from.equals(to)) return List.of();
            Map<BlockPos, BlockPos> parent = new HashMap<>();
            Map<BlockPos, Edge> via = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(from); parent.put(from, from);
            while (!queue.isEmpty() && !parent.containsKey(to)) {
                BlockPos current = queue.remove();
                for (Edge edge : edges.get(current)) if (!parent.containsKey(edge.to())) {
                    parent.put(edge.to(), current); via.put(edge.to(), edge); queue.add(edge.to());
                }
            }
            List<Edge> segments = new ArrayList<>();
            for (BlockPos node = to; !node.equals(from); node = parent.get(node)) segments.add(via.get(node));
            Collections.reverse(segments);
            List<Waypoint> path = new ArrayList<>();
            for (Edge edge : segments) append(path, edge.steps());
            return List.copyOf(path);
        });
    }
    private static void append(List<Waypoint> result, List<BlockPos> steps) {
        for (int i = 0; i < steps.size(); i++) {
            BlockPos pos = steps.get(i);
            if (!result.isEmpty() && result.get(result.size() - 1).pos().equals(pos)) continue;
            result.add(new Waypoint(pos, i == 0));
        }
    }
    public List<Waypoint> fromPosition(BlockPos position, BlockPos home, BlockPos target, BlockPos entrance) {
        if (!working(home) || !edges.containsKey(target)
                || !Objects.equals(components.get(home), components.get(target))) return List.of();
        Location best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(position.offset(-2, -2, -2), position.offset(2, 2, 2))) {
            List<Location> candidates = locations.get(pos);
            if (candidates == null) continue;
            for (Location location : candidates) {
                if (!Objects.equals(components.get(home), components.get(location.node()))) continue;
                double score = pos.distSqr(position) + (location.prefix().size() + between(location.node(), target).size()) * .0001;
                if (score < distance) { distance = score; best = location; }
            }
        }
        List<Waypoint> path = new ArrayList<>();
        if (best != null) {
            append(path, best.prefix()); appendWaypoints(path, between(best.node(), target));
        } else {
            appendWaypoints(path, between(home, target));
            if (path.isEmpty() && home.equals(target)) path.add(new Waypoint(entrance, true));
        }
        return List.copyOf(path);
    }
    private static void appendWaypoints(List<Waypoint> path, List<Waypoint> extra) {
        for (Waypoint point : extra)
            if (path.isEmpty() || !path.get(path.size() - 1).pos().equals(point.pos())) path.add(point);
    }
}
