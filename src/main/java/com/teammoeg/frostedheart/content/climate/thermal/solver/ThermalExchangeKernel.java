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

import java.util.Objects;

/** Stable finite-capacity pair and fixed-temperature boundary operators. */
public final class ThermalExchangeKernel {
    private ThermalExchangeKernel() {
    }

    public enum Status {
        APPLIED,
        NUMERIC_DEGRADED
    }

    /** Caller-owned pair result for allocation-free sweep execution. */
    public static final class MutablePairResult {
        private Status status = Status.NUMERIC_DEGRADED;
        private double enthalpyAJ;
        private double enthalpyBJ;

        public Status status() {
            return status;
        }

        public boolean applied() {
            return status == Status.APPLIED;
        }

        public double enthalpyAJ() {
            return enthalpyAJ;
        }

        public double enthalpyBJ() {
            return enthalpyBJ;
        }

        private void set(
                Status nextStatus,
                double nextEnthalpyA,
                double nextEnthalpyB
        ) {
            status = nextStatus;
            enthalpyAJ = nextEnthalpyA;
            enthalpyBJ = nextEnthalpyB;
        }

    }

    /** Caller-owned boundary result for allocation-free sweep execution. */
    public static final class MutableBoundaryResult {
        private Status status = Status.NUMERIC_DEGRADED;
        private double enthalpyJ;
        private double energyFromBoundaryJ;

        public Status status() {
            return status;
        }

        public boolean applied() {
            return status == Status.APPLIED;
        }

        public double enthalpyJ() {
            return enthalpyJ;
        }

        public double energyFromBoundaryJ() {
            return energyFromBoundaryJ;
        }

        private void set(
                Status nextStatus,
                double nextEnthalpy,
                double nextEnergyFromBoundary
        ) {
            status = nextStatus;
            enthalpyJ = nextEnthalpy;
            energyFromBoundaryJ = nextEnergyFromBoundary;
        }

    }

    public static Status exchangePairInto(
            double enthalpyAJ,
            double capacityAJPerK,
            double enthalpyBJ,
            double capacityBJPerK,
            double conductanceWPerK,
            double dtSeconds,
            MutablePairResult result
    ) {
        Objects.requireNonNull(result, "result");
        if (!finite(enthalpyAJ)
                || !positiveFinite(capacityAJPerK)
                || !finite(enthalpyBJ)
                || !positiveFinite(capacityBJPerK)
                || !nonNegativeFinite(conductanceWPerK)
                || !nonNegativeFinite(dtSeconds)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }

        double temperatureOffsetA = enthalpyAJ / capacityAJPerK;
        double temperatureOffsetB = enthalpyBJ / capacityBJPerK;
        if (!finite(temperatureOffsetA) || !finite(temperatureOffsetB)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }
        if (conductanceWPerK == 0.0D || dtSeconds == 0.0D) {
            result.set(Status.APPLIED, enthalpyAJ, enthalpyBJ);
            return Status.APPLIED;
        }

        double approach = pairApproachFraction(
                capacityAJPerK,
                capacityBJPerK,
                conductanceWPerK,
                dtSeconds
        );
        double reducedCapacity = reducedCapacity(capacityAJPerK, capacityBJPerK);
        double energyFromAToB = reducedCapacity
                * approach
                * (temperatureOffsetA - temperatureOffsetB);
        if (!finite(energyFromAToB)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }

        double nextA = enthalpyAJ - energyFromAToB;
        double nextB = enthalpyBJ + energyFromAToB;
        if (!finite(nextA) || !finite(nextB)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }
        result.set(Status.APPLIED, nextA, nextB);
        return Status.APPLIED;
    }

    public static Status exchangeFixedBoundaryInto(
            double enthalpyJ,
            double capacityJPerK,
            double referenceTemperatureC,
            double boundaryTemperatureC,
            double conductanceWPerK,
            double dtSeconds,
            MutableBoundaryResult result
    ) {
        Objects.requireNonNull(result, "result");
        if (!finite(enthalpyJ)
                || !positiveFinite(capacityJPerK)
                || !finite(referenceTemperatureC)
                || !finite(boundaryTemperatureC)
                || !nonNegativeFinite(conductanceWPerK)
                || !nonNegativeFinite(dtSeconds)) {
            return degradedBoundary(enthalpyJ, result);
        }

        double cellOffsetK = enthalpyJ / capacityJPerK;
        double boundaryOffsetK = boundaryTemperatureC - referenceTemperatureC;
        if (!finite(cellOffsetK) || !finite(boundaryOffsetK)) {
            return degradedBoundary(enthalpyJ, result);
        }
        if (conductanceWPerK == 0.0D || dtSeconds == 0.0D) {
            result.set(Status.APPLIED, enthalpyJ, 0.0D);
            return Status.APPLIED;
        }

        double approach = boundaryApproachFraction(
                capacityJPerK,
                conductanceWPerK,
                dtSeconds
        );
        double energyFromBoundary = capacityJPerK
                * approach
                * (boundaryOffsetK - cellOffsetK);
        double next = enthalpyJ + energyFromBoundary;
        if (!finite(energyFromBoundary) || !finite(next)) {
            return degradedBoundary(enthalpyJ, result);
        }
        result.set(Status.APPLIED, next, energyFromBoundary);
        return Status.APPLIED;
    }

    private static double pairApproachFraction(
            double capacityA,
            double capacityB,
            double conductance,
            double dtSeconds
    ) {
        if (conductance == 0.0D || dtSeconds == 0.0D) {
            return 0.0D;
        }
        double rate = conductance / capacityA + conductance / capacityB;
        return approachFraction(rate * dtSeconds);
    }

    private static double boundaryApproachFraction(
            double capacity,
            double conductance,
            double dtSeconds
    ) {
        if (conductance == 0.0D || dtSeconds == 0.0D) {
            return 0.0D;
        }
        return approachFraction(conductance / capacity * dtSeconds);
    }

    private static double approachFraction(double exponent) {
        if (exponent == Double.POSITIVE_INFINITY) {
            return 1.0D;
        }
        return -Math.expm1(-exponent);
    }

    private static double reducedCapacity(double capacityA, double capacityB) {
        if (capacityA <= capacityB) {
            return capacityA / (1.0D + capacityA / capacityB);
        }
        return capacityB / (1.0D + capacityB / capacityA);
    }

    private static Status degradedPair(
            double enthalpyA,
            double enthalpyB,
            MutablePairResult result
    ) {
        result.set(Status.NUMERIC_DEGRADED, enthalpyA, enthalpyB);
        return Status.NUMERIC_DEGRADED;
    }

    private static Status degradedBoundary(
            double enthalpy,
            MutableBoundaryResult result
    ) {
        result.set(Status.NUMERIC_DEGRADED, enthalpy, 0.0D);
        return Status.NUMERIC_DEGRADED;
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }

    private static boolean positiveFinite(double value) {
        return value > 0.0D && finite(value);
    }

    private static boolean nonNegativeFinite(double value) {
        return value >= 0.0D && finite(value);
    }
}
