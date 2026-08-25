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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryMigrationLedgerTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void pureLodWrapperPreservesExactTotalEnthalpy() {
        double[] children = GeometryMigrationLedger.splitPureLod(
                123.0D,
                64.0D,
                new double[]{8.0D, 8.0D, 16.0D, 32.0D}
        );

        assertEquals(123.0D, GeometryMigrationLedger.mergePureLod(children));
    }

    @Test
    void signedIngressAndEgressAccumulateWithoutHidingResidual() {
        GeometryMigrationLedger ledger = new GeometryMigrationLedger();

        GeometryMigrationLedger.MigrationResult removed = ledger.migrateGeometry(
                new double[]{-200.0D},
                new double[]{100.0D},
                new double[]{40.0D},
                new double[][]{{40.0D}},
                new double[]{0.0D},
                0.0D
        );
        assertArrayEquals(new double[]{-80.0D}, removed.newEnthalpiesJ(), EPSILON);
        assertEquals(-120.0D, removed.geometryEgressJ(), EPSILON);
        assertEquals(0.0D, removed.residualJ(), EPSILON);

        GeometryMigrationLedger.MigrationResult added = ledger.migrateGeometry(
                new double[]{-80.0D},
                new double[]{40.0D},
                new double[]{100.0D},
                new double[][]{{40.0D}},
                new double[]{-5.0D},
                0.0D
        );
        assertArrayEquals(new double[]{-380.0D}, added.newEnthalpiesJ(), EPSILON);
        assertEquals(-300.0D, added.geometryIngressJ(), EPSILON);

        GeometryMigrationLedger.Snapshot totals = ledger.snapshot();
        assertEquals(-300.0D, totals.geometryIngressJ(), EPSILON);
        assertEquals(-120.0D, totals.geometryEgressJ(), EPSILON);
        assertTrue(totals.residualWithin(EPSILON));
    }

    @Test
    void migrationResultDoesNotExposeItsMutableEnthalpyArray() {
        GeometryMigrationLedger ledger = new GeometryMigrationLedger();
        GeometryMigrationLedger.MigrationResult migration = ledger.migrateGeometry(
                new double[]{20.0D},
                new double[]{10.0D},
                new double[]{10.0D},
                new double[][]{{10.0D}},
                new double[]{0.0D},
                0.0D
        );

        double[] exposed = migration.newEnthalpiesJ();
        exposed[0] = 0.0D;
        assertArrayEquals(new double[]{20.0D}, migration.newEnthalpiesJ(), EPSILON);
    }

    @Test
    void sparseMigrationMatchesTheDenseReference() {
        GeometryMigrationLedger.MigrationResult dense =
                GeometryMigrationLedger.calculateGeometryMigration(
                        new double[]{20.0D, -30.0D},
                        new double[]{10.0D, 20.0D},
                        new double[]{15.0D, 15.0D},
                        new double[][]{{10.0D, 0.0D}, {5.0D, 15.0D}},
                        new double[]{4.0D, 4.0D},
                        0.0D);
        GeometryMigrationLedger.MigrationResult sparse =
                GeometryMigrationLedger.calculateSparseGeometryMigration(
                        new double[]{20.0D, -30.0D},
                        new double[]{10.0D, 20.0D},
                        new double[]{15.0D, 15.0D},
                        new int[]{0, 1, 1, 99},
                        new int[]{0, 0, 1, 99},
                        new double[]{10.0D, 5.0D, 15.0D, Double.NaN},
                        3,
                        new double[]{4.0D, 4.0D},
                        0.0D);

        assertArrayEquals(dense.newEnthalpiesJ(), sparse.newEnthalpiesJ(), EPSILON);
        assertEquals(dense.geometryIngressJ(), sparse.geometryIngressJ(), EPSILON);
        assertEquals(dense.geometryEgressJ(), sparse.geometryEgressJ(), EPSILON);
        assertEquals(dense.residualJ(), sparse.residualJ(), EPSILON);
    }

    @Test
    void aggregatedMigrationMatchesTheDenseReference() {
        GeometryMigrationLedger.MigrationResult dense =
                GeometryMigrationLedger.calculateGeometryMigration(
                        new double[]{20.0D, -30.0D},
                        new double[]{10.0D, 20.0D},
                        new double[]{15.0D, 15.0D},
                        new double[][]{{10.0D, 0.0D}, {5.0D, 15.0D}},
                        new double[]{4.0D, 4.0D},
                        0.0D);
        GeometryMigrationLedger.MigrationResult aggregated =
                GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                        new double[]{20.0D, -30.0D},
                        new double[]{10.0D, 20.0D},
                        new double[]{15.0D, 15.0D},
                        new double[]{10.0D, 20.0D},
                        new double[]{15.0D, 15.0D},
                        new double[]{12.5D, -22.5D},
                        new double[]{4.0D, 4.0D},
                        0.0D);

        assertArrayEquals(dense.newEnthalpiesJ(), aggregated.newEnthalpiesJ(), EPSILON);
        assertEquals(dense.geometryIngressJ(), aggregated.geometryIngressJ(), EPSILON);
        assertEquals(dense.geometryEgressJ(), aggregated.geometryEgressJ(), EPSILON);
        assertEquals(dense.residualJ(), aggregated.residualJ(), EPSILON);
    }

}
