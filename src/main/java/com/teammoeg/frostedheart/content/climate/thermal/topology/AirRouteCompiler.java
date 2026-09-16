/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/** Geometry-only, resumable routing through ventilated matter; owns no heat state. */
final class AirRouteCompiler {
    private static final int DISCOVER_POSITIONS = 0;
    private static final int SEED_AIR_PORTS = 1;
    private static final int PROPAGATE_RESISTANCE = 2;
    private static final int BUILD_CONTACTS = 3;
    private static final int VISITS_PER_CUT = 4096;
    private final ThermalSignatureTable signatures;
    private final ThermalCellArena arena;
    private final int maximumPositions;
    private final Long2ObjectOpenHashMap<Component> atPosition = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<Component>> bySection =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ArrayList<EdgeSlice>> edgesByBrick =
            new Long2ObjectOpenHashMap<>();
    private final ObjectOpenHashSet<Component> components = new ObjectOpenHashSet<>();
    private final ArrayDeque<Component> pending = new ArrayDeque<>();
    private final LongLinkedOpenHashSet seeds = new LongLinkedOpenHashSet();
    private final LongOpenHashSet dirtyBricks = new LongOpenHashSet();
    private final LongOpenHashSet changedSections = new LongOpenHashSet();
    private final LongOpenHashSet invalidatedSourceSections = new LongOpenHashSet();
    private final ObjectOpenHashSet<Component> unpublished = new ObjectOpenHashSet<>();
    private long lastVisitCount;

    AirRouteCompiler(
            ThermalSignatureTable signatures, ThermalCellArena arena, int maximumPositions) {
        this.signatures = signatures;
        this.arena = arena;
        this.maximumPositions = maximumPositions;
    }

    boolean hasPendingWork() {
        return !pending.isEmpty() || !seeds.isEmpty() || !dirtyBricks.isEmpty();
    }

    long lastVisitCount() {
        return lastVisitCount;
    }

    void beginCut() {
        lastVisitCount = 0;
        invalidatedSourceSections.clear();
    }

    long[] invalidatedSourceSections() {
        return invalidatedSourceSections.toLongArray();
    }

    void committed() {
        for (Component component : unpublished) if (component.alive) component.validity.commit();
        unpublished.clear();
        dirtyBricks.clear();
    }

    AirRouteValidity validity(long position) {
        return atPosition.get(position).validity;
    }

    void invalidate(ThermalInputBatch batch) {
        changedSections.clear();
        for (var admission : batch.admissions()) changedSections.add(admission.page().sectionKey());
        for (var retirement : batch.retirements())
            changedSections.add(retirement.page().sectionKey());
        for (var residency : batch.residencyUpdates())
            changedSections.add(residency.page().sectionKey());
        for (int index = 0; index < batch.geometry().size(); index++)
            changedSections.add(batch.geometry().page(index).sectionKey());
        for (var halo : batch.geometry().halos()) {
            changedSections.add(halo.page().sectionKey());
            for (long position : halo.halo().positions()) changedSections.add(section(position));
        }
        for (long section : changedSections) {
            var affected = bySection.get(section);
            if (affected == null) continue;
            for (Component component : affected.toArray(Component[]::new)) discard(component);
        }
    }

    void prepare(TopologyView view, IntOpenHashSet fragments) {
        includeDirtyFragments(view, fragments);
        int[] initial = fragments.toIntArray();
        Arrays.sort(initial);
        for (int fragment : initial) {
            var page = view.pageSlot(fragment / 64);
            if (page == null || !view.resident(page, fragment % 64)) continue;
            long origin = brickPosition(page, fragment % 64);
            for (int block = 0; block < 64; block++) {
                long position = blockPosition(origin, block);
                int signature =
                        view.signatureAtWorld(
                                BlockPos.getX(position),
                                BlockPos.getY(position),
                                BlockPos.getZ(position));
                if (signatures.materialProfileId(signature) == 0
                        || signatures.ventilation(signature) == 0
                        || atPosition.containsKey(position)
                        || atPosition.size() >= maximumPositions) continue;
                seeds.add(position);
            }
        }
        int budget = VISITS_PER_CUT;
        while (budget >= 12 && (!pending.isEmpty() || !seeds.isEmpty())) {
            if (pending.isEmpty()) {
                long position = seeds.removeFirstLong();
                budget--;
                int signature =
                        view.signatureAtWorld(
                                BlockPos.getX(position),
                                BlockPos.getY(position),
                                BlockPos.getZ(position));
                if (atPosition.containsKey(position)
                        || atPosition.size() >= maximumPositions
                        || signatures.materialProfileId(signature) == 0
                        || signatures.ventilation(signature) == 0) continue;
                Component component = new Component();
                components.add(component);
                component.add(position, signature, view);
                pending.addLast(component);
            }
            Component component = pending.removeFirst();
            if (!component.alive) continue;
            int used = component.advance(view, budget);
            budget -= used;
            if (!component.ready && component.alive) {
                pending.addLast(component);
                if (used == 0) break;
            }
        }
        lastVisitCount = VISITS_PER_CUT - budget;
        includeDirtyFragments(view, fragments);
    }

    private void includeDirtyFragments(TopologyView view, IntOpenHashSet fragments) {
        for (long position : dirtyBricks) {
            int x = BlockPos.getX(position),
                    y = BlockPos.getY(position),
                    z = BlockPos.getZ(position);
            var page = view.page(section(position));
            int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
            if (page != null && view.resident(page, brick))
                fragments.add(page.fragmentIndex(brick));
        }
    }

    private void discard(Component component) {
        if (!component.alive) return;
        component.alive = false;
        if (component.validity != null) component.validity.retire();
        for (long position : component.positions) atPosition.remove(position, component);
        dirtyBricks.addAll(component.memberBricks);
        for (long brick : component.memberBricks) invalidatedSourceSections.add(section(brick));
        unpublished.remove(component);
        for (long section : component.dependencies.keySet()) {
            var indexed = bySection.get(section);
            if (indexed != null) {
                indexed.remove(component);
                if (indexed.isEmpty()) bySection.remove(section);
            }
        }
        for (long brick : component.edgeOwners) {
            var slices = edgesByBrick.get(brick);
            if (slices != null) {
                slices.removeIf(slice -> slice.component == component);
                if (slices.isEmpty()) edgesByBrick.remove(brick);
            }
        }
        components.remove(component);
    }

    boolean hasRegion(long position) {
        Component component = atPosition.get(position);
        return component != null
                && component.ready
                && component.alive
                && Double.isFinite(component.pathResistance[component.indexes.get(position)]);
    }

    long region(long position) {
        Component component = atPosition.get(position);
        return component.airRegionIds[component.indexes.get(position)];
    }

    boolean closed(long position) {
        Component component = atPosition.get(position);
        return component != null
                && component.alive
                && component.ready
                && component.portRegions.isEmpty()
                && component.missingBricks.isEmpty();
    }

    double normalizedResistance(long position) {
        Component component = atPosition.get(position);
        return component.pathResistance[component.indexes.get(position)];
    }

    BlockBrickLayout publishRoutes(long origin, BlockBrickLayout layout) {
        if (layout == null) return null;
        long mask = 0;
        long[] targets = null;
        AirRouteValidity[] validity = null;
        for (int block = 0; block < 64; block++) {
            long position = blockPosition(origin, block);
            if (!hasRegion(position)) continue;
            if (targets == null) {
                targets = new long[64];
                validity = new AirRouteValidity[64];
            }
            mask |= 1L << block;
            targets[block] = region(position);
            validity[block] = atPosition.get(position).validity;
        }
        return layout.withAirRoutes(mask, targets, validity);
    }

    void appendEdges(
            long ownerBrick,
            TopologyView view,
            double mixing,
            ThermalFragment.RoutedContacts.Builder pairs) {
        var slices = edgesByBrick.get(ownerBrick);
        if (slices == null) return;
        for (EdgeSlice slice : slices) {
            Component component = slice.component;
            for (int edge : slice.indexes) {
                int first = view.airSlotAt(component.edgeFirst[edge]);
                int second = view.airSlotAt(component.edgeSecond[edge]);
                if (first >= 0 && second >= 0 && first != second) {
                    pairs.add(
                            first,
                            second,
                            mixing / component.edgeResistance[edge],
                            component.validity);
                }
            }
        }
    }

    void collectRequiredBricks(WorkerPageStore pages, Long2LongOpenHashMap desired) {
        for (Component component : components) {
            boolean interested = false;
            for (long brick : component.memberBricks) {
                if (pages.hasThermalInterestAt(brick)) {
                    interested = true;
                    break;
                }
            }
            if (!interested) continue;
            for (long brick : component.memberBricks) requireBrick(desired, brick);
            if (component.ready && component.portRegions.isEmpty()) {
                for (long brick : component.missingBricks) requireBrick(desired, brick);
            }
        }
    }

    private static void requireBrick(Long2LongOpenHashMap desired, long position) {
        long section = section(position);
        int brick =
                (BlockPos.getX(position) & 15) >>> 2
                        | ((BlockPos.getZ(position) & 15) >>> 2) << 2
                        | ((BlockPos.getY(position) & 15) >>> 2) << 4;
        desired.put(section, desired.get(section) | 1L << brick);
    }

    private final class Component {
        final LongArrayList positions = new LongArrayList();
        final IntArrayList ventilation = new IntArrayList();
        final Long2IntOpenHashMap indexes = new Long2IntOpenHashMap();
        final Long2LongOpenHashMap dependencies = new Long2LongOpenHashMap();
        final LongOpenHashSet memberBricks = new LongOpenHashSet(),
                missingBricks = new LongOpenHashSet(),
                edgeOwners = new LongOpenHashSet();
        final IntArrayList portNodes = new IntArrayList();
        final LongArrayList portRegions = new LongArrayList();
        final DoubleArrayList portResistance = new DoubleArrayList();
        final Long2ObjectOpenHashMap<Long2DoubleOpenHashMap> candidates =
                new Long2ObjectOpenHashMap<>();
        boolean alive = true, ready;
        int stage, cursor, seedCursor;
        // Cumulative normalized resistance to an Air region, not geometric distance or heat.
        double[] pathResistance;
        long[] airRegionIds, edgeFirst, edgeSecond;
        double[] edgeResistance;
        int[] heap, heapPositions;
        int heapSize;
        AirRouteValidity validity;

        Component() {
            indexes.defaultReturnValue(-1);
        }

        void dependency(long position, TopologyView view) {
            long section = section(position);
            if (dependencies.containsKey(section)) return;
            var page = view.page(section);
            dependencies.put(section, page == null ? -1 : view.geometryRevision(page));
            bySection.computeIfAbsent(section, ignored -> new ObjectOpenHashSet<>()).add(this);
        }

        void add(long position, int signature, TopologyView view) {
            indexes.put(position, positions.size());
            positions.add(position);
            ventilation.add(signatures.ventilation(signature));
            atPosition.put(position, this);
            if (view.brickAtWorld(
                            BlockPos.getX(position),
                            BlockPos.getY(position),
                            BlockPos.getZ(position))
                    != null) {
                memberBricks.add(brick(position));
            }
            dirtyBricks.add(brick(position));
            dependency(position, view);
        }

        /** Resume the same search under this cut's visit budget; no stage restarts on yield. */
        int advance(TopologyView view, int budget) {
            int used = 0;
            while (alive && !ready && budget - used >= 12) {
                if (stage == DISCOVER_POSITIONS) {
                    if (cursor == positions.size()) {
                        pathResistance = new double[positions.size()];
                        Arrays.fill(pathResistance, Double.POSITIVE_INFINITY);
                        airRegionIds = new long[positions.size()];
                        heap = new int[positions.size()];
                        heapPositions = new int[positions.size()];
                        Arrays.fill(heapPositions, -1);
                        stage = SEED_AIR_PORTS;
                        continue;
                    }
                    int node = cursor++;
                    long position = positions.getLong(node);
                    int x = BlockPos.getX(position),
                            y = BlockPos.getY(position),
                            z = BlockPos.getZ(position);
                    for (int face = 0; face < 6; face++) {
                        long neighbor =
                                BlockPos.asLong(
                                        x + TopologyView.DX[face],
                                        y + TopologyView.DY[face],
                                        z + TopologyView.DZ[face]);
                        dependency(neighbor, view);
                        int signature =
                                view.signatureAtWorld(
                                        BlockPos.getX(neighbor),
                                        BlockPos.getY(neighbor),
                                        BlockPos.getZ(neighbor));
                        if (!signatures.valid(signature)) {
                            missingBricks.add(brick(neighbor));
                            continue;
                        }
                        if (signatures.isAir(signature)) {
                            int airSlot = view.airSlotAt(neighbor);
                            if (airSlot < 0) {
                                missingBricks.add(brick(neighbor));
                                continue;
                            }
                            long region = view.airRegionAt(neighbor);
                            int axis = face / 2;
                            double plane =
                                    (axis == 0 ? x : axis == 1 ? y : z) + (face % 2 == 0 ? 0 : 1);
                            double resistance =
                                    50.0 / ventilation.getInt(node)
                                            + Math.max(
                                                    0.5,
                                                    Math.abs(arena.center(airSlot, axis) - plane));
                            portNodes.add(node);
                            portRegions.add(region);
                            portResistance.add(resistance);
                            memberBricks.add(brick(region));
                        } else if (signatures.ventilation(signature) > 0
                                && indexes.get(neighbor) < 0) {
                            Component previous = atPosition.get(neighbor);
                            if (previous != null && previous != this) discard(previous);
                            if (atPosition.size() >= maximumPositions) {
                                missingBricks.add(brick(neighbor));
                                continue;
                            }
                            add(neighbor, signature, view);
                        }
                    }
                    used += 6;
                } else if (stage == SEED_AIR_PORTS) {
                    if (seedCursor == portNodes.size()) {
                        stage = PROPAGATE_RESISTANCE;
                        continue;
                    }
                    int port = seedCursor++;
                    improve(
                            portNodes.getInt(port),
                            portResistance.getDouble(port),
                            portRegions.getLong(port));
                    used++;
                } else if (stage == PROPAGATE_RESISTANCE) {
                    if (heapSize == 0) {
                        stage = BUILD_CONTACTS;
                        cursor = 0;
                        seedCursor = 0;
                        continue;
                    }
                    int node = pop();
                    long position = positions.getLong(node);
                    for (int face = 0; face < 6; face++) {
                        long neighbor = adjacent(position, face);
                        int next = indexes.get(neighbor);
                        if (next >= 0)
                            improve(
                                    next,
                                    pathResistance[node]
                                            + 50.0 / ventilation.getInt(node)
                                            + 50.0 / ventilation.getInt(next),
                                    airRegionIds[node]);
                    }
                    used += 6;
                } else {
                    if (cursor < positions.size()) {
                        int node = cursor++;
                        if (Double.isFinite(pathResistance[node]))
                            for (int face = 1; face < 6; face += 2) {
                                int next = indexes.get(adjacent(positions.getLong(node), face));
                                if (next >= 0 && Double.isFinite(pathResistance[next]))
                                    candidate(
                                            airRegionIds[node],
                                            airRegionIds[next],
                                            pathResistance[node]
                                                    + pathResistance[next]
                                                    + 50.0 / ventilation.getInt(node)
                                                    + 50.0 / ventilation.getInt(next));
                            }
                        used += 6;
                    } else if (seedCursor < portNodes.size()) {
                        int port = seedCursor++, node = portNodes.getInt(port);
                        candidate(
                                airRegionIds[node],
                                portRegions.getLong(port),
                                pathResistance[node] + portResistance.getDouble(port));
                        used++;
                    } else finish(view);
                }
            }
            return used;
        }

        void candidate(long first, long second, double resistance) {
            if (first == second || !Double.isFinite(resistance)) return;
            long low = Math.min(first, second), high = Math.max(first, second);
            var targets =
                    candidates.computeIfAbsent(
                            low,
                            ignored -> {
                                var result = new Long2DoubleOpenHashMap();
                                result.defaultReturnValue(Double.POSITIVE_INFINITY);
                                return result;
                            });
            if (resistance < targets.get(high)) targets.put(high, resistance);
        }

        void finish(TopologyView view) {
            var handles = new ArrayList<ThermalPageHandle>();
            var revisions = new LongArrayList();
            for (var dependency : dependencies.long2LongEntrySet()) {
                var page = view.page(dependency.getLongKey());
                if (page == null) continue;
                if (view.geometryRevision(page) != dependency.getLongValue()) {
                    discard(this);
                    return;
                }
                handles.add(page.handle);
                revisions.add(dependency.getLongValue());
            }
            validity =
                    new AirRouteValidity(
                            handles.toArray(ThermalPageHandle[]::new), revisions.toLongArray());
            int count = 0;
            for (var values : candidates.values()) count += values.size();
            edgeFirst = new long[count];
            edgeSecond = new long[count];
            edgeResistance = new double[count];
            var ownerIndexes = new Long2ObjectOpenHashMap<IntArrayList>();
            long[] sources = candidates.keySet().toLongArray();
            Arrays.sort(sources);
            int edge = 0;
            for (long first : sources) {
                var targets = candidates.get(first);
                long[] seconds = targets.keySet().toLongArray();
                Arrays.sort(seconds);
                for (long second : seconds) {
                    edgeFirst[edge] = first;
                    edgeSecond[edge] = second;
                    edgeResistance[edge] = targets.get(second);
                    ownerIndexes
                            .computeIfAbsent(brick(first), ignored -> new IntArrayList())
                            .add(edge++);
                }
            }
            for (var owner : ownerIndexes.long2ObjectEntrySet()) {
                edgesByBrick
                        .computeIfAbsent(owner.getLongKey(), ignored -> new ArrayList<>())
                        .add(new EdgeSlice(this, owner.getValue()));
                edgeOwners.add(owner.getLongKey());
            }
            ready = true;
            unpublished.add(this);
            dirtyBricks.addAll(memberBricks);
            candidates.clear();
        }

        void improve(int node, double candidateResistance, long airRegionId) {
            if (candidateResistance > pathResistance[node]
                    || candidateResistance == pathResistance[node]
                            && airRegionId >= airRegionIds[node]) return;
            pathResistance[node] = candidateResistance;
            airRegionIds[node] = airRegionId;
            int index = heapPositions[node];
            if (index < 0) {
                index = heapSize++;
                heap[index] = node;
                heapPositions[node] = index;
            }
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (!less(heap[index], heap[parent])) break;
                swap(index, parent);
                index = parent;
            }
        }

        int pop() {
            int node = heap[0];
            heapPositions[node] = -1;
            if (--heapSize == 0) return node;
            heap[0] = heap[heapSize];
            heapPositions[heap[0]] = 0;
            int index = 0;
            while (index * 2 + 1 < heapSize) {
                int child = index * 2 + 1;
                if (child + 1 < heapSize && less(heap[child + 1], heap[child])) child++;
                if (!less(heap[child], heap[index])) break;
                swap(index, child);
                index = child;
            }
            return node;
        }

        boolean less(int first, int second) {
            int order = Double.compare(pathResistance[first], pathResistance[second]);
            if (order != 0) return order < 0;
            order = Long.compare(airRegionIds[first], airRegionIds[second]);
            return order != 0 ? order < 0 : positions.getLong(first) < positions.getLong(second);
        }

        void swap(int first, int second) {
            int value = heap[first];
            heap[first] = heap[second];
            heap[second] = value;
            heapPositions[heap[first]] = first;
            heapPositions[heap[second]] = second;
        }
    }

    private record EdgeSlice(Component component, IntArrayList indexes) {}

    static long brickPosition(WorkerPageStore.PageState page, int brick) {
        return BlockPos.asLong(
                SectionPos.sectionToBlockCoord(SectionPos.x(page.handle.sectionKey()))
                        + (brick & 3) * 4,
                SectionPos.sectionToBlockCoord(SectionPos.y(page.handle.sectionKey()))
                        + (brick >>> 4 & 3) * 4,
                SectionPos.sectionToBlockCoord(SectionPos.z(page.handle.sectionKey()))
                        + (brick >>> 2 & 3) * 4);
    }

    static long blockPosition(long origin, int block) {
        return BlockPos.asLong(
                BlockPos.getX(origin) + (block & 3),
                BlockPos.getY(origin) + (block >>> 4),
                BlockPos.getZ(origin) + (block >>> 2 & 3));
    }

    private static long adjacent(long position, int face) {
        return BlockPos.asLong(
                BlockPos.getX(position) + TopologyView.DX[face],
                BlockPos.getY(position) + TopologyView.DY[face],
                BlockPos.getZ(position) + TopologyView.DZ[face]);
    }

    private static long brick(long position) {
        return BlockPos.asLong(
                BlockPos.getX(position) & ~3,
                BlockPos.getY(position) & ~3,
                BlockPos.getZ(position) & ~3);
    }

    private static long section(long position) {
        return SectionPos.asLong(
                BlockPos.getX(position) >> 4,
                BlockPos.getY(position) >> 4,
                BlockPos.getZ(position) >> 4);
    }
}
