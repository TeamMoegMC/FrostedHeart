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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Brick-addressable primitive operation storage for {@link ThermalSweep}.
 * Topology commits replace only changed fragments while solve traversal keeps
 * the same canonical order as the former flat arrays.
 */
public final class ThermalSweepFragments {
    private final ThermalCellArena arena;
    private final PhaseTransitionRuntime phaseRuntime;
    private final BuoyancyConductance.Parameters buoyancyParameters;
    private final PairFragment[] airPairs;
    private final PairFragment[] materialPairs;
    private final BoundaryFragment[] materialBoundaries;
    private final PhaseFragment[] phases;
    private final Long2ObjectOpenHashMap<MaterialEdge> materialEdges =
            new Long2ObjectOpenHashMap<>();
    private int[] farGeneration = new int[0];
    private double[] farTemperature = new double[0];
    private double[] farConductance = new double[0];
    private long[] farPresent = new long[0];
    private int[] stateReferences = new int[0];
    private long[] statePresent = new long[0];
    private int airPairCount;
    private int materialBoundaryCount;
    private int phaseCount;
    private int farBoundaryCount;
    private long version;

    private final BuoyancyConductance.MutableResult buoyancyScratch =
            new BuoyancyConductance.MutableResult();
    private final ThermalExchangeKernel.MutablePairResult pairScratch =
            new ThermalExchangeKernel.MutablePairResult();
    private final ThermalExchangeKernel.MutableBoundaryResult boundaryScratch =
            new ThermalExchangeKernel.MutableBoundaryResult();

    private ThermalSweepFragments(Builder builder) {
        arena = builder.arena;
        phaseRuntime = builder.phaseRuntime;
        buoyancyParameters = builder.buoyancyParameters;
        airPairs = builder.airPairs;
        materialPairs = builder.materialPairs;
        materialBoundaries = builder.materialBoundaries;
        phases = builder.phases;
        ensureSlotCapacity(arena.highWaterMark());

        for (PairFragment fragment : airPairs) {
            airPairCount = Math.addExact(airPairCount, fragment.size());
            addPairStateReferences(fragment);
        }
        for (int fragmentIndex = 0; fragmentIndex < materialPairs.length;
             fragmentIndex++) {
            addMaterialContributions(fragmentIndex, materialPairs[fragmentIndex]);
        }
        LongOpenHashSet allMaterialEdges = new LongOpenHashSet();
        allMaterialEdges.addAll(materialEdges.keySet());
        recomputeMaterialEdges(allMaterialEdges);
        for (BoundaryFragment fragment : materialBoundaries) {
            materialBoundaryCount = Math.addExact(
                    materialBoundaryCount, fragment.size());
            addBoundaryStateReferences(fragment);
        }
        for (PhaseFragment fragment : phases) {
            phaseCount = Math.addExact(phaseCount, fragment.size());
            addPhaseStateReferences(fragment);
        }
        for (FarBoundary boundary : builder.farBoundaries.values()) {
            setFarBoundary(boundary.slot, boundary);
        }
    }

    public static Builder builder(
            ThermalCellArena arena,
            PhaseTransitionRuntime phaseRuntime,
            BuoyancyConductance.Parameters buoyancyParameters,
            int fragmentCount
    ) {
        return new Builder(
                arena, phaseRuntime, buoyancyParameters, fragmentCount);
    }

    public Patch beginPatch() {
        return new Patch(this, version);
    }

    ThermalCellArena arena() {
        return arena;
    }

    public ThermalSweep.Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            ThermalSweep.Direction direction
    ) {
        Objects.requireNonNull(epoch, "epoch");
        Objects.requireNonNull(direction, "direction");
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireNonNegativeFinite("dtSeconds", dtSeconds);
        requireCurrentTargets();

        double initialEnthalpy = compensatedStateSum();
        MutableCounts counts = new MutableCounts();
        if (direction == ThermalSweep.Direction.FORWARD) {
            applyAirPairsForward(referenceTemperatureC, epoch, dtSeconds, counts);
            applyMaterialPairsForward(referenceTemperatureC, epoch, dtSeconds, counts);
            applyFarBoundariesForward(referenceTemperatureC, dtSeconds, counts);
            applyMaterialBoundariesForward(referenceTemperatureC, dtSeconds, counts);
            applyPhasesForward(referenceTemperatureC, dtSeconds, counts);
        } else {
            applyPhasesReverse(referenceTemperatureC, dtSeconds, counts);
            applyMaterialPairsReverse(referenceTemperatureC, epoch, dtSeconds, counts);
            applyAirPairsReverse(referenceTemperatureC, epoch, dtSeconds, counts);
            applyMaterialBoundariesReverse(referenceTemperatureC, dtSeconds, counts);
            applyFarBoundariesReverse(referenceTemperatureC, dtSeconds, counts);
        }
        double finalEnthalpy = compensatedStateSum();
        double residual = finalEnthalpy
                - (initialEnthalpy + counts.boundaryEnergyJ);
        return new ThermalSweep.Result(
                direction,
                counts.appliedPairs,
                counts.appliedBoundaries,
                counts.appliedPhases,
                counts.numericDegraded,
                initialEnthalpy,
                finalEnthalpy,
                counts.boundaryEnergyJ,
                residual);
    }

    public int pairOperationCount() {
        return airPairCount + materialEdges.size();
    }

    public int boundaryOperationCount() {
        return farBoundaryCount + materialBoundaryCount;
    }

    public int phaseOperationCount() {
        return phaseCount;
    }

    public int stateCellCount() {
        int count = 0;
        for (long word : statePresent) {
            count += Long.bitCount(word);
        }
        return count;
    }

    public double maxTemperatureResidualC(double referenceTemperatureC) {
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireCurrentTargets();
        double residual = 0.0D;
        for (PairFragment fragment : airPairs) {
            residual = maxPairResidual(fragment, false, referenceTemperatureC, residual);
        }
        for (PairFragment fragment : materialPairs) {
            residual = maxPairResidual(
                    fragment, true, referenceTemperatureC, residual);
        }
        for (int wordIndex = 0; wordIndex < farPresent.length; wordIndex++) {
            long remaining = farPresent[wordIndex];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int slot = (wordIndex << 6) + bit;
                residual = maxBoundaryResidual(
                        slot, farTemperature[slot], referenceTemperatureC, residual);
                remaining &= remaining - 1L;
            }
        }
        for (BoundaryFragment fragment : materialBoundaries) {
            for (int operation = 0; operation < fragment.size(); operation++) {
                residual = maxBoundaryResidual(
                        fragment.cell[operation],
                        fragment.temperature[operation],
                        referenceTemperatureC,
                        residual);
            }
        }
        for (PhaseFragment fragment : phases) {
            for (int operation = 0; operation < fragment.size(); operation++) {
                double temperature = arena.temperatureC(
                        fragment.air[operation], referenceTemperatureC);
                if (!Double.isFinite(temperature)) {
                    return Double.POSITIVE_INFINITY;
                }
                residual = Math.max(
                        residual,
                        Math.max(0.0D, temperature
                                - arena.phaseTransitionTemperatureC(
                                        fragment.reservoir[operation])));
            }
        }
        return residual;
    }

    private void applyAirPairsForward(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (PairFragment fragment : airPairs) {
            applyPairFragment(fragment, false, false,
                    referenceTemperatureC, epoch, dtSeconds, counts);
        }
    }

    private void applyAirPairsReverse(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = airPairs.length - 1; index >= 0; index--) {
            applyPairFragment(airPairs[index], false, true,
                    referenceTemperatureC, epoch, dtSeconds, counts);
        }
    }

    private void applyMaterialPairsForward(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (PairFragment fragment : materialPairs) {
            applyPairFragment(fragment, true, false,
                    referenceTemperatureC, epoch, dtSeconds, counts);
        }
    }

    private void applyMaterialPairsReverse(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = materialPairs.length - 1; index >= 0; index--) {
            applyPairFragment(materialPairs[index], true, true,
                    referenceTemperatureC, epoch, dtSeconds, counts);
        }
    }

    private void applyPairFragment(
            PairFragment fragment,
            boolean useEffectiveConductance,
            boolean reverse,
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        int operation = reverse ? fragment.size() - 1 : 0;
        int end = reverse ? -1 : fragment.size();
        int step = reverse ? -1 : 1;
        for (; operation != end; operation += step) {
            double conductance = useEffectiveConductance
                    ? fragment.effectiveConductance[operation]
                    : fragment.conductance[operation];
            if (conductance == 0.0D) {
                continue;
            }
            int first = fragment.first[operation];
            int second = fragment.second[operation];
            if (fragment.buoyant[operation]) {
                double temperatureA = temperatureC(
                        arena.enthalpyJ(first), arena.capacityJPerK(first),
                        referenceTemperatureC);
                double temperatureB = temperatureC(
                        arena.enthalpyJ(second), arena.capacityJPerK(second),
                        referenceTemperatureC);
                BuoyancyConductance.evaluateInto(
                        conductance,
                        temperatureA,
                        fragment.centerYFirst[operation],
                        temperatureB,
                        fragment.centerYSecond[operation],
                        buoyancyParameters,
                        buoyancyScratch);
                if (!buoyancyScratch.applied()) {
                    counts.numericDegraded++;
                    continue;
                }
                conductance = buoyancyScratch.conductanceWPerK();
            }
            ThermalExchangeKernel.exchangePairInto(
                    arena.enthalpyJ(first),
                    arena.capacityJPerK(first),
                    arena.enthalpyJ(second),
                    arena.capacityJPerK(second),
                    conductance,
                    dtSeconds,
                    pairScratch);
            if (!pairScratch.applied()) {
                counts.numericDegraded++;
                continue;
            }
            arena.setEnthalpyJ(first, pairScratch.enthalpyAJ());
            arena.setEnthalpyJ(second, pairScratch.enthalpyBJ());
            counts.appliedPairs++;
        }
    }

    private void applyFarBoundariesForward(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int wordIndex = 0; wordIndex < farPresent.length; wordIndex++) {
            long remaining = farPresent[wordIndex];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int slot = (wordIndex << 6) + bit;
                applyBoundary(slot, farTemperature[slot], farConductance[slot],
                        referenceTemperatureC, dtSeconds, counts);
                remaining &= remaining - 1L;
            }
        }
    }

    private void applyFarBoundariesReverse(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int wordIndex = farPresent.length - 1; wordIndex >= 0; wordIndex--) {
            long remaining = farPresent[wordIndex];
            while (remaining != 0L) {
                int bit = 63 - Long.numberOfLeadingZeros(remaining);
                int slot = (wordIndex << 6) + bit;
                applyBoundary(slot, farTemperature[slot], farConductance[slot],
                        referenceTemperatureC, dtSeconds, counts);
                remaining &= ~(1L << bit);
            }
        }
    }

    private void applyMaterialBoundariesForward(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (BoundaryFragment fragment : materialBoundaries) {
            applyBoundaryFragment(fragment, false,
                    referenceTemperatureC, dtSeconds, counts);
        }
    }

    private void applyMaterialBoundariesReverse(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = materialBoundaries.length - 1; index >= 0; index--) {
            applyBoundaryFragment(
                    materialBoundaries[index], true,
                    referenceTemperatureC, dtSeconds, counts);
        }
    }

    private void applyBoundaryFragment(
            BoundaryFragment fragment,
            boolean reverse,
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        int operation = reverse ? fragment.size() - 1 : 0;
        int end = reverse ? -1 : fragment.size();
        int step = reverse ? -1 : 1;
        for (; operation != end; operation += step) {
            applyBoundary(
                    fragment.cell[operation],
                    fragment.temperature[operation],
                    fragment.conductance[operation],
                    referenceTemperatureC,
                    dtSeconds,
                    counts);
        }
    }

    private void applyBoundary(
            int cell,
            double temperature,
            double conductance,
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        ThermalExchangeKernel.exchangeFixedBoundaryInto(
                arena.enthalpyJ(cell),
                arena.capacityJPerK(cell),
                referenceTemperatureC,
                temperature,
                conductance,
                dtSeconds,
                boundaryScratch);
        if (!boundaryScratch.applied()) {
            counts.numericDegraded++;
            return;
        }
        arena.setEnthalpyJ(cell, boundaryScratch.enthalpyJ());
        counts.addBoundaryEnergy(boundaryScratch.energyFromBoundaryJ());
        counts.appliedBoundaries++;
    }

    private void applyPhasesForward(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (PhaseFragment fragment : phases) {
            applyPhaseFragment(
                    fragment, false, referenceTemperatureC, dtSeconds, counts);
        }
    }

    private void applyPhasesReverse(
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = phases.length - 1; index >= 0; index--) {
            applyPhaseFragment(
                    phases[index], true,
                    referenceTemperatureC, dtSeconds, counts);
        }
    }

    private void applyPhaseFragment(
            PhaseFragment fragment,
            boolean reverse,
            double referenceTemperatureC,
            double dtSeconds,
            MutableCounts counts
    ) {
        int operation = reverse ? fragment.size() - 1 : 0;
        int end = reverse ? -1 : fragment.size();
        int step = reverse ? -1 : 1;
        for (; operation != end; operation += step) {
            if (!phaseRuntime.applyContact(
                    fragment.air[operation],
                    fragment.reservoir[operation],
                    fragment.conductance[operation],
                    referenceTemperatureC,
                    dtSeconds)) {
                counts.numericDegraded++;
                continue;
            }
            counts.appliedPhases++;
        }
    }

    private void requireCurrentTargets() {
        for (PairFragment fragment : airPairs) {
            requireCurrent(fragment);
        }
        for (PairFragment fragment : materialPairs) {
            requireCurrent(fragment);
        }
        for (int wordIndex = 0; wordIndex < farPresent.length; wordIndex++) {
            long remaining = farPresent[wordIndex];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int slot = (wordIndex << 6) + bit;
                requireCurrentCell(slot, farGeneration[slot]);
                remaining &= remaining - 1L;
            }
        }
        for (BoundaryFragment fragment : materialBoundaries) {
            requireCurrent(fragment);
        }
        for (PhaseFragment fragment : phases) {
            requireCurrent(fragment);
        }
    }

    private void requireCurrent(PairFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            requireCurrentCell(fragment.first[operation], fragment.firstGeneration[operation]);
            requireCurrentCell(fragment.second[operation], fragment.secondGeneration[operation]);
        }
    }

    private void requireCurrent(BoundaryFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            requireCurrentCell(fragment.cell[operation], fragment.generation[operation]);
        }
    }

    private void requireCurrent(PhaseFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            requireCurrentCell(fragment.air[operation], fragment.airGeneration[operation]);
            requireCurrentCell(
                    fragment.reservoir[operation], fragment.reservoirGeneration[operation]);
        }
    }

    private void requireCurrentCell(int slot, int generation) {
        if (!arena.isLive(slot) || arena.lifecycleGeneration(slot) != generation) {
            throw new IllegalStateException(
                    "compiled sweep references a stale arena slot: " + slot);
        }
    }

    private double maxPairResidual(
            PairFragment fragment,
            boolean effective,
            double referenceTemperatureC,
            double residual
    ) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            if (effective && fragment.effectiveConductance[operation] == 0.0D) {
                continue;
            }
            double first = arena.temperatureC(
                    fragment.first[operation], referenceTemperatureC);
            double second = arena.temperatureC(
                    fragment.second[operation], referenceTemperatureC);
            if (!Double.isFinite(first) || !Double.isFinite(second)) {
                return Double.POSITIVE_INFINITY;
            }
            residual = Math.max(residual, Math.abs(first - second));
        }
        return residual;
    }

    private double maxBoundaryResidual(
            int slot,
            double boundaryTemperature,
            double referenceTemperatureC,
            double residual
    ) {
        double temperature = arena.temperatureC(slot, referenceTemperatureC);
        return Double.isFinite(temperature)
                ? Math.max(residual, Math.abs(temperature - boundaryTemperature))
                : Double.POSITIVE_INFINITY;
    }

    private double compensatedStateSum() {
        double sum = 0.0D;
        double compensation = 0.0D;
        for (int wordIndex = 0; wordIndex < statePresent.length; wordIndex++) {
            long remaining = statePresent[wordIndex];
            while (remaining != 0L) {
                int bit = Long.numberOfTrailingZeros(remaining);
                int slot = (wordIndex << 6) + bit;
                double adjusted = arena.enthalpyJ(slot) - compensation;
                double next = sum + adjusted;
                compensation = (next - sum) - adjusted;
                sum = next;
                remaining &= remaining - 1L;
            }
        }
        return sum;
    }

    private void commit(Patch patch) {
        if (patch.owner != this || patch.committed || patch.baseVersion != version) {
            throw new IllegalStateException("fragment patch is stale or already committed");
        }
        ensureSlotCapacity(arena.highWaterMark());
        LongOpenHashSet affectedMaterialEdges = new LongOpenHashSet();

        for (FragmentReplacement<PairFragment> replacement
                : patch.airReplacements.values()) {
            PairFragment old = airPairs[replacement.index];
            removePairStateReferences(old);
            airPairCount -= old.size();
            airPairs[replacement.index] = replacement.fragment;
            airPairCount = Math.addExact(airPairCount, replacement.fragment.size());
            addPairStateReferences(replacement.fragment);
        }

        for (FragmentReplacement<PairFragment> replacement
                : patch.materialPairReplacements.values()) {
            PairFragment old = materialPairs[replacement.index];
            collectMaterialKeys(old, affectedMaterialEdges);
            collectMaterialKeys(replacement.fragment, affectedMaterialEdges);
        }
        removeEffectiveMaterialStateReferences(affectedMaterialEdges);
        for (FragmentReplacement<PairFragment> replacement
                : patch.materialPairReplacements.values()) {
            removeMaterialContributions(replacement.index,
                    materialPairs[replacement.index]);
            materialPairs[replacement.index] = replacement.fragment;
            addMaterialContributions(replacement.index, replacement.fragment);
        }
        recomputeMaterialEdges(affectedMaterialEdges);

        for (FragmentReplacement<BoundaryFragment> replacement
                : patch.boundaryReplacements.values()) {
            BoundaryFragment old = materialBoundaries[replacement.index];
            removeBoundaryStateReferences(old);
            materialBoundaryCount -= old.size();
            materialBoundaries[replacement.index] = replacement.fragment;
            materialBoundaryCount = Math.addExact(
                    materialBoundaryCount, replacement.fragment.size());
            addBoundaryStateReferences(replacement.fragment);
        }
        for (FragmentReplacement<PhaseFragment> replacement
                : patch.phaseReplacements.values()) {
            PhaseFragment old = phases[replacement.index];
            removePhaseStateReferences(old);
            phaseCount -= old.size();
            phases[replacement.index] = replacement.fragment;
            phaseCount = Math.addExact(phaseCount, replacement.fragment.size());
            addPhaseStateReferences(replacement.fragment);
        }
        for (FarBoundary boundary : patch.farReplacements.values()) {
            setFarBoundary(boundary.slot, boundary);
        }
        patch.committed = true;
        version++;
    }

    private void setFarBoundary(int slot, FarBoundary boundary) {
        ensureSlotCapacity(slot + 1);
        boolean oldPresent = bitPresent(farPresent, slot);
        if (oldPresent) {
            removeStateReference(slot);
            farBoundaryCount--;
        }
        if (!boundary.present) {
            clearBit(farPresent, slot);
            return;
        }
        if (!arena.isLive(slot)) {
            throw new IllegalArgumentException(
                    "far-field boundary references a non-live slot: " + slot);
        }
        farGeneration[slot] = arena.lifecycleGeneration(slot);
        farTemperature[slot] = boundary.temperature;
        farConductance[slot] = boundary.conductance;
        setBit(farPresent, slot);
        farBoundaryCount++;
        addStateReference(slot);
    }

    private void addMaterialContributions(int fragmentIndex, PairFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            long key = pairKey(fragment.first[operation], fragment.second[operation]);
            materialEdges.computeIfAbsent(key, ignored -> new MaterialEdge())
                    .contributions.add(new Contribution(
                            fragmentIndex, operation, fragment));
        }
    }

    private void removeMaterialContributions(int fragmentIndex, PairFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            long key = pairKey(fragment.first[operation], fragment.second[operation]);
            MaterialEdge edge = materialEdges.get(key);
            if (edge == null) {
                throw new IllegalStateException("material edge contribution is missing");
            }
            int targetOperation = operation;
            edge.contributions.removeIf(contribution ->
                    contribution.fragmentIndex == fragmentIndex
                            && contribution.operation == targetOperation
                            && contribution.fragment == fragment);
            if (edge.contributions.isEmpty()) {
                materialEdges.remove(key);
            }
        }
    }

    private void removeEffectiveMaterialStateReferences(LongOpenHashSet keys) {
        for (long key : keys) {
            MaterialEdge edge = materialEdges.get(key);
            if (edge != null && edge.first != null) {
                removeStateReference((int) (key >>> 32));
                removeStateReference((int) key);
                edge.first.fragment.effectiveConductance[edge.first.operation] = 0.0D;
                edge.first = null;
            }
        }
    }

    private void recomputeMaterialEdges(LongOpenHashSet keys) {
        for (long key : keys) {
            MaterialEdge edge = materialEdges.get(key);
            if (edge == null) {
                continue;
            }
            edge.contributions.sort(Comparator
                    .comparingInt((Contribution contribution) ->
                            contribution.fragmentIndex)
                    .thenComparingInt(contribution -> contribution.operation));
            double conductance = 0.0D;
            for (Contribution contribution : edge.contributions) {
                contribution.fragment.effectiveConductance[contribution.operation] = 0.0D;
                conductance += contribution.fragment.conductance[contribution.operation];
            }
            if (!Double.isFinite(conductance) || conductance <= 0.0D) {
                throw new IllegalStateException(
                        "compiled material conductance is invalid");
            }
            edge.first = edge.contributions.get(0);
            edge.first.fragment.effectiveConductance[edge.first.operation] = conductance;
            addStateReference((int) (key >>> 32));
            addStateReference((int) key);
        }
    }

    private static void collectMaterialKeys(
            PairFragment fragment,
            LongOpenHashSet target
    ) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            target.add(pairKey(fragment.first[operation], fragment.second[operation]));
        }
    }

    private void addPairStateReferences(PairFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            addStateReference(fragment.first[operation]);
            addStateReference(fragment.second[operation]);
        }
    }

    private void removePairStateReferences(PairFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            removeStateReference(fragment.first[operation]);
            removeStateReference(fragment.second[operation]);
        }
    }

    private void addBoundaryStateReferences(BoundaryFragment fragment) {
        for (int slot : fragment.cell) {
            addStateReference(slot);
        }
    }

    private void removeBoundaryStateReferences(BoundaryFragment fragment) {
        for (int slot : fragment.cell) {
            removeStateReference(slot);
        }
    }

    private void addPhaseStateReferences(PhaseFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            addStateReference(fragment.air[operation]);
            addStateReference(fragment.reservoir[operation]);
        }
    }

    private void removePhaseStateReferences(PhaseFragment fragment) {
        for (int operation = 0; operation < fragment.size(); operation++) {
            removeStateReference(fragment.air[operation]);
            removeStateReference(fragment.reservoir[operation]);
        }
    }

    private void addStateReference(int slot) {
        ensureSlotCapacity(slot + 1);
        if (stateReferences[slot]++ == 0) {
            setBit(statePresent, slot);
        }
    }

    private void removeStateReference(int slot) {
        if (slot < 0 || slot >= stateReferences.length
                || stateReferences[slot] <= 0) {
            throw new IllegalStateException("state reference underflow for slot " + slot);
        }
        if (--stateReferences[slot] == 0) {
            clearBit(statePresent, slot);
        }
    }

    private void ensureSlotCapacity(int required) {
        if (required <= stateReferences.length) {
            return;
        }
        int capacity = grownCapacity(stateReferences.length, required);
        stateReferences = Arrays.copyOf(stateReferences, capacity);
        statePresent = Arrays.copyOf(statePresent, (capacity + 63) >>> 6);
        farGeneration = Arrays.copyOf(farGeneration, capacity);
        farTemperature = Arrays.copyOf(farTemperature, capacity);
        farConductance = Arrays.copyOf(farConductance, capacity);
        farPresent = Arrays.copyOf(farPresent, (capacity + 63) >>> 6);
    }

    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffff_ffffL);
    }

    private static boolean bitPresent(long[] bits, int index) {
        return (bits[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    private static void setBit(long[] bits, int index) {
        bits[index >>> 6] |= 1L << (index & 63);
    }

    private static void clearBit(long[] bits, int index) {
        bits[index >>> 6] &= ~(1L << (index & 63));
    }

    private static int grownCapacity(int current, int required) {
        int capacity = Math.max(16, current);
        while (capacity < required) {
            capacity = Math.max(required, capacity + (capacity >>> 1));
        }
        return capacity;
    }

    private static double temperatureC(
            double enthalpy,
            double capacity,
            double referenceTemperatureC
    ) {
        return Double.isFinite(enthalpy) && Double.isFinite(capacity) && capacity > 0.0D
                ? referenceTemperatureC + enthalpy / capacity
                : Double.NaN;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireNonNegativeFinite(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative");
        }
    }

    public static final class Builder {
        private final ThermalCellArena arena;
        private final PhaseTransitionRuntime phaseRuntime;
        private final BuoyancyConductance.Parameters buoyancyParameters;
        private final PairFragment[] airPairs;
        private final PairFragment[] materialPairs;
        private final BoundaryFragment[] materialBoundaries;
        private final PhaseFragment[] phases;
        private final Int2ObjectOpenHashMap<FarBoundary> farBoundaries =
                new Int2ObjectOpenHashMap<>();
        private boolean built;

        private Builder(
                ThermalCellArena arena,
                PhaseTransitionRuntime phaseRuntime,
                BuoyancyConductance.Parameters buoyancyParameters,
                int fragmentCount
        ) {
            if (fragmentCount < 0) {
                throw new IllegalArgumentException("fragmentCount must be non-negative");
            }
            this.arena = Objects.requireNonNull(arena, "arena");
            this.phaseRuntime = phaseRuntime;
            this.buoyancyParameters = Objects.requireNonNull(
                    buoyancyParameters, "buoyancyParameters");
            airPairs = new PairFragment[fragmentCount];
            materialPairs = new PairFragment[fragmentCount];
            materialBoundaries = new BoundaryFragment[fragmentCount];
            phases = new PhaseFragment[fragmentCount];
            for (int index = 0; index < fragmentCount; index++) {
                airPairs[index] = PairFragment.empty();
                materialPairs[index] = PairFragment.empty();
                materialBoundaries[index] = BoundaryFragment.empty();
                phases[index] = PhaseFragment.empty();
            }
        }

        public void setAirPairs(int fragment, List<ThermalSweep.PairOperation> operations) {
            requireOpen();
            airPairs[fragment] = PairFragment.from(arena, operations, false);
        }

        public void setMaterial(
                int fragment,
                List<ThermalSweep.PairOperation> pairs,
                List<ThermalSweep.BoundaryOperation> boundaries,
                List<ThermalSweep.PhaseOperation> phaseOperations
        ) {
            requireOpen();
            materialPairs[fragment] = PairFragment.from(arena, pairs, true);
            materialBoundaries[fragment] = BoundaryFragment.from(arena, boundaries);
            phases[fragment] = PhaseFragment.from(arena, phaseRuntime, phaseOperations);
        }

        public void setFarBoundary(
                int slot,
                double temperatureC,
                double conductanceWPerK
        ) {
            requireOpen();
            farBoundaries.put(slot, FarBoundary.present(
                    slot, temperatureC, conductanceWPerK));
        }

        public ThermalSweep build() {
            requireOpen();
            built = true;
            return ThermalSweep.fragmented(new ThermalSweepFragments(this));
        }

        private void requireOpen() {
            if (built) {
                throw new IllegalStateException("fragment builder was already consumed");
            }
        }
    }

    public static final class Patch {
        private final ThermalSweepFragments owner;
        private final long baseVersion;
        private final Int2ObjectOpenHashMap<FragmentReplacement<PairFragment>>
                airReplacements = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<FragmentReplacement<PairFragment>>
                materialPairReplacements = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<FragmentReplacement<BoundaryFragment>>
                boundaryReplacements = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<FragmentReplacement<PhaseFragment>>
                phaseReplacements = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<FarBoundary> farReplacements =
                new Int2ObjectOpenHashMap<>();
        private boolean committed;

        private Patch(ThermalSweepFragments owner, long baseVersion) {
            this.owner = owner;
            this.baseVersion = baseVersion;
        }

        public void replaceAirPairs(
                int fragment,
                List<ThermalSweep.PairOperation> operations
        ) {
            requireOpen();
            requireFragment(fragment);
            airReplacements.put(fragment, new FragmentReplacement<>(
                    fragment, PairFragment.from(owner.arena, operations, false)));
        }

        public void replaceMaterial(
                int fragment,
                List<ThermalSweep.PairOperation> pairs,
                List<ThermalSweep.BoundaryOperation> boundaries,
                List<ThermalSweep.PhaseOperation> phases
        ) {
            requireOpen();
            requireFragment(fragment);
            materialPairReplacements.put(fragment, new FragmentReplacement<>(
                    fragment, PairFragment.from(owner.arena, pairs, true)));
            boundaryReplacements.put(fragment, new FragmentReplacement<>(
                    fragment, BoundaryFragment.from(owner.arena, boundaries)));
            phaseReplacements.put(fragment, new FragmentReplacement<>(
                    fragment, PhaseFragment.from(
                            owner.arena, owner.phaseRuntime, phases)));
        }

        public void setFarBoundary(
                int slot,
                double temperatureC,
                double conductanceWPerK
        ) {
            requireOpen();
            farReplacements.put(slot, FarBoundary.present(
                    slot, temperatureC, conductanceWPerK));
        }

        public void clearFarBoundary(int slot) {
            requireOpen();
            if (slot < 0) {
                throw new IllegalArgumentException("slot must be non-negative");
            }
            farReplacements.put(slot, FarBoundary.absent(slot));
        }

        void commit() {
            owner.commit(this);
        }

        private void requireFragment(int fragment) {
            if (fragment < 0 || fragment >= owner.airPairs.length) {
                throw new IllegalArgumentException("fragment index is out of bounds");
            }
        }

        private void requireOpen() {
            if (committed || owner.version != baseVersion) {
                throw new IllegalStateException("fragment patch is no longer current");
            }
        }
    }

    private static final class PairFragment {
        private static final PairFragment EMPTY = new PairFragment(0);
        private final int[] first;
        private final int[] second;
        private final int[] firstGeneration;
        private final int[] secondGeneration;
        private final double[] conductance;
        private final double[] effectiveConductance;
        private final double[] centerYFirst;
        private final double[] centerYSecond;
        private final boolean[] buoyant;

        private PairFragment(int size) {
            first = new int[size];
            second = new int[size];
            firstGeneration = new int[size];
            secondGeneration = new int[size];
            conductance = new double[size];
            effectiveConductance = new double[size];
            centerYFirst = new double[size];
            centerYSecond = new double[size];
            buoyant = new boolean[size];
        }

        private static PairFragment empty() {
            return EMPTY;
        }

        private static PairFragment from(
                ThermalCellArena arena,
                List<ThermalSweep.PairOperation> operations,
                boolean material
        ) {
            Objects.requireNonNull(operations, "operations");
            if (operations.isEmpty()) {
                return EMPTY;
            }
            PairFragment fragment = new PairFragment(operations.size());
            for (int index = 0; index < operations.size(); index++) {
                ThermalSweep.PairOperation operation = Objects.requireNonNull(
                        operations.get(index), "operations contains null");
                int first = operation.cellA();
                int second = operation.cellB();
                if (material && first > second) {
                    int swap = first;
                    first = second;
                    second = swap;
                }
                if (!arena.isLive(first) || !arena.isLive(second) || first == second) {
                    throw new IllegalArgumentException(
                            "pair fragment references invalid arena slots");
                }
                fragment.first[index] = first;
                fragment.second[index] = second;
                fragment.firstGeneration[index] = arena.lifecycleGeneration(first);
                fragment.secondGeneration[index] = arena.lifecycleGeneration(second);
                fragment.conductance[index] = operation.baseConductanceWPerK();
                fragment.effectiveConductance[index] = material
                        ? 0.0D : operation.baseConductanceWPerK();
                fragment.centerYFirst[index] = operation.centerYA();
                fragment.centerYSecond[index] = operation.centerYB();
                fragment.buoyant[index] = operation.applyBuoyancy();
            }
            return fragment;
        }

        private int size() {
            return first.length;
        }
    }

    private static final class BoundaryFragment {
        private static final BoundaryFragment EMPTY = new BoundaryFragment(0);
        private final int[] cell;
        private final int[] generation;
        private final double[] temperature;
        private final double[] conductance;

        private BoundaryFragment(int size) {
            cell = new int[size];
            generation = new int[size];
            temperature = new double[size];
            conductance = new double[size];
        }

        private static BoundaryFragment empty() {
            return EMPTY;
        }

        private static BoundaryFragment from(
                ThermalCellArena arena,
                List<ThermalSweep.BoundaryOperation> operations
        ) {
            Objects.requireNonNull(operations, "operations");
            if (operations.isEmpty()) {
                return EMPTY;
            }
            BoundaryFragment fragment = new BoundaryFragment(operations.size());
            for (int index = 0; index < operations.size(); index++) {
                ThermalSweep.BoundaryOperation operation = Objects.requireNonNull(
                        operations.get(index), "operations contains null");
                if (!arena.isLive(operation.cell())) {
                    throw new IllegalArgumentException(
                            "boundary fragment references a non-live slot");
                }
                fragment.cell[index] = operation.cell();
                fragment.generation[index] = arena.lifecycleGeneration(operation.cell());
                fragment.temperature[index] = operation.boundaryTemperatureC();
                fragment.conductance[index] = operation.conductanceWPerK();
            }
            return fragment;
        }

        private int size() {
            return cell.length;
        }
    }

    private static final class PhaseFragment {
        private static final PhaseFragment EMPTY = new PhaseFragment(0);
        private final int[] air;
        private final int[] airGeneration;
        private final int[] reservoir;
        private final int[] reservoirGeneration;
        private final double[] conductance;

        private PhaseFragment(int size) {
            air = new int[size];
            airGeneration = new int[size];
            reservoir = new int[size];
            reservoirGeneration = new int[size];
            conductance = new double[size];
        }

        private static PhaseFragment empty() {
            return EMPTY;
        }

        private static PhaseFragment from(
                ThermalCellArena arena,
                PhaseTransitionRuntime runtime,
                List<ThermalSweep.PhaseOperation> operations
        ) {
            Objects.requireNonNull(operations, "operations");
            if (operations.isEmpty()) {
                return EMPTY;
            }
            if (!operations.isEmpty() && (runtime == null || !runtime.targets(arena))) {
                throw new IllegalArgumentException(
                        "phase fragments require the arena's phase runtime");
            }
            PhaseFragment fragment = new PhaseFragment(operations.size());
            for (int index = 0; index < operations.size(); index++) {
                ThermalSweep.PhaseOperation operation = Objects.requireNonNull(
                        operations.get(index), "operations contains null");
                if (!arena.isLive(operation.airCell())
                        || !arena.isLive(operation.phaseReservoir())
                        || arena.isPhaseReservoir(operation.airCell())
                        || !arena.isPhaseReservoir(operation.phaseReservoir())) {
                    throw new IllegalArgumentException(
                            "phase fragment references invalid arena slots");
                }
                fragment.air[index] = operation.airCell();
                fragment.airGeneration[index] = arena.lifecycleGeneration(
                        operation.airCell());
                fragment.reservoir[index] = operation.phaseReservoir();
                fragment.reservoirGeneration[index] = arena.lifecycleGeneration(
                        operation.phaseReservoir());
                fragment.conductance[index] = operation.conductanceWPerK();
            }
            return fragment;
        }

        private int size() {
            return air.length;
        }
    }

    private static final class MaterialEdge {
        private final List<Contribution> contributions = new ArrayList<>();
        private Contribution first;
    }

    private record Contribution(
            int fragmentIndex,
            int operation,
            PairFragment fragment
    ) {
    }

    private record FragmentReplacement<T>(int index, T fragment) {
    }

    private record FarBoundary(
            int slot,
            boolean present,
            double temperature,
            double conductance
    ) {
        private static FarBoundary present(
                int slot,
                double temperature,
                double conductance
        ) {
            if (slot < 0) {
                throw new IllegalArgumentException("slot must be non-negative");
            }
            requireFinite("temperature", temperature);
            requireNonNegativeFinite("conductance", conductance);
            return new FarBoundary(slot, true, temperature, conductance);
        }

        private static FarBoundary absent(int slot) {
            return new FarBoundary(slot, false, 0.0D, 0.0D);
        }
    }

    private static final class MutableCounts {
        private int appliedPairs;
        private int appliedBoundaries;
        private int appliedPhases;
        private int numericDegraded;
        private double boundaryEnergyJ;
        private double boundaryEnergyCompensation;

        private void addBoundaryEnergy(double energyJ) {
            double adjusted = energyJ - boundaryEnergyCompensation;
            double next = boundaryEnergyJ + adjusted;
            boundaryEnergyCompensation = (next - boundaryEnergyJ) - adjusted;
            boundaryEnergyJ = next;
        }
    }
}
