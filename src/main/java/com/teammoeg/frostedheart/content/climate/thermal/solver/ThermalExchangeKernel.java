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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;

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

    /** Caller-owned result for a pair that can cross sensible/latent branches. */
    public static final class MutableMaterialPairResult {
        private double firstJ, secondJ;
        private byte firstBranch, secondBranch;
        private boolean firstWaiting, secondWaiting;
        private int updates;
        public double firstJ() { return firstJ; }
        public double secondJ() { return secondJ; }
        public byte firstBranch() { return firstBranch; }
        public byte secondBranch() { return secondBranch; }
        public boolean firstWaiting() { return firstWaiting; }
        public boolean secondWaiting() { return secondWaiting; }
        public int updates() { return updates; }
    }

    /** Shared enthalpy integration for regional and fixed-tile material contacts. */
    public static void exchangeMaterialsInto(MaterialThermalLaw first, double firstJ, byte firstBranch,
            boolean firstWaiting, MaterialThermalLaw second, double secondJ, byte secondBranch,
            boolean secondWaiting, double conductance, double duration, MutableMaterialPairResult out) {
        int updates = 0;
        if (conductance != 0 && !firstWaiting && !secondWaiting) {
            double remaining = duration;
            for (int segment = 0; segment < 6 && remaining > 0; segment++) {
                double delta = first.temperatureC(firstJ, firstBranch) - second.temperatureC(secondJ, secondBranch);
                if (delta == 0) break;
                double direction = Math.signum(delta);
                firstBranch = first.selectBranch(firstJ, firstBranch, -direction);
                secondBranch = second.selectBranch(secondJ, secondBranch, direction);
                double limit = Math.min(first.energyLimitJ(firstJ, firstBranch, -direction),
                        second.energyLimitJ(secondJ, secondBranch, direction));
                if (limit == 0) break;
                delta = first.temperatureC(firstJ, firstBranch) - second.temperatureC(secondJ, secondBranch);
                if (delta == 0 || Math.signum(delta) != direction) break;
                double slope = first.slopeKPerJ(firstJ, firstBranch) + second.slopeKPerJ(secondJ, secondBranch);
                double requested = slope == 0 ? Math.abs(delta) * conductance * remaining
                        : Math.abs(delta) / slope * -Math.expm1(-conductance * slope * remaining);
                double transferred = Math.min(requested, limit);
                if (transferred == 0) break;
                firstJ -= direction * transferred; secondJ += direction * transferred; updates++;
                var a = first.transition(firstBranch);
                var b = second.transition(secondBranch);
                firstWaiting |= a != null && a.complete(firstJ);
                secondWaiting |= b != null && b.complete(secondJ);
                if (transferred == requested || firstWaiting || secondWaiting) break;
                double elapsed = slope == 0 ? transferred / (conductance * Math.abs(delta))
                        : -Math.log1p(-transferred * slope / Math.abs(delta)) / (conductance * slope);
                remaining = Math.max(0, remaining - elapsed);
            }
        }
        out.firstJ = firstJ; out.secondJ = secondJ;
        out.firstBranch = firstBranch; out.secondBranch = secondBranch;
        out.firstWaiting = firstWaiting; out.secondWaiting = secondWaiting; out.updates = updates;
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
