/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThermalSignatureTableTest {
    @Test
    void exactSignaturesReceiveDenseStableIdsWithoutRuntimeReverseLookup() {
        ResolvedThermalSignature air = ThermalTestFixtures.fullAirSignature();
        ResolvedThermalSignature solid = ThermalTestFixtures.solidSignature();
        ThermalSignatureTable.Builder builder = ThermalSignatureTable.builder();

        assertEquals(0, builder.intern(air));
        assertEquals(0, builder.intern(air));
        assertEquals(1, builder.intern(solid));
        ThermalSignatureTable table = builder.build();

        assertEquals(air.airGeometry(), table.geometry(0));
        assertEquals(solid.airGeometry(), table.geometry(1));
        assertEquals(0, table.materialProfileId(0));
        assertEquals(0, table.materialContactPatternId(0));
        assertNull(table.geometry(2));
        assertSame(table.uniformPayload(0), table.uniformPayload(0));
        assertNotNull(table.uniformPayload(ThermalSignatureTable.UNRESOLVED));
    }

    @Test
    void componentOrdinalsAreSharedByGeometryAcrossMaterialVariants() {
        ResolvedThermalSignature air = ThermalTestFixtures.fullAirSignature();
        ThermalSignatureTable.Builder builder = ThermalSignatureTable.builder();
        int neutral = builder.intern(air);
        int material = builder.intern(new ResolvedThermalSignature(
                air.airGeometry(), 3, 7));
        int solid = builder.intern(ThermalTestFixtures.solidSignature());
        ThermalSignatureTable table = builder.build();

        assertEquals(0, table.componentOrdinal(neutral, 0));
        assertEquals(0, table.componentOrdinal(material, 63));
        assertEquals(7, table.materialContactPatternId(material));
        assertEquals(0xff, table.componentOrdinal(solid, 0));
        assertEquals(0xff, table.componentOrdinal(-1, 0));
    }
}
