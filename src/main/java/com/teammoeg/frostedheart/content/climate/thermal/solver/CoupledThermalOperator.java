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
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;

/** Regional projection system and compiled edges for symmetric pairwise transport. */
final class CoupledThermalOperator {
    private static final int PAIRWISE_EDGE_COUNT = 12;
    private static final int[] PAIRWISE_FIRST_CORNERS = new int[PAIRWISE_EDGE_COUNT];
    private static final int[] PAIRWISE_SECOND_CORNERS = new int[PAIRWISE_EDGE_COUNT];
    private static final int[] PAIRWISE_MATRIX_ENTRIES = new int[PAIRWISE_EDGE_COUNT];
    static {
        int edge = 0, entry = 0;
        for (int row = 0; row < 8; row++) for (int column = row; column < 8; column++, entry++) {
            if (Integer.bitCount(row ^ column) != 1) continue;
            PAIRWISE_FIRST_CORNERS[edge] = row;
            PAIRWISE_SECOND_CORNERS[edge] = column;
            PAIRWISE_MATRIX_ENTRIES[edge++] = entry;
        }
    }
    // One fragment per component; volumeStarts preserves the original per-cell contribution addresses.
    final AirOperatorFragment[] volumes;
    private final int[] volumeStarts;
    final double[] massJPerK;
    final double[] buoyancyFactors;
    LocalMaterialBoundary materialBoundary;
    private final int[] surfaceNodes, surfaceBodies;
    private final double[] surfaceConductance;
    private final int[] contactOffsets;
    private final int[] contactIndices;
    private final ContactWeightTable contactWeights;
    private final double[] conductanceWPerK;
    private final int[] boundaryPages;
    private final AirRouteValidity[] routes;
    private final double[] activeConductanceWPerK;
    private final double[] boundaryOffsetC;
    private boolean frozenEnvironment;
    private double frozenWindScale;
    private double[] frozenBoundaryOffsetC;
    private boolean[] frozenRouteActivity;
    private final int airUnknownCount;
    private final int[] transportFirst, transportSecond;
    private final double[] transportOffDiagonal;
    private final int[] pairwiseVolumeEdgePositions;
    private double contactWindScale;
    private final boolean transportGraphReused;
    private final long transportAssemblyNanos;
    private final long contactEncodingNanos, transportPlanNanos, transportStructureNanos;
    private final String transportReuseFailure;
    private final long reusedWorkspaceBytes;
    private long immutableBackingBytes = -1;

    private CoupledThermalOperator(AirFieldLayout layout, AirOperatorFragment[] volumes,
            CoupledThermalOperator previous, Preparation prepared) {
        long assemblyStarted = System.nanoTime();
        int airUnknownCount = layout.coefficientCount();
        this.airUnknownCount = airUnknownCount;
        this.volumes = volumes;
        volumeStarts = prepared.volumeStarts;
        surfaceNodes = prepared.surfaceNodes; surfaceBodies = prepared.surfaceBodies;
        surfaceConductance = prepared.surfaceConductance;
        massJPerK = prepared.mass;
        buoyancyFactors = workspace(volumes.length, previous == null ? null : previous.buoyancyFactors);
        contactOffsets = prepared.offsets;
        contactIndices = prepared.indices;
        contactWeights = prepared.weights;
        conductanceWPerK = prepared.conductance;
        boundaryPages = prepared.pages;
        routes = prepared.routes;
        activeConductanceWPerK = workspace(conductanceWPerK.length, previous == null ? null : previous.activeConductanceWPerK);
        boundaryOffsetC = workspace(conductanceWPerK.length, previous == null ? null : previous.boundaryOffsetC);
        long planningStarted = System.nanoTime();
        contactEncodingNanos = planningStarted - assemblyStarted;
        transportReuseFailure = prepared.reuseFailure;
        transportGraphReused = transportReuseFailure == null;
        long structureStarted = System.nanoTime();
        transportPlanNanos = structureStarted - planningStarted;
        long structureElapsed;
        int[] pairwisePositions = null;
        {
            if (transportGraphReused) {
                transportFirst = previous.transportFirst;
                transportSecond = previous.transportSecond;
                pairwisePositions = previous.pairwiseVolumeEdgePositions;
            } else {
                PairwiseGraph graph = prepared.graph;
                transportFirst = graph.first;
                transportSecond = graph.second;
                pairwisePositions = graph.positions;
            }
            structureElapsed = System.nanoTime() - structureStarted;
            transportOffDiagonal = workspace(transportFirst.length, previous == null ? null : previous.transportOffDiagonal);
        }
        pairwiseVolumeEdgePositions = pairwisePositions;
        transportStructureNanos = structureElapsed;
        transportAssemblyNanos = System.nanoTime() - assemblyStarted;
        if (AirBackingMeter.debugEnabled()) {
            try (var workspaces = new AirBackingMeter(); var oldWorkspaces = new AirBackingMeter()) {
                recordWorkspaceBacking(workspaces);
                long candidateWorkspaceBytes = workspaces.bytes();
                if (previous != null) previous.recordWorkspaceBacking(oldWorkspaces);
                if (previous != null) previous.recordWorkspaceBacking(workspaces);
                reusedWorkspaceBytes = candidateWorkspaceBytes + oldWorkspaces.bytes() - workspaces.bytes();
            }
        } else reusedWorkspaceBytes = -1;
    }

    /** Builds the shared pairwise operator without rerunning a completed graph or palette pass. */
    static final class Preparation {
        final AirFieldLayout layout;
        final AirOperatorFragment[] volumes;
        final int[] volumeStarts;
        final Contacts contacts;
        final CoupledThermalOperator previous;
        final Runnable graphAdmission;
        final double[] mass, conductance;
        final int[] offsets, indices, pages;
        final int[] surfaceNodes, surfaceBodies;
        final double[] surfaceConductance;
        final AirRouteValidity[] routes;
        ContactWeightTable.Preparation weightPreparation;
        ContactWeightTable weights;
        PairwiseGraphPreparation graphPreparation;
        PairwiseGraph graph;
        String reuseFailure;
        int phase, cursor;
        long volumeCells;
        CoupledThermalOperator result;
        Preparation(AirFieldLayout layout, AirOperatorFragment[] volumes, Contacts contacts,
                CoupledThermalOperator previous, Runnable graphAdmission) {
            this.layout = layout; this.volumes = volumes; this.contacts = contacts;
            this.previous = previous;
            this.graphAdmission = graphAdmission;
            volumeStarts = new int[volumes.length + 1];
            mass = new double[layout.coefficientCount()];
            offsets = new int[contacts.offsets.size()]; indices = new int[contacts.indices.size()];
            int count = contacts.conductances.size();
            pages = new int[count];
            conductance = new double[count]; routes = new AirRouteValidity[count];
            surfaceNodes = new int[contacts.surfaceNodes.size()]; surfaceBodies = new int[contacts.surfaceBodies.size()];
            surfaceConductance = new double[surfaceBodies.length];
            reuseFailure = previous == null ? "cold" : layout.coefficientCount() != previous.airUnknownCount ? "air_rows"
                    : volumes.length != previous.volumes.length ? "volume_count" : null;
        }
        boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int remaining = 128;
                    while (remaining-- > 0 && cursor < volumes.length) {
                        var volume = volumes[cursor];
                        if (volumeCells == 0) {
                            volumeCells = volume.cells();
                            volumeStarts[cursor + 1] = Math.addExact(volumeStarts[cursor], Long.bitCount(volumeCells));
                            if (reuseFailure == null && volumeCells != previous.volumes[cursor].cells()) reuseFailure = "volume_cells";
                            if (volumeCells == 0) { cursor++; continue; }
                        }
                        int cell = Long.numberOfTrailingZeros(volumeCells);
                        for (int row = 0; row < 8; row++) mass[volume.index(cell, row)] += volume.capacityJPerK[row];
                        if (reuseFailure == null) {
                            var old = previous.volumes[cursor];
                            for (int row = 0; row < 8; row++)
                                if (volume.index(cell, row) != old.index(cell, row)) reuseFailure = "volume_addresses";
                            if (reuseFailure == null && (volume.horizontal(cell) != old.horizontal(cell) || volume.vertical(cell) != old.vertical(cell))) {
                                int entry = 0;
                                for (int row = 0; row < 8; row++)
                                    for (int column = row; column < 8; column++, entry++)
                                        if (row != column && transportEntry(volume, cell, entry) != transportEntry(old, cell, entry))
                                            reuseFailure = "volume_structure";
                            }
                        }
                        volumeCells &= volumeCells - 1;
                        if (volumeCells == 0) cursor++;
                    }
                    if (cursor == volumes.length) { phase++; cursor = 0; }
                } else if (phase == 1) {
                    int end = Math.min(offsets.length, cursor + 4096);
                    System.arraycopy(contacts.offsets.elements(), cursor, offsets, cursor, end - cursor); cursor = end;
                    if (cursor == offsets.length) { phase++; cursor = 0; }
                } else if (phase == 2) {
                    int end = Math.min(indices.length, cursor + 4096);
                    System.arraycopy(contacts.indices.elements(), cursor, indices, cursor, end - cursor); cursor = end;
                    if (cursor == indices.length) { phase++; cursor = 0; }
                } else if (phase == 3) {
                    int count = Math.max(conductance.length, surfaceNodes.length);
                    int end = Math.min(count, cursor + 256);
                    for (; cursor < end; cursor++) {
                        if (cursor < conductance.length) {
                            conductance[cursor] = contacts.conductances.getDouble(cursor); pages[cursor] = contacts.boundaryPages.getInt(cursor);
                            routes[cursor] = contacts.routes.get(cursor);
                        }
                        if (cursor < surfaceNodes.length) surfaceNodes[cursor] = contacts.surfaceNodes.getInt(cursor);
                        if (cursor < surfaceBodies.length) {
                            surfaceBodies[cursor] = contacts.surfaceBodies.getInt(cursor);
                            surfaceConductance[cursor] = contacts.surfaceConductance.getDouble(cursor);
                        }
                    }
                    if (cursor == count) { phase++; cursor = 0; }
                } else if (phase == 4) {
                    if (weightPreparation == null) weightPreparation = new ContactWeightTable.Preparation(contacts.weights, previous == null ? null : previous.contactWeights);
                    if (!weightPreparation.run(deadline)) return false;
                    weights = weightPreparation.result(); weightPreparation = null; phase++;
                } else if (phase == 5) {
                    if (reuseFailure == null) graph = new PairwiseGraph(previous.transportFirst, previous.transportSecond, previous.pairwiseVolumeEdgePositions);
                    else {
                        if (graphPreparation == null) {
                            if (graphAdmission != null) graphAdmission.run();
                            graphPreparation = new PairwiseGraphPreparation(volumes, layout.coefficientCount(), volumeStarts[volumes.length]);
                        }
                        if (!graphPreparation.run(deadline)) return false;
                        graph = graphPreparation.result(); graphPreparation = null;
                    }
                    phase++;
                } else {
                    result = new CoupledThermalOperator(layout, volumes, previous, this);
                    return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        CoupledThermalOperator result() { return result; }
    }

    /**
     * Only derived work arrays are borrowed. Preparation overwrites their active
     * ranges before use; accepted Air/H/branches, immutable M/K and nominal
     * contacts remain independent. A refused candidate's old operator is prepared
     * again before the next transport step.
     */
    private static double[] workspace(int required, double[] previous) {
        if (required == 0) return new double[0];
        if (previous != null && previous.length >= required) return previous;
        return new double[Math.addExact(required, Math.max(256, required / 8))];
    }

    private void recordWorkspaceBacking(AirBackingMeter meter) {
        meter.array(buoyancyFactors);
        meter.array(activeConductanceWPerK); meter.array(boundaryOffsetC);
        meter.array(transportOffDiagonal);
    }

    private static boolean transportEntry(AirOperatorFragment volume, int cell, int entry) {
        return volume.horizontal(cell)[entry] != 0 || volume.vertical(cell)[entry] != 0;
    }

    String assemblyReuseSummary() {
        return "staticGraphReused=" + transportGraphReused
                + ",transportAssemblyUs=" + transportAssemblyNanos / 1000
                + ",contactEncodingUs=" + contactEncodingNanos / 1000
                + ",transportPlanUs=" + transportPlanNanos / 1000
                + ",transportStructureUs=" + transportStructureNanos / 1000
                + ",transportReuseFailure=" + transportReuseFailure
                + ",reusedWorkspaceBytes=" + reusedWorkspaceBytes + "," + contactWeights.describe();
    }

    int surfaceContactCount() { return surfaceNodes.length; }
    int surfaceBody(int contact) { return surfaceBodies[contact >>> 2]; }
    // Logical contacts/entries start with four singleton points per ordinary face.
    // Only the suffix uses variable-width contact rows and route/environment storage.
    int contactStart(int contact) {
        return contact < surfaceNodes.length ? contact : surfaceNodes.length + contactOffsets[contact - surfaceNodes.length];
    }
    int contactEnd(int contact) {
        return contact < surfaceNodes.length ? contact + (surfaceNodes[contact] < 0 ? 0 : 1)
                : surfaceNodes.length + contactOffsets[contact - surfaceNodes.length + 1];
    }
    int contactRow(int entry) { return entry < surfaceNodes.length ? surfaceNodes[entry] : contactIndices[entry - surfaceNodes.length]; }
    double contactWeight(int entry) { return entry < surfaceNodes.length ? 1 : contactWeights.get(entry - surfaceNodes.length); }
    double nominalContactConductance(int contact) {
        return contact < surfaceNodes.length ? surfaceConductance[contact >>> 2] : conductanceWPerK[contact - surfaceNodes.length];
    }
    double activeContactConductance(int contact) {
        return contact < surfaceNodes.length ? (materialBoundary.contactWaiting(contact) ? 0 : surfaceConductance[contact >>> 2])
                : activeConductanceWPerK[contact - surfaceNodes.length];
    }
    boolean environmentContact(int contact) { return contact >= surfaceNodes.length && boundaryPages[contact - surfaceNodes.length] >= 0; }
    boolean contactRouteActive(int contact) {
        return contact < surfaceNodes.length || routes[contact - surfaceNodes.length] == null || !routes[contact - surfaceNodes.length].retired();
    }
    String contactModeSummary() {
        return "compiled=true,trial=SHARED_GRID,projectionOperator=nodal_control_volume"
                + ",materialSurfaceOperator=assembled"
                + ",advancement=symmetric_pairwise,projection=diagonal_direct"
                + ",windScale=" + contactWindScale;
    }

    void recordBacking(AirBackingMeter meter) {
        if (meter.summaries()) {
            if (immutableBackingBytes < 0) {
                try (var local = new AirBackingMeter()) { recordImmutableBacking(local); immutableBackingBytes = local.bytes(); }
            }
            meter.objectOnce(this, immutableBackingBytes);
        } else recordImmutableBacking(meter);
        // These arrays can be allocated or released after construction. Never cache them.
        if (materialBoundary != null) materialBoundary.recordBacking(meter);
        meter.array(frozenBoundaryOffsetC); meter.array(frozenRouteActivity);
    }

    private void recordImmutableBacking(AirBackingMeter meter) {
        meter.array(volumes);
        meter.array(volumeStarts);
        for (AirOperatorFragment volume : volumes) volume.recordBacking(meter);
        meter.array(massJPerK);
        meter.array(buoyancyFactors);
        meter.array(surfaceNodes); meter.array(surfaceBodies); meter.array(surfaceConductance);
        meter.array(contactOffsets);
        meter.array(contactIndices);
        contactWeights.recordBacking(meter);
        meter.array(conductanceWPerK);
        meter.array(boundaryPages);
        meter.array(routes);
        meter.array(activeConductanceWPerK);
        meter.array(boundaryOffsetC);
        meter.array(transportFirst);
        meter.array(transportSecond);
        meter.array(transportOffDiagonal);
        meter.array(pairwiseVolumeEdgePositions);
        if (pairwiseVolumeEdgePositions != null) {
            meter.array(PAIRWISE_FIRST_CORNERS); meter.array(PAIRWISE_SECOND_CORNERS);
            meter.array(PAIRWISE_MATRIX_ENTRIES);
        }
    }

    ProjectionPreparation prepareProjection(double[] rhs, double[] solution, double toleranceC) {
        return new ProjectionPreparation(massJPerK, rhs, solution, airUnknownCount, toleranceC);
    }

    /** Projects private candidate Air from inherited nodal energy, using the transport's same mass. */
    static final class ProjectionPreparation {
        private final double[] mass, rhs, solution;
        private final int size;
        private final double toleranceC;
        private int cursor;

        ProjectionPreparation(double[] mass, double[] rhs, double[] solution, int size, double toleranceC) {
            this.mass = mass; this.rhs = rhs; this.solution = solution;
            this.size = size; this.toleranceC = toleranceC;
        }

        boolean run(long deadline) {
            do {
                int end = Math.min(size, cursor + 1024);
                for (; cursor < end; cursor++) {
                    double capacity = mass[cursor], energy = rhs[cursor];
                    double value = energy / capacity;
                    double errorC = (energy - capacity * value) / capacity;
                    if (!(capacity > 0) || !Double.isFinite(capacity) || !Double.isFinite(value)
                            || !Double.isFinite(errorC) || Math.abs(errorC) > toleranceC)
                        throw new IllegalStateException("Air diagonal projection failed: row=" + cursor
                                + ",massJPerK=" + capacity + ",rhsJ=" + energy + ",valueC=" + value
                                + ",errorC=" + errorC + ",toleranceC=" + toleranceC);
                    solution[cursor] = value;
                }
                if (cursor == size) return true;
            } while (System.nanoTime() < deadline);
            return false;
        }
    }

    /** Contact activity and environment stay fixed throughout one material/buoyancy trial. */
    void freezeEnvironment(double windScale, java.util.function.IntToDoubleFunction naturalTemperature, double referenceC) {
        if (frozenBoundaryOffsetC == null) frozenBoundaryOffsetC = new double[conductanceWPerK.length];
        if (frozenRouteActivity == null) frozenRouteActivity = new boolean[conductanceWPerK.length];
        frozenWindScale = windScale;
        for (int contact = 0; contact < conductanceWPerK.length; contact++) {
            frozenBoundaryOffsetC[contact] = boundaryPages[contact] >= 0
                    ? naturalTemperature.applyAsDouble(boundaryPages[contact]) - referenceC : 0;
            frozenRouteActivity[contact] = routes[contact] == null || routes[contact].activeForSolve();
        }
        frozenEnvironment = true;
    }

    void releaseEnvironment() { frozenEnvironment = false; }

    int pairwiseContactCount() { return surfaceNodes.length + conductanceWPerK.length; }
    int pairwiseEdgeCount() { return transportFirst.length; }

    void refreshContacts(ThermalSolver geometry, double referenceC, int from, int to) {
        contactWindScale = frozenEnvironment ? frozenWindScale : geometry.windScale();
        for (int contact = Math.max(from, surfaceNodes.length); contact < to; contact++) {
            int index = contact - surfaceNodes.length;
            boolean paused = materialBoundary != null && materialBoundary.contactWaiting(contact)
                    || (frozenEnvironment ? !frozenRouteActivity[index]
                            : routes[index] != null && !routes[index].activeForSolve());
            boolean boundary = boundaryPages[index] >= 0;
            activeConductanceWPerK[index] = paused ? 0 : conductanceWPerK[index] * (boundary ? contactWindScale : 1);
            boundaryOffsetC[index] = frozenEnvironment ? frozenBoundaryOffsetC[index]
                    : boundary ? geometry.naturalTemperatureC(boundaryPages[index]) - referenceC : 0;
        }
    }

    private static long pairKey(int first, int second) {
        int low = Math.min(first, second), high = Math.max(first, second);
        return (long) low << 32 | (high & 0xffffffffL);
    }

    private record PairwiseGraph(int[] first, int[] second, int[] positions) {}

    /** Array/output bound for changed unit-grid regions, before any graph allocation. */
    static long pairwiseGraphPreparationBytes(long volumes, long maximumEdges, long maximumRegions) {
        if (volumes == 0) return 0;
        long occurrences = Math.multiplyExact(volumes, PAIRWISE_EDGE_COUNT);
        // Input occurrences keep multiplicity; output endpoints use the proved
        // local edge bound (joining across Bricks can only reduce that count).
        // Eight vertices per voxel is an upper bound on the eventual node count.
        // If even that bound fits the packed key, the real compiled layout will fit.
        boolean packed = packedPairwiseKey(Math.multiplyExact(volumes, 8L), occurrences);
        long keys = Math.multiplyExact(occurrences, packed ? Long.BYTES : 2L * Long.BYTES);
        // The radix cursor is released before allocating the unique endpoints.
        // Keys and occurrence positions remain; sort backing and output endpoints
        // occupy successive phases, so reserve their maximum, not their sum.
        long arrays = Math.addExact(Math.multiplyExact(occurrences, Integer.BYTES),
                Math.addExact(keys, Math.max(keys, Math.multiplyExact(maximumEdges, 2L * Integer.BYTES))));
        return Math.addExact(arrays, Math.multiplyExact(maximumRegions, 160L));
    }

    /** Paired fastutil radix sort uses fixed stacks/buckets; quicksort allocates no arrays. */
    static long pairwiseSortScratchBytes(int sorters) { return Math.multiplyExact(sorters, 64L * 1024L); }

    private static int indexBits(long count) {
        return count <= 1 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(count - 1);
    }

    private static boolean packedPairwiseKey(long nodes, long positions) {
        // Keep the sign bit clear, so ordinary signed long sorting retains the
        // original (first node, second node, occurrence address) ordering.
        return 2 * indexBits(nodes) + indexBits(positions) <= Long.SIZE - 1;
    }

    /** Same sorted axial pairs and original occurrence scatter; every traversal can resume. */
    private static final class PairwiseGraphPreparation {
        final AirOperatorFragment[] volumes;
        final int positionCount, nodeBits, positionBits;
        final boolean packed;
        final long positionMask, nodeMask;
        int phase, volume, fragment, count, cursor, occurrence, unique, edge = -1;
        long cells;
        long[] keys, occurrences;
        int[] positions, first, second;
        com.teammoeg.frostedheart.content.climate.thermal.runtime.LongRadixSortCursor sort;
        PairwiseGraphPreparation(AirOperatorFragment[] volumes, int airUnknownCount, int volumeCount) {
            this.volumes = volumes;
            positionCount = Math.multiplyExact(volumeCount, PAIRWISE_EDGE_COUNT);
            packed = packedPairwiseKey(airUnknownCount, positionCount);
            nodeBits = indexBits(airUnknownCount); positionBits = indexBits(positionCount);
            positionMask = (1L << positionBits) - 1; nodeMask = (1L << nodeBits) - 1;
        }
        boolean run(long deadline) {
            do {
                if (phase == 0 || phase == 2) {
                    int remaining = 128;
                    while (remaining-- > 0 && fragment < volumes.length) {
                        var item = volumes[fragment];
                        if (cells == 0) { cells = item.cells(); if (cells == 0) { fragment++; continue; } }
                        int cell = Long.numberOfTrailingZeros(cells);
                        for (int ordinal = 0; ordinal < PAIRWISE_EDGE_COUNT; ordinal++) {
                            int row = PAIRWISE_FIRST_CORNERS[ordinal], column = PAIRWISE_SECOND_CORNERS[ordinal];
                            int first = item.index(cell, row), second = item.index(cell, column);
                            if (first == second || !transportEntry(item, cell, PAIRWISE_MATRIX_ENTRIES[ordinal])) continue;
                            if (phase == 0) count++;
                            else {
                                int a = Math.min(first, second), b = Math.max(first, second);
                                if (packed) keys[occurrence++] = (((long) a << nodeBits | b) << positionBits) | (volume * PAIRWISE_EDGE_COUNT + ordinal);
                                else { keys[occurrence] = pairKey(a, b); occurrences[occurrence++] = volume * PAIRWISE_EDGE_COUNT + ordinal; }
                            }
                        }
                        volume++; cells &= cells - 1;
                        if (cells == 0) fragment++;
                    }
                    if (fragment == volumes.length) {
                        volume = fragment = 0; phase++;
                        if (phase == 1) { keys = new long[count]; occurrences = packed ? null : new long[count]; positions = new int[positionCount]; }
                    }
                } else if (phase == 1) {
                    int end = Math.min(positionCount, cursor + 4096);
                    Arrays.fill(positions, cursor, end, -1); cursor = end;
                    if (cursor == positionCount) { phase++; cursor = 0; }
                } else if (phase == 3) {
                    // Contributions already arrive in address order. Stable pair sorting
                    // preserves that order without sorting the packed address bits again.
                    if (sort == null) sort = new com.teammoeg.frostedheart.content.climate.thermal.runtime.LongRadixSortCursor(
                            keys, occurrences, packed ? positionBits : 0, packed ? 2 * nodeBits : Long.SIZE);
                    if (!sort.run(deadline)) return false;
                    keys = sort.sortedKeys(); occurrences = sort.sortedPaired();
                    sort = null; phase++; cursor = 0;
                } else if (phase == 4) {
                    int end = Math.min(count, cursor + 4096);
                    for (; cursor < end; cursor++) if (cursor == 0 || pair(cursor) != pair(cursor - 1)) unique++;
                    if (cursor == count) { first = new int[unique]; second = new int[unique]; cursor = 0; phase++; }
                } else if (phase == 5) {
                    int end = Math.min(count, cursor + 4096);
                    for (; cursor < end; cursor++) {
                        long pair = pair(cursor);
                        if (cursor == 0 || pair != pair(cursor - 1)) {
                            edge++; first[edge] = (int) (pair >>> (packed ? nodeBits : Integer.SIZE));
                            second[edge] = (int) (packed ? pair & nodeMask : pair);
                        }
                        positions[(int) (packed ? keys[cursor] & positionMask : occurrences[cursor])] = edge;
                    }
                    if (cursor == count) return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        private long pair(int index) { return packed ? keys[index] >>> positionBits : keys[index]; }
        PairwiseGraph result() { return new PairwiseGraph(first, second, positions); }
    }
    void clearPairwiseEdges(int from, int to) {
        Arrays.fill(transportOffDiagonal, from, to, 0);
    }

    void accumulatePairwiseVolumes(int from, int to) {
        for (int fragment = from; fragment < to; fragment++) {
            AirOperatorFragment volume = volumes[fragment];
            int base = volumeStarts[fragment] * PAIRWISE_EDGE_COUNT;
            long cells = volume.cells();
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                double[] horizontal = volume.horizontal(cell), vertical = volume.vertical(cell);
                for (int ordinal = 0; ordinal < PAIRWISE_EDGE_COUNT; ordinal++) {
                    int edge = pairwiseVolumeEdgePositions[base + ordinal];
                    int entry = PAIRWISE_MATRIX_ENTRIES[ordinal];
                    if (edge >= 0) transportOffDiagonal[edge] -= horizontal[entry] + buoyancyFactors[fragment] * vertical[entry];
                }
                base += PAIRWISE_EDGE_COUNT;
            }
        }
    }

    void preparePairwiseFactors(double halfSeconds, int from, int to) {
        for (int edge = from; edge < to; edge++) {
            double g = transportOffDiagonal[edge];
            if (g == 0) continue;
            double inverse = 1 / massJPerK[transportFirst[edge]] + 1 / massJPerK[transportSecond[edge]];
            transportOffDiagonal[edge] = -Math.expm1(-g * inverse * halfSeconds) / inverse;
        }
    }

    void sweepPairs(double[] temperatures, boolean reverse, int from, int to) {
        for (int ordinal = from; ordinal < to; ordinal++) {
            int edge = reverse ? transportFirst.length - 1 - ordinal : ordinal;
            double exchangeJPerK = transportOffDiagonal[edge];
            if (exchangeJPerK == 0) continue;
            int first = transportFirst[edge], second = transportSecond[edge];
            double energy = (temperatures[second] - temperatures[first]) * exchangeJPerK;
            temperatures[first] += energy / massJPerK[first];
            temperatures[second] -= energy / massJPerK[second];
        }
    }

    /** Exact rank-one boundary/route flow; finite material surfaces use their actual H law. */
    void sweepPairwiseContacts(double[] temperatures, double[] h, byte[] branches, double halfSeconds,
            double referenceC, boolean reverse, int from, int to) {
        for (int ordinal = from; ordinal < to; ordinal++) {
            int contact = reverse ? pairwiseContactCount() - 1 - ordinal : ordinal;
            double g = activeContactConductance(contact);
            if (g == 0) continue;
            if (materialBoundary.contactBody(contact) >= 0) {
                materialBoundary.exchangeAirContact(contact, temperatures, h, branches, halfSeconds, referenceC);
                continue;
            }
            double trace = -boundaryOffsetC[contact - surfaceNodes.length], inverse = 0;
            for (int entry = contactStart(contact); entry < contactEnd(contact); entry++) {
                int row = contactRow(entry); double weight = contactWeight(entry);
                trace += weight * temperatures[row];
                inverse += weight * weight / massJPerK[row];
            }
            if (inverse == 0) continue;
            double energy = trace * -Math.expm1(-g * inverse * halfSeconds) / inverse;
            for (int entry = contactStart(contact); entry < contactEnd(contact); entry++) {
                int row = contactRow(entry);
                temperatures[row] -= contactWeight(entry) * energy / massJPerK[row];
            }
        }
    }

    /** Compile-time builder; stable numerical steps read only the resulting primitive arrays. */
    static final class Contacts {
        // Ordinary fine faces precede routed/boundary contacts and retain their four-point order.
        final IntArrayList surfaceNodes = new IntArrayList(), surfaceBodies = new IntArrayList();
        final DoubleArrayList surfaceConductance = new DoubleArrayList();
        int surfaceTerms;
        final IntArrayList offsets = new IntArrayList(new int[]{0});
        final IntArrayList indices = new IntArrayList();
        final DoubleArrayList weights = new DoubleArrayList();
        final DoubleArrayList conductances = new DoubleArrayList();
        final IntArrayList boundaryPages = new IntArrayList();
        final ArrayList<AirRouteValidity> routes = new ArrayList<>();
        final IntArrayList localBodies = new IntArrayList();
        private Int2IntOpenHashMap termPositions;
        private int indexedStart = -1;

        void surface(int body, double conductance, int[] nodes) {
            surfaceBodies.add(body); surfaceConductance.add(conductance);
            for (int point = 0; point < 4; point++) {
                surfaceNodes.add(nodes[point]);
                if (nodes[point] >= 0) surfaceTerms++;
            }
        }

        void term(int index, double weight) {
            if (weight == 0) return;
            int start = offsets.getInt(offsets.size() - 1);
            // Dense source overlap makes the duplicate lookup quadratic even when
            // most terms are distinct. Preserve insertion/summation order with
            // one reusable index, and keep small ordinary contacts allocation-free.
            if (indices.size() - start >= 16) {
                if (termPositions == null) {
                    termPositions = new Int2IntOpenHashMap();
                    termPositions.defaultReturnValue(-1);
                }
                if (indexedStart != start) {
                    termPositions.clear();
                    for (int entry = start; entry < indices.size(); entry++)
                        termPositions.put(indices.getInt(entry), entry);
                    indexedStart = start;
                }
                int entry = termPositions.get(index);
                if (entry >= 0) weights.set(entry, weights.getDouble(entry) + weight);
                else {
                    termPositions.put(index, indices.size());
                    indices.add(index);
                    weights.add(weight);
                }
                return;
            }
            for (int entry = start; entry < indices.size(); entry++) {
                if (indices.getInt(entry) == index) {
                    weights.set(entry, weights.getDouble(entry) + weight);
                    return;
                }
            }
            indices.add(index);
            weights.add(weight);
        }

        void finish(double conductance, int boundaryPage, AirRouteValidity route) {
            offsets.add(indices.size());
            conductances.add(conductance);
            boundaryPages.add(boundaryPage);
            routes.add(route);
            localBodies.add(-1);
            indexedStart = -1;
        }

        void finishMaterialSurface(double conductance, int body, AirRouteValidity route) {
            finish(conductance, -1, route);
            localBodies.set(localBodies.size() - 1, body);
        }

    }
}
