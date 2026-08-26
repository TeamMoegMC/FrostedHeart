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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolverBlockViewTest {
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);
    private static final DependencyOffsetMask.Offset WEST =
            new DependencyOffsetMask.Offset(-1, 0, 0);
    private static final DependencyOffsetMask.Offset NORTH =
            new DependencyOffsetMask.Offset(0, 0, -1);

    @Test
    void snapshotCopiesDeclaredCellsAndReturnsExplicitSentinels() {
        DependencyOffsetMask mask = DependencyOffsetMask.explicit(EAST, WEST);
        Map<DependencyOffsetMask.Offset, ResolverBlockView.SnapshotCell<String, String>> union =
                new HashMap<>();
        union.put(DependencyOffsetMask.SELF,
                ResolverBlockView.SnapshotCell.present("stone", "empty"));
        union.put(EAST, ResolverBlockView.SnapshotCell.unloaded());
        union.put(NORTH, ResolverBlockView.SnapshotCell.present("extra", "extra-fluid"));

        ResolverBlockView<String, String> snapshot = ResolverBlockView.snapshot(mask, union);
        union.put(DependencyOffsetMask.SELF,
                ResolverBlockView.SnapshotCell.present("mutated", "mutated-fluid"));

        ResolverBlockView.Access<String, String> access = snapshot.openAccess();
        assertEquals("stone", access.lookup(0, 0, 0).value().orElseThrow().blockState());
        assertEquals(ResolverBlockView.LookupStatus.UNLOADED, access.lookup(EAST).status());
        assertEquals(ResolverBlockView.LookupStatus.MISSING, access.lookup(WEST).status());
        assertEquals(ResolverBlockView.LookupStatus.OUTSIDE_DECLARED_MASK,
                access.lookup(NORTH).status());
        assertEquals(ResolverBlockView.LookupStatus.OUTSIDE_DECLARED_MASK,
                access.lookup(2, 0, 0).status());
        access.lookup(EAST);
    }

    @Test
    void accessAuditNormalizesIgnoredUnavailableAndForbiddenReads() {
        Map<DependencyOffsetMask.Offset, ResolverBlockView.SnapshotCell<String, String>> cells =
                new HashMap<>();
        cells.put(DependencyOffsetMask.SELF,
                ResolverBlockView.SnapshotCell.present("door", "empty"));
        cells.put(EAST, ResolverBlockView.SnapshotCell.unloaded());
        ResolverBlockView<String, String> snapshot = ResolverBlockView.snapshot(
                DependencyOffsetMask.explicit(EAST), cells);

        ResolverBlockView.Access<String, String> unavailableAccess = snapshot.openAccess();
        unavailableAccess.lookup(EAST);
        ThermalResolution<String> ignoredUnavailable =
                unavailableAccess.normalize(ThermalResolution.resolved("incorrect-opening"));
        assertEquals(ThermalResolution.Status.UNRESOLVED, ignoredUnavailable.status());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_UNLOADED,
                ignoredUnavailable.reason());

        ResolverBlockView.Access<String, String> forbiddenAccess = snapshot.openAccess();
        forbiddenAccess.lookup(EAST);
        forbiddenAccess.blockEntity(0, 0, 0);
        ThermalResolution<String> ignoredBlockEntity =
                forbiddenAccess.normalize(ThermalResolution.resolved("incorrect-opening"));
        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED,
                ignoredBlockEntity.status());
        assertEquals(ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT,
                ignoredBlockEntity.reason());
    }

    @Test
    void resolverEntryPointEnforcesMaskAuditAndRegionBound() {
        ResolverBlockView<String, String> self = ResolverBlockView.snapshot(
                DependencyOffsetMask.SELF_ONLY,
                Map.of(DependencyOffsetMask.SELF,
                        ResolverBlockView.SnapshotCell.present("air", "empty"))
        );
        ThermalSignatureResolver<String, String> ignoredOutsideRead = resolver(
                DependencyOffsetMask.SELF_ONLY,
                1,
                access -> {
                    access.lookup(1, 0, 0);
                    return ThermalResolution.resolved(signature(List.of(region(0, 1L))));
                }
        );

        ThermalResolution<ResolvedThermalSignature> audited =
                ignoredOutsideRead.resolveSnapshot(self);
        assertEquals(ThermalResolution.Status.UNRESOLVED, audited.status());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_OUTSIDE_DECLARED_MASK,
                audited.reason());

        ThermalSignatureResolver<String, String> regionOverflow = resolver(
                DependencyOffsetMask.SELF_ONLY,
                1,
                access -> ThermalResolution.resolved(signature(List.of(
                        region(0, 1L),
                        region(1, 2L))))
        );
        ThermalResolution<ResolvedThermalSignature> overflow =
                regionOverflow.resolveSnapshot(self);
        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED, overflow.status());
        assertEquals(ThermalResolution.Reason.REGION_LIMIT_EXCEEDED, overflow.reason());
    }

    @Test
    void completeSnapshotNeverNeedsAWorldOrChunkCallback() {
        Map<DependencyOffsetMask.Offset, ResolverBlockView.SnapshotCell<String, String>> cells =
                new HashMap<>();
        for (DependencyOffsetMask.Offset offset : DependencyOffsetMask.NEIGHBOR_6.offsets()) {
            cells.put(offset, ResolverBlockView.SnapshotCell.present("state-" + offset, "empty"));
        }
        ResolverBlockView<String, String> snapshot = ResolverBlockView.snapshot(
                DependencyOffsetMask.NEIGHBOR_6, cells);

        ResolverBlockView.Access<String, String> access = snapshot.openAccess();
        for (DependencyOffsetMask.Offset offset : DependencyOffsetMask.NEIGHBOR_6.offsets()) {
            assertEquals(ResolverBlockView.LookupStatus.PRESENT, access.lookup(offset).status());
        }
        assertTrue(access.normalize(ThermalResolution.resolved("complete")).isResolved());
    }

    private static ThermalSignatureResolver<String, String> resolver(
            DependencyOffsetMask mask,
            int maxOutputRegions,
            java.util.function.Function<ResolverBlockView.Access<String, String>,
                    ThermalResolution<ResolvedThermalSignature>> function
    ) {
        return new ThermalSignatureResolver<>() {
            @Override
            public String resolverId() {
                return "test:resolver";
            }

            @Override
            public DependencyOffsetMask dependencyMask() {
                return mask;
            }

            @Override
            public int maxOutputRegions() {
                return maxOutputRegions;
            }

            @Override
            public ThermalResolution<ResolvedThermalSignature> resolve(
                    ResolverBlockView.Access<String, String> view
            ) {
                return function.apply(view);
            }
        };
    }

    private static ResolvedThermalSignature signature(List<LocalAirRegionPattern> regions) {
        return new ResolvedThermalSignature(0, 0, regions, 0, 0, 0, 0, 0);
    }

    private static LocalAirRegionPattern region(int id, long microcellMask) {
        return new LocalAirRegionPattern(id, microcellMask, 0, 0, 0, 0, 0, 0);
    }
}
