/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
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
    // A preparation owns only its delta. The committed directory is never
    // invalidated until that preparation is installed by the coordinator.
    private final AirRouteCompiler committed;
    private final ObjectOpenHashSet<Component> removed = new ObjectOpenHashSet<>();
    private final ObjectOpenHashSet<Component> borrowed = new ObjectOpenHashSet<>();
    private int removedPositions;
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
    private final ObjectOpenHashSet<Component> unpublished = new ObjectOpenHashSet<>();
    private long lastVisitCount;

    AirRouteCompiler(
            ThermalSignatureTable signatures, ThermalCellArena arena, int maximumPositions) {
        this(signatures, arena, maximumPositions, null);
    }

    private AirRouteCompiler(ThermalSignatureTable signatures, ThermalCellArena arena,
            int maximumPositions, AirRouteCompiler committed) {
        this.signatures = signatures;
        this.arena = arena;
        this.maximumPositions = maximumPositions;
        this.committed = committed;
    }

    AirRouteCompiler forPreparation() {
        return new AirRouteCompiler(signatures, arena, maximumPositions, this);
    }

    void recordBacking(AirBackingMeter meter) {
        meter.container(atPosition, atPosition.size(), 16); meter.container(bySection, bySection.size(), 16);
        meter.container(edgesByBrick, edgesByBrick.size(), 16); meter.container(components, components.size(), 8);
        meter.container(pending, components.size(), 8); meter.container(seeds, seeds.size(), 8);
        meter.container(dirtyBricks, dirtyBricks.size(), 8); meter.container(changedSections, changedSections.size(), 8);
        meter.container(unpublished, unpublished.size(), 8); meter.container(removed, removed.size(), 8);
        meter.container(borrowed, borrowed.size(), 8);
        for (var indexed : bySection.values()) meter.container(indexed, indexed.size(), 8);
        for (var slices : edgesByBrick.values()) {
            meter.container(slices, slices.size(), 8);
            for (var slice : slices) { meter.objectOnce(slice, 32); meter.array(slice.indexes.elements()); }
        }
        for (var component : components) component.recordBacking(meter);
    }

    /** Local directory capacities and cached component payloads; never walk all route positions. */
    long preparationBackingBytes() {
        long bytes = AirBackingMeter.containerBackingBytes(atPosition, atPosition.size(), 16)
                + AirBackingMeter.containerBackingBytes(bySection, bySection.size(), 16)
                + AirBackingMeter.containerBackingBytes(edgesByBrick, edgesByBrick.size(), 16)
                + AirBackingMeter.containerBackingBytes(components, components.size(), 8)
                + AirBackingMeter.containerBackingBytes(pending, components.size(), 8)
                + AirBackingMeter.containerBackingBytes(seeds, seeds.size(), 8)
                + AirBackingMeter.containerBackingBytes(dirtyBricks, dirtyBricks.size(), 8)
                + AirBackingMeter.containerBackingBytes(changedSections, changedSections.size(), 8)
                + AirBackingMeter.containerBackingBytes(unpublished, unpublished.size(), 8)
                + AirBackingMeter.containerBackingBytes(removed, removed.size(), 8)
                + AirBackingMeter.containerBackingBytes(borrowed, borrowed.size(), 8);
        for (var indexed : bySection.values()) bytes += AirBackingMeter.containerBackingBytes(indexed, indexed.size(), 8);
        for (var slices : edgesByBrick.values()) {
            bytes += AirBackingMeter.containerBackingBytes(slices, slices.size(), 8);
            for (var slice : slices) bytes += 32L + AirBackingMeter.shallowArrayBytes(slice.indexes.elements());
        }
        for (var component : components) bytes += component.backingBytes;
        return bytes;
    }

    /** Route components and edge slices survive the local directory's installation. */
    void recordInstalledBacking(AirBackingMeter meter) {
        for (var component : components) component.recordBacking(meter);
        for (var slices : edgesByBrick.values()) for (var slice : slices) {
            meter.objectOnce(slice, 32); meter.array(slice.indexes.elements());
        }
    }

    long preparationGrowthBytes() {
        // One resumable search can add at most VISITS_PER_CUT positions/ports.
        // Finishing a component additionally materializes its already known
        // candidate edges and outlet arrays, rather than a world-sized estimate.
        long bytes = 1024L * VISITS_PER_CUT;
        for (var component : pending) {
            bytes += 128L * component.positions.size() + 256L * component.portNodes.size();
            bytes += 128L * component.candidateCount;
        }
        return bytes;
    }

    private Component at(long position) {
        Component local = atPosition.get(position);
        if (local != null || committed == null) return local;
        Component installed = committed.at(position);
        if (installed != null) borrowed.add(installed);
        return removed.contains(installed) ? null : installed;
    }

    private ObjectOpenHashSet<Component> inSection(long section) {
        var found = new ObjectOpenHashSet<Component>();
        if (committed != null) {
            var installed = committed.bySection.get(section);
            if (installed != null) for (var component : installed)
                if (!removed.contains(component)) found.add(component);
        }
        var local = bySection.get(section);
        if (local != null) found.addAll(local);
        return found;
    }

    boolean dependenciesCurrent() {
        for (var component : removed) if (!component.alive) return false;
        for (var component : borrowed) if (!component.alive) return false;
        return true;
    }

    void discardPreparation() {
        if (committed == null) return;
        for (var component : components) if (component.validity != null) component.validity.retire();
        components.clear(); atPosition.clear(); bySection.clear(); edgesByBrick.clear();
        pending.clear(); seeds.clear(); dirtyBricks.clear(); unpublished.clear(); removed.clear(); borrowed.clear(); removedPositions = 0;
    }

    boolean hasPendingWork() {
        return !pending.isEmpty() || !seeds.isEmpty() || !dirtyBricks.isEmpty();
    }

    long lastVisitCount() {
        return lastVisitCount;
    }

    boolean discoveryIncomplete() { return !pending.isEmpty() || !seeds.isEmpty(); }
    int capturedPositionCount() { return atPosition.size() + (committed == null ? 0 : committed.atPosition.size() - removedPositions); }
    int dependencySectionCount() { return bySection.size() + (committed == null ? 0 : committed.bySection.size()); }
    int componentCountBound() { return components.size() + (committed == null ? 0 : committed.components.size()); }
    long pendingSectionCountBound() {
        long count = dirtyBricks.size() + (long) seeds.size();
        for (var component : pending) if (component.alive) count += component.dependencies.size();
        return count;
    }

    /** Includes previously captured passage dependencies, even outside resident Pages. */
    boolean hasDependencySection(long section) {
        return bySection.containsKey(section) || committed != null && committed.bySection.containsKey(section);
    }

    /** Read-only origins of unfinished searches; never resumes or completes a route here. */
    void includePendingSections(LongOpenHashSet sections, LongArrayList queue) {
        // A refused candidate may leave completed but uncommitted route changes.
        // They still hold their local regions until ordinary topology installation.
        for (long position : dirtyBricks) {
            long key = section(position);
            if (sections.add(key)) queue.add(key);
        }
        for (long position : seeds) {
            long key = section(position);
            if (sections.add(key)) queue.add(key);
        }
        for (Component component : pending) if (component.alive)
            for (long key : component.dependencies.keySet()) if (sections.add(key)) queue.add(key);
    }

    /** Read-only transitive dependency closure, including routes with only one Air port. */
    void expandAffectedSections(LongOpenHashSet sections, LongArrayList queue) {
        var visited = new ObjectOpenHashSet<Component>();
        for (int index = 0; index < queue.size(); index++) {
            long section = queue.getLong(index);
            if (committed != null) expandSection(committed.bySection.get(section), sections, queue, visited);
            expandSection(bySection.get(section), sections, queue, visited);
        }
    }
    private void expandSection(ObjectOpenHashSet<Component> affected, LongOpenHashSet sections,
            LongArrayList queue, ObjectOpenHashSet<Component> visited) {
        if (affected == null) return;
        for (Component component : affected) if (component.alive && !removed.contains(component) && visited.add(component))
            for (long dependency : component.dependencies.keySet())
                if (sections.add(dependency)) queue.add(dependency);
    }

    void beginCut() {
        lastVisitCount = 0;
    }

    void committed() {
        if (committed != null) {
            for (var component : removed) committed.discard(component);
            for (var component : components) if (component.alive) {
                committed.components.add(component);
                for (long position : component.positions) committed.atPosition.put(position, component);
                for (long section : component.dependencies.keySet())
                    committed.bySection.computeIfAbsent(section, ignored -> new ObjectOpenHashSet<>()).add(component);
            }
            for (var entry : edgesByBrick.long2ObjectEntrySet())
                committed.edgesByBrick.computeIfAbsent(entry.getLongKey(), ignored -> new ArrayList<>()).addAll(entry.getValue());
            committed.dirtyBricks.clear();
            removed.clear(); removedPositions = 0;
        }
        for (Component component : unpublished) if (component.alive) component.validity.commit();
        unpublished.clear();
        dirtyBricks.clear();
    }

    AirRouteValidity validity(long position) {
        return at(position).validity;
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
            var affected = inSection(section);
            for (Component component : affected.toArray(Component[]::new)) discard(component);
        }
    }

    void beginPrepare(TopologyView view, IntOpenHashSet fragments) {
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
                        || at(position) != null
                        || capturedPositionCount() >= maximumPositions) continue;
                seeds.add(position);
            }
        }
    }

    boolean advancePreparation(TopologyView view, IntOpenHashSet fragments) {
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
                if (at(position) != null
                        || capturedPositionCount() >= maximumPositions
                        || signatures.materialProfileId(signature) == 0
                        || signatures.ventilation(signature) == 0) continue;
                Component component = new Component();
                components.add(component);
                component.add(position, signature, view, this);
                pending.addLast(component);
            }
            Component component = pending.removeFirst();
            if (!component.alive) continue;
            int used = component.advance(view, budget, this);
            if (component.alive) component.refreshBackingBytes();
            budget -= used;
            if (!component.ready && component.alive) {
                pending.addLast(component);
                if (used == 0) break;
            }
        }
        lastVisitCount += VISITS_PER_CUT - budget;
        includeDirtyFragments(view, fragments);
        return !discoveryIncomplete();
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

    private void markComponentDirty(Component component) {
        dirtyBricks.addAll(component.memberBricks);
        dirtyBricks.addAll(component.edgeOwners);
    }
    /** Immutable candidate-only invalidation; the published field keeps its live routes. */
    java.util.Set<AirRouteValidity> retiringRoutes() {
        var routes = new java.util.HashSet<AirRouteValidity>();
        for (var component : removed) if (component.validity != null) routes.add(component.validity);
        return java.util.Set.copyOf(routes);
    }

    private void discard(Component component) {
        if (!component.alive) return;
        if (committed != null && !components.contains(component)) {
            if (removed.add(component)) {
                removedPositions += component.positions.size();
                markComponentDirty(component);
            }
            return;
        }
        component.alive = false;
        if (component.validity != null) component.validity.retire();
        for (long position : component.positions) atPosition.remove(position, component);
        markComponentDirty(component);
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
        Component component = at(position);
        return component != null
                && component.ready
                && component.alive
                && Double.isFinite(component.pathResistance[component.indexes.get(position)]);
    }

    long region(long position) {
        Component component = at(position);
        return component.airRegionIds[component.indexes.get(position)];
    }

    boolean closed(long position) {
        Component component = at(position);
        return component != null
                && component.alive
                && component.ready
                && component.portRegions.isEmpty()
                && component.missingBricks.isEmpty();
    }

    double normalizedResistance(long position) {
        Component component = at(position);
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
            validity[block] = at(position).validity;
        }
        return layout.withAirRoutes(mask, targets, validity);
    }

    void appendEdges(
            long ownerBrick,
            TopologyView view,
            double mixing,
            ThermalFragment.RoutedContacts.Builder pairs) {
        if (committed != null) appendEdges(committed.edgesByBrick.get(ownerBrick), view, mixing, pairs);
        appendEdges(edgesByBrick.get(ownerBrick), view, mixing, pairs);
    }

    private void appendEdges(ArrayList<EdgeSlice> slices, TopologyView view, double mixing,
            ThermalFragment.RoutedContacts.Builder pairs) {
        if (slices == null) return;
        for (EdgeSlice slice : slices) {
            Component component = slice.component;
            if (removed.contains(component)) continue;
            for (int edge : slice.indexes) {
                int first = view.airSlotAt(component.edgeFirst[edge]);
                int second = view.airSlotAt(component.edgeSecond[edge]);
                if (first >= 0 && second >= 0 && first != second) {
                    pairs.add(
                            first,
                            second,
                            mixing / component.edgeResistance[edge],
                            component.validity,
                            component.outletTraces.get(component.edgeFirst[edge]),
                            component.outletTraces.get(component.edgeSecond[edge]));
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

    private static final class Component {
        final LongArrayList positions = new LongArrayList();
        final IntArrayList ventilation = new IntArrayList();
        final Long2IntOpenHashMap indexes = new Long2IntOpenHashMap();
        final Long2LongOpenHashMap dependencies = new Long2LongOpenHashMap();
        final LongOpenHashSet memberBricks = new LongOpenHashSet(),
                missingBricks = new LongOpenHashSet(),
                edgeOwners = new LongOpenHashSet();
        final IntArrayList portNodes = new IntArrayList();
        final LongArrayList portRegions = new LongArrayList();
        final LongArrayList portAirBlocks = new LongArrayList();
        final it.unimi.dsi.fastutil.bytes.ByteArrayList portAirFaces = new it.unimi.dsi.fastutil.bytes.ByteArrayList();
        final Long2ObjectOpenHashMap<ThermalFragment.AirFaceTrace> outletTraces = new Long2ObjectOpenHashMap<>();
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
        int[] outletPortIndices;
        ThermalFragment.AirFaceTrace[] portTraces;
        int heapSize;
        int candidateCount;
        long backingBytes;
        AirRouteValidity validity;

        Component() {
            indexes.defaultReturnValue(-1);
        }

        void recordBacking(AirBackingMeter meter) {
            if (meter.summaries()) { meter.objectOnce(this, backingBytes); return; }
            if (!meter.objectOnce(this, 256)) return;
            meter.array(positions.elements()); meter.array(ventilation.elements());
            meter.container(indexes, positions.size(), 12); meter.container(dependencies, dependencies.size(), 16);
            meter.container(memberBricks, memberBricks.size(), 8); meter.container(missingBricks, missingBricks.size(), 8);
            meter.container(edgeOwners, edgeOwners.size(), 8); meter.array(portNodes.elements());
            meter.array(portRegions.elements()); meter.array(portAirBlocks.elements()); meter.array(portAirFaces.elements());
            meter.array(portResistance.elements()); meter.container(outletTraces, outletTraces.size(), 16);
            meter.container(candidates, candidates.size(), 16);
            for (var candidate : candidates.values()) meter.container(candidate, candidate.size(), 16);
            meter.array(pathResistance); meter.array(airRegionIds); meter.array(edgeFirst); meter.array(edgeSecond);
            meter.array(edgeResistance); meter.array(heap); meter.array(heapPositions); meter.array(outletPortIndices);
            meter.array(portTraces);
            if (portTraces != null) for (var trace : portTraces) if (trace != null) {
                meter.objectOnce(trace, 40); meter.array(trace.blocks()); meter.array(trace.faces());
            }
            for (var trace : outletTraces.values()) {
                meter.objectOnce(trace, 40); meter.array(trace.blocks()); meter.array(trace.faces());
            }
            if (validity != null) validity.recordBacking(meter);
        }

        /** Refresh only the component that actually progressed in this route slice. */
        void refreshBackingBytes() {
            long bytes = 256L + AirBackingMeter.shallowArrayBytes(positions.elements())
                    + AirBackingMeter.shallowArrayBytes(ventilation.elements())
                    + AirBackingMeter.containerBackingBytes(indexes, positions.size(), 12)
                    + AirBackingMeter.containerBackingBytes(dependencies, dependencies.size(), 16)
                    + AirBackingMeter.containerBackingBytes(memberBricks, memberBricks.size(), 8)
                    + AirBackingMeter.containerBackingBytes(missingBricks, missingBricks.size(), 8)
                    + AirBackingMeter.containerBackingBytes(edgeOwners, edgeOwners.size(), 8)
                    + AirBackingMeter.shallowArrayBytes(portNodes.elements()) + AirBackingMeter.shallowArrayBytes(portRegions.elements())
                    + AirBackingMeter.shallowArrayBytes(portAirBlocks.elements()) + AirBackingMeter.shallowArrayBytes(portAirFaces.elements())
                    + AirBackingMeter.shallowArrayBytes(portResistance.elements())
                    + AirBackingMeter.containerBackingBytes(outletTraces, outletTraces.size(), 16)
                    + AirBackingMeter.containerBackingBytes(candidates, candidates.size(), 16)
                    + AirBackingMeter.shallowArrayBytes(pathResistance) + AirBackingMeter.shallowArrayBytes(airRegionIds)
                    + AirBackingMeter.shallowArrayBytes(edgeFirst) + AirBackingMeter.shallowArrayBytes(edgeSecond)
                    + AirBackingMeter.shallowArrayBytes(edgeResistance) + AirBackingMeter.shallowArrayBytes(heap)
                    + AirBackingMeter.shallowArrayBytes(heapPositions) + AirBackingMeter.shallowArrayBytes(outletPortIndices)
                    + AirBackingMeter.shallowArrayBytes(portTraces);
            for (var targets : candidates.values()) bytes += AirBackingMeter.containerBackingBytes(targets, targets.size(), 16);
            if (portTraces != null) {
                bytes += 40L * (portTraces.length + outletTraces.size());
                // All traces of one component share the same blocks/faces arrays.
                if (portTraces.length != 0) bytes += AirBackingMeter.shallowArrayBytes(portTraces[0].blocks())
                        + AirBackingMeter.shallowArrayBytes(portTraces[0].faces());
            }
            if (validity != null) bytes += validity.backingBytes();
            backingBytes = bytes;
        }

        void dependency(long position, TopologyView view, AirRouteCompiler owner) {
            long section = section(position);
            if (dependencies.containsKey(section)) return;
            var page = view.page(section);
            dependencies.put(section, page == null ? -1 : view.geometryRevision(page));
            owner.bySection.computeIfAbsent(section, ignored -> new ObjectOpenHashSet<>()).add(this);
        }

        void add(long position, int signature, TopologyView view, AirRouteCompiler owner) {
            indexes.put(position, positions.size());
            positions.add(position);
            ventilation.add(owner.signatures.ventilation(signature));
            owner.atPosition.put(position, this);
            if (view.brickAtWorld(
                            BlockPos.getX(position),
                            BlockPos.getY(position),
                            BlockPos.getZ(position))
                    != null) {
                memberBricks.add(brick(position));
            }
            owner.dirtyBricks.add(brick(position));
            dependency(position, view, owner);
        }

        /** Resume the same search under this cut's visit budget; no stage restarts on yield. */
        int advance(TopologyView view, int budget, AirRouteCompiler owner) {
            var signatures = owner.signatures;
            var arena = owner.arena;
            int used = 0;
            while (alive && !ready && budget - used >= 12) {
                if (stage == DISCOVER_POSITIONS) {
                    if (cursor == positions.size()) {
                        pathResistance = new double[positions.size()];
                        Arrays.fill(pathResistance, Double.POSITIVE_INFINITY);
                        airRegionIds = new long[positions.size()];
                        outletPortIndices = new int[positions.size()];
                        Arrays.fill(outletPortIndices, -1);
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
                        dependency(neighbor, view, owner);
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
                            portAirBlocks.add(neighbor);
                            portAirFaces.add((byte) (face ^ 1));
                            portResistance.add(resistance);
                            memberBricks.add(brick(region));
                        } else if (signatures.ventilation(signature) > 0
                                && indexes.get(neighbor) < 0) {
                            Component previous = owner.at(neighbor);
                            if (previous != null && previous != this) owner.discard(previous);
                            if (owner.capturedPositionCount() >= owner.maximumPositions) {
                                missingBricks.add(brick(neighbor));
                                continue;
                            }
                            add(neighbor, signature, view, owner);
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
                            portRegions.getLong(port), port);
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
                                    airRegionIds[node], outletPortIndices[node]);
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
                    } else finish(view, owner);
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
            double previous = targets.get(high);
            if (resistance < previous) {
                if (!Double.isFinite(previous)) candidateCount++;
                targets.put(high, resistance);
            }
        }

        void finish(TopologyView view, AirRouteCompiler owner) {
            var portsByRegion = new Long2ObjectOpenHashMap<IntArrayList>();
            for (int port = 0; port < portRegions.size(); port++) {
                portsByRegion.computeIfAbsent(portRegions.getLong(port), ignored -> new IntArrayList()).add(port);
            }
            long[] blocks = new long[portNodes.size()];
            byte[] faces = new byte[portNodes.size()];
            portTraces = new ThermalFragment.AirFaceTrace[portNodes.size()];
            long[] regions = portsByRegion.keySet().toLongArray();
            Arrays.sort(regions);
            int nextPort = 0;
            for (long region : regions) {
                IntArrayList ports = portsByRegion.get(region);
                int firstPort = nextPort;
                for (int index = 0; index < ports.size(); index++) {
                    int port = ports.getInt(index);
                    blocks[nextPort] = portAirBlocks.getLong(port);
                    faces[nextPort] = portAirFaces.getByte(port);
                    portTraces[port] = new ThermalFragment.AirFaceTrace(blocks, faces, nextPort, 1);
                    nextPort++;
                }
                outletTraces.put(region, new ThermalFragment.AirFaceTrace(blocks, faces, firstPort, ports.size()));
            }
            var handles = new ArrayList<ThermalPageHandle>();
            var revisions = new LongArrayList();
            for (var dependency : dependencies.long2LongEntrySet()) {
                var page = view.page(dependency.getLongKey());
                if (page == null) continue;
                if (view.geometryRevision(page) != dependency.getLongValue()) {
                    owner.discard(this);
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
            for (var entry : ownerIndexes.long2ObjectEntrySet()) {
                owner.edgesByBrick
                        .computeIfAbsent(entry.getLongKey(), ignored -> new ArrayList<>())
                        .add(new EdgeSlice(this, entry.getValue()));
                edgeOwners.add(entry.getLongKey());
            }
            ready = true;
            owner.unpublished.add(this);
            owner.dirtyBricks.addAll(memberBricks);
            candidates.clear();
            candidateCount = 0;
        }

        void improve(int node, double candidateResistance, long airRegionId, int outletPort) {
            if (candidateResistance > pathResistance[node]
                    || candidateResistance == pathResistance[node]
                            && airRegionId >= airRegionIds[node]) return;
            pathResistance[node] = candidateResistance;
            airRegionIds[node] = airRegionId;
            outletPortIndices[node] = outletPort;
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

    ThermalFragment.AirFaceTrace outletTrace(long position) {
        Component component = at(position);
        if (component == null || !component.ready) return null;
        int index = component.indexes.get(position);
        if (index < 0 || component.outletPortIndices[index] < 0) return null;
        return component.portTraces[component.outletPortIndices[index]];
    }

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
