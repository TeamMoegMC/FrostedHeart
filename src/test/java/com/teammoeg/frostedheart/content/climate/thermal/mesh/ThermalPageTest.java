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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDelta;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaCoalescer;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummaryCache;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalPageTest {
    @Test
    void stoneMutationImmediatelyInvalidatesAllAirCoarseSupport() {
        ThermalPage page = ThermalPage.allAir(17L, 3L, 100, 0);
        GeometryDeltaRing ring = new GeometryDeltaRing(4);
        assertTrue(page.tryPublishGeometry(0L, 1L, 0L));

        ThermalPage.MutationObservation mutation =
                page.recordGeometryMutation(1, 2, 3, 20L, ring);

        assertEquals(1L, mutation.geometryRevision());
        assertEquals(0, mutation.baseBrickIndex());
        assertTrue(mutation.coarseSupportInvalidated());
        assertTrue(mutation.materializedBrick());
        assertFalse(mutation.coalescedWithExistingBrickDelta());
        assertEquals(ThermalPage.NO_COVERAGE, page.coverageRefAtBase(0));
        assertEquals(0, page.coverageWidthAtBase(0));
        assertEquals(1L, page.mixedBrickMask());
        assertEquals(1L, page.dirtyBrickMask());
        assertTrue(page.coverageRepartitionRequired());
        assertEquals(GeometrySummary.Kind.UNKNOWN,
                page.geometrySummary(GeometrySummaryCache.SECTION_SUMMARY_INDEX).kind());
        assertFalse(page.publishedGeometryIsCurrent());

        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);
        assertEquals(1, sealed.offeredDeltas());
        assertFalse(sealed.overflowed());
        GeometryDelta delta = ring.poll();
        assertNotNull(delta);
        assertEquals(0, delta.baseBrickIndex());
        assertEquals(1L << GeometrySummaryCache.brickVoxelIndex(1, 2, 3),
                delta.changedVoxelMask());
        assertNull(ring.poll());
    }

    @Test
    void twentySameTickMutationsInOneBrickProduceOneRebuildDelta() {
        ThermalPage page = ThermalPage.allAir(18L, 4L, 200, 0);
        GeometryDeltaRing ring = new GeometryDeltaRing(2);

        for (int voxel = 0; voxel < 20; voxel++) {
            int localX = voxel & 3;
            int localZ = (voxel >>> 2) & 3;
            int localY = (voxel >>> 4) & 3;
            ThermalPage.MutationObservation mutation =
                    page.recordGeometryMutation(localX, localY, localZ, 30L, ring);
            assertEquals(voxel != 0, mutation.coalescedWithExistingBrickDelta());
        }

        assertEquals(20L, page.liveGeometryRevision());
        assertEquals(1, page.dirtyBrickCount());
        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);
        assertEquals(1, sealed.offeredDeltas());
        assertFalse(sealed.overflowed());

        GeometryDelta delta = ring.poll();
        assertNotNull(delta);
        assertEquals(20L, delta.geometryRevision());
        assertEquals(20, Long.bitCount(delta.changedVoxelMask()));
        assertNull(ring.poll());
    }

    @Test
    void ringOverflowCreatesStickyPageResyncAndRejectsStaleAcknowledgement() {
        ThermalPage page = ThermalPage.allAir(19L, 5L, 300, 0);
        GeometryDeltaRing ring = new GeometryDeltaRing(1);
        page.recordGeometryMutation(0, 0, 0, 40L, ring);
        page.recordGeometryMutation(4, 0, 0, 40L, ring);

        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);

        assertTrue(sealed.overflowed());
        assertEquals(2, sealed.droppedDeltas());
        assertEquals(0, ring.size());
        assertEquals(2L, page.liveGeometryRevision());
        assertTrue(page.fullGeometryResyncRequired());
        assertEquals(ThermalPage.FULL_GEOMETRY_RESYNC_REQUIRED,
                page.flags() & ThermalPage.FULL_GEOMETRY_RESYNC_REQUIRED);
        ThermalPage.GeometryResyncToken staleToken = page.beginFullGeometryResync();
        assertNotNull(staleToken);
        assertEquals(ThermalPage.GeometryResyncReason.RING_OVERFLOW, staleToken.reason());

        page.requireFullGeometryResync(ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
        ThermalPage.FullGeometryState rebuilt = ThermalPage.FullGeometryState.uniformAllAir(
                301, 0, new ArenaSpan(301, 1));
        assertFalse(page.tryInstallFullGeometryResync(staleToken, rebuilt));
        assertTrue(page.fullGeometryResyncRequired());

        ThermalPage.GeometryResyncToken currentToken = page.beginFullGeometryResync();
        assertNotNull(currentToken);
        assertEquals(3L, currentToken.requiredRevision());
        assertEquals(ThermalPage.GeometryResyncReason.RING_OVERFLOW, currentToken.reason());
        assertTrue(page.tryInstallFullGeometryResync(currentToken, rebuilt));
        assertFalse(page.fullGeometryResyncRequired());
        assertEquals(0L, page.dirtyBrickMask());
        assertFalse(page.coverageRepartitionRequired());
        assertTrue(page.tryPublishGeometry(
                page.liveGeometryRevision(), page.topologyGeneration(), 0L));
        assertTrue(page.publishedGeometryIsCurrent());
    }

    @Test
    void coarseMutationRequiresCompleteCoverageRepartitionBeforePublication() {
        ThermalPage page = ThermalPage.allAir(20L, 6L, 400, 0);
        GeometryDeltaRing ring = new GeometryDeltaRing(2);
        ThermalPage.MutationObservation mutation =
                page.recordGeometryMutation(0, 0, 0, 50L, ring);
        page.sealGeometryDeltas(ring);

        assertFalse(page.acknowledgeBrickRebuild(
                0, mutation.geometryRevision(), 500,
                GeometrySummary.mixed(GeometrySummary.MATERIAL_INTERFACE)));
        assertFalse(page.tryPublishGeometry(
                page.liveGeometryRevision(), page.topologyGeneration(), 0L));

        ThermalPage.FullGeometryState rebuilt = fineStateWithOneMixedBrick(0, 500);
        assertTrue(page.tryInstallGeometryBuild(mutation.geometryRevision(), rebuilt));
        assertEquals(500, page.coverageRefAtBase(0));
        assertEquals(4, page.coverageWidthAtBase(0));
        assertEquals(1L, page.mixedBrickMask());
        assertEquals(0L, page.dirtyBrickMask());
        assertTrue(page.tryPublishGeometry(
                page.liveGeometryRevision(), page.topologyGeneration(), 0L));
    }

    @Test
    void pageUsesDenseUnboundedCellSpanInsteadOfFixedMasks() {
        ThermalPage page = ThermalPage.allAir(21L, 7L, 600, 0);

        page.setCellSpan(new ArenaSpan(10, 513));

        assertEquals(513, page.cellSpan().count());
        assertEquals(523, page.cellSpan().endSlotExclusive());
    }

    @Test
    void stableCoverageStateRejectsMisalignedCoarseGroups() {
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
        Arrays.fill(refs, 1);
        Arrays.fill(widths, (byte) 16);
        refs[63] = 2;
        GeometrySummary[] summaries = new GeometrySummary[GeometrySummaryCache.SUMMARY_COUNT];
        Arrays.fill(summaries, GeometrySummary.singleAir(0));

        assertThrows(IllegalArgumentException.class, () -> new ThermalPage.FullGeometryState(
                refs, widths, summaries, 0L, new ArenaSpan(1, 1)));
    }

    @Test
    void publishedCoverageQueryIsConstantShapeAndClearsOnMutation() {
        ThermalPage page = ThermalPage.allAir(22L, 8L, 700, 3);
        ThermalPage.MutableCoverageQuery query = new ThermalPage.MutableCoverageQuery();

        assertFalse(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertFalse(query.valid());
        assertEquals(ThermalPage.NO_COVERAGE, query.coverageRef());

        assertTrue(page.tryPublishGeometry(0L, page.topologyGeneration(), 9L));
        assertTrue(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertTrue(query.valid());
        assertEquals(63, query.baseBrickIndex());
        assertEquals(700, query.coverageRef());
        assertEquals(16, query.coverageWidth());
        assertEquals(0L, query.geometryRevision());
        assertEquals(1L, query.topologyGeneration());
        assertEquals(9L, query.solveEpoch());

        page.recordGeometryMutation(15, 15, 15, 60L, new GeometryDeltaRing(1));
        assertFalse(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertFalse(query.valid());
        assertEquals(ThermalPage.NO_COVERAGE, query.coverageRef());
        assertEquals(0, query.coverageWidth());
        assertEquals(1L, query.geometryRevision());
        assertThrows(IllegalArgumentException.class, () ->
                page.tryQueryPublishedCoverage(16, 0, 0, query));
    }

    private static ThermalPage.FullGeometryState fineStateWithOneMixedBrick(
            int mixedBrick,
            int firstSupportRef
    ) {
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
        GeometrySummaryCache summaries = new GeometrySummaryCache();
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            refs[baseIndex] = firstSupportRef + baseIndex;
            widths[baseIndex] = 4;
            summaries.setBaseSummary(baseIndex, baseIndex == mixedBrick
                    ? GeometrySummary.mixed(GeometrySummary.MATERIAL_INTERFACE)
                    : GeometrySummary.singleAir(0));
        }
        return new ThermalPage.FullGeometryState(
                refs,
                widths,
                summaries.snapshot(),
                1L << mixedBrick,
                new ArenaSpan(firstSupportRef, ThermalPage.BASE_BRICK_COUNT)
        );
    }
}
