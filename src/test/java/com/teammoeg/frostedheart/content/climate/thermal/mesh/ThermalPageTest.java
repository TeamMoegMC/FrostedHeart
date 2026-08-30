/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalPageTest {
    @Test
    void mutationInvalidatesTheOldPublicationUntilMatchingRevisionCommits() {
        ThermalPageHandle page = new ThermalPageHandle(11L, 3L);
        PageSignatures signatures =
                ThermalTestFixtures.filledPageSignatures(0);
        page.publish(publication(0L, 1L, signatures, 7, 4));
        assertEquals(7, page.currentPublication()
                .brickAt(0, 0, 0).coverageSlot());

        assertEquals(1L, page.beginGeometryMutation());
        assertNull(page.currentPublication());

        PagePublication current = publication(
                1L, 2L, signatures, 9, 5);
        page.publish(current);
        assertSame(current, page.currentPublication());
    }

    @Test
    void fullResyncAckRequiresExactLifecycleRevisionAndReason() {
        ThermalPageHandle page = new ThermalPageHandle(13L, 4L);
        page.requireFullGeometryResync(
                ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
        ThermalPageHandle.GeometryResyncToken stale =
                page.pendingFullGeometryResync();
        page.requireFullGeometryResync(
                ThermalPageHandle.GeometryResyncReason.EXPLICIT_INVALIDATION);

        assertFalse(page.acknowledgeFullGeometryResync(stale));
        ThermalPageHandle.GeometryResyncToken current =
                page.pendingFullGeometryResync();
        assertTrue(page.acknowledgeFullGeometryResync(current));
        assertNull(page.pendingFullGeometryResync());
    }

    @Test
    void PageSignaturesReplaceOnlyNamedBrickPayloads() {
        PageSignatures original =
                ThermalTestFixtures.filledPageSignatures(1);
        int[] changed = new int[PageSignatures.ENTRIES_PER_BRICK];
        Arrays.fill(changed, 70_000);

        PageSignatures replacement = original.withBricks(
                new int[]{5}, new int[][]{changed});

        assertSame(original.brickPayload(4), replacement.brickPayload(4));
        assertEquals(70_000, replacement.get((1 << 2) | (1 << 6)));
        assertEquals(1, original.get((1 << 2) | (1 << 6)));
    }

    @Test
    void publicationResolvesAirPointAndPhaseCandidateWithoutSearch() {
        ThermalSignatureRegistry.Builder registry =
                ThermalSignatureRegistry.builder();
        int signatureId = registry.intern(
                ThermalTestFixtures.fullAirSignature());
        PageSignatures signatures =
                ThermalTestFixtures.filledPageSignatures(signatureId);
        PagePublication.Brick[] bricks = emptyBricks();
        bricks[0] = new PagePublication.Brick(
                12,
                8,
                signatures.brickPayload(0),
                null,
                PagePublication.PhaseCandidates.owned(
                        new int[]{6}, new long[]{1L << 21}));
        PagePublication publication = PagePublication.owned(0L, 1L, bricks);

        assertEquals(12, publication.resolveAirPoint(
                1, 1, 1, 63, registry.build()));
        assertTrue(publication.hasPhaseCandidate(1, 1, 1, 6));
        assertFalse(publication.hasPhaseCandidate(1, 1, 1, 7));
    }

    private static PagePublication publication(
            long geometryRevision,
            long topologyGeneration,
            PageSignatures signatures,
            int coverage,
            int generation
    ) {
        PagePublication.Brick[] bricks = emptyBricks();
        bricks[0] = new PagePublication.Brick(
                coverage,
                generation,
                signatures.brickPayload(0),
                null,
                PagePublication.PhaseCandidates.EMPTY);
        return PagePublication.owned(
                geometryRevision, topologyGeneration, bricks);
    }

    private static PagePublication.Brick[] emptyBricks() {
        PagePublication.Brick[] bricks =
                new PagePublication.Brick[ThermalPageHandle.BASE_BRICK_COUNT];
        Arrays.fill(bricks, PagePublication.Brick.EMPTY);
        return bricks;
    }
}
