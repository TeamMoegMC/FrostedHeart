package com.teammoeg.frostedheart.content.town.block.blockscanner;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
import java.util.concurrent.*;

/** Main-thread classification, immutable worker snapshots, main-thread topology commit. */
public final class TownConnectivity {
    private static final AbstractWorld.BlockType[] TYPES = AbstractWorld.BlockType.values();
    public static final int MAX_DISTANCE = 60;
    public static final int INTERVAL_TICKS = 1200;
    public static final int SECTIONS_PER_TICK = 2;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "town-connectivity");
        thread.setDaemon(true);
        return thread;
    });
    private record Request(SectionPos section, CompletableFuture<byte[]> result) {}
    private static final class Batch {
        final Queue<Request> requests = new ConcurrentLinkedQueue<>();
        final Map<BlockPos, AbstractTownBuilding> buildings;
        final Map<BlockPos, List<BlockPos>> entrances;
        final BlockPos tower;
        final Object dimension;
        volatile boolean cancelled;
        CompletableFuture<Result> result;
        Batch(Map<BlockPos, AbstractTownBuilding> buildings, BlockPos tower, Object dimension) {
            this.buildings = new HashMap<>(buildings);
            this.entrances = new HashMap<>();
            buildings.forEach((pos, building) -> this.entrances.put(pos, building.getRoadEntrances()));
            this.tower = tower;
            this.dimension = dimension;
        }
    }
    public record Route(BlockPos from, BlockPos to, List<BlockPos> steps, boolean powered) {
        public Route { steps = List.copyOf(steps); }
    }
    public record Result(Set<BlockPos> connected, Set<BlockPos> working, List<Route> routes) {
        public Result { connected = Set.copyOf(connected); working = Set.copyOf(working); routes = List.copyOf(routes); }
        public Result(Set<BlockPos> connected, List<Route> routes) { this(connected, connected, routes); }
    }
    private Result view = new Result(Set.of(), List.of());
    private UUID version = UUID.randomUUID();
    public Result view() { return view; }
    public UUID version() { return version; }
    private void publish(Result value) {
        if (!view.equals(value)) { view = value; version = UUID.randomUUID(); }
    }
    private Batch batch;
    private long nextScan;
    private Map<BlockPos, AbstractTownBuilding> observedBuildings = Map.of();
    private Map<BlockPos, List<BlockPos>> observedEntrances = Map.of();
    private BlockPos observedTower;
    private Object observedDimension;

    public void tick(ServerLevel level, BlockPos tower, Map<BlockPos, AbstractTownBuilding> buildings) {
        Map<BlockPos, List<BlockPos>> currentEntrances = new HashMap<>();
        buildings.forEach((pos, building) -> currentEntrances.put(pos, building.getRoadEntrances()));
        if (!currentEntrances.equals(observedEntrances) || !Objects.equals(tower, observedTower) || !Objects.equals(level.dimension(), observedDimension)
                || !observedBuildings.equals(buildings)) {
            observedEntrances = currentEntrances;
            observedTower = tower;
            observedDimension = level.dimension();
            observedBuildings = new HashMap<>(buildings);
            nextScan = 0;
            if (batch != null) batch.cancelled = true;
            buildings.values().forEach(building -> building.updateBatch(() -> {
                building.setConnectedToTower(false);
                building.setConnectivityWorkable(false);
            }));
            publish(new Result(Set.of(), List.of()));
        }
        if (batch != null) {
            LoadedTownWorld world = new LoadedTownWorld(level);
            for (int count = 0; count < SECTIONS_PER_TICK; count++) {
                Request request = batch.requests.poll();
                if (request == null) break;
                byte[] cells = new byte[4096];
                Arrays.fill(cells, (byte) AbstractWorld.BlockType.WALL.ordinal());
                SectionPos section = request.section();
                if (!batch.cancelled && level.getChunkSource().getChunkNow(section.x(), section.z()) != null) {
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                        pos.set(section.minBlockX() + x, section.minBlockY() + y, section.minBlockZ() + z);
                        cells[(y << 8) | (z << 4) | x] = (byte) world.getPathBlockType(pos).ordinal();
                    }
                }
                request.result().complete(cells);
            }
            if (!batch.result.isDone()) return;
            try {
                Result result = batch.result.join();
                if (!batch.cancelled) {
                    buildings.forEach((pos, building) -> building.updateBatch(() -> {
                        building.setConnectedToTower(result.connected().contains(pos));
                        building.setConnectivityWorkable(result.working().contains(pos));
                    }));
                    publish(result);
                }
            } catch (CompletionException failure) {
                com.teammoeg.frostedheart.FHMain.LOGGER.error("Town connectivity scan failed", failure);
            }
            boolean changed = batch.cancelled;
            batch = null;
            nextScan = level.getGameTime() + (changed ? 0 : INTERVAL_TICKS);
        }
        if (buildings.isEmpty() || level.getGameTime() < nextScan) return;
        Batch started = new Batch(buildings, tower == null ? null : tower.immutable(), level.dimension());
        batch = started;
        started.result = CompletableFuture.supplyAsync(() -> calculate(started), WORKERS);
    }

    private static final class Snapshot implements AbstractWorld {
        private final Batch batch;
        private final Map<SectionPos, byte[]> sections = new HashMap<>();
        Snapshot(Batch batch) { this.batch = batch; }
        public BlockType getBlockType(BlockPos pos) {
            if (batch.cancelled) throw new CancellationException();
            byte[] cells = sections.computeIfAbsent(SectionPos.of(pos), section -> {
                CompletableFuture<byte[]> result = new CompletableFuture<>();
                batch.requests.add(new Request(section, result));
                // A retired town/server must not retain a worker indefinitely.
                try { return result.get(30, TimeUnit.SECONDS); }
                catch (Exception failure) { throw new CompletionException(failure); }
            });
            return TYPES[cells[((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15)]];
        }
        public int getMaxY(BlockPos pos) { return pos.getY() + 2; }
    }

    private static List<BlockPos> entrances(AbstractWorld world, BlockPos core) {
        List<BlockPos> positions = new ArrayList<>();
        // Building controllers are solid: attach through walkable neighbouring feet.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = core.relative(direction).offset(0, dy, 0);
                if (RoomPathfinder.canStandAt(world, pos)) positions.add(pos);
            }
        }
        return positions;
    }

    private static List<BlockPos> entrances(AbstractWorld world, BlockPos core, Map<BlockPos, List<BlockPos>> ports) {
        List<BlockPos> declared = ports.getOrDefault(core, List.of());
        return declared.isEmpty() ? entrances(world, core)
                : declared.stream().filter(pos -> RoomPathfinder.canStandAt(world, pos)).toList();
    }

    private static Result calculate(Batch batch) {
        return calculateView(batch.buildings.keySet(), batch.tower, () -> new Snapshot(batch), () -> batch.cancelled, batch.entrances);
    }

    static Set<BlockPos> calculate(Set<BlockPos> buildings, BlockPos tower,
            java.util.function.Supplier<AbstractWorld> worlds, java.util.function.BooleanSupplier cancelled) {
        return calculateView(buildings, tower, worlds, cancelled).connected();
    }

    static Result calculateView(Set<BlockPos> buildings, BlockPos tower,
            java.util.function.Supplier<AbstractWorld> worlds, java.util.function.BooleanSupplier cancelled) {
        return calculateView(buildings, tower, worlds, cancelled, Map.of());
    }

    static Result calculateView(Set<BlockPos> buildings, BlockPos tower,
            java.util.function.Supplier<AbstractWorld> worlds, java.util.function.BooleanSupplier cancelled,
            Map<BlockPos, List<BlockPos>> ports) {
        if (cancelled.getAsBoolean()) return new Result(Set.of(), List.of());
        List<Route> routes = new ArrayList<>();
        List<BlockPos> nodes = new ArrayList<>(buildings);
        nodes.sort(BlockPos::compareTo);
        if (tower != null) nodes.add(tower);
        Map<BlockPos, Integer> entranceRadii = new HashMap<>();
        for (BlockPos node : nodes) entranceRadii.put(node, ports.getOrDefault(node, List.of()).stream()
                .mapToInt(node::distManhattan).max().orElse(2));
        int[] parents = new int[nodes.size()];
        for (int i = 0; i < parents.length; i++) parents[i] = i;
        for (int source = 0; source < nodes.size(); source++) {
            AbstractWorld world = worlds.get(); // Release each source's section cache after its search.
            Map<BlockPos, List<Integer>> targets = new HashMap<>();
            for (int target = source + 1; target < nodes.size(); target++) {
                if (find(parents, source) == find(parents, target)) continue;
                if (nodes.get(source).distManhattan(nodes.get(target)) > 2 * MAX_DISTANCE + entranceRadii.get(nodes.get(source)) + entranceRadii.get(nodes.get(target))) continue;
                for (BlockPos entrance : entrances(world, nodes.get(target), ports))
                    targets.computeIfAbsent(entrance, key -> new ArrayList<>()).add(target);
            }
            if (targets.isEmpty()) continue;
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Map<BlockPos, Integer> distances = new HashMap<>();
            Map<BlockPos, BlockPos> previous = new HashMap<>();
            for (BlockPos pos : entrances(world, nodes.get(source), ports)) {
                if (distances.putIfAbsent(pos, 0) == null) queue.add(pos);
            }
            final int sourceIndex = source;
            while (!queue.isEmpty() && !targets.isEmpty()) {
                if (cancelled.getAsBoolean()) return new Result(Set.of(), List.of());
                BlockPos pos = queue.remove();
                List<Integer> reached = targets.remove(pos);
                if (reached != null) {
                    for (int target : reached) {
                        if (find(parents, target) == find(parents, source)) continue;
                        parents[find(parents, target)] = find(parents, source);
                        List<BlockPos> steps = new ArrayList<>();
                        for (BlockPos step = pos; step != null; step = previous.get(step)) steps.add(step);
                        Collections.reverse(steps);
                        routes.add(new Route(nodes.get(source), nodes.get(target), steps, false));
                    }
                    // Once a target joins our cluster, its other entrances need no further search.
                    targets.entrySet().removeIf(entry -> {
                        entry.getValue().removeIf(target -> find(parents, target) == find(parents, sourceIndex));
                        return entry.getValue().isEmpty();
                    });
                }
                int distance = distances.get(pos);
                if (distance >= MAX_DISTANCE) continue;
                for (BlockPos next : RoomPathfinder.getNeighbors(world, nodes.get(source), pos)) {
                    if (distances.putIfAbsent(next, distance + 1) == null) {
                        previous.put(next, pos);
                        queue.add(next);
                    }
                }
            }
        }
        Set<BlockPos> connected = new HashSet<>();
        int buildingCount = buildings.size();
        if (tower != null) {
            int towerRoot = find(parents, nodes.size() - 1);
            for (int i = 0; i < buildingCount; i++) if (find(parents, i) == towerRoot) connected.add(nodes.get(i));
        }
        Set<BlockPos> working = new HashSet<>(connected);
        if (working.isEmpty() && buildingCount > 0) {
            Map<Integer, Integer> sizes = new HashMap<>();
            for (int i = 0; i < buildingCount; i++) sizes.merge(find(parents, i), 1, Integer::sum);
            // Nodes are sorted: ties consistently choose the cluster with the first controller.
            int selected = find(parents, 0);
            for (int i = 1; i < buildingCount; i++) {
                int candidate = find(parents, i);
                if (sizes.get(candidate) > sizes.get(selected)) selected = candidate;
            }
            for (int i = 0; i < buildingCount; i++) if (find(parents, i) == selected) working.add(nodes.get(i));
        }
        List<Route> marked = routes.stream().map(route -> new Route(route.from(), route.to(), route.steps(),
                connected.contains(route.from()) || route.from().equals(tower))).toList();
        return new Result(connected, working, marked);
    }
    private static int find(int[] parents, int node) {
        while (parents[node] != node) { parents[node] = parents[parents[node]]; node = parents[node]; }
        return node;
    }
}
