/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSignatureRegistryTest {
    @Test
    void registryDeduplicatesAndFrozenSnapshotDoesNotFollowBuilder() {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();
        ResolvedThermalSignature first = signature(4, 7, 0);
        ResolvedThermalSignature radiationVariant = signature(4, 8, 0);

        assertEquals(0, builder.intern(first));
        assertEquals(0, builder.intern(first));
        assertEquals(1, builder.intern(radiationVariant));

        ThermalSignatureRegistry frozen = builder.build();
        builder.intern(signature(5, 8, 0));

        assertEquals(2, frozen.signatureCount());
        assertEquals(first, frozen.signature(0).orElseThrow());
        assertEquals(1, frozen.idOf(radiationVariant).orElseThrow());
        assertTrue(frozen.signature(2).isEmpty());
        assertTrue(frozen.signature(-1).isEmpty());
    }

    @Test
    void resolutionStatesAndReasonsRemainIndependentOfRegistryIds() {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();

        ThermalSignatureResolution resolved = ThermalSignatureResolution.resolved(
                builder.intern(signature(1, 2, 0)));
        ThermalSignatureResolution unloaded = ThermalSignatureResolution.failure(
                ThermalResolution.unresolved(ThermalResolution.Reason.DEPENDENCY_UNLOADED));
        ThermalSignatureResolution unsupported = ThermalSignatureResolution.failure(
                ThermalResolution.unsupported(ThermalResolution.Reason.NOT_REGISTERED));

        assertTrue(resolved.isResolved());
        assertEquals(0, resolved.signatureId());
        assertEquals(ThermalResolution.Status.UNRESOLVED, unloaded.status());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_UNLOADED, unloaded.reason());
        assertEquals(ThermalSignatureResolution.NO_SIGNATURE_ID, unloaded.signatureId());
        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED, unsupported.status());
        assertEquals(ThermalResolution.Reason.NOT_REGISTERED, unsupported.reason());
        assertEquals(1, builder.signatureCount());
    }

    @Test
    void correctnessSignatureIdsAreNotNarrowedToUnsignedShort() {
        ThermalSignatureRegistry.Builder builder = ThermalSignatureRegistry.builder();
        int lastId = -1;
        for (int unique = 0; unique <= 65_536; unique++) {
            lastId = builder.intern(signature(1, 2, unique));
        }

        assertEquals(65_536, lastId);
        assertEquals(65_537, builder.signatureCount());
        assertEquals(65_536, builder.build().idOf(signature(1, 2, 65_536)).orElseThrow());
    }

    @Test
    void signatureCopiesRegionsAndKeepsChannelsIndependent() {
        List<LocalAirRegionPattern> regions = new ArrayList<>();
        regions.add(region(0, 1L));
        ResolvedThermalSignature signature = new ResolvedThermalSignature(
                1, 2, regions, 3, 4, 5, 6, 7);
        regions.clear();

        assertEquals(1, signature.localAirRegionCount());
        assertEquals(3, signature.materialContactPatternId());
        assertEquals(4, signature.radiationOcclusionPatternId());
        assertFalse(signature.airRegions().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> signature.airRegions().add(region(1, 2L)));
        assertThrows(IllegalArgumentException.class, () -> new ResolvedThermalSignature(
                1, 2, List.of(region(1, 1L)), 3, 4, 5, 6, 7));
        assertThrows(IllegalArgumentException.class, () -> new ResolvedThermalSignature(
                1, 2, List.of(region(0, 1L), region(1, 1L)), 3, 4, 5, 6, 7));
    }

    private static ResolvedThermalSignature signature(
            int materialContactPatternId,
            int radiationOcclusionPatternId,
            int flags
    ) {
        return new ResolvedThermalSignature(
                1,
                2,
                List.of(region(0, -1L)),
                materialContactPatternId,
                radiationOcclusionPatternId,
                3,
                0,
                flags
        );
    }

    private static LocalAirRegionPattern region(int id, long microcellMask) {
        return new LocalAirRegionPattern(id, microcellMask, 0xffff, 0xffff,
                0xffff, 0xffff, 0xffff, 0xffff);
    }
}
