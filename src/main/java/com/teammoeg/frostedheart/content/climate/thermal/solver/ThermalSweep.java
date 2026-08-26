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

import java.util.Objects;

/**
 * Single-writer solve facade over one Brick-addressable primitive layout.
 */
public final class ThermalSweep {
    private final ThermalSweepFragments fragments;
    private ThermalSweepFragments.Patch pendingFragmentPatch;

    private ThermalSweep(
            ThermalSweepFragments fragments,
            ThermalSweepFragments.Patch pendingFragmentPatch
    ) {
        this.fragments = Objects.requireNonNull(fragments, "fragments");
        this.pendingFragmentPatch = pendingFragmentPatch;
    }

    static ThermalSweep fragmented(ThermalSweepFragments fragments) {
        return new ThermalSweep(fragments, null);
    }

    public ThermalSweepFragments.Patch beginFragmentPatch() {
        if (pendingFragmentPatch != null) {
            throw new IllegalStateException("sweep already has a pending fragment patch");
        }
        return fragments.beginPatch();
    }

    public int fragmentCount() {
        return fragments.fragmentCount();
    }

    public ThermalSweep withFragmentPatch(ThermalSweepFragments.Patch patch) {
        if (patch == null || pendingFragmentPatch != null) {
            throw new IllegalStateException("fragment patch cannot target this sweep");
        }
        return new ThermalSweep(fragments, patch);
    }

    /** Called only by the runtime while it owns the logical writer. */
    public void commitPendingFragmentPatch() {
        if (pendingFragmentPatch != null) {
            pendingFragmentPatch.commit();
            pendingFragmentPatch = null;
        }
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
                throw new IllegalArgumentException(
                        "pair cell indices must be non-negative");
            }
            requireNonNegativeFinite(
                    "baseConductanceWPerK", baseConductanceWPerK);
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
                    cellA, cellB, baseConductanceWPerK,
                    centerYA, centerYB, true);
        }
    }

    public record BoundaryOperation(
            int cell,
            double boundaryTemperatureC,
            double conductanceWPerK
    ) {
        public BoundaryOperation {
            if (cell < 0) {
                throw new IllegalArgumentException(
                        "boundary cell index must be non-negative");
            }
            requireFinite("boundaryTemperatureC", boundaryTemperatureC);
            requireNonNegativeFinite("conductanceWPerK", conductanceWPerK);
        }
    }

    public record PhaseOperation(
            int airCell,
            int phaseReservoir,
            double conductanceWPerK
    ) {
        public PhaseOperation {
            if (airCell < 0 || phaseReservoir < 0) {
                throw new IllegalArgumentException(
                        "phase operation cell indices must be non-negative");
            }
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
            int appliedPhaseContacts,
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
        return apply(referenceTemperatureC, epoch, Direction.forEpoch(epoch));
    }

    public Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch,
            Direction direction
    ) {
        return apply(referenceTemperatureC, epoch, epoch.dtSeconds(), direction);
    }

    /** Applies one time-plan substep; the caller supplies its bounded seconds value. */
    public Result apply(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            Direction direction
    ) {
        preflight(referenceTemperatureC, epoch, direction);
        return applyAfterPreflight(
                referenceTemperatureC, epoch, dtSeconds, direction);
    }

    /** Validates every endpoint before source or transport state is mutated. */
    void preflight(
            double referenceTemperatureC,
            SolveEpoch epoch,
            Direction direction
    ) {
        fragments.preflight(referenceTemperatureC, epoch, direction);
    }

    /** Executes after one logical-writer preflight for the whole time plan. */
    Result applyAfterPreflight(
            double referenceTemperatureC,
            SolveEpoch epoch,
            double dtSeconds,
            Direction direction
    ) {
        return fragments.applyAfterPreflight(
                referenceTemperatureC, epoch, dtSeconds, direction);
    }

    public boolean targets(ThermalCellArena candidate) {
        return fragments.arena() == candidate;
    }

    public int pairOperationCount() {
        return fragments.pairOperationCount();
    }

    public int boundaryOperationCount() {
        return fragments.boundaryOperationCount();
    }

    public int phaseOperationCount() {
        return fragments.phaseOperationCount();
    }

    public int stateCellCount() {
        return fragments.stateCellCount();
    }

    /** Largest current temperature difference across any compiled operator. */
    public double maxTemperatureResidualC(double referenceTemperatureC) {
        return fragments.maxTemperatureResidualC(referenceTemperatureC);
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
}
