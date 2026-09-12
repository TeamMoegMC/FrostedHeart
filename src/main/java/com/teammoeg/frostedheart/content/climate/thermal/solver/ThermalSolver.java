/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;
import java.util.Objects;

/**
 * Worker-owned primitive topology storage and fixed-step thermal solver.
 *
 * <p>Topology preparation reserves every backing array and material-index
 * insertion before calling the allocation-free install methods.</p>
 */
public final class ThermalSolver {
    private final ThermalCellArena arena;
    private final PhaseTransitionRuntime phaseRuntime;
    private final BuoyancyConductance.Parameters buoyancyParameters;
    private final double referenceTemperatureC;

    private ThermalFragment[] fragments;
    private ThermalMaterialExecution[] materialExecutions;
    private long[] fragmentPresent;
    private long[] airPresent;
    private long[] materialPresent;
    private long[] farPresent;
    private long[] phasePresent;
    private int[] stateReferences;
    private double[] naturalTemperatureByPage;
    private final MaterialEdgeTable materialEdges;
    private final Int2IntOpenHashMap replacementReferenceDelta =
            new Int2IntOpenHashMap();

    private int airPairCount;
    private int phaseContactCount;
    private int farBoundaryCount;
    private long structuralVersion;
    private double windScale = 1.0D;
    private long windGeneration;

    private final ThermalExchangeKernel.MutablePairResult pairScratch =
            new ThermalExchangeKernel.MutablePairResult();
    private final ThermalExchangeKernel.MutableBoundaryResult boundaryScratch =
            new ThermalExchangeKernel.MutableBoundaryResult();
    private final BuoyancyConductance.MutableResult buoyancyScratch =
            new BuoyancyConductance.MutableResult();

    public ThermalSolver(
            ThermalCellArena arena,
            PhaseTransitionRuntime phaseRuntime,
            BuoyancyConductance.Parameters buoyancyParameters,
            double referenceTemperatureC,
            int initialFragmentCapacity,
            int initialPageCapacity,
            int initialMaterialEdgeCapacity
    ) {
        if (!Double.isFinite(referenceTemperatureC)
                || initialFragmentCapacity < 0
                || initialPageCapacity < 0
                || initialMaterialEdgeCapacity < 0) {
            throw new IllegalArgumentException("solver configuration is invalid");
        }
        this.arena = Objects.requireNonNull(arena, "arena");
        this.phaseRuntime = Objects.requireNonNull(phaseRuntime, "phaseRuntime");
        if (!phaseRuntime.targets(arena)) {
            throw new IllegalArgumentException("phase runtime targets another arena");
        }
        this.buoyancyParameters = Objects.requireNonNull(
                buoyancyParameters, "buoyancyParameters");
        this.referenceTemperatureC = referenceTemperatureC;
        int fragmentCapacity = Math.max(1, initialFragmentCapacity);
        fragments = new ThermalFragment[fragmentCapacity];
        materialExecutions = new ThermalMaterialExecution[fragmentCapacity];
        Arrays.fill(fragments, ThermalFragment.EMPTY);
        Arrays.fill(materialExecutions, ThermalMaterialExecution.EMPTY);
        fragmentPresent = new long[(fragmentCapacity + 63) >>> 6];
        airPresent = new long[fragmentPresent.length];
        materialPresent = new long[fragmentPresent.length];
        farPresent = new long[fragmentPresent.length];
        phasePresent = new long[fragmentPresent.length];
        int stateCapacity = Math.max(1, arena.highWaterMark());
        stateReferences = new int[stateCapacity];
        naturalTemperatureByPage = new double[Math.max(1, initialPageCapacity)];
        Arrays.fill(naturalTemperatureByPage, Double.NaN);
        materialEdges = new MaterialEdgeTable(initialMaterialEdgeCapacity);
        replacementReferenceDelta.defaultReturnValue(0);
    }

    public long structuralVersion() {
        return structuralVersion;
    }

    public int materialEdgeCount() {
        return materialEdges.size();
    }

    private int stateReferenceCount(int arenaSlot) {
        return arenaSlot >= 0 && arenaSlot < stateReferences.length
                ? stateReferences[arenaSlot]
                : 0;
    }

    public boolean references(ArenaSpan span) {
        Objects.requireNonNull(span, "span");
        int end = Math.min(span.endSlotExclusive(), stateReferences.length);
        for (int slot = span.firstSlot(); slot < end; slot++) {
            if (stateReferences[slot] != 0) {
                return true;
            }
        }
        return false;
    }

    public ThermalFragment fragment(int index) {
        return index >= 0 && index < fragments.length
                ? fragments[index]
                : ThermalFragment.EMPTY;
    }

    public ThermalMaterialExecution materialExecution(int fragmentIndex) {
        return fragmentIndex >= 0 && fragmentIndex < materialExecutions.length
                ? materialExecutions[fragmentIndex]
                : ThermalMaterialExecution.EMPTY;
    }

    public ThermalMaterialEdge materialEdge(long key) {
        return materialEdges.get(key);
    }

    /** Fallible preparation-time growth; never called after commit begins. */
    public void reserveTopologyCapacity(
            int requiredFragments,
            int requiredArenaSlots,
            int requiredPageSlots
    ) {
        if (requiredFragments < 0 || requiredArenaSlots < 0
                || requiredPageSlots < 0) {
            throw new IllegalArgumentException("solver reserve sizes are invalid");
        }
        if (requiredFragments > fragments.length) {
            int old = fragments.length;
            int capacity = grownCapacity(old, requiredFragments);
            fragments = Arrays.copyOf(fragments, capacity);
            materialExecutions = Arrays.copyOf(materialExecutions, capacity);
            Arrays.fill(fragments, old, capacity, ThermalFragment.EMPTY);
            Arrays.fill(
                    materialExecutions, old, capacity,
                    ThermalMaterialExecution.EMPTY);
            fragmentPresent = Arrays.copyOf(
                    fragmentPresent, (capacity + 63) >>> 6);
            airPresent = Arrays.copyOf(airPresent, fragmentPresent.length);
            materialPresent = Arrays.copyOf(
                    materialPresent, fragmentPresent.length);
            farPresent = Arrays.copyOf(farPresent, fragmentPresent.length);
            phasePresent = Arrays.copyOf(phasePresent, fragmentPresent.length);
        }
        if (requiredArenaSlots > stateReferences.length) {
            int capacity = grownCapacity(
                    stateReferences.length, requiredArenaSlots);
            stateReferences = Arrays.copyOf(stateReferences, capacity);
        }
        if (requiredPageSlots > naturalTemperatureByPage.length) {
            int old = naturalTemperatureByPage.length;
            int capacity = grownCapacity(old, requiredPageSlots);
            naturalTemperatureByPage = Arrays.copyOf(
                    naturalTemperatureByPage, capacity);
            Arrays.fill(
                    naturalTemperatureByPage, old, capacity, Double.NaN);
        }
    }

    /** Reserves exact edge-table writes before an allocation-free commit. */
    public void reserveMaterialEdgeChanges(
            int expectedFinalSize,
            int possibleInsertions
    ) {
        materialEdges.reserve(expectedFinalSize, possibleInsertions);
    }

    /**
     * Validates one sparse replacement against the solver's committed
     * references and returns exact post-commit work counts.
     */
    public ProjectedWork preflightReplacement(
            int[] fragmentIndexes,
            ThermalFragment[] replacements,
            ArenaSpan[] retiringSpans,
            int finalMaterialEdgeCount
    ) {
        if (fragmentIndexes.length != replacements.length
                || finalMaterialEdgeCount < 0) {
            throw new IllegalArgumentException("solver replacement is invalid");
        }
        replacementReferenceDelta.clear();
        int airPairs = airPairCount;
        int boundaries = farBoundaryCount;
        int phases = phaseContactCount;
        for (int index = 0; index < fragmentIndexes.length; index++) {
            ThermalFragment old = fragment(fragmentIndexes[index]);
            ThermalFragment next = replacements[index];
            applyReferences(old, -1, false);
            applyReferences(next, 1, false);
            airPairs += next.airPairs().size() - old.airPairs().size();
            boundaries += next.farBoundaries().size()
                    - old.farBoundaries().size();
            phases += next.phaseContacts().size()
                    - old.phaseContacts().size();
        }
        for (var entry : replacementReferenceDelta.int2IntEntrySet()) {
            if (stateReferenceCount(entry.getIntKey())
                    + entry.getIntValue() < 0) {
                throw new IllegalStateException(
                        "solver reference count would underflow");
            }
        }
        for (ArenaSpan span : retiringSpans) {
            for (int slot = span.firstSlot();
                 slot < span.endSlotExclusive();
                 slot++) {
                if (stateReferenceCount(slot)
                        + replacementReferenceDelta.get(slot) != 0) {
                    throw new IllegalStateException(
                            "replacement closure still references an old span");
                }
            }
        }
        return new ProjectedWork(
                Math.addExact(airPairs, finalMaterialEdgeCount),
                boundaries,
                phases);
    }

    /**
     * Allocation-free prepared write. The caller has already validated every
     * endpoint generation and next reference count.
     */
    public void installFragment(int index, ThermalFragment next) {
        ThermalFragment previous = fragments[index];
        applyReferences(previous, -1, true);
        adjustCounts(previous, -1);
        fragments[index] = next;
        updatePresence(fragmentPresent, index, !next.isEmpty());
        updatePresence(airPresent, index, next.airPairs().size() != 0);
        updatePresence(farPresent, index, next.farBoundaries().size() != 0);
        updatePresence(phasePresent, index, next.phaseContacts().size() != 0);
        adjustCounts(next, 1);
        applyReferences(next, 1, true);
    }

    /** Allocation-free unique-material execution replacement. */
    public void installMaterialExecution(
            int fragmentIndex,
            ThermalMaterialExecution execution
    ) {
        materialExecutions[fragmentIndex] = execution;
        updatePresence(
                materialPresent, fragmentIndex, execution.size() != 0);
    }

    /** Allocation-free material edge replacement or deletion. */
    public void installMaterialEdge(long key, ThermalMaterialEdge edge) {
        if (edge == null) {
            materialEdges.removeReserved(key);
        } else {
            materialEdges.putReserved(key, edge);
        }
    }

    public void installNaturalTemperature(int pageSlot, double temperatureC) {
        naturalTemperatureByPage[pageSlot] = temperatureC;
    }

    public void clearNaturalTemperature(int pageSlot) {
        naturalTemperatureByPage[pageSlot] = Double.NaN;
    }

    public void updateWindScale(double nextScale) {
        if (!Double.isFinite(nextScale) || nextScale <= 0.0D) {
            throw new IllegalArgumentException("wind scale must be finite and positive");
        }
        if (Double.doubleToLongBits(nextScale)
                != Double.doubleToLongBits(windScale)) {
            windScale = nextScale;
            windGeneration = Math.incrementExact(windGeneration);
        }
    }

    /** Final primitive write of a prepared structural version. */
    public void finishTopologyCommit(long nextStructuralVersion) {
        structuralVersion = nextStructuralVersion;
    }

    public enum StepStatus {
        COMPLETED,
        NUMERIC_DEGRADED
    }

    /** Allocation-free fixed or abnormal transport step. */
    public StepStatus step(double dtSeconds, boolean forward) {
        if (!Double.isFinite(dtSeconds) || dtSeconds < 0.0D) {
            throw new IllegalArgumentException("dtSeconds must be finite and non-negative");
        }
        boolean degraded;
        if (forward) {
            degraded = applyAir(false, dtSeconds);
            degraded |= applyMaterial(false, dtSeconds);
            degraded |= applyFar(false, dtSeconds);
            degraded |= applyPhase(false, dtSeconds);
        } else {
            degraded = applyPhase(true, dtSeconds);
            degraded |= applyFar(true, dtSeconds);
            degraded |= applyMaterial(true, dtSeconds);
            degraded |= applyAir(true, dtSeconds);
        }
        return degraded ? StepStatus.NUMERIC_DEGRADED : StepStatus.COMPLETED;
    }

    private boolean applyAir(boolean reverse, double dtSeconds) {
        boolean degraded = false;
        for (int fragment = firstFragment(airPresent, reverse);
             fragment >= 0;
             fragment = nextFragment(airPresent, fragment, reverse)) {
            ThermalFragment.AirPairs pairs = fragments[fragment].airPairs();
            int operation = reverse ? pairs.size() - 1 : 0;
            int end = reverse ? -1 : pairs.size();
            int increment = reverse ? -1 : 1;
            for (; operation != end; operation += increment) {
                int first = pairs.first(operation);
                int second = pairs.second(operation);
                double conductance = pairs.conductance(operation);
                BuoyancyConductance.evaluateInto(
                        conductance,
                        temperatureC(first),
                        pairs.firstCenterY(operation),
                        temperatureC(second),
                        pairs.secondCenterY(operation),
                        buoyancyParameters,
                        buoyancyScratch);
                if (!buoyancyScratch.applied()) {
                    degraded = true;
                    continue;
                }
                ThermalExchangeKernel.exchangePairWithInverseInto(
                        arena.enthalpyJ(first),
                        arena.capacityJPerK(first),
                        arena.inverseCapacityKPerJ(first),
                        arena.enthalpyJ(second),
                        arena.capacityJPerK(second),
                        arena.inverseCapacityKPerJ(second),
                        buoyancyScratch.conductanceWPerK(),
                        dtSeconds,
                        pairScratch);
                if (pairScratch.applied()) {
                    arena.setEnthalpyJ(first, pairScratch.enthalpyAJ());
                    arena.setEnthalpyJ(second, pairScratch.enthalpyBJ());
                } else {
                    degraded = true;
                }
            }
        }
        return degraded;
    }

    private boolean applyMaterial(boolean reverse, double dtSeconds) {
        boolean degraded = false;
        for (int fragment = firstFragment(materialPresent, reverse);
             fragment >= 0;
             fragment = nextFragment(materialPresent, fragment, reverse)) {
            ThermalMaterialExecution pairs = materialExecutions[fragment];
            int operation = reverse ? pairs.size() - 1 : 0;
            int end = reverse ? -1 : pairs.size();
            int increment = reverse ? -1 : 1;
            for (; operation != end; operation += increment) {
                int first = pairs.first(operation);
                int second = pairs.second(operation);
                if (dtSeconds == 1.0D) {
                    ThermalExchangeKernel.exchangeCompiledPairInto(
                            arena.enthalpyJ(first),
                            arena.inverseCapacityKPerJ(first),
                            arena.enthalpyJ(second),
                            arena.inverseCapacityKPerJ(second),
                            pairs.coefficient(operation),
                            pairScratch);
                } else {
                    ThermalExchangeKernel.exchangePairWithInverseInto(
                            arena.enthalpyJ(first),
                            arena.capacityJPerK(first),
                            arena.inverseCapacityKPerJ(first),
                            arena.enthalpyJ(second),
                            arena.capacityJPerK(second),
                            arena.inverseCapacityKPerJ(second),
                            pairs.conductance(operation),
                            dtSeconds,
                            pairScratch);
                }
                if (pairScratch.applied()) {
                    arena.setEnthalpyJ(first, pairScratch.enthalpyAJ());
                    arena.setEnthalpyJ(second, pairScratch.enthalpyBJ());
                } else {
                    degraded = true;
                }
            }
        }
        return degraded;
    }

    private boolean applyFar(boolean reverse, double dtSeconds) {
        boolean degraded = false;
        for (int fragment = firstFragment(farPresent, reverse);
             fragment >= 0;
             fragment = nextFragment(farPresent, fragment, reverse)) {
            ThermalFragment.FarBoundaries boundaries =
                    fragments[fragment].farBoundaries();
            int pageSlot = boundaries.pageSlot();
            double boundaryTemperature = naturalTemperatureByPage[pageSlot];
            if (dtSeconds == 1.0D
                    && boundaries.coefficientWindGeneration()
                    != windGeneration) {
                for (int index = 0; index < boundaries.size(); index++) {
                    int cell = boundaries.cell(index);
                    boundaries.cacheCoefficient(
                            index,
                            ThermalExchangeKernel
                                    .compileBoundaryCoefficientJPerK(
                                            arena.capacityJPerK(cell),
                                            boundaries.baseConductance(index)
                                                    * windScale,
                                            1.0D));
                }
                boundaries.finishCoefficientRefresh(windGeneration);
            }
            int operation = reverse ? boundaries.size() - 1 : 0;
            int end = reverse ? -1 : boundaries.size();
            int increment = reverse ? -1 : 1;
            for (; operation != end; operation += increment) {
                int cell = boundaries.cell(operation);
                double conductance = boundaries.baseConductance(operation)
                        * windScale;
                if (dtSeconds == 1.0D) {
                    ThermalExchangeKernel.exchangeCompiledBoundaryInto(
                            arena.enthalpyJ(cell),
                            arena.inverseCapacityKPerJ(cell),
                            referenceTemperatureC,
                            boundaryTemperature,
                            boundaries.coefficient(operation),
                            boundaryScratch);
                } else {
                    ThermalExchangeKernel.exchangeFixedBoundaryWithInverseInto(
                            arena.enthalpyJ(cell),
                            arena.capacityJPerK(cell),
                            arena.inverseCapacityKPerJ(cell),
                            referenceTemperatureC,
                            boundaryTemperature,
                            conductance,
                            dtSeconds,
                            boundaryScratch);
                }
                if (boundaryScratch.applied()) {
                    arena.setEnthalpyJ(cell, boundaryScratch.enthalpyJ());
                } else {
                    degraded = true;
                }
            }
        }
        return degraded;
    }

    private boolean applyPhase(boolean reverse, double dtSeconds) {
        boolean degraded = false;
        for (int fragment = firstFragment(phasePresent, reverse);
             fragment >= 0;
             fragment = nextFragment(phasePresent, fragment, reverse)) {
            ThermalFragment.PhaseContacts contacts =
                    fragments[fragment].phaseContacts();
            int operation = reverse ? contacts.size() - 1 : 0;
            int end = reverse ? -1 : contacts.size();
            int increment = reverse ? -1 : 1;
            for (; operation != end; operation += increment) {
                if (!phaseRuntime.applyContact(
                        contacts.air(operation),
                        contacts.reservoir(operation),
                        contacts.conductance(operation),
                        referenceTemperatureC,
                        dtSeconds)) {
                    degraded = true;
                }
            }
        }
        return degraded;
    }

    /** Exact final sleep gate; callers invoke it only after quiet batches. */
    public double maxTemperatureResidualC() {
        double residual = 0.0D;
        for (int fragment = firstFragment(fragmentPresent, false);
             fragment >= 0;
             fragment = nextFragment(fragmentPresent, fragment, false)) {
            ThermalFragment current = fragments[fragment];
            ThermalFragment.AirPairs air = current.airPairs();
            for (int index = 0; index < air.size(); index++) {
                residual = pairResidual(
                        air.first(index), air.second(index), residual);
            }
            ThermalMaterialExecution material = materialExecutions[fragment];
            for (int index = 0; index < material.size(); index++) {
                residual = pairResidual(
                        material.first(index), material.second(index), residual);
            }
            ThermalFragment.FarBoundaries far = current.farBoundaries();
            for (int index = 0; index < far.size(); index++) {
                residual = boundaryResidual(
                        far.cell(index),
                        naturalTemperatureByPage[far.pageSlot()],
                        residual);
            }
            ThermalFragment.PhaseContacts phase = current.phaseContacts();
            for (int index = 0; index < phase.size(); index++) {
                double temperature = temperatureC(phase.air(index));
                if (!Double.isFinite(temperature)) {
                    return Double.POSITIVE_INFINITY;
                }
                int reservoir = phase.reservoir(index);
                double delta = temperature
                        - arena.phaseTransitionTemperatureC(reservoir);
                if (delta < 0.0D
                        && arena.phaseAvailableEnergyJ(reservoir) <= 0.0D) {
                    continue;
                }
                residual = Math.max(residual, Math.abs(delta));
            }
        }
        return residual;
    }

    private double pairResidual(int first, int second, double residual) {
        double firstTemperature = temperatureC(first);
        double secondTemperature = temperatureC(second);
        return Double.isFinite(firstTemperature)
                && Double.isFinite(secondTemperature)
                ? Math.max(
                        residual,
                        Math.abs(firstTemperature - secondTemperature))
                : Double.POSITIVE_INFINITY;
    }

    private double boundaryResidual(
            int cell,
            double boundaryTemperature,
            double residual
    ) {
        double cellTemperature = temperatureC(cell);
        return Double.isFinite(cellTemperature)
                && Double.isFinite(boundaryTemperature)
                ? Math.max(
                        residual,
                        Math.abs(cellTemperature - boundaryTemperature))
                : Double.POSITIVE_INFINITY;
    }

    private double temperatureC(int slot) {
        return referenceTemperatureC
                + arena.enthalpyJ(slot) * arena.inverseCapacityKPerJ(slot);
    }

    private void adjustCounts(ThermalFragment fragment, int direction) {
        airPairCount += direction * fragment.airPairs().size();
        phaseContactCount += direction * fragment.phaseContacts().size();
        farBoundaryCount += direction * fragment.farBoundaries().size();
    }

    private void applyReferences(
            ThermalFragment fragment,
            int direction,
            boolean committed
    ) {
        ThermalFragment.AirPairs air = fragment.airPairs();
        for (int index = 0; index < air.size(); index++) {
            applyReference(air.first(index), direction, committed);
            applyReference(air.second(index), direction, committed);
        }
        ThermalFragment.MaterialContributions material =
                fragment.materialContributions();
        for (int index = 0; index < material.size(); index++) {
            applyReference(material.first(index), direction, committed);
            applyReference(material.second(index), direction, committed);
        }
        ThermalFragment.PhaseContacts phase = fragment.phaseContacts();
        for (int index = 0; index < phase.size(); index++) {
            applyReference(phase.air(index), direction, committed);
            applyReference(phase.reservoir(index), direction, committed);
        }
        ThermalFragment.FarBoundaries far = fragment.farBoundaries();
        for (int index = 0; index < far.size(); index++) {
            applyReference(far.cell(index), direction, committed);
        }
    }

    private void applyReference(int slot, int direction, boolean committed) {
        if (!committed) {
            replacementReferenceDelta.put(
                    slot, replacementReferenceDelta.get(slot) + direction);
            return;
        }
        stateReferences[slot] += direction;
    }

    private int firstFragment(long[] presence, boolean reverse) {
        return reverse
                ? previousPresent(presence, fragments.length - 1)
                : nextPresent(presence, 0, fragments.length);
    }

    private int nextFragment(
            long[] presence,
            int current,
            boolean reverse
    ) {
        return reverse
                ? previousPresent(presence, current - 1)
                : nextPresent(presence, current + 1, fragments.length);
    }

    private static int nextPresent(long[] bits, int start, int limit) {
        if (start < 0) {
            start = 0;
        }
        if (start >= limit) {
            return -1;
        }
        int wordIndex = start >>> 6;
        long word = bits[wordIndex] & (-1L << (start & 63));
        while (true) {
            if (word != 0L) {
                int value = wordIndex << 6 | Long.numberOfTrailingZeros(word);
                return value < limit ? value : -1;
            }
            if (++wordIndex >= bits.length) {
                return -1;
            }
            word = bits[wordIndex];
        }
    }

    private static int previousPresent(long[] bits, int start) {
        if (start < 0 || bits.length == 0) {
            return -1;
        }
        int wordIndex = Math.min(start >>> 6, bits.length - 1);
        int bit = Math.min(start & 63, 63);
        long word = bits[wordIndex] & (-1L >>> (63 - bit));
        while (true) {
            if (word != 0L) {
                return wordIndex << 6
                        | 63 - Long.numberOfLeadingZeros(word);
            }
            if (--wordIndex < 0) {
                return -1;
            }
            word = bits[wordIndex];
        }
    }

    private static void setBit(long[] bits, int index) {
        bits[index >>> 6] |= 1L << (index & 63);
    }

    private static void clearBit(long[] bits, int index) {
        bits[index >>> 6] &= ~(1L << (index & 63));
    }

    private static void updatePresence(
            long[] bits,
            int index,
            boolean present
    ) {
        if (present) {
            setBit(bits, index);
        } else {
            clearBit(bits, index);
        }
    }

    private static int grownCapacity(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(capacity, Math.max(8, capacity >>> 1));
        }
        return capacity;
    }

    public record ProjectedWork(
            int pairOperations,
            int boundaryOperations,
            int phaseOperations
    ) {
    }

    /** Project-owned deletable table with explicit preparation-time growth. */
    private static final class MaterialEdgeTable {
        private static final byte EMPTY = 0;
        private static final byte OCCUPIED = 1;
        private static final byte DELETED = 2;

        private long[] keys;
        private ThermalMaterialEdge[] values;
        private byte[] states;
        private int size;
        private int used;
        private int resizeThreshold;

        private MaterialEdgeTable(int expected) {
            allocate(tableCapacity(Math.max(1, expected)));
        }

        private int size() {
            return size;
        }

        private ThermalMaterialEdge get(long key) {
            int index = findIndex(key);
            return index < 0 ? null : values[index];
        }

        private void reserve(int expectedFinalSize, int possibleInsertions) {
            if (expectedFinalSize < 0 || possibleInsertions < 0) {
                throw new IllegalArgumentException(
                        "material edge reserve is invalid");
            }
            int insertionPeak = Math.addExact(size, possibleInsertions);
            int requiredSize = Math.max(expectedFinalSize, insertionPeak);
            if (requiredSize > resizeThreshold) {
                rehash(tableCapacity(requiredSize));
            } else if (Math.addExact(used, possibleInsertions)
                    > resizeThreshold) {
                rehash(states.length);
            }
        }

        private void putReserved(long key, ThermalMaterialEdge value) {
            int mask = states.length - 1;
            int index = hash(key) & mask;
            int deleted = -1;
            while (states[index] != EMPTY) {
                if (states[index] == OCCUPIED && keys[index] == key) {
                    values[index] = value;
                    return;
                }
                if (states[index] == DELETED && deleted < 0) {
                    deleted = index;
                }
                index = index + 1 & mask;
            }
            int destination = deleted >= 0 ? deleted : index;
            if (states[destination] == EMPTY) {
                if (used >= resizeThreshold) {
                    throw new IllegalStateException(
                            "material edge capacity was not reserved");
                }
                used++;
            }
            states[destination] = OCCUPIED;
            keys[destination] = key;
            values[destination] = value;
            size++;
        }

        private void removeReserved(long key) {
            int index = findIndex(key);
            if (index < 0) {
                return;
            }
            states[index] = DELETED;
            values[index] = null;
            size--;
        }

        private int findIndex(long key) {
            int mask = states.length - 1;
            int index = hash(key) & mask;
            while (states[index] != EMPTY) {
                if (states[index] == OCCUPIED && keys[index] == key) {
                    return index;
                }
                index = index + 1 & mask;
            }
            return -1;
        }

        private void allocate(int capacity) {
            keys = new long[capacity];
            values = new ThermalMaterialEdge[capacity];
            states = new byte[capacity];
            size = 0;
            used = 0;
            resizeThreshold = Math.max(1, (int) (capacity * 0.6D));
        }

        private void rehash(int capacity) {
            long[] oldKeys = keys;
            ThermalMaterialEdge[] oldValues = values;
            byte[] oldStates = states;
            allocate(capacity);
            for (int index = 0; index < oldStates.length; index++) {
                if (oldStates[index] == OCCUPIED) {
                    putReserved(oldKeys[index], oldValues[index]);
                }
            }
        }

        private static int tableCapacity(int expected) {
            int required = Math.max(4, (int) Math.ceil(expected / 0.6D));
            int highest = Integer.highestOneBit(required - 1);
            if (highest >= 1 << 29) {
                throw new IllegalArgumentException(
                        "material edge table is too large");
            }
            return highest << 1;
        }

        private static int hash(long key) {
            long value = key;
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdl;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53l;
            value ^= value >>> 33;
            return (int) value;
        }
    }
}
