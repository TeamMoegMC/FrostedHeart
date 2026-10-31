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
        private double enthalpyAJ;
        private double enthalpyBJ;

        public double enthalpyAJ() {
            return enthalpyAJ;
        }

        public double enthalpyBJ() {
            return enthalpyBJ;
        }

        private void set(
                double nextEnthalpyA,
                double nextEnthalpyB
        ) {
            enthalpyAJ = nextEnthalpyA;
            enthalpyBJ = nextEnthalpyB;
        }

    }

    /** Caller-owned boundary result for allocation-free sweep execution. */
    public static final class MutableBoundaryResult {
        private double enthalpyJ;

        public double enthalpyJ() {
            return enthalpyJ;
        }

        private void set(
                double nextEnthalpy
        ) {
            enthalpyJ = nextEnthalpy;
        }

    }

    /** Generic pair path using arena-owned inverse capacities. */
    public static Status exchangePairWithInverseInto(
            double enthalpyAJ,
            double capacityAJPerK,
            double inverseCapacityAKPerJ,
            double enthalpyBJ,
            double capacityBJPerK,
            double inverseCapacityBKPerJ,
            double conductanceWPerK,
            double dtSeconds,
            MutablePairResult result
    ) {
        Objects.requireNonNull(result, "result");
        if (!finite(enthalpyAJ)
                || !positiveFinite(capacityAJPerK)
                || !positiveFinite(inverseCapacityAKPerJ)
                || !finite(enthalpyBJ)
                || !positiveFinite(capacityBJPerK)
                || !positiveFinite(inverseCapacityBKPerJ)
                || !nonNegativeFinite(conductanceWPerK)
                || !nonNegativeFinite(dtSeconds)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }

        double temperatureOffsetA = enthalpyAJ * inverseCapacityAKPerJ;
        double temperatureOffsetB = enthalpyBJ * inverseCapacityBKPerJ;
        if (!finite(temperatureOffsetA) || !finite(temperatureOffsetB)) {
            return degradedPair(enthalpyAJ, enthalpyBJ, result);
        }
        if (conductanceWPerK == 0.0D || dtSeconds == 0.0D) {
            result.set(enthalpyAJ, enthalpyBJ);
            return Status.APPLIED;
        }

        double approach = approachFraction(
                conductanceWPerK
                        * (inverseCapacityAKPerJ + inverseCapacityBKPerJ)
                        * dtSeconds);
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
        result.set(nextA, nextB);
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
        return exchangeFixedBoundaryWithInverseInto(
                enthalpyJ,
                capacityJPerK,
                1.0D / capacityJPerK,
                referenceTemperatureC,
                boundaryTemperatureC,
                conductanceWPerK,
                dtSeconds,
                result);
    }

    /** Generic fixed-boundary path using the arena-owned inverse capacity. */
    public static Status exchangeFixedBoundaryWithInverseInto(
            double enthalpyJ,
            double capacityJPerK,
            double inverseCapacityKPerJ,
            double referenceTemperatureC,
            double boundaryTemperatureC,
            double conductanceWPerK,
            double dtSeconds,
            MutableBoundaryResult result
    ) {
        Objects.requireNonNull(result, "result");
        if (!finite(enthalpyJ)
                || !positiveFinite(capacityJPerK)
                || !positiveFinite(inverseCapacityKPerJ)
                || !finite(referenceTemperatureC)
                || !finite(boundaryTemperatureC)
                || !nonNegativeFinite(conductanceWPerK)
                || !nonNegativeFinite(dtSeconds)) {
            return degradedBoundary(enthalpyJ, result);
        }

        double cellOffsetK = enthalpyJ * inverseCapacityKPerJ;
        double boundaryOffsetK = boundaryTemperatureC - referenceTemperatureC;
        if (!finite(cellOffsetK) || !finite(boundaryOffsetK)) {
            return degradedBoundary(enthalpyJ, result);
        }
        if (conductanceWPerK == 0.0D || dtSeconds == 0.0D) {
            result.set(enthalpyJ);
            return Status.APPLIED;
        }

        double approach = approachFraction(
                conductanceWPerK * inverseCapacityKPerJ * dtSeconds);
        double energyFromBoundary = capacityJPerK
                * approach
                * (boundaryOffsetK - cellOffsetK);
        double next = enthalpyJ + energyFromBoundary;
        if (!finite(energyFromBoundary) || !finite(next)) {
            return degradedBoundary(enthalpyJ, result);
        }
        result.set(next);
        return Status.APPLIED;
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
        result.set(enthalpyA, enthalpyB);
        return Status.NUMERIC_DEGRADED;
    }

    private static Status degradedBoundary(
            double enthalpy,
            MutableBoundaryResult result
    ) {
        result.set(enthalpy);
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
