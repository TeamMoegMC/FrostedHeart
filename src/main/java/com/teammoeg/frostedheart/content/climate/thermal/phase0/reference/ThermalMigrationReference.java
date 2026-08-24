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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.GeometryMigrationLedger;

/** Phase 0 verification facade for the production-owned migration formulas. */
public final class ThermalMigrationReference {
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

    public static double[] splitPureLod(
            double parentEnthalpyJ,
            double parentCapacityJPerK,
            double[] childCapacitiesJPerK
    ) {
        return GeometryMigrationLedger.splitPureLod(
                parentEnthalpyJ, parentCapacityJPerK, childCapacitiesJPerK);
    }

    public static double mergePureLod(double[] childEnthalpiesJ) {
        return GeometryMigrationLedger.mergePureLod(childEnthalpiesJ);
    }

    public static GeometryMigration migrateGeometry(
            double[] oldEnthalpiesJ,
            double[] oldCapacitiesJPerK,
            double[] newCapacitiesJPerK,
            double[][] overlapCapacitiesJPerK,
            double[] newAirInitialTemperaturesC,
            double referenceTemperatureC
    ) {
        GeometryMigrationLedger.MigrationResult result =
                GeometryMigrationLedger.calculateGeometryMigration(
                        oldEnthalpiesJ,
                        oldCapacitiesJPerK,
                        newCapacitiesJPerK,
                        overlapCapacitiesJPerK,
                        newAirInitialTemperaturesC,
                        referenceTemperatureC);
        return new GeometryMigration(
                result.newEnthalpiesJ(),
                result.geometryIngressJ(),
                result.geometryEgressJ(),
                result.residualJ());
    }
}
