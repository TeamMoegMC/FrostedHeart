/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirGeometryView;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;

/** Summarizes detailed state; transport edges are prepared only for an executing minimum model. */
final class MinimumThermalModel {
    /** Region-owned cache of geometry-only input addresses, retained across temperature commits. */
    static final class InputPlan {
        private final java.util.HashMap<com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target, Mapping>
                entries = new java.util.HashMap<>();
        private Object identity;
        private AirFieldLayout air;
        private int capacity;
        private long hits, misses;
        long hits() { return hits; }
        long misses() { return misses; }
        static long bytes(int targets) { return 512L + 384L * targets; }
        long retainedBytes() { return bytes(capacity); }
        void reserve(int targets) { capacity = Math.max(capacity, targets); }
        void begin(RegionalQuerySnapshot field) {
            var next = field.minimumState().layout().inputMappingIdentity();
            if (next != identity || air != field.airLayout()) {
                entries.clear(); identity = next; air = field.airLayout();
            }
        }
        Mapping get(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target target,
                RegionalQuerySnapshot field, java.util.function.ToIntFunction<com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target> materialSlot) {
            var cached = entries.get(target);
            if (cached != null) { hits++; return cached; }
            misses++;
            var next = resolve(target, field, materialSlot);
            if (next.count > 0 && entries.size() < capacity) entries.put(target, next);
            return next;
        }
        static Mapping resolve(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target target,
                RegionalQuerySnapshot field, java.util.function.ToIntFunction<com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target> materialSlot) {
            long block = target.position();
            int x = net.minecraft.core.BlockPos.getX(block), y = net.minecraft.core.BlockPos.getY(block), z = net.minecraft.core.BlockPos.getZ(block);
            var layout = field.minimumState().layout();
            if (target.face() == com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.MATERIAL) {
                int slot = materialSlot.applyAsInt(target), body = field.materialBodyIndex(slot, target.incarnation());
                if (body < 0) return Mapping.EMPTY;
                int explicit = layout.phaseIndex(slot);
                int node = explicit < 0 ? layout.materialNode(x + .5, y + .5, z + .5) : MinimumThermalState.POOLED_NODES + explicit;
                return node < 0 ? Mapping.EMPTY : new Mapping(new int[] {node}, new double[] {1}, 1, body);
            }
            if (field.airLayout().componentAt(x, y, z) == null) return Mapping.EMPTY;
            int[] nodes = new int[8]; double[] weights = new double[8];
            int count = layout.airInputWeights(x + .5, y + .5, z + .5, nodes, weights);
            return count == 0 ? Mapping.EMPTY : new Mapping(nodes, weights, count, -1);
        }
    }
    static final class Mapping {
        static final Mapping EMPTY = new Mapping(new int[0], new double[0], 0, -1);
        final int[] nodes;
        final double[] weights;
        final int count, body;
        Mapping(int[] nodes, double[] weights, int count, int body) {
            this.nodes = nodes; this.weights = weights; this.count = count; this.body = body;
        }
        void add(double joules, double[] destination, int offset) {
            double assigned = 0;
            for (int i = 0; i < count; i++) {
                double amount = i == count - 1 ? joules - assigned : joules * weights[i];
                destination[offset + nodes[i]] += amount; assigned += amount;
            }
        }
    }
    private static final int NODES = MinimumThermalState.MAXIMUM_NODES;
    private static final int AIR = MinimumThermalState.AIR_NODES;
    private static final int POOLED = MinimumThermalState.POOLED_NODES;
    private final MinimumThermalState.Layout layout;
    private final double blockCapacity;
    private final long[] airPositions;
    private final double[] airCapacity;
    private final double[] bodyBackgroundC;
    private final int[] bodyNodes, phaseBodies;

    private MinimumThermalModel(Preparation prepared) {
        layout = prepared.layout; blockCapacity = prepared.blockCapacity;
        airPositions = prepared.air.isUnitGrid() ? prepared.airPositions : new long[0];
        airCapacity = prepared.air.isUnitGrid() ? prepared.mass : new double[0];
        bodyNodes = prepared.bodyNodes; phaseBodies = prepared.phaseBodies;
        bodyBackgroundC = prepared.bodyBackgroundC;
    }

    static long preparationBytes(int air, int bodies) {
        return preparationBytes(air, bodies, true);
    }
    static long preparationBytes(int air, int bodies, boolean transport) {
        // One Air row may support eight coarse nodes; material bodies use one pool.
        // Only nodes with physical heat capacity enter the compact pair table.
        int nodes = (int) Math.min(AIR, 8L * air) + Math.min(NODES - AIR, bodies);
        int exportedPairs = transport ? nodes * (nodes - 1) / 2 : 0;
        return 4096L + AirBackingMeter.arrayPayloadBytes(air, 1)
                + AirBackingMeter.arrayPayloadBytes(air, 8)
                + AirBackingMeter.arrayPayloadBytes(air, 8)
                + AirBackingMeter.arrayPayloadBytes(air, 8)
                + AirBackingMeter.arrayPayloadBytes(bodies, 4)
                + AirBackingMeter.arrayPayloadBytes(bodies, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(NODES, 8)
                + 128L * Math.min(3200, air + (long) bodies)
                + AirBackingMeter.arrayPayloadBytes(NODES, 4)
                + AirBackingMeter.arrayPayloadBytes(exportedPairs, 8)
                + AirBackingMeter.arrayPayloadBytes(NODES, 8)
                + AirBackingMeter.arrayPayloadBytes(exportedPairs, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(NODES, 8)
                + 2 * AirBackingMeter.arrayPayloadBytes(exportedPairs, 4)
                + AirBackingMeter.arrayPayloadBytes(exportedPairs, 8);
    }

    static long projectionBytes() {
        return 1024L + 4 * AirBackingMeter.arrayPayloadBytes(NODES, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(AIR, 8)
                + 2 * AirBackingMeter.arrayPayloadBytes(MinimumThermalState.PHASE_BODIES, 1);
    }

    void recordBacking(AirBackingMeter meter) {
        if (!meter.objectOnce(this, 64)) return;
        layout.recordBacking(meter);
        meter.array(airPositions); meter.array(airCapacity);
        meter.array(bodyNodes); meter.array(phaseBodies); meter.array(bodyBackgroundC);
    }

    Projection project(RegionalQuerySnapshot detailed) { return new Projection(detailed); }
    MinimumThermalState.Layout layout() { return layout; }
    double[] backgroundOffsetsC() { return layout.background().borrowedAirOffsetsC(); }

    final class Projection {
        private final RegionalQuerySnapshot detailed;
        private final double[] energy = new double[NODES], references = new double[NODES];
        private final double[] capacities = layout.copyCapacities();
        private final double[] materialTemperatureMass = new double[AIR];
        private final double[] backgroundTemperatureMass = new double[AIR];
        private final double[] backgroundC = layout.background().copyNodesC();
        private final byte[] phaseBranches = new byte[phaseBodies.length];
        private final boolean[] phaseWaiting = new boolean[phaseBodies.length];
        private MinimumThermalState result;
        private RegionalQuerySnapshot.MaterialAnchor anchor;
        private int phase, cursor;
        private long blocks;
        private boolean componentStarted;
        private final int[] nodes = new int[8];
        private final double[] weights = new double[8];

        Projection(RegionalQuerySnapshot detailed) { this.detailed = detailed; }

        boolean run(long deadline) {
            if (result != null) return true;
            do {
                if (phase == 0) {
                    var coefficients = detailed.borrowedAirCoefficients();
                    var air = detailed.airLayout();
                    if (air.isUnitGrid()) {
                        // Unit-grid masses already sum the same eight cell-side vertices.
                        int end = Math.min(airPositions.length, cursor + 512);
                        for (; cursor < end; cursor++) {
                            long position = airPositions[cursor];
                            int count = layout.airInputWeights(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position), nodes, weights);
                            double amount = airCapacity[cursor] * (referenceTemperature() + coefficients[cursor]);
                            for (int index = 0; index < count; index++) energy[nodes[index]] += amount * weights[index];
                        }
                        if (cursor == airPositions.length) { phase++; cursor = 0; }
                        continue;
                    }
                    int remaining = 64;
                    while (remaining-- > 0 && cursor < air.componentCount()) {
                        var component = air.component(cursor);
                        if (!componentStarted) { blocks = component.airBlocks; componentStarted = true; }
                        if (blocks == 0) { cursor++; componentStarted = false; continue; }
                        int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                        for (int point = 0; point < 8; point++) {
                            double x = component.minX + (block & 3) + (point & 1);
                            double y = component.minY + (block >>> 4) + (point >>> 2);
                            double z = component.minZ + (block >>> 2 & 3) + (point >>> 1 & 1);
                            double temperature = referenceTemperature() + component.temperatureOffsetAtAirCell(block, x, y, z, coefficients);
                            int count = layout.airInputWeights(x, y, z, nodes, weights);
                            for (int index = 0; index < count; index++) energy[nodes[index]] += blockCapacity * .125 * weights[index] * temperature;
                        }
                    }
                    if (cursor == air.componentCount()) { phase++; cursor = 0; }
                } else if (phase == 1) {
                    int end = Math.min(bodyNodes.length, cursor + 512);
                    for (; cursor < end; cursor++) {
                        int node = bodyNodes[cursor];
                        var law = detailed.materialLaw(cursor);
                        double h = detailed.materialEnthalpyJ(cursor);
                        energy[node] += h;
                        if (node < POOLED) {
                            if (detailed.materialWaiting(cursor)) capacities[node] = Math.max(0, capacities[node] - law.capacityJPerK());
                            else {
                                materialTemperatureMass[node - AIR] += law.capacityJPerK() * law.temperatureC(h, detailed.materialBranch(cursor));
                                backgroundTemperatureMass[node - AIR] += law.capacityJPerK() * bodyBackgroundC[cursor];
                            }
                        }
                        else {
                            int index = node - POOLED;
                            phaseBranches[index] = detailed.materialBranch(cursor);
                            var transition = law.transition(phaseBranches[index]);
                            phaseWaiting[index] = transition != null && transition.complete(h);
                        }
                    }
                    if (cursor == bodyNodes.length) { phase++; cursor = 0; }
                } else {
                    double[] initialTemperature = new double[AIR];
                    for (int node = AIR; node < POOLED; node++) {
                        if (capacities[node] > 0) {
                            initialTemperature[node - AIR] = materialTemperatureMass[node - AIR] / capacities[node];
                            backgroundC[node] = backgroundTemperatureMass[node - AIR] / capacities[node];
                        }
                        references[node] = energy[node] - materialTemperatureMass[node - AIR];
                    }
                    result = new MinimumThermalState(layout.withPoolState(capacities, references,
                            layout.background().withNodesC(backgroundC)), energy, phaseBranches, phaseWaiting);
                    anchor = detailed.materialAnchor(bodyNodes, initialTemperature);
                    return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private double referenceTemperature() { return detailed.referenceTemperatureC(); }
        MinimumThermalState result() { return result; }
        RegionalQuerySnapshot.MaterialAnchor anchor() { return anchor; }
    }

    static final class Preparation {
        private final AirFieldLayout air;
        private final CoupledThermalOperator operator;
        private final AirOperatorFragment[] volumes;
        private final CoupledThermalOperator.Contacts contacts;
        private final double[] mass;
        private final LocalMaterialBoundary material;
        private final ThermalFragment[] fragments;
        private final int[] slots;
        private final ThermalCellArena arena;
        private final AirGeometryView geometry;
        private final double referenceC;
        private final boolean transport;
        private final Int2DoubleLinkedOpenHashMap pageBackgroundC = new Int2DoubleLinkedOpenHashMap();
        private final double[] airBackgroundOffsetsC, bodyBackgroundC;
        private final double[] nodeBackgroundMass = new double[NODES];
        private final long[] airPositions;
        private final boolean[] airMapped;
        private final int[] bodyNodes;
        private int[] phaseBodies;
        private final double[] capacity = new double[NODES], boundary = new double[NODES];
        private final int[] pairRows = new int[NODES];
        private int pairNodes;
        private double[] pairs;
        private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        private double spanX, spanY, spanZ;
        private double totalAirCapacity, blockCapacity;
        private long totalAirBlocks, blocks;
        private boolean componentStarted;
        private final int[] firstNodes = new int[8], secondNodes = new int[8];
        private final double[] firstWeights = new double[8], secondWeights = new double[8];
        private int phase, cursor, basis, phaseCount, volume, row, column = 1, contactEntry, otherEntry;
        private int fragment, edge;
        private long volumeCells;
        private MinimumThermalState.Layout layout;
        private MinimumThermalModel result;

        Preparation(AirFieldLayout air, CoupledThermalOperator operator, LocalMaterialBoundary material,
                int[] slots, ThermalCellArena arena, AirGeometryView geometry, double referenceC) {
            this(air, operator, material, slots, arena, geometry, referenceC, false);
        }
        Preparation(AirFieldLayout air, CoupledThermalOperator operator, LocalMaterialBoundary material,
                int[] slots, ThermalCellArena arena, AirGeometryView geometry, double referenceC, boolean transport) {
            this(air, operator, operator.volumes, null, material, null, slots, arena, geometry, referenceC, transport);
        }

        /** Minimum construction uses local volumes and contacts, never a global operator or PCG. */
        Preparation(AirFieldLayout air, AirOperatorFragment[] volumes, CoupledThermalOperator.Contacts contacts,
                ThermalFragment[] fragments, int[] slots, ThermalCellArena arena, AirGeometryView geometry, double referenceC) {
            this(air, null, volumes, contacts, null, fragments, slots, arena, geometry, referenceC, true);
        }

        private Preparation(AirFieldLayout air, CoupledThermalOperator operator, AirOperatorFragment[] volumes,
                CoupledThermalOperator.Contacts contacts, LocalMaterialBoundary material, ThermalFragment[] fragments, int[] slots, ThermalCellArena arena,
                AirGeometryView geometry, double referenceC, boolean transport) {
            this.air = air; this.operator = operator; this.volumes = volumes; this.contacts = contacts;
            this.material = material; this.fragments = fragments; this.slots = slots; this.arena = arena;
            this.geometry = geometry; this.referenceC = referenceC; this.transport = transport;
            mass = operator == null ? new double[air.coefficientCount()] : operator.massJPerK;
            airBackgroundOffsetsC = new double[air.coefficientCount()]; bodyBackgroundC = new double[slots.length];
            phase = -2;
            airPositions = new long[air.coefficientCount()]; airMapped = new boolean[air.coefficientCount()];
            bodyNodes = new int[slots.length];
            Arrays.fill(pairRows, -1);
            phaseBodies = new int[Math.min(MinimumThermalState.PHASE_BODIES, slots.length)];
        }

        boolean run(long deadline) {
            do {
                switch (phase) {
                    case -2 -> {
                        int remaining = 128;
                        while (remaining-- > 0 && cursor < volumes.length) {
                            var piece = volumes[cursor];
                            if (volumeCells == 0) { volumeCells = piece.cells(); if (volumeCells == 0) { cursor++; continue; } }
                            int cell = Long.numberOfTrailingZeros(volumeCells);
                            double natural = naturalC(arena.pageSlot(piece.component.arenaSlot));
                            for (int basis = 0; basis < 8; basis++) {
                                int index = piece.index(cell, basis);
                                double capacity = piece.capacityJPerK[basis];
                                if (operator == null) mass[index] += capacity;
                                airBackgroundOffsetsC[index] += capacity * (natural - referenceC);
                            }
                            volumeCells &= volumeCells - 1;
                            if (volumeCells == 0) cursor++;
                        }
                        if (cursor == volumes.length) { phase++; cursor = 0; }
                    }
                    case -1 -> {
                        int end = Math.min(airBackgroundOffsetsC.length, cursor + 1024);
                        for (; cursor < end; cursor++) {
                            totalAirCapacity += mass[cursor];
                            airBackgroundOffsetsC[cursor] = mass[cursor] > 0 ? airBackgroundOffsetsC[cursor] / mass[cursor] : 0;
                        }
                        if (cursor == airBackgroundOffsetsC.length) { phase++; cursor = 0; }
                    }
                    case 0 -> {
                        int end = Math.min(air.componentCount(), cursor + 256);
                        for (; cursor < end; cursor++) {
                            var component = air.component(cursor);
                            totalAirBlocks += Long.bitCount(component.airBlocks);
                            include(component.minX, component.minY, component.minZ, 4);
                        }
                        if (cursor == air.componentCount()) { phase++; cursor = 0; }
                    }
                    case 1 -> {
                        int end = Math.min(slots.length, cursor + 512);
                        for (; cursor < end; cursor++) {
                            int slot = slots[cursor];
                            bodyBackgroundC[cursor] = naturalC(arena.pageSlot(slot));
                            include(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2), 1);
                            var law = arena.materialLaw(slot);
                            if (phaseCount < phaseBodies.length && (law.heating() != null || law.cooling() != null))
                                phaseBodies[phaseCount++] = cursor;
                        }
                        if (cursor == slots.length) {
                            if (!Double.isFinite(minX)) { minX = minY = minZ = 0; maxX = maxY = maxZ = 1; }
                            // Never subdivide one real block into unsupported sub-block coarse nodes.
                            spanX = Math.max(3, maxX - minX); spanY = Math.max(3, maxY - minY); spanZ = Math.max(3, maxZ - minZ);
                            blockCapacity = totalAirBlocks == 0 ? 0 : totalAirCapacity / totalAirBlocks;
                            phaseBodies = Arrays.copyOf(phaseBodies, phaseCount);
                            phase++; cursor = 0;
                        }
                    }
                    case 2 -> mapAirRows();
                    case 3 -> integrateAirCapacity();
                    case 4 -> {
                        int end = Math.min(slots.length, cursor + 512);
                        for (; cursor < end; cursor++) {
                            int slot = slots[cursor], node = AIR + nodeAt(arena.minimum(slot, 0) + .5,
                                    arena.minimum(slot, 1) + .5, arena.minimum(slot, 2) + .5);
                            int explicit = Arrays.binarySearch(phaseBodies, cursor);
                            if (explicit >= 0) node = POOLED + explicit;
                            bodyNodes[cursor] = node; capacity[node] += arena.materialLaw(slot).capacityJPerK();
                            pairRows[node] = 0;
                            nodeBackgroundMass[node] += arena.materialLaw(slot).capacityJPerK() * bodyBackgroundC[cursor];
                        }
                        if (cursor == slots.length) {
                            for (int node = 0; node < NODES; node++) if (capacity[node] > 0) pairRows[node] = pairNodes++;
                            pairs = new double[transport ? pairNodes * (pairNodes - 1) / 2 : 0];
                            phase = transport ? 5 : 8; cursor = 0;
                        }
                    }
                    case 5 -> addVolumes();
                    case 6 -> addContacts();
                    case 7 -> {
                        if (contacts == null) {
                            int end = material == null ? 0 : Math.min(material.internalPairCount(), cursor + 512);
                            for (; cursor < end; cursor++) addPair(bodyNodes[material.internalPairFirst(cursor)],
                                    bodyNodes[material.internalPairSecond(cursor)], material.internalPairConductance(cursor));
                            if (material == null || cursor == material.internalPairCount()) { phase++; cursor = 0; }
                        } else addMaterialContacts();
                    }
                    case 8 -> {
                        int count = 0;
                        for (double g : pairs) if (g > 0) count++;
                        int[] first = new int[count], second = new int[count]; double[] g = new double[count];
                        int offset = 0, pair = 0;
                        for (int a = 0; transport && a < NODES; a++) if (pairRows[a] >= 0)
                            for (int b = a + 1; b < NODES; b++) if (pairRows[b] >= 0) {
                                if (pairs[pair] > 0) { first[offset] = a; second[offset] = b; g[offset++] = pairs[pair]; }
                                pair++;
                            }
                        int[] phaseSlots = new int[phaseCount]; MaterialThermalLaw[] laws = new MaterialThermalLaw[phaseCount];
                        for (int index = 0; index < phaseCount; index++) {
                            phaseSlots[index] = slots[phaseBodies[index]]; laws[index] = arena.materialLaw(phaseSlots[index]);
                        }
                        double[] nodeBackgroundC = new double[NODES];
                        for (int node = 0; node < NODES; node++)
                            nodeBackgroundC[node] = capacity[node] > 0 ? nodeBackgroundMass[node] / capacity[node] : referenceC;
                        int[] backgroundPages = pageBackgroundC.keySet().toIntArray();
                        double[] pageNaturalC = new double[backgroundPages.length];
                        for (int index = 0; index < backgroundPages.length; index++) pageNaturalC[index] = pageBackgroundC.get(backgroundPages[index]);
                        var background = new MinimumThermalState.Background(airBackgroundOffsetsC, nodeBackgroundC,
                                backgroundPages, pageNaturalC, referenceC);
                        layout = new MinimumThermalState.Layout(minX, minY, minZ, spanX, spanY, spanZ,
                                capacity, new double[NODES], boundary, first, second, g, phaseSlots, laws, background);
                        result = new MinimumThermalModel(this); phase++;
                        return true;
                    }
                    default -> { return true; }
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private void mapAirRows() {
            int remaining = 1024;
            while (remaining-- > 0 && cursor < air.componentCount()) {
                var component = air.component(cursor);
                if (basis == component.basisCount()) { cursor++; basis = 0; continue; }
                int local = basis++, index = component.coefficientIndex(local);
                if (airMapped[index]) continue;
                double x, y, z;
                if (component.isUnitGrid()) {
                    int position = component.unitPosition(local);
                    x = component.minX + (position & 7); z = component.minZ + (position >>> 3 & 7);
                    y = component.minY + (position >>> 6);
                } else {
                    x = component.minX + ((local & 1) == 0 ? 0 : 4);
                    z = component.minZ + ((local & 2) == 0 ? 0 : 4);
                    y = component.minY + ((local & 4) == 0 ? 0 : 4);
                }
                airPositions[index] = BlockPos.asLong((int) x, (int) y, (int) z); airMapped[index] = true;
            }
            if (cursor == air.componentCount()) { phase++; cursor = 0; }
        }

        private void integrateAirCapacity() {
            if (air.isUnitGrid()) {
                int end = Math.min(airPositions.length, cursor + 512);
                for (; cursor < end; cursor++) {
                    long position = airPositions[cursor];
                    integratePoint(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position), mass[cursor],
                            referenceC + airBackgroundOffsetsC[cursor]);
                }
                if (cursor == airPositions.length) { phase++; cursor = 0; }
                return;
            }
            int remaining = 64;
            while (remaining-- > 0 && cursor < air.componentCount()) {
                var component = air.component(cursor);
                if (!componentStarted) { blocks = component.airBlocks; componentStarted = true; }
                if (blocks == 0) { cursor++; componentStarted = false; continue; }
                int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                for (int point = 0; point < 8; point++) {
                    double x = component.minX + (block & 3) + (point & 1);
                    double y = component.minY + (block >>> 4) + (point >>> 2);
                    double z = component.minZ + (block >>> 2 & 3) + (point >>> 1 & 1);
                    double background = referenceC + component.temperatureOffsetAtAirCell(block, x, y, z, airBackgroundOffsetsC);
                    integratePoint(x, y, z, blockCapacity * .125, background);
                }
            }
            if (cursor == air.componentCount()) { phase++; cursor = 0; }
        }

        private void integratePoint(double x, double y, double z, double mass, double background) {
            int count = MinimumThermalState.Layout.airWeights(x, y, z, minX, minY, minZ, spanX, spanY, spanZ, firstNodes, firstWeights);
            for (int index = 0; index < count; index++) {
                int node = firstNodes[index]; double amount = mass * firstWeights[index];
                capacity[node] += amount; nodeBackgroundMass[node] += amount * background;
            }
        }

        private void addVolumes() {
            int remaining = 512;
            while (remaining-- > 0 && volume < volumes.length) {
                var piece = volumes[volume];
                if (volumeCells == 0) { volumeCells = piece.cells(); if (volumeCells == 0) { volume++; continue; } }
                if (row == 8) {
                    volumeCells &= volumeCells - 1; row = 0; column = 1;
                    if (volumeCells == 0) volume++;
                    continue;
                }
                if (column == 8) { row++; column = row + 1; continue; }
                int a = row, b = column++;
                if (Integer.bitCount(a ^ b) != 1) continue;
                int entry = a * 8 - a * (a - 1) / 2 + b - a;
                int cell = Long.numberOfTrailingZeros(volumeCells);
                addMappedPair(piece.index(cell, a), piece.index(cell, b),
                        -(piece.horizontal(cell)[entry] + piece.vertical(cell)[entry]));
            }
            if (volume == volumes.length) { phase++; cursor = contactEntry = otherEntry = 0; }
        }

        private void addContacts() {
            int remaining = 256;
            int count = contacts == null ? operator.pairwiseContactCount() : contacts.conductances.size();
            while (remaining-- > 0 && cursor < count) {
                double g = contacts == null ? operator.nominalContactConductance(cursor) : contacts.conductances.getDouble(cursor);
                int begin = contacts == null ? operator.contactStart(cursor) : contacts.offsets.getInt(cursor);
                int end = contacts == null ? operator.contactEnd(cursor) : contacts.offsets.getInt(cursor + 1);
                // Prepared routes belong to the topology installed together with this descriptor.
                if (!(g > 0) || contacts == null && !operator.contactRouteActive(cursor)) {
                    cursor++; contactEntry = otherEntry = 0; continue;
                }
                if (contactEntry < begin) contactEntry = begin;
                if (contactEntry == end) { cursor++; contactEntry = otherEntry = 0; continue; }
                int entry = contactEntry;
                int row = contactRow(entry);
                double weight = contactWeight(entry);
                if (contacts == null ? operator.environmentContact(cursor) : contacts.boundaryPages.getInt(cursor) >= 0) {
                    int mapped = rowWeights(row, firstNodes, firstWeights);
                    for (int index = 0; index < mapped; index++) boundary[firstNodes[index]] += g * Math.abs(weight) * firstWeights[index];
                    contactEntry++; continue;
                }
                int body = contacts != null ? contacts.localBodies.getInt(cursor) : material == null ? -1 : material.contactBody(cursor);
                if (body >= 0) { addMappedPair(row, airPositions.length + body, g * Math.abs(weight)); contactEntry++; continue; }
                if (weight <= 0) { contactEntry++; continue; }
                if (otherEntry < begin) otherEntry = begin;
                if (otherEntry == end) { contactEntry++; otherEntry = 0; continue; }
                double other = contactWeight(otherEntry);
                int otherRow = contactRow(otherEntry++);
                if (other < 0) addMappedPair(row, otherRow, -g * weight * other);
            }
            if (cursor == count) { phase++; cursor = 0; }
        }

        private int contactRow(int entry) { return contacts == null ? operator.contactRow(entry) : contacts.indices.getInt(entry); }
        private double contactWeight(int entry) { return contacts == null ? operator.contactWeight(entry) : contacts.weights.getDouble(entry); }

        private int rowWeights(int row, int[] nodes, double[] weights) {
            if (row >= airPositions.length) { nodes[0] = bodyNodes[row - airPositions.length]; weights[0] = 1; return 1; }
            long position = airPositions[row];
            int count = MinimumThermalState.Layout.airWeights(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position),
                    minX, minY, minZ, spanX, spanY, spanZ, nodes, weights);
            int supported = 0; double total = 0;
            for (int index = 0; index < count; index++) if (capacity[nodes[index]] > 0) {
                nodes[supported] = nodes[index]; weights[supported++] = weights[index]; total += weights[index];
            }
            for (int index = 0; index < supported; index++) weights[index] /= total;
            return supported;
        }

        private void addMappedPair(int first, int second, double conductance) {
            if (!(conductance > 0)) return;
            int a = rowWeights(first, firstNodes, firstWeights), b = rowWeights(second, secondNodes, secondWeights);
            for (int i = 0; i < a; i++) for (int j = 0; j < b; j++)
                addPair(firstNodes[i], secondNodes[j], conductance * firstWeights[i] * secondWeights[j]);
        }

        private void addMaterialContacts() {
            int remaining = 512;
            while (remaining-- > 0 && fragment < fragments.length) {
                var current = fragments[fragment].materialContacts();
                if (edge == current.size()) { fragment++; edge = 0; continue; }
                int first = Arrays.binarySearch(slots, current.first(edge)), second = Arrays.binarySearch(slots, current.second(edge));
                double g = current.conductance(edge++);
                if (first >= 0 && second >= 0) addPair(bodyNodes[first], bodyNodes[second], g);
            }
            if (fragment < fragments.length) return;
            phase++; cursor = 0;
        }

        private void addPair(int first, int second, double g) {
            if (first == second || !(g > 0) || first >= NODES || second >= NODES) return;
            if (first > second) { int old = first; first = second; second = old; }
            first = pairRows[first]; second = pairRows[second];
            if (first < 0 || second < 0) return;
            int index = first * (2 * pairNodes - first - 1) / 2 + second - first - 1;
            pairs[index] += g;
        }

        private double naturalC(int pageSlot) {
            if (pageBackgroundC.containsKey(pageSlot)) return pageBackgroundC.get(pageSlot);
            double value = geometry.naturalTemperatureC(pageSlot);
            if (!Double.isFinite(value)) value = referenceC;
            pageBackgroundC.put(pageSlot, value);
            return value;
        }

        private void include(double x, double y, double z, int width) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x + width); maxY = Math.max(maxY, y + width); maxZ = Math.max(maxZ, z + width);
        }

        private int nodeAt(double x, double y, double z) {
            int ix = (int) Math.round(Math.max(0, Math.min(3, (x - minX) * 3 / spanX)));
            int iy = (int) Math.round(Math.max(0, Math.min(3, (y - minY) * 3 / spanY)));
            int iz = (int) Math.round(Math.max(0, Math.min(3, (z - minZ) * 3 / spanZ)));
            return ix + 4 * iz + 16 * iy;
        }

        MinimumThermalModel result() { return result; }

        void recordBacking(AirBackingMeter meter) {
            if (!meter.objectOnce(this, 256)) return;
            meter.array(airPositions); meter.array(airMapped); meter.array(bodyNodes); meter.array(phaseBodies);
            meter.array(mass);
            meter.array(airBackgroundOffsetsC); meter.array(bodyBackgroundC); meter.array(nodeBackgroundMass);
            meter.container(pageBackgroundC, pageBackgroundC.size(), 16);
            meter.array(capacity); meter.array(boundary); meter.array(pairRows); meter.array(pairs);
            meter.array(firstNodes); meter.array(secondNodes); meter.array(firstWeights); meter.array(secondWeights);
            if (result != null) result.recordBacking(meter);
        }
    }
}
