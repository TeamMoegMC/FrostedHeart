/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThermalSignatureRegistryTest {
    @Test
    void registryDeduplicatesSignaturesIntoDenseStableIds() {
        ResolvedThermalSignature air =
                ThermalTestFixtures.fullAirSignature();
        ResolvedThermalSignature solid =
                ThermalTestFixtures.solidSignature();
        ThermalSignatureRegistry.Builder builder =
                ThermalSignatureRegistry.builder();

        assertEquals(0, builder.intern(air));
        assertEquals(0, builder.intern(air));
        assertEquals(1, builder.intern(solid));
        ThermalSignatureRegistry registry = builder.build();

        assertEquals(2, registry.signatureCount());
        assertSame(air, registry.signatureOrNull(0));
        assertSame(solid, registry.signatureOrNull(1));
        assertNull(registry.signatureOrNull(2));
        assertEquals(0, registry.idOrDefault(air, -1));
        assertEquals(-1, registry.idOrDefault(
                new ResolvedThermalSignature(
                        1, 0, java.util.List.of(), 0, 0, 0, 0, 0),
                -1));
    }

    @Test
    void componentOrdinalIsPrecomputedOnceForAllConsumers() {
        ThermalSignatureRegistry.Builder builder =
                ThermalSignatureRegistry.builder();
        int air = builder.intern(ThermalTestFixtures.fullAirSignature());
        int solid = builder.intern(ThermalTestFixtures.solidSignature());
        ThermalSignatureRegistry registry = builder.build();

        assertEquals(0, registry.componentOrdinal(air, 0));
        assertEquals(0, registry.componentOrdinal(air, 63));
        assertEquals(0xff, registry.componentOrdinal(solid, 0));
        assertEquals(0xff, registry.componentOrdinal(-1, 0));
    }
}
