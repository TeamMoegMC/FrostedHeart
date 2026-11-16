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
    /** How long a town whose terrain is not loaded waits before it looks again. */
    public static final int UNLOADED_RETRY_TICKS = 100;
    public static final int SECTIONS_PER_TICK = TownSectionSnapshots.SECTIONS_PER_TICK;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "town-connectivity");
        thread.setDaemon(true);
        return thread;
    });
    private static final class Batch {
        final SectionSnapshotCache cache;
        final Map<BlockPos, AbstractTownBuilding> buildings;
        final Map<BlockPos, List<BlockPos>> entrances;
        final BlockPos tower;
        final Object dimension;
        /** A building or the tower lay outside loaded terrain, so this scan sees only part of the town. */
        final boolean partial;
        /** Sections this scan found unloaded. One worker runs a batch, so a plain map is enough. */
        final Map<Long, SectionSnapshotCache.Snapshot> missing = new HashMap<>();
        volatile boolean cancelled;
        CompletableFuture<Result> result;
        Batch(Map<BlockPos, AbstractTownBuilding> buildings, BlockPos tower, Object dimension, SectionSnapshotCache cache,
                boolean partial) {
            this.cache = cache;
            this.buildings = new HashMap<>(buildings);
            this.entrances = new HashMap<>();
            buildings.forEach((pos, building) -> this.entrances.put(pos, building.getRoadEntrances()));
            this.tower = tower;
            this.dimension = dimension;
            this.partial = partial;
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
            boolean otherDimension = observedDimension != null && !observedDimension.equals(level.dimension());
            observedEntrances = currentEntrances;
            observedTower = tower;
            observedDimension = level.dimension();
            observedBuildings = new HashMap<>(buildings);
            nextScan = 0;
            if (batch != null) batch.cancelled = true;
            if (otherDimension) {
                // Links found in another dimension say nothing about this one.
                buildings.values().forEach(building -> building.updateBatch(() -> {
                    building.setConnectedToTower(false);
                    building.setConnectivityWorkable(false);
                }));
                publish(new Result(Set.of(), List.of()));
            } else {
                // Buildings keep their links until the rescan's result arrives; removed ones drop out of the view.
                publish(retain(view, buildings.keySet(), tower));
            }
        }
        if (batch != null) {
            if (!batch.result.isDone()) return;
            long wait = INTERVAL_TICKS;
            try {
                Result result = batch.result.join();
                if (batch.cancelled) {
                    // Superseded: the rescan starts below.
                } else if (!inLoadedTerrain(level, batch)) {
                    wait = UNLOADED_RETRY_TICKS; // its terrain unloaded while it ran: keep the links, look again later
                } else if (batch.partial) {
                    grant(result, buildings, batch);
                } else {
                    buildings.forEach((pos, building) -> building.updateBatch(() -> {
                        building.setConnectedToTower(result.connected().contains(pos));
                        building.setConnectivityWorkable(result.working().contains(pos));
                    }));
                    publish(result);
                }
            } catch (CompletionException failure) {
                // A cancelled scan ends by throwing; only a scan that should have finished has failed.
                if (!batch.cancelled) com.teammoeg.frostedheart.FHMain.LOGGER.error("Town connectivity scan failed", failure);
            }
            boolean changed = batch.cancelled;
            batch = null;
            nextScan = level.getGameTime() + (changed ? 0 : wait);
        }
        if (buildings.isEmpty() || level.getGameTime() < nextScan) return;
        // A scan judges only loaded terrain. Buildings outside it keep their links; with none inside, look again later.
        Map<BlockPos, AbstractTownBuilding> seen = new HashMap<>();
        buildings.forEach((pos, building) -> {
            if (inLoadedTerrain(level, pos, building.getRoadEntrances())) seen.put(pos, building);
        });
        BlockPos seenTower = tower != null && inLoadedTerrain(level, tower, List.of()) ? tower.immutable() : null;
        if (seen.isEmpty()) {
            nextScan = level.getGameTime() + UNLOADED_RETRY_TICKS;
            return;
        }
        Batch started = new Batch(seen, seenTower, level.dimension(), TownSectionSnapshots.get(level),
                seen.size() < buildings.size() || seenTower == null && tower != null);
        batch = started;
        started.result = CompletableFuture.supplyAsync(() -> calculate(started), WORKERS);
    }

    /** Whether the chunks around a controller, and those of its entrances, are loaded. Server thread only. */
    private static boolean inLoadedTerrain(ServerLevel level, BlockPos core, List<BlockPos> entrances) {
        // Entrances without scan data stand next to the controller; the corners of a 5x5 square reach their chunks.
        for (int dx = -2; dx <= 2; dx += 4)
            for (int dz = -2; dz <= 2; dz += 4)
                if (!TownSectionSnapshots.isLoaded(level, core.offset(dx, 0, dz))) return false;
        for (BlockPos entrance : entrances) if (!TownSectionSnapshots.isLoaded(level, entrance)) return false;
        return true;
    }

    private static boolean inLoadedTerrain(ServerLevel level, Batch batch) {
        if (batch.tower != null && !inLoadedTerrain(level, batch.tower, List.of())) return false;
        for (var entry : batch.entrances.entrySet()) if (!inLoadedTerrain(level, entry.getKey(), entry.getValue())) return false;
        return true;
    }

    /** The view without buildings that are gone, and without routes that end at one of them or at a former tower. */
    private static Result retain(Result view, Set<BlockPos> buildings, BlockPos tower) {
        Set<BlockPos> connected = new HashSet<>(view.connected());
        connected.retainAll(buildings);
        Set<BlockPos> working = new HashSet<>(view.working());
        working.retainAll(buildings);
        List<Route> routes = view.routes().stream()
                .filter(route -> (buildings.contains(route.from()) || route.from().equals(tower))
                        && (buildings.contains(route.to()) || route.to().equals(tower)))
                .toList();
        return new Result(connected, working, routes);
    }

    /**
     * Applies a scan that saw only part of the town. It cannot know what lies outside loaded terrain, so it only adds
     * links: a scanned building works once its part of the network holds a working building or the tower. A town
     * with no working building yet takes the rule's verdict on the part it saw.
     */
    private void grant(Result result, Map<BlockPos, AbstractTownBuilding> buildings, Batch scanned) {
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        for (Route route : result.routes()) parents.put(root(parents, route.from()), root(parents, route.to()));
        Set<BlockPos> anchors = new HashSet<>();
        for (BlockPos pos : scanned.buildings.keySet()) {
            AbstractTownBuilding building = buildings.get(pos);
            if (building != null && building.isConnectivityWorkable()) anchors.add(root(parents, pos));
        }
        Set<BlockPos> working = new HashSet<>();
        if (anchors.isEmpty()) {
            working.addAll(result.working());
        } else {
            if (scanned.tower != null) anchors.add(root(parents, scanned.tower));
            for (BlockPos pos : scanned.buildings.keySet()) if (anchors.contains(root(parents, pos))) working.add(pos);
        }
        buildings.forEach((pos, building) -> building.updateBatch(() -> {
            if (result.connected().contains(pos)) building.setConnectedToTower(true);
            if (working.contains(pos)) building.setConnectivityWorkable(true);
        }));
        // Routes between scanned nodes come from this scan; the unscanned part keeps the routes it had.
        Set<BlockPos> nodes = new HashSet<>(scanned.buildings.keySet());
        if (scanned.tower != null) nodes.add(scanned.tower);
        List<Route> routes = new ArrayList<>(result.routes());
        for (Route route : view.routes()) if (!nodes.contains(route.from()) || !nodes.contains(route.to())) routes.add(route);
        Set<BlockPos> connected = new HashSet<>(view.connected());
        connected.addAll(result.connected());
        working.addAll(view.working());
        publish(new Result(connected, working, routes));
    }

    private static BlockPos root(Map<BlockPos, BlockPos> parents, BlockPos pos) {
        BlockPos parent = parents.get(pos);
        if (parent == null || parent.equals(pos)) return pos;
        BlockPos top = root(parents, parent);
        parents.put(pos, top);
        return top;
    }

    private static final class Snapshot implements AbstractWorld {
        private final Batch batch;
        private record Local(SectionSnapshotCache.Snapshot snapshot,long checkedUntil,long expiryObserved) {}
        private final Map<SectionPos,Local> sections=new HashMap<>();
        Snapshot(Batch batch){this.batch=batch;}
        public BlockType getBlockType(BlockPos pos){
            if(batch.cancelled)throw new CancellationException();
            SectionPos section=SectionPos.of(pos);Local local=sections.get(section);long now=batch.cache.currentTick();
            if(local==null||local.snapshot().retired()||now>=local.checkedUntil()
                    ||(local.snapshot().expiresAt()<local.expiryObserved()&&now>=local.snapshot().expiresAt())){
                // Unloaded sections are never cached, so each search start would wait for them in the shared
                // queue again: within one scan, one that was unloaded stays unloaded.
                var snapshot=batch.missing.get(section.asLong());
                if(snapshot==null){snapshot=load(section);if(snapshot.missing())batch.missing.put(section.asLong(),snapshot);}
                now=batch.cache.currentTick();
                long check=snapshot.missing()?Long.MAX_VALUE:Math.min(snapshot.removeAt(),snapshot.expiresAt()>now?snapshot.expiresAt():now+1);
                local=new Local(snapshot,check,snapshot.expiresAt());sections.put(section,local);
            }
            return TYPES[local.snapshot().valueAt(((pos.getY()&15)<<8)|((pos.getZ()&15)<<4)|(pos.getX()&15))];
        }
        private SectionSnapshotCache.Snapshot load(SectionPos section){
            if(batch.cancelled)throw new CancellationException();
            CompletableFuture<SectionSnapshotCache.Snapshot> result=batch.cache.request(section.asLong());
            try{return result.get(30,TimeUnit.SECONDS);}
            catch(InterruptedException failure){Thread.currentThread().interrupt();throw new CompletionException(failure);}
            catch(ExecutionException|TimeoutException failure){throw new CompletionException(failure);}
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
