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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThermalMigrationReferenceTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void pureLodSplitAndMergePreservePositiveAndNegativeEnthalpy() {
        double[] positive = ThermalMigrationReference.splitPureLod(
                120.0,
                60.0,
                new double[]{10.0, 20.0, 30.0}
        );
        assertArrayEquals(new double[]{20.0, 40.0, 60.0}, positive, EPSILON);
        assertEquals(120.0, ThermalMigrationReference.mergePureLod(positive), EPSILON);

        double[] negative = ThermalMigrationReference.splitPureLod(
                -120.0,
                60.0,
                new double[]{30.0, 20.0, 10.0}
        );
        assertArrayEquals(new double[]{-60.0, -40.0, -20.0}, negative, EPSILON);
        assertEquals(-120.0, ThermalMigrationReference.mergePureLod(negative), EPSILON);
    }

    @Test
    void removingAirBooksSignedGeometryEgressInsteadOfPretendingPureConservation() {
        ThermalMigrationReference.GeometryMigration migration =
                ThermalMigrationReference.migrateGeometry(
                        new double[]{200.0},
                        new double[]{100.0},
                        new double[]{40.0},
                        new double[][]{{40.0}},
                        new double[]{0.0},
                        0.0
                );

        assertArrayEquals(new double[]{80.0}, migration.newEnthalpiesJ(), EPSILON);
        assertEquals(0.0, migration.geometryIngressJ(), EPSILON);
        assertEquals(120.0, migration.geometryEgressJ(), EPSILON);
        assertEquals(0.0, migration.residualJ(), EPSILON);
    }

    @Test
    void addingAirBooksNaturalInitializationAsGeometryIngress() {
        ThermalMigrationReference.GeometryMigration migration =
                ThermalMigrationReference.migrateGeometry(
                        new double[]{80.0},
                        new double[]{40.0},
                        new double[]{100.0},
                        new double[][]{{40.0}},
                        new double[]{5.0},
                        0.0
                );

        assertArrayEquals(new double[]{380.0}, migration.newEnthalpiesJ(), EPSILON);
        assertEquals(300.0, migration.geometryIngressJ(), EPSILON);
        assertEquals(0.0, migration.geometryEgressJ(), EPSILON);
        assertEquals(0.0, migration.residualJ(), EPSILON);
    }

    @Test
    void overlapMatrixSupportsSplitAndMergeWithoutChangingAirVolume() {
        ThermalMigrationReference.GeometryMigration migration =
                ThermalMigrationReference.migrateGeometry(
                        new double[]{100.0, -50.0},
                        new double[]{50.0, 50.0},
                        new double[]{25.0, 50.0, 25.0},
                        new double[][]{
                                {25.0, 25.0, 0.0},
                                {0.0, 25.0, 25.0}
                        },
                        new double[]{0.0, 0.0, 0.0},
                        0.0
                );

        assertArrayEquals(new double[]{50.0, 25.0, -25.0}, migration.newEnthalpiesJ(), EPSILON);
        assertEquals(0.0, migration.geometryIngressJ(), EPSILON);
        assertEquals(0.0, migration.geometryEgressJ(), EPSILON);
        assertEquals(0.0, migration.residualJ(), EPSILON);
    }

    @Test
    void completeAirRemovalAndCreationUseSignedGeometryLedgers() {
        ThermalMigrationReference.GeometryMigration removed =
                ThermalMigrationReference.migrateGeometry(
                        new double[]{-20.0},
                        new double[]{10.0},
                        new double[]{},
                        new double[][]{{}},
                        new double[]{},
                        0.0
                );
        assertArrayEquals(new double[]{}, removed.newEnthalpiesJ(), EPSILON);
        assertEquals(0.0, removed.geometryIngressJ(), EPSILON);
        assertEquals(-20.0, removed.geometryEgressJ(), EPSILON);
        assertEquals(0.0, removed.residualJ(), EPSILON);

        ThermalMigrationReference.GeometryMigration created =
                ThermalMigrationReference.migrateGeometry(
                        new double[]{},
                        new double[]{},
                        new double[]{10.0},
                        new double[][]{},
                        new double[]{5.0},
                        0.0
                );
        assertArrayEquals(new double[]{50.0}, created.newEnthalpiesJ(), EPSILON);
        assertEquals(50.0, created.geometryIngressJ(), EPSILON);
        assertEquals(0.0, created.geometryEgressJ(), EPSILON);
        assertEquals(0.0, created.residualJ(), EPSILON);
    }

    @Test
    void invalidCapacityChangesCannotMasqueradeAsPureLodOrOverlap() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalMigrationReference.splitPureLod(
                        10.0,
                        10.0,
                        new double[]{4.0, 4.0}
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalMigrationReference.migrateGeometry(
                        new double[]{10.0},
                        new double[]{10.0},
                        new double[]{10.0},
                        new double[][]{{11.0}},
                        new double[]{0.0},
                        0.0
                )
        );
    }
}
