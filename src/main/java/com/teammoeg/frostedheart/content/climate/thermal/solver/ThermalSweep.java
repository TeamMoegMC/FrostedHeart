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

import java.util.List;
import java.util.Objects;

/**
 * A frozen sequential pair/boundary sweep over authoritative arena state.
 * Instances are single-writer and reuse mutable kernel scratch on the hot path.
 */
public final class ThermalSweep {
    private final ThermalCellArena arena;
    private final int[] pairA;
    private final int[] pairB;
    private final int[] pairGenerationA;
    private final int[] pairGenerationB;
    private final double[] pairConductance;
    private final double[] pairCenterYA;
    private final double[] pairCenterYB;
    private final boolean[] pairUsesBuoyancy;
    private final int[] boundaryCell;
    private final int[] boundaryGeneration;
    private final double[] boundaryTemperature;
    private final double[] boundaryConductance;
    private final int[] stateSlots;
    private final BuoyancyConductance.Parameters buoyancyParameters;
    private final BuoyancyConductance.MutableResult buoyancyScratch =
            new BuoyancyConductance.MutableResult();
    private final ThermalExchangeKernel.MutablePairResult pairScratch =
            new ThermalExchangeKernel.MutablePairResult();
    private final ThermalExchangeKernel.MutableBoundaryResult boundaryScratch =
            new ThermalExchangeKernel.MutableBoundaryResult();

    public ThermalSweep(
            ThermalCellArena arena,
            List<PairOperation> pairOperations,
            List<BoundaryOperation> boundaryOperations,
            BuoyancyConductance.Parameters buoyancyParameters
    ) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.buoyancyParameters = Objects.requireNonNull(
                buoyancyParameters,
                "buoyancyParameters"
        );

        Objects.requireNonNull(pairOperations, "pairOperations");
        pairA = new int[pairOperations.size()];
        pairB = new int[pairOperations.size()];
        pairGenerationA = new int[pairOperations.size()];
        pairGenerationB = new int[pairOperations.size()];
        pairConductance = new double[pairOperations.size()];
        pairCenterYA = new double[pairOperations.size()];
        pairCenterYB = new double[pairOperations.size()];
        pairUsesBuoyancy = new boolean[pairOperations.size()];
        for (int index = 0; index < pairOperations.size(); index++) {
            PairOperation operation = Objects.requireNonNull(
                    pairOperations.get(index),
                    "pairOperations contains null"
            );
            requireLiveCell(operation.cellA());
            requireLiveCell(operation.cellB());
            if (operation.cellA() == operation.cellB()) {
                throw new IllegalArgumentException("a pair must contain two different cells");
            }
            pairA[index] = operation.cellA();
            pairB[index] = operation.cellB();
            pairGenerationA[index] = arena.lifecycleGeneration(operation.cellA());
            pairGenerationB[index] = arena.lifecycleGeneration(operation.cellB());
            pairConductance[index] = operation.baseConductanceWPerK();
            pairCenterYA[index] = operation.centerYA();
            pairCenterYB[index] = operation.centerYB();
            pairUsesBuoyancy[index] = operation.applyBuoyancy();
        }

        Objects.requireNonNull(boundaryOperations, "boundaryOperations");
        boundaryCell = new int[boundaryOperations.size()];
        boundaryGeneration = new int[boundaryOperations.size()];
        boundaryTemperature = new double[boundaryOperations.size()];
        boundaryConductance = new double[boundaryOperations.size()];
        for (int index = 0; index < boundaryOperations.size(); index++) {
            BoundaryOperation operation = Objects.requireNonNull(
                    boundaryOperations.get(index),
                    "boundaryOperations contains null"
            );
            requireLiveCell(operation.cell());
            boundaryCell[index] = operation.cell();
            boundaryGeneration[index] = arena.lifecycleGeneration(operation.cell());
            boundaryTemperature[index] = operation.boundaryTemperatureC();
            boundaryConductance[index] = operation.conductanceWPerK();
        }
        stateSlots = collectStateSlots();
    }

    /** Operations must already be in canonical spatial order. */
    public record PairOperation(
            int cellA,
            int cellB,
            double baseConductanceWPerK,
            double centerYA,
            double centerYB,
            boolean applyBuoyancy
    ) {
        public PairOperation {
            if (cellA < 0 || cellB < 0) {
                throw new IllegalArgumentException("pair cell indices must be non-negative");
            }
            requireNonNegativeFinite("baseConductanceWPerK", baseConductanceWPerK);
            requireFinite("centerYA", centerYA);
            requireFinite("centerYB", centerYB);
        }

        public static PairOperation fixed(
                int cellA,
                int cellB,
                double conductanceWPerK
        ) {
            return new PairOperation(
                    cellA, cellB, conductanceWPerK, 0.0D, 0.0D, false);
        }

        public static PairOperation buoyant(
                int cellA,
                int cellB,
                double baseConductanceWPerK,
                double centerYA,
                double centerYB
        ) {
            return new PairOperation(
                    cellA, cellB, baseConductanceWPerK, centerYA, centerYB, true);
        }
    }

    public record BoundaryOperation(
            int cell,
            double boundaryTemperatureC,
            double conductanceWPerK
    ) {
        public BoundaryOperation {
            if (cell < 0) {
                throw new IllegalArgumentException("boundary cell index must be non-negative");
            }
            requireFinite("boundaryTemperatureC", boundaryTemperatureC);
            requireNonNegativeFinite("conductanceWPerK", conductanceWPerK);
        }
    }

    public enum Direction {
        FORWARD,
        REVERSE;

        /** Odd epochs run forward; even epochs run the exact reverse lists. */
        public static Direction forEpoch(SolveEpoch epoch) {
            Objects.requireNonNull(epoch, "epoch");
            return (epoch.epochId() & 1L) == 1L ? FORWARD : REVERSE;
        }
    }

    public record Result(
            Direction direction,
            int appliedPairs,
            int appliedBoundaries,
            int numericDegradedOperations,
            double initialEnthalpyJ,
            double finalEnthalpyJ,
            double boundaryEnergyJ,
            double conservationResidualJ
    ) {
    }

    public Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch
    ) {
        return apply(
                referenceTemperatureC,
                epoch,
                Direction.forEpoch(epoch)
        );
    }

    public Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch,
            Direction direction
    ) {
        return apply(
                referenceTemperatureC,
                epoch,
                epoch.dtSeconds(),
                direction
        );
    }

    /** Applies one time-plan substep; the caller supplies its bounded seconds value. */
    public Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            Direction direction
    ) {
        Objects.requireNonNull(epoch, "epoch");
        Objects.requireNonNull(direction, "direction");
        requireCurrentTargets();
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireNonNegativeFinite("dtSeconds", dtSeconds);

        double initialEnthalpy = compensatedSum(arena, stateSlots);
        MutableCounts counts = new MutableCounts();
        if (direction == Direction.FORWARD) {
            applyPairsForward(
                    arena, referenceTemperatureC,
                    epoch, dtSeconds, counts);
            applyBoundariesForward(
                    arena, referenceTemperatureC,
                    dtSeconds, counts);
        } else {
            applyPairsReverse(
                    arena, referenceTemperatureC,
                    epoch, dtSeconds, counts);
            applyBoundariesReverse(
                    arena, referenceTemperatureC,
                    dtSeconds, counts);
        }
        double finalEnthalpy = compensatedSum(arena, stateSlots);
        double residual = finalEnthalpy - (initialEnthalpy + counts.boundaryEnergyJ);
        return new Result(
                direction,
                counts.appliedPairs,
                counts.appliedBoundaries,
                counts.numericDegraded,
                initialEnthalpy,
                finalEnthalpy,
                counts.boundaryEnergyJ,
                residual
        );
    }

    private void applyPairsForward(
            ThermalCellArena arena,
            double referenceTemperature,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = 0; index < pairA.length; index++) {
            applyPair(index, arena, referenceTemperature,
                    epoch, dtSeconds, counts);
        }
    }

    private void applyPairsReverse(
            ThermalCellArena arena,
            double referenceTemperature,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = pairA.length - 1; index >= 0; index--) {
            applyPair(index, arena, referenceTemperature,
                    epoch, dtSeconds, counts);
        }
    }

    private void applyPair(
            int operation,
            ThermalCellArena arena,
            double referenceTemperature,
            SolveEpoch epoch,
            double dtSeconds,
            MutableCounts counts
    ) {
        int first = pairA[operation];
        int second = pairB[operation];
        double conductance = pairConductance[operation];
        if (pairUsesBuoyancy[operation]) {
            double temperatureA = temperatureC(
                    arena.enthalpyJ(first), arena.capacityJPerK(first), referenceTemperature);
            double temperatureB = temperatureC(
                    arena.enthalpyJ(second), arena.capacityJPerK(second), referenceTemperature);
            BuoyancyConductance.evaluateInto(
                    conductance,
                    temperatureA,
                    pairCenterYA[operation],
                    temperatureB,
                    pairCenterYB[operation],
                    buoyancyParameters,
                    buoyancyScratch
            );
            if (!buoyancyScratch.applied()) {
                counts.numericDegraded++;
                return;
            }
            conductance = buoyancyScratch.conductanceWPerK();
        }

        ThermalExchangeKernel.exchangePairInto(
                arena.enthalpyJ(first),
                arena.capacityJPerK(first),
                epoch,
                arena.enthalpyJ(second),
                arena.capacityJPerK(second),
                epoch,
                conductance,
                dtSeconds,
                pairScratch
        );
        if (!pairScratch.applied()) {
            counts.numericDegraded++;
            return;
        }
        arena.setEnthalpyJ(first, pairScratch.enthalpyAJ());
        arena.setEnthalpyJ(second, pairScratch.enthalpyBJ());
        counts.appliedPairs++;
    }

    private void applyBoundariesForward(
            ThermalCellArena arena,
            double referenceTemperature,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = 0; index < boundaryCell.length; index++) {
            applyBoundary(index, arena, referenceTemperature,
                    dtSeconds, counts);
        }
    }

    private void applyBoundariesReverse(
            ThermalCellArena arena,
            double referenceTemperature,
            double dtSeconds,
            MutableCounts counts
    ) {
        for (int index = boundaryCell.length - 1; index >= 0; index--) {
            applyBoundary(index, arena, referenceTemperature,
                    dtSeconds, counts);
        }
    }

    private void applyBoundary(
            int operation,
            ThermalCellArena arena,
            double referenceTemperature,
            double dtSeconds,
            MutableCounts counts
    ) {
        int cell = boundaryCell[operation];
        ThermalExchangeKernel.exchangeFixedBoundaryInto(
                arena.enthalpyJ(cell),
                arena.capacityJPerK(cell),
                referenceTemperature,
                boundaryTemperature[operation],
                boundaryConductance[operation],
                dtSeconds,
                boundaryScratch
        );
        if (!boundaryScratch.applied()) {
            counts.numericDegraded++;
            return;
        }
        arena.setEnthalpyJ(cell, boundaryScratch.enthalpyJ());
        counts.addBoundaryEnergy(boundaryScratch.energyFromBoundaryJ());
        counts.appliedBoundaries++;
    }

    public boolean targets(ThermalCellArena candidate) {
        return arena == candidate;
    }

    public int pairOperationCount() {
        return pairA.length;
    }

    public int boundaryOperationCount() {
        return boundaryCell.length;
    }

    public int stateCellCount() {
        return stateSlots.length;
    }

    /**
     * Largest current temperature difference across a compiled pair or fixed
     * boundary. A non-finite cell state is never eligible for sleep.
     */
    public double maxTemperatureResidualC(double referenceTemperatureC) {
        requireCurrentTargets();
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        double residual = 0.0D;
        for (int index = 0; index < pairA.length; index++) {
            double temperatureA = temperatureC(
                    arena.enthalpyJ(pairA[index]),
                    arena.capacityJPerK(pairA[index]),
                    referenceTemperatureC);
            double temperatureB = temperatureC(
                    arena.enthalpyJ(pairB[index]),
                    arena.capacityJPerK(pairB[index]),
                    referenceTemperatureC);
            if (!Double.isFinite(temperatureA) || !Double.isFinite(temperatureB)) {
                return Double.POSITIVE_INFINITY;
            }
            residual = Math.max(residual, Math.abs(temperatureA - temperatureB));
        }
        for (int index = 0; index < boundaryCell.length; index++) {
            double temperature = temperatureC(
                    arena.enthalpyJ(boundaryCell[index]),
                    arena.capacityJPerK(boundaryCell[index]),
                    referenceTemperatureC);
            if (!Double.isFinite(temperature)) {
                return Double.POSITIVE_INFINITY;
            }
            residual = Math.max(
                    residual,
                    Math.abs(temperature - boundaryTemperature[index]));
        }
        return residual;
    }

    private void requireLiveCell(int cell) {
        if (!arena.isLive(cell)) {
            throw new IllegalArgumentException(
                    "operation references a non-live arena slot: " + cell);
        }
    }

    private void requireCurrentTargets() {
        for (int index = 0; index < pairA.length; index++) {
            requireCurrentCell(pairA[index], pairGenerationA[index]);
            requireCurrentCell(pairB[index], pairGenerationB[index]);
        }
        for (int index = 0; index < boundaryCell.length; index++) {
            requireCurrentCell(boundaryCell[index], boundaryGeneration[index]);
        }
    }

    private void requireCurrentCell(int slot, int generation) {
        if (!arena.isLive(slot) || arena.lifecycleGeneration(slot) != generation) {
            throw new IllegalStateException(
                    "compiled sweep references a stale arena slot: " + slot);
        }
    }

    private int[] collectStateSlots() {
        boolean[] present = new boolean[arena.highWaterMark()];
        int count = 0;
        for (int index = 0; index < pairA.length; index++) {
            if (!present[pairA[index]]) {
                present[pairA[index]] = true;
                count++;
            }
            if (!present[pairB[index]]) {
                present[pairB[index]] = true;
                count++;
            }
        }
        for (int slot : boundaryCell) {
            if (!present[slot]) {
                present[slot] = true;
                count++;
            }
        }
        int[] slots = new int[count];
        int write = 0;
        for (int slot = 0; slot < present.length; slot++) {
            if (present[slot]) {
                slots[write++] = slot;
            }
        }
        return slots;
    }

    private static double temperatureC(
            double enthalpy,
            double capacity,
            double referenceTemperature
    ) {
        if (!Double.isFinite(enthalpy)
                || !Double.isFinite(capacity)
                || capacity <= 0.0D) {
            return Double.NaN;
        }
        return referenceTemperature + enthalpy / capacity;
    }

    private static double compensatedSum(ThermalCellArena arena, int[] slots) {
        double sum = 0.0D;
        double compensation = 0.0D;
        for (int slot : slots) {
            double adjusted = arena.enthalpyJ(slot) - compensation;
            double next = sum + adjusted;
            compensation = (next - sum) - adjusted;
            sum = next;
        }
        return sum;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireNonNegativeFinite(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static final class MutableCounts {
        private int appliedPairs;
        private int appliedBoundaries;
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
