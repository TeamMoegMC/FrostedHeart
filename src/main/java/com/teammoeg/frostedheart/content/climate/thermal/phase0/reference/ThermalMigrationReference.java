/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

/** Reference rules for pure LOD redistribution and air-volume geometry changes. */
public final class ThermalMigrationReference {
    private static final double CAPACITY_TOLERANCE = 1.0e-10;

    private ThermalMigrationReference() {
    }

    public record GeometryMigration(
            double[] newEnthalpiesJ,
            double geometryIngressJ,
            double geometryEgressJ,
            double residualJ
    ) {
        public GeometryMigration {
            newEnthalpiesJ = newEnthalpiesJ.clone();
        }

        @Override
        public double[] newEnthalpiesJ() {
            return newEnthalpiesJ.clone();
        }
    }

    /**
     * Splits one cell without changing represented air volume. All children keep the
     * parent's temperature offset, and the final child absorbs floating-point remainder.
     */
    public static double[] splitPureLod(
            double parentEnthalpyJ,
            double parentCapacityJPerK,
            double[] childCapacitiesJPerK
    ) {
        ThermalUnits.requireFinite("parentEnthalpyJ", parentEnthalpyJ);
        ThermalUnits.requirePositive("parentCapacityJPerK", parentCapacityJPerK);
        requireNonEmpty("childCapacitiesJPerK", childCapacitiesJPerK);

        double totalChildCapacity = 0.0;
        for (double childCapacity : childCapacitiesJPerK) {
            ThermalUnits.requirePositive("child capacity", childCapacity);
            totalChildCapacity = ThermalUnits.requireFiniteResult(
                    "total child capacity",
                    totalChildCapacity + childCapacity
            );
        }
        requireEquivalentCapacity(parentCapacityJPerK, totalChildCapacity);

        double[] childEnthalpies = new double[childCapacitiesJPerK.length];
        double assigned = 0.0;
        for (int i = 0; i < childEnthalpies.length - 1; i++) {
            childEnthalpies[i] = ThermalUnits.requireFiniteResult(
                    "child enthalpy",
                    parentEnthalpyJ * (childCapacitiesJPerK[i] / parentCapacityJPerK)
            );
            assigned = ThermalUnits.requireFiniteResult(
                    "assigned child enthalpy",
                    assigned + childEnthalpies[i]
            );
        }
        childEnthalpies[childEnthalpies.length - 1] = ThermalUnits.requireFiniteResult(
                "final child enthalpy",
                parentEnthalpyJ - assigned
        );
        return childEnthalpies;
    }

    /** Merges cells without changing represented air volume. */
    public static double mergePureLod(double[] childEnthalpiesJ) {
        requireNonEmpty("childEnthalpiesJ", childEnthalpiesJ);
        double total = 0.0;
        double compensation = 0.0;
        for (double childEnthalpy : childEnthalpiesJ) {
            ThermalUnits.requireFinite("child enthalpy", childEnthalpy);
            double adjusted = childEnthalpy - compensation;
            double next = ThermalUnits.requireFiniteResult("merged enthalpy", total + adjusted);
            compensation = (next - total) - adjusted;
            total = next;
        }
        return total;
    }

    /**
     * Migrates air enthalpy through an old/new capacity-overlap matrix.
     *
     * <p>Overlap capacity retains each old cell's temperature. Removed capacity is booked
     * as signed geometry egress. Added capacity is initialized from the supplied natural or
     * neighboring temperature and booked as signed geometry ingress.</p>
     */
    public static GeometryMigration migrateGeometry(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            double[][] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        requireNonEmpty("oldEnthalpiesJ", oldEnthalpiesJ);
        requireNonEmpty("oldCapacitiesJPerK", oldCapacitiesJPerK);
        requireNonEmpty("newCapacitiesJPerK", newCapacitiesJPerK);
        requireNonEmpty("newAirInitialTemperaturesC", newAirInitialTemperaturesC);
        ThermalUnits.requireFinite("referenceTemperatureC", referenceTemperatureC);
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
            ThermalUnits.requireFinite("old enthalpy", oldEnthalpiesJ[oldIndex]);
            ThermalUnits.requirePositive("old capacity", oldCapacitiesJPerK[oldIndex]);
            oldTemperatureOffsets[oldIndex] = oldEnthalpiesJ[oldIndex]
                    / oldCapacitiesJPerK[oldIndex];

            double[] row = overlapCapacitiesJPerK[oldIndex];
            if (row == null || row.length != newCapacitiesJPerK.length) {
                throw new IllegalArgumentException("each overlap row must match new cells");
            }
            for (int newIndex = 0; newIndex < newCapacitiesJPerK.length; newIndex++) {
                double overlapCapacity = row[newIndex];
                ThermalUnits.requireNonNegative("overlap capacity", overlapCapacity);
                rowOverlap[oldIndex] = ThermalUnits.requireFiniteResult(
                        "old overlap row capacity",
                        rowOverlap[oldIndex] + overlapCapacity
                );
                columnOverlap[newIndex] = ThermalUnits.requireFiniteResult(
                        "new overlap column capacity",
                        columnOverlap[newIndex] + overlapCapacity
                );
                newEnthalpies[newIndex] = ThermalUnits.requireFiniteResult(
                        "overlap enthalpy",
                        newEnthalpies[newIndex]
                                + overlapCapacity * oldTemperatureOffsets[oldIndex]
                );
            }
            requireNotAboveCapacity(
                    "old overlap row",
                    rowOverlap[oldIndex],
                    oldCapacitiesJPerK[oldIndex]
            );
        }

        double geometryEgressJ = 0.0;
        for (int oldIndex = 0; oldIndex < oldCapacitiesJPerK.length; oldIndex++) {
            double removedCapacity = nonNegativeRemainder(
                    oldCapacitiesJPerK[oldIndex],
                    rowOverlap[oldIndex]
            );
            geometryEgressJ = ThermalUnits.requireFiniteResult(
                    "geometry egress",
                    geometryEgressJ + removedCapacity * oldTemperatureOffsets[oldIndex]
            );
        }

        double geometryIngressJ = 0.0;
        for (int newIndex = 0; newIndex < newCapacitiesJPerK.length; newIndex++) {
            ThermalUnits.requirePositive("new capacity", newCapacitiesJPerK[newIndex]);
            ThermalUnits.requireFinite(
                    "new air initial temperature",
                    newAirInitialTemperaturesC[newIndex]
            );
            requireNotAboveCapacity(
                    "new overlap column",
                    columnOverlap[newIndex],
                    newCapacitiesJPerK[newIndex]
            );
            double addedCapacity = nonNegativeRemainder(
                    newCapacitiesJPerK[newIndex],
                    columnOverlap[newIndex]
            );
            double ingress = ThermalUnits.requireFiniteResult(
                    "new-air ingress",
                    addedCapacity
                            * (newAirInitialTemperaturesC[newIndex] - referenceTemperatureC)
            );
            geometryIngressJ = ThermalUnits.requireFiniteResult(
                    "geometry ingress",
                    geometryIngressJ + ingress
            );
            newEnthalpies[newIndex] = ThermalUnits.requireFiniteResult(
                    "new enthalpy",
                    newEnthalpies[newIndex] + ingress
            );
        }

        double oldTotal = mergePureLod(oldEnthalpiesJ);
        double newTotal = mergePureLod(newEnthalpies);
        double residualJ = ThermalUnits.requireFiniteResult(
                "geometry ledger residual",
                oldTotal - geometryEgressJ + geometryIngressJ - newTotal
        );
        return new GeometryMigration(
                newEnthalpies,
                geometryIngressJ,
                geometryEgressJ,
                residualJ
        );
    }

    private static double nonNegativeRemainder(double capacity, double overlap) {
        double remainder = capacity - overlap;
        if (remainder < 0.0 && nearlyEqual(capacity, overlap)) {
            return 0.0;
        }
        return remainder;
    }

    private static void requireEquivalentCapacity(double expected, double actual) {
        if (!nearlyEqual(expected, actual)) {
            throw new IllegalArgumentException(
                    "pure LOD migration changed capacity: " + expected + " != " + actual
            );
        }
    }

    private static void requireNotAboveCapacity(String name, double overlap, double capacity) {
        if (overlap > capacity && !nearlyEqual(overlap, capacity)) {
            throw new IllegalArgumentException(name + " exceeds represented capacity");
        }
    }

    private static boolean nearlyEqual(double left, double right) {
        double scale = Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)));
        return Math.abs(left - right) <= CAPACITY_TOLERANCE * scale;
    }

    private static void requireNonEmpty(String name, double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }
}
