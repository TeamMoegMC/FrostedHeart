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
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometryMigrationLedgerTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void aggregatedMigrationPreservesOverlapAndSignedGeometryEnergy() {
        GeometryMigrationLedger.MigrationResult removed =
                GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                        new double[]{-200.0D},
                        new double[]{100.0D},
                        new double[]{40.0D},
                        new double[]{40.0D},
                        new double[]{40.0D},
                        new double[]{-80.0D},
                        new double[]{0.0D},
                        0.0D);
        assertArrayEquals(new double[]{-80.0D}, removed.newEnthalpiesJ(), EPSILON);
        assertEquals(-120.0D, removed.geometryEgressJ(), EPSILON);
        assertEquals(0.0D, removed.geometryIngressJ(), EPSILON);
        assertEquals(0.0D, removed.residualJ(), EPSILON);

        GeometryMigrationLedger.MigrationResult added =
                GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                        new double[]{-80.0D},
                        new double[]{40.0D},
                        new double[]{100.0D},
                        new double[]{40.0D},
                        new double[]{40.0D},
                        new double[]{-80.0D},
                        new double[]{-5.0D},
                        0.0D);
        assertArrayEquals(new double[]{-380.0D}, added.newEnthalpiesJ(), EPSILON);
        assertEquals(-300.0D, added.geometryIngressJ(), EPSILON);
        assertEquals(0.0D, added.residualJ(), EPSILON);
    }

    @Test
    void migrationResultDoesNotExposeItsMutableEnthalpyArray() {
        GeometryMigrationLedger.MigrationResult migration =
                GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                        new double[]{20.0D},
                        new double[]{10.0D},
                        new double[]{10.0D},
                        new double[]{10.0D},
                        new double[]{10.0D},
                        new double[]{20.0D},
                        new double[]{0.0D},
                        0.0D);

        double[] exposed = migration.newEnthalpiesJ();
        exposed[0] = 0.0D;
        assertArrayEquals(new double[]{20.0D}, migration.newEnthalpiesJ(), EPSILON);
    }

    @Test
    void aggregatedMigrationRejectsOverlapAboveRepresentedCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                        new double[]{20.0D},
                        new double[]{10.0D},
                        new double[]{10.0D},
                        new double[]{11.0D},
                        new double[]{10.0D},
                        new double[]{20.0D},
                        new double[]{0.0D},
                        0.0D));
    }
}
