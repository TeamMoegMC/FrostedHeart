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

/** Runtime migration formulas and cumulative signed geometry-energy ledger. */
public final class GeometryMigrationLedger {
    private static final double CAPACITY_TOLERANCE = 1.0e-10D;

    private double geometryIngressJ;
    private double geometryEgressJ;
    private double residualJ;

    /** Pure LOD split: every child inherits the parent temperature offset. */
    public static double[] splitPureLod(
            double parentEnthalpyJ,
            double parentCapacityJPerK,
            double[] childCapacitiesJPerK
    ) {
        requireFinite("parentEnthalpyJ", parentEnthalpyJ);
        requirePositive("parentCapacityJPerK", parentCapacityJPerK);
        requireNonEmpty("childCapacitiesJPerK", childCapacitiesJPerK);

        double totalChildCapacity = 0.0D;
        for (double childCapacity : childCapacitiesJPerK) {
            requirePositive("child capacity", childCapacity);
            totalChildCapacity = finiteSum(
                    "total child capacity", totalChildCapacity, childCapacity);
        }
        requireEquivalentCapacity(parentCapacityJPerK, totalChildCapacity);

        double[] childEnthalpies = new double[childCapacitiesJPerK.length];
        double assigned = 0.0D;
        for (int index = 0; index < childEnthalpies.length - 1; index++) {
            childEnthalpies[index] = finiteProduct(
                    "child enthalpy",
                    parentEnthalpyJ,
                    childCapacitiesJPerK[index] / parentCapacityJPerK);
            assigned = finiteSum(
                    "assigned child enthalpy", assigned, childEnthalpies[index]);
        }
        childEnthalpies[childEnthalpies.length - 1] = finiteDifference(
                "final child enthalpy", parentEnthalpyJ, assigned);
        return childEnthalpies;
    }

    /** Pure LOD merge with compensated summation. */
    public static double mergePureLod(double[] childEnthalpiesJ) {
        requireNonEmpty("childEnthalpiesJ", childEnthalpiesJ);
        double total = 0.0D;
        double compensation = 0.0D;
        for (double childEnthalpy : childEnthalpiesJ) {
            requireFinite("child enthalpy", childEnthalpy);
            double adjusted = childEnthalpy - compensation;
            double next = finiteSum("merged enthalpy", total, adjusted);
            compensation = (next - total) - adjusted;
            total = next;
        }
        return total;
    }

    /** Calculates one migration without changing a cumulative ledger. */
    public static MigrationResult calculateGeometryMigration(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            double[][] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        requireArray("oldEnthalpiesJ", oldEnthalpiesJ);
        requireArray("oldCapacitiesJPerK", oldCapacitiesJPerK);
        requireArray("newCapacitiesJPerK", newCapacitiesJPerK);
        requireArray("newAirInitialTemperaturesC", newAirInitialTemperaturesC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        if (oldEnthalpiesJ.length != oldCapacitiesJPerK.length) {
            throw new IllegalArgumentException("old enthalpy and capacity lengths must match");
        }
        if (newCapacitiesJPerK.length != newAirInitialTemperaturesC.length) {
            throw new IllegalArgumentException("new capacity and temperature lengths must match");
        }
        if (overlapCapacitiesJPerK == null
                || overlapCapacitiesJPerK.length != oldCapacitiesJPerK.length) {
            throw new IllegalArgumentException("overlap matrix row count must match old cells");
        }

        double[] oldTemperatureOffsets = new double[oldCapacitiesJPerK.length];
        double[] rowOverlap = new double[oldCapacitiesJPerK.length];
        double[] columnOverlap = new double[newCapacitiesJPerK.length];
        double[] newEnthalpies = new double[newCapacitiesJPerK.length];

        for (int oldIndex = 0; oldIndex < oldCapacitiesJPerK.length; oldIndex++) {
            requireFinite("old enthalpy", oldEnthalpiesJ[oldIndex]);
            requirePositive("old capacity", oldCapacitiesJPerK[oldIndex]);
            oldTemperatureOffsets[oldIndex] = oldEnthalpiesJ[oldIndex]
                    / oldCapacitiesJPerK[oldIndex];

            double[] row = overlapCapacitiesJPerK[oldIndex];
            if (row == null || row.length != newCapacitiesJPerK.length) {
                throw new IllegalArgumentException("each overlap row must match new cells");
            }
            for (int newIndex = 0; newIndex < newCapacitiesJPerK.length; newIndex++) {
                double overlapCapacity = row[newIndex];
                requireNonNegative("overlap capacity", overlapCapacity);
                rowOverlap[oldIndex] = finiteSum(
                        "old overlap row capacity",
                        rowOverlap[oldIndex], overlapCapacity);
                columnOverlap[newIndex] = finiteSum(
                        "new overlap column capacity",
                        columnOverlap[newIndex], overlapCapacity);
                newEnthalpies[newIndex] = finiteSum(
                        "overlap enthalpy",
                        newEnthalpies[newIndex],
                        finiteProduct(
                                "overlap enthalpy contribution",
                                overlapCapacity,
                                oldTemperatureOffsets[oldIndex]));
            }
            requireNotAboveCapacity(
                    "old overlap row", rowOverlap[oldIndex], oldCapacitiesJPerK[oldIndex]);
        }

        double geometryEgress = 0.0D;
        for (int oldIndex = 0; oldIndex < oldCapacitiesJPerK.length; oldIndex++) {
            double removedCapacity = nonNegativeRemainder(
                    oldCapacitiesJPerK[oldIndex], rowOverlap[oldIndex]);
            geometryEgress = finiteSum(
                    "geometry egress",
                    geometryEgress,
                    finiteProduct(
                            "geometry egress contribution",
                            removedCapacity,
                            oldTemperatureOffsets[oldIndex]));
        }

        double geometryIngress = 0.0D;
        for (int newIndex = 0; newIndex < newCapacitiesJPerK.length; newIndex++) {
            requirePositive("new capacity", newCapacitiesJPerK[newIndex]);
            requireFinite("new air initial temperature", newAirInitialTemperaturesC[newIndex]);
            requireNotAboveCapacity(
                    "new overlap column", columnOverlap[newIndex], newCapacitiesJPerK[newIndex]);
            double addedCapacity = nonNegativeRemainder(
                    newCapacitiesJPerK[newIndex], columnOverlap[newIndex]);
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
                finiteSum(
                        "geometry ledger expected total",
                        finiteDifference("geometry ledger egress", oldTotal, geometryEgress),
                        geometryIngress),
                newTotal);
        return new MigrationResult(newEnthalpies, geometryIngress, geometryEgress, residual);
    }

    /**
     * Sparse equivalent of {@link #calculateGeometryMigration}; each overlap
     * entry identifies one old/new cell pair and a represented heat capacity.
     */
    public static MigrationResult calculateSparseGeometryMigration(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            int[] overlapOldIndices,
            int[] overlapNewIndices,
            double[] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        return calculateSparseGeometryMigration(
                oldEnthalpiesJ,
                oldCapacitiesJPerK,
                newCapacitiesJPerK,
                overlapOldIndices,
                overlapNewIndices,
                overlapCapacitiesJPerK,
                overlapOldIndices == null ? 0 : overlapOldIndices.length,
                newAirInitialTemperaturesC,
                referenceTemperatureC);
    }

    /** Counted variant for callers that reuse bounded primitive scratch arrays. */
    public static MigrationResult calculateSparseGeometryMigration(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            int[] overlapOldIndices,
            int[] overlapNewIndices,
            double[] overlapCapacitiesJPerK,
            int overlapCount,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        requireArray("oldEnthalpiesJ", oldEnthalpiesJ);
        requireArray("oldCapacitiesJPerK", oldCapacitiesJPerK);
        requireArray("newCapacitiesJPerK", newCapacitiesJPerK);
        requireArray("newAirInitialTemperaturesC", newAirInitialTemperaturesC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        if (oldEnthalpiesJ.length != oldCapacitiesJPerK.length) {
            throw new IllegalArgumentException("old enthalpy and capacity lengths must match");
        }
        if (newCapacitiesJPerK.length != newAirInitialTemperaturesC.length) {
            throw new IllegalArgumentException("new capacity and temperature lengths must match");
        }
        if (overlapOldIndices == null || overlapNewIndices == null
                || overlapCapacitiesJPerK == null
                || overlapCount < 0
                || overlapCount > overlapOldIndices.length
                || overlapCount > overlapNewIndices.length
                || overlapCount > overlapCapacitiesJPerK.length) {
            throw new IllegalArgumentException("sparse overlap arrays do not cover overlapCount");
        }

        double[] rowOverlap = new double[oldCapacitiesJPerK.length];
        double[] columnOverlap = new double[newCapacitiesJPerK.length];
        double[] overlapEnthalpies = new double[newCapacitiesJPerK.length];
        for (int oldIndex = 0; oldIndex < oldCapacitiesJPerK.length; oldIndex++) {
            requireFinite("old enthalpy", oldEnthalpiesJ[oldIndex]);
            requirePositive("old capacity", oldCapacitiesJPerK[oldIndex]);
        }
        for (int entry = 0; entry < overlapCount; entry++) {
            int oldIndex = overlapOldIndices[entry];
            int newIndex = overlapNewIndices[entry];
            if (oldIndex < 0 || oldIndex >= oldCapacitiesJPerK.length
                    || newIndex < 0 || newIndex >= newCapacitiesJPerK.length) {
                throw new IllegalArgumentException("sparse overlap index is out of bounds");
            }
            double overlap = overlapCapacitiesJPerK[entry];
            requireNonNegative("overlap capacity", overlap);
            rowOverlap[oldIndex] = finiteSum(
                    "old overlap row capacity", rowOverlap[oldIndex], overlap);
            columnOverlap[newIndex] = finiteSum(
                    "new overlap column capacity", columnOverlap[newIndex], overlap);
            overlapEnthalpies[newIndex] = finiteSum(
                    "overlap enthalpy",
                    overlapEnthalpies[newIndex],
                    finiteProduct("overlap enthalpy contribution",
                            overlap,
                            oldEnthalpiesJ[oldIndex] / oldCapacitiesJPerK[oldIndex]));
        }
        return calculateAggregatedGeometryMigration(
                oldEnthalpiesJ,
                oldCapacitiesJPerK,
                newCapacitiesJPerK,
                rowOverlap,
                columnOverlap,
                overlapEnthalpies,
                newAirInitialTemperaturesC,
                referenceTemperatureC);
    }

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

    public MigrationResult migrateGeometry(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            double[][] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        MigrationResult migration = calculateGeometryMigration(
                oldEnthalpiesJ,
                oldCapacitiesJPerK,
                newCapacitiesJPerK,
                overlapCapacitiesJPerK,
                newAirInitialTemperaturesC,
                referenceTemperatureC);
        geometryIngressJ = finiteSum(
                "cumulative geometry ingress", geometryIngressJ, migration.geometryIngressJ());
        geometryEgressJ = finiteSum(
                "cumulative geometry egress", geometryEgressJ, migration.geometryEgressJ());
        residualJ = finiteSum(
                "cumulative geometry residual", residualJ, migration.residualJ());
        return migration;
    }

    public MigrationResult migrateSparseGeometry(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            int[] overlapOldIndices,
            int[] overlapNewIndices,
            double[] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        MigrationResult migration = calculateSparseGeometryMigration(
                oldEnthalpiesJ,
                oldCapacitiesJPerK,
                newCapacitiesJPerK,
                overlapOldIndices,
                overlapNewIndices,
                overlapCapacitiesJPerK,
                newAirInitialTemperaturesC,
                referenceTemperatureC);
        geometryIngressJ = finiteSum(
                "cumulative geometry ingress", geometryIngressJ, migration.geometryIngressJ());
        geometryEgressJ = finiteSum(
                "cumulative geometry egress", geometryEgressJ, migration.geometryEgressJ());
        residualJ = finiteSum(
                "cumulative geometry residual", residualJ, migration.residualJ());
        return migration;
    }

    /** Records a migration only after its topology transaction has committed. */
    public void record(MigrationResult migration) {
        if (migration == null) {
            throw new IllegalArgumentException("migration is required");
        }
        geometryIngressJ = finiteSum(
                "cumulative geometry ingress", geometryIngressJ, migration.geometryIngressJ());
        geometryEgressJ = finiteSum(
                "cumulative geometry egress", geometryEgressJ, migration.geometryEgressJ());
        residualJ = finiteSum(
                "cumulative geometry residual", residualJ, migration.residualJ());
    }

    public Snapshot snapshot() {
        return new Snapshot(geometryIngressJ, geometryEgressJ, residualJ);
    }

    public void reset() {
        geometryIngressJ = 0.0D;
        geometryEgressJ = 0.0D;
        residualJ = 0.0D;
    }

    private static double nonNegativeRemainder(double capacity, double overlap) {
        double remainder = capacity - overlap;
        return remainder < 0.0D && nearlyEqual(capacity, overlap) ? 0.0D : remainder;
    }

    private static void requireEquivalentCapacity(double expected, double actual) {
        if (!nearlyEqual(expected, actual)) {
            throw new IllegalArgumentException(
                    "pure LOD migration changed capacity: " + expected + " != " + actual);
        }
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
        return values.length == 0 ? 0.0D : mergePureLod(values);
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

    private static void requireNonEmpty(String name, double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
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

    public record Snapshot(double geometryIngressJ, double geometryEgressJ, double residualJ) {
        public boolean residualWithin(double absoluteToleranceJ) {
            if (!Double.isFinite(absoluteToleranceJ) || absoluteToleranceJ < 0.0D) {
                throw new IllegalArgumentException(
                        "absoluteToleranceJ must be finite and non-negative");
            }
            return Math.abs(residualJ) <= absoluteToleranceJ;
        }
    }
}
