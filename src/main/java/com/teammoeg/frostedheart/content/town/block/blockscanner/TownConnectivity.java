package com.teammoeg.frostedheart.content.town.block.blockscanner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        /** Routes the previous scan found; they still hold wherever their terrain is not loaded. */
        final List<Route> known;
        /** Sections this scan found unloaded. One worker runs a batch, so a plain map is enough. */
        final Map<Long, SectionSnapshotCache.Snapshot> missing = new HashMap<>();
        volatile boolean cancelled;
        CompletableFuture<Result> result;
        Batch(Map<BlockPos, AbstractTownBuilding> buildings, BlockPos tower, Object dimension, SectionSnapshotCache cache,
                List<Route> known) {
            this.cache = cache;
            this.buildings = new HashMap<>(buildings);
            this.entrances = new HashMap<>();
            buildings.forEach((pos, building) -> this.entrances.put(pos, building.getRoadEntrances()));
            this.tower = tower;
            this.dimension = dimension;
            this.known = List.copyOf(known);
        }
    }
    public record Route(BlockPos from, BlockPos to, List<BlockPos> steps, boolean powered) {
        public Route { steps = List.copyOf(steps); }
    }
    public record Result(Set<BlockPos> connected, Set<BlockPos> working, List<Route> routes) {
        public Result { connected = Set.copyOf(connected); working = Set.copyOf(working); routes = List.copyOf(routes); }
        public Result(Set<BlockPos> connected, List<Route> routes) { this(connected, connected, routes); }
    }
    public static final Result EMPTY = new Result(Set.of(), List.of());
    private static final Codec<List<BlockPos>> POSITIONS = Codec.LONG_STREAM.xmap(
            stream -> stream.mapToObj(BlockPos::of).toList(), list -> list.stream().mapToLong(BlockPos::asLong));
    private static final Codec<Route> ROUTE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("from").forGetter(Route::from),
            BlockPos.CODEC.fieldOf("to").forGetter(Route::to),
            POSITIONS.fieldOf("steps").forGetter(Route::steps),
            Codec.BOOL.optionalFieldOf("powered", false).forGetter(Route::powered)
    ).apply(instance, Route::new));
    /** The saved form of a view: its routes are what the scans after a restart check first. */
    public static final Codec<Result> RESULT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            POSITIONS.fieldOf("connected").forGetter(result -> result.connected().stream().sorted().toList()),
            POSITIONS.fieldOf("working").forGetter(result -> result.working().stream().sorted().toList()),
            ROUTE_CODEC.listOf().fieldOf("routes").forGetter(Result::routes)
    ).apply(instance, (connected, working, routes) -> new Result(Set.copyOf(connected), Set.copyOf(working), routes)));
    private Result view = EMPTY;
    private UUID version = UUID.randomUUID();
    public Result view() { return view; }
    public UUID version() { return version; }
    /** Takes a saved view back before the first scan; the caller restores the buildings' own links. */
    public void restore(Result saved) {
        view = saved;
        version = UUID.randomUUID();
    }
    private void publish(Result value) {
        if (!view.equals(value)) { view = value; version = UUID.randomUUID(); }
    }
    private Batch batch;
    private long nextScan;
    private Map<BlockPos, AbstractTownBuilding> observedBuildings = Map.of();
    private Map<BlockPos, List<BlockPos>> observedEntrances = Map.of();
    private BlockPos observedTower;
    private Object observedDimension;

    /** The buildings that take part in the road network; remote sites such as mines stay out of it. */
    static Map<BlockPos, AbstractTownBuilding> onRoads(Map<BlockPos, AbstractTownBuilding> buildings) {
        Map<BlockPos, AbstractTownBuilding> linked = new HashMap<>();
        buildings.forEach((pos, building) -> {
            if (building.requiresRoad()) linked.put(pos, building);
        });
        return linked;
    }

    public void tick(ServerLevel level, BlockPos tower, Map<BlockPos, AbstractTownBuilding> allBuildings) {
        Map<BlockPos, AbstractTownBuilding> buildings = onRoads(allBuildings);
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
                // A cancelled scan ends by throwing; only a scan that should have finished has failed.
                if (!batch.cancelled) com.teammoeg.frostedheart.FHMain.LOGGER.error("Town connectivity scan failed", failure);
            }
            boolean changed = batch.cancelled;
            batch = null;
            nextScan = level.getGameTime() + (changed ? 0 : INTERVAL_TICKS);
        }
        if (buildings.isEmpty() || level.getGameTime() < nextScan) return;
        // Terrain only changes while it is loaded, so a town with none of it loaded has nothing new to find.
        boolean anyLoaded = false;
        for (var entry : buildings.entrySet()) {
            if (inLoadedTerrain(level, entry.getKey(), entry.getValue().getRoadEntrances())) {
                anyLoaded = true;
                break;
            }
        }
        if (!anyLoaded) {
            nextScan = level.getGameTime() + UNLOADED_RETRY_TICKS;
            return;
        }
        Batch started = new Batch(buildings, tower == null ? null : tower.immutable(), level.dimension(),
                TownSectionSnapshots.get(level), view.routes());
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

    private static final class Snapshot implements AbstractWorld {
        private final Batch batch;
        private record Local(SectionSnapshotCache.Snapshot snapshot,long checkedUntil,long expiryObserved) {}
        private final Map<SectionPos,Local> sections=new HashMap<>();
        Snapshot(Batch batch){this.batch=batch;}
        public BlockType getBlockType(BlockPos pos){
            return TYPES[local(pos).snapshot().valueAt(((pos.getY()&15)<<8)|((pos.getZ()&15)<<4)|(pos.getX()&15))];
        }
        /** Unloaded terrain is unknown: the scan reads it as walls, and remembered routes take it as it was. */
        @Override public boolean isKnown(BlockPos pos){return !local(pos).snapshot().missing();}
        private Local local(BlockPos pos){
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
            return local;
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

    /** Horizontal distance from the tower controller to the ring just outside a 3x3 tower base. */
    static final int TOWER_RING = 2;
    /** Largest Manhattan offset of a tower entrance from its controller: ring corners, two blocks down. */
    static final int TOWER_ENTRANCE_RADIUS = 2 * TOWER_RING + 2;

    /**
     * 能量塔的连接点。成型的能量塔把控制方块包在塔身里，旁边没有能站的格子；这时改用紧贴 3×3 塔底座外侧一圈、
     * 控制方块下两格到上一格之间能站的格子，路修到塔脚下任一面即可。
     * <p>
     * Where roads meet the tower. A formed tower encloses its controller, leaving nothing beside it to stand on; then
     * the standable cells in the ring just outside its 3x3 base, from two below the controller to one above, are used,
     * so a road may reach the foot of the tower on any side.
     */
    static List<BlockPos> towerEntrances(AbstractWorld world, BlockPos tower) {
        List<BlockPos> beside = entrances(world, tower);
        if (!beside.isEmpty()) return beside;
        List<BlockPos> around = new ArrayList<>();
        for (int dx = -TOWER_RING; dx <= TOWER_RING; dx++)
            for (int dz = -TOWER_RING; dz <= TOWER_RING; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != TOWER_RING) continue;
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos pos = tower.offset(dx, dy, dz);
                    if (RoomPathfinder.canStandAt(world, pos)) around.add(pos);
                }
            }
        return around;
    }

    private static List<BlockPos> entrances(AbstractWorld world, BlockPos node, BlockPos tower, Map<BlockPos, List<BlockPos>> ports) {
        return node.equals(tower) ? towerEntrances(world, node) : entrances(world, node, ports);
    }

    private static Result calculate(Batch batch) {
        return calculateView(batch.buildings.keySet(), batch.tower, () -> new Snapshot(batch), () -> batch.cancelled,
                batch.entrances, batch.known);
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
        return calculateView(buildings, tower, worlds, cancelled, ports, List.of());
    }

    /**
     * Clusters and the working set. {@code known} are routes an earlier scan found: unloaded terrain cannot change,
     * so a route still holds where the world does not know its terrain, while every step where it does must still be
     * walkable. They link first; the searches then only look for what they do not already link.
     */
    static Result calculateView(Set<BlockPos> buildings, BlockPos tower,
            java.util.function.Supplier<AbstractWorld> worlds, java.util.function.BooleanSupplier cancelled,
            Map<BlockPos, List<BlockPos>> ports, Collection<Route> known) {
        if (cancelled.getAsBoolean()) return new Result(Set.of(), List.of());
        List<Route> routes = new ArrayList<>();
        List<BlockPos> nodes = new ArrayList<>(buildings);
        nodes.sort(BlockPos::compareTo);
        if (tower != null) nodes.add(tower);
        Map<BlockPos, Integer> entranceRadii = new HashMap<>();
        for (BlockPos node : nodes) entranceRadii.put(node, ports.getOrDefault(node, List.of()).stream()
                .mapToInt(node::distManhattan).max().orElse(2));
        if (tower != null) entranceRadii.put(tower, TOWER_ENTRANCE_RADIUS);
        int[] parents = new int[nodes.size()];
        for (int i = 0; i < parents.length; i++) parents[i] = i;
        if (!known.isEmpty()) {
            Map<BlockPos, Integer> index = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) index.put(nodes.get(i), i);
            AbstractWorld world = worlds.get();
            for (Route route : known) {
                if (cancelled.getAsBoolean()) return new Result(Set.of(), List.of());
                Integer from = index.get(route.from()), to = index.get(route.to());
                if (from == null || to == null || find(parents, from) == find(parents, to)) continue;
                if (!intact(world, route, tower, ports)) continue;
                parents[find(parents, to)] = find(parents, from);
                routes.add(route);
            }
        }
        for (int source = 0; source < nodes.size(); source++) {
            AbstractWorld world = worlds.get(); // Release each source's section cache after its search.
            Map<BlockPos, List<Integer>> targets = new HashMap<>();
            for (int target = source + 1; target < nodes.size(); target++) {
                if (find(parents, source) == find(parents, target)) continue;
                if (nodes.get(source).distManhattan(nodes.get(target)) > 2 * MAX_DISTANCE + entranceRadii.get(nodes.get(source)) + entranceRadii.get(nodes.get(target))) continue;
                for (BlockPos entrance : entrances(world, nodes.get(target), tower, ports))
                    targets.computeIfAbsent(entrance, key -> new ArrayList<>()).add(target);
            }
            if (targets.isEmpty()) continue;
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Map<BlockPos, Integer> distances = new HashMap<>();
            Map<BlockPos, BlockPos> previous = new HashMap<>();
            for (BlockPos pos : entrances(world, nodes.get(source), tower, ports)) {
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
    /** Whether a remembered route still holds: steps in unknown terrain stand as they were, known ones are checked. */
    private static boolean intact(AbstractWorld world, Route route, BlockPos tower, Map<BlockPos, List<BlockPos>> ports) {
        List<BlockPos> steps = route.steps();
        if (steps.isEmpty()) return false;
        BlockPos first = steps.get(0), last = steps.get(steps.size() - 1);
        if (world.isKnown(first) && !entrances(world, route.from(), tower, ports).contains(first)) return false;
        if (world.isKnown(last) && !entrances(world, route.to(), tower, ports).contains(last)) return false;
        for (int i = 1; i < steps.size(); i++) {
            BlockPos previous = steps.get(i - 1), step = steps.get(i);
            if (!world.isKnown(step)) continue;
            boolean walkable = world.isKnown(previous)
                    ? RoomPathfinder.getNeighbors(world, route.from(), previous).contains(step)
                    : RoomPathfinder.canStandAt(world, step);
            if (!walkable) return false;
        }
        return true;
    }

    private static int find(int[] parents, int node) {
        while (parents[node] != node) { parents[node] = parents[parents[node]]; node = parents[node]; }
        return node;
    }
}
