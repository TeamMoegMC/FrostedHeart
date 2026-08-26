/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Stateless geometry-energy migration formula used by topology replacement. */
public final class GeometryMigrationLedger {
    private static final double CAPACITY_TOLERANCE = 1.0e-10D;

    /**
     * Calculates a migration from caller-aggregated overlap rows, columns, and
     * enthalpy contributions. This avoids retaining one tuple per microcell.
     */
    public static MigrationResult calculateAggregatedGeometryMigration(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            double[] oldOverlapCapacitiesJPerK,
            double[] newOverlapCapacitiesJPerK,
            double[] newOverlapEnthalpiesJ,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        requireArray("oldEnthalpiesJ", oldEnthalpiesJ);
        requireArray("oldCapacitiesJPerK", oldCapacitiesJPerK);
        requireArray("newCapacitiesJPerK", newCapacitiesJPerK);
        requireArray("oldOverlapCapacitiesJPerK", oldOverlapCapacitiesJPerK);
        requireArray("newOverlapCapacitiesJPerK", newOverlapCapacitiesJPerK);
        requireArray("newOverlapEnthalpiesJ", newOverlapEnthalpiesJ);
        requireArray("newAirInitialTemperaturesC", newAirInitialTemperaturesC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        if (oldEnthalpiesJ.length != oldCapacitiesJPerK.length
                || oldEnthalpiesJ.length != oldOverlapCapacitiesJPerK.length) {
            throw new IllegalArgumentException("old migration arrays must have equal lengths");
        }
        if (newCapacitiesJPerK.length != newOverlapCapacitiesJPerK.length
                || newCapacitiesJPerK.length != newOverlapEnthalpiesJ.length
                || newCapacitiesJPerK.length != newAirInitialTemperaturesC.length) {
            throw new IllegalArgumentException("new migration arrays must have equal lengths");
        }

        double geometryEgress = 0.0D;
        for (int oldIndex = 0; oldIndex < oldCapacitiesJPerK.length; oldIndex++) {
            double oldEnthalpy = oldEnthalpiesJ[oldIndex];
            double oldCapacity = oldCapacitiesJPerK[oldIndex];
            double overlapCapacity = oldOverlapCapacitiesJPerK[oldIndex];
            requireFinite("old enthalpy", oldEnthalpy);
            requirePositive("old capacity", oldCapacity);
            requireNonNegative("old overlap row capacity", overlapCapacity);
            requireNotAboveCapacity("old overlap row", overlapCapacity, oldCapacity);
            double removedCapacity = nonNegativeRemainder(oldCapacity, overlapCapacity);
            geometryEgress = finiteSum(
                    "geometry egress",
                    geometryEgress,
                    finiteProduct("geometry egress contribution",
                            removedCapacity, oldEnthalpy / oldCapacity));
        }

        double geometryIngress = 0.0D;
        double[] newEnthalpies = newOverlapEnthalpiesJ.clone();
        for (int newIndex = 0; newIndex < newCapacitiesJPerK.length; newIndex++) {
            double newCapacity = newCapacitiesJPerK[newIndex];
            double overlapCapacity = newOverlapCapacitiesJPerK[newIndex];
            requirePositive("new capacity", newCapacity);
            requireNonNegative("new overlap column capacity", overlapCapacity);
            requireFinite("overlap enthalpy", newEnthalpies[newIndex]);
            requireFinite("new air initial temperature", newAirInitialTemperaturesC[newIndex]);
            requireNotAboveCapacity("new overlap column", overlapCapacity, newCapacity);
            double addedCapacity = nonNegativeRemainder(newCapacity, overlapCapacity);
            double ingress = finiteProduct(
                    "new-air ingress",
                    addedCapacity,
                    newAirInitialTemperaturesC[newIndex] - referenceTemperatureC);
            geometryIngress = finiteSum("geometry ingress", geometryIngress, ingress);
            newEnthalpies[newIndex] = finiteSum(
                    "new enthalpy", newEnthalpies[newIndex], ingress);
        }

        double oldTotal = sumPossiblyEmpty(oldEnthalpiesJ);
        double newTotal = sumPossiblyEmpty(newEnthalpies);
        double residual = finiteDifference(
                "geometry ledger residual",
                finiteSum("geometry ledger expected total",
                        finiteDifference("geometry ledger egress", oldTotal, geometryEgress),
                        geometryIngress),
                newTotal);
        return new MigrationResult(newEnthalpies, geometryIngress, geometryEgress, residual);
    }

    private static double nonNegativeRemainder(double capacity, double overlap) {
        double remainder = capacity - overlap;
        return remainder < 0.0D && nearlyEqual(capacity, overlap) ? 0.0D : remainder;
    }

    private static void requireNotAboveCapacity(String name, double overlap, double capacity) {
        if (overlap > capacity && !nearlyEqual(overlap, capacity)) {
            throw new IllegalArgumentException(name + " exceeds represented capacity");
        }
    }

    private static boolean nearlyEqual(double left, double right) {
        double scale = Math.max(1.0D, Math.max(Math.abs(left), Math.abs(right)));
        return Math.abs(left - right) <= CAPACITY_TOLERANCE * scale;
    }

    private static double sumPossiblyEmpty(double[] values) {
        double total = 0.0D;
        double compensation = 0.0D;
        for (double value : values) {
            requireFinite("enthalpy", value);
            double adjusted = value - compensation;
            double next = finiteSum("total enthalpy", total, adjusted);
            compensation = (next - total) - adjusted;
            total = next;
        }
        return total;
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
        requireFiniteResult(name, result);
        return result;
    }

    private static double finiteSum(String name, double left, double right) {
        double result = left + right;
        requireFiniteResult(name, result);
        return result;
    }

    private static double finiteDifference(String name, double left, double right) {
        double result = left - right;
        requireFiniteResult(name, result);
        return result;
    }

    private static void requireArray(String name, double[] values) {
        if (values == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void requireNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFiniteResult(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " exceeded the finite runtime domain");
        }
    }

    public record MigrationResult(
            double[] newEnthalpiesJ,
            double geometryIngressJ,
            double geometryEgressJ,
            double residualJ
    ) {
        public MigrationResult {
            newEnthalpiesJ = newEnthalpiesJ.clone();
        }

        @Override
        public double[] newEnthalpiesJ() {
            return newEnthalpiesJ.clone();
        }
    }
}
