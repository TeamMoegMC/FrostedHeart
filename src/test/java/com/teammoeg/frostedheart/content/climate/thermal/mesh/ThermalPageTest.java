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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaCoalescer;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalPageTest {
    @Test
    void mutationInvalidatesOnlyItsInstalledBrick() {
        ThermalPage page = installedPage(17L, 3L, 100);
        GeometryDeltaRing ring = new GeometryDeltaRing(4);
        assertTrue(page.tryPublishGeometry(0L, page.topologyGeneration(), 0L));

        ThermalPage.MutationObservation mutation =
                page.recordGeometryMutation(1, 2, 3, 20L, ring);

        assertEquals(1L, mutation.geometryRevision());
        assertEquals(0, mutation.baseBrickIndex());
        assertFalse(mutation.coalescedWithExistingBrickDelta());
        assertFalse(mutation.fullResyncRequired());
        assertEquals(ThermalPage.NO_COVERAGE, coverageAt(page, 0));
        assertEquals(101, coverageAt(page, 1));
        assertFalse(page.publishedGeometryIsCurrent());

        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);
        assertEquals(1, sealed.offeredDeltas());
        GeometryDeltaRing.MutableGeometryDelta delta =
                new GeometryDeltaRing.MutableGeometryDelta();
        assertTrue(ring.poll(delta));
        assertEquals(0, delta.baseBrickIndex());
        assertFalse(ring.poll(delta));
    }

    @Test
    void twentySameTickMutationsInOneBrickProduceOneRebuildDelta() {
        ThermalPage page = installedPage(18L, 4L, 200);
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
        assertEquals(ThermalPage.NO_COVERAGE, coverageAt(page, 0));
        assertEquals(201, coverageAt(page, 1));
        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);
        assertEquals(1, sealed.offeredDeltas());
        assertFalse(sealed.overflowed());
        GeometryDeltaRing.MutableGeometryDelta delta =
                new GeometryDeltaRing.MutableGeometryDelta();
        assertTrue(ring.poll(delta));
        assertEquals(20L, delta.geometryRevision());
        assertEquals(0, delta.baseBrickIndex());
    }

    @Test
    void ringOverflowCreatesStickyResyncAndRejectsStaleAcknowledgement() {
        ThermalPage page = installedPage(19L, 5L, 300);
        GeometryDeltaRing ring = new GeometryDeltaRing(1);
        page.recordGeometryMutation(0, 0, 0, 40L, ring);
        page.recordGeometryMutation(4, 0, 0, 40L, ring);

        GeometryDeltaCoalescer.SealResult sealed = page.sealGeometryDeltas(ring);

        assertTrue(sealed.overflowed());
        assertEquals(2, sealed.droppedDeltas());
        assertTrue(page.fullGeometryResyncRequired());
        ThermalPage.GeometryResyncToken staleToken = page.beginFullGeometryResync();
        assertNotNull(staleToken);

        page.requireFullGeometryResync(ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
        ThermalPage.FullGeometryState rebuilt = state(400, -1);
        assertFalse(page.tryInstallFullGeometryResync(staleToken, rebuilt));
        ThermalPage.GeometryResyncToken currentToken = page.beginFullGeometryResync();
        assertNotNull(currentToken);
        assertTrue(page.tryInstallFullGeometryResync(currentToken, rebuilt));
        assertFalse(page.fullGeometryResyncRequired());
        assertEquals(400, coverageAt(page, 0));
    }

    @Test
    void rebuiltBrickRestoresCoverageAndMixedState() {
        ThermalPage page = installedPage(20L, 6L, 400);
        GeometryDeltaRing ring = new GeometryDeltaRing(2);
        ThermalPage.MutationObservation mutation =
                page.recordGeometryMutation(0, 0, 0, 50L, ring);
        page.sealGeometryDeltas(ring);

        int[] coverage = page.coverageSnapshot();
        GeometrySummary[] summaries = new GeometrySummary[ThermalPage.BASE_BRICK_COUNT];
        coverage[0] = 500;
        summaries[0] = GeometrySummary.mixed(GeometrySummary.MATERIAL_INTERFACE);
        assertTrue(page.tryInstallBrickBuilds(
                mutation.geometryRevision(), 1L, coverage, summaries));
        assertEquals(500, coverageAt(page, 0));
        assertEquals(1L, page.mixedBrickMask());
        assertTrue(page.tryPublishGeometry(
                page.liveGeometryRevision(), page.topologyGeneration(), 0L));
    }

    @Test
    void publishedCoverageQueryClearsOnMutation() {
        ThermalPage page = installedPage(22L, 8L, 700);
        ThermalPage.MutableCoverageQuery query = new ThermalPage.MutableCoverageQuery();

        assertFalse(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertTrue(page.tryPublishGeometry(0L, page.topologyGeneration(), 9L));
        assertTrue(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertEquals(63, query.baseBrickIndex());
        assertEquals(763, query.coverageRef());
        assertEquals(0L, query.geometryRevision());
        assertEquals(9L, query.solveEpoch());

        page.recordGeometryMutation(15, 15, 15, 60L, new GeometryDeltaRing(1));
        assertFalse(page.tryQueryPublishedCoverage(15, 15, 15, query));
        assertFalse(query.valid());
        assertEquals(ThermalPage.NO_COVERAGE, query.coverageRef());
        assertEquals(1L, query.geometryRevision());
        assertThrows(IllegalArgumentException.class, () ->
                page.tryQueryPublishedCoverage(16, 0, 0, query));
    }

    private static ThermalPage installedPage(long sectionKey, long generation, int firstRef) {
        ThermalPage page = new ThermalPage(sectionKey, generation);
        assertTrue(page.tryInstallGeometryBuild(0L, state(firstRef, -1)));
        return page;
    }

    private static int coverageAt(ThermalPage page, int baseIndex) {
        return page.coverageSnapshot()[baseIndex];
    }

    private static ThermalPage.FullGeometryState state(int firstRef, int mixedBrick) {
        int[] refs = new int[ThermalPage.BASE_BRICK_COUNT];
        GeometrySummary[] summaries = new GeometrySummary[ThermalPage.BASE_BRICK_COUNT];
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            refs[baseIndex] = firstRef + baseIndex;
            summaries[baseIndex] = baseIndex == mixedBrick
                    ? GeometrySummary.mixed(GeometrySummary.MATERIAL_INTERFACE)
                    : GeometrySummary.singleAir(0);
        }
        return new ThermalPage.FullGeometryState(
                refs,
                summaries,
                mixedBrick < 0 ? 0L : 1L << mixedBrick);
    }
}
