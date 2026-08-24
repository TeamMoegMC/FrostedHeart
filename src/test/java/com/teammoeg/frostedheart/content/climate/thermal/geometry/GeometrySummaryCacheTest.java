/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometrySummaryCacheTest {
    @Test
    void fixedLayoutContainsSixtyFourBricksEightOctantsAndOneSection() {
        assertEquals(73, GeometrySummaryCache.SUMMARY_COUNT);
        assertEquals(0, GeometrySummaryCache.baseIndex(0, 0, 0));
        assertEquals(1, GeometrySummaryCache.baseIndex(4, 0, 0));
        assertEquals(4, GeometrySummaryCache.baseIndex(0, 0, 4));
        assertEquals(16, GeometrySummaryCache.baseIndex(0, 4, 0));
        assertEquals(63, GeometrySummaryCache.baseIndex(15, 15, 15));
        assertEquals(0, GeometrySummaryCache.octantIndexForBase(0));
        assertEquals(7, GeometrySummaryCache.octantIndexForBase(63));
        assertEquals(63, GeometrySummaryCache.brickVoxelIndex(3, 3, 3));
    }

    @Test
    void cheapAllAirProofAndTargetedInvalidationMaintainCachedAncestors() {
        GeometrySummaryCache cache = new GeometrySummaryCache();
        cache.installAllAirProof(7);

        for (int index = 0; index < GeometrySummaryCache.SUMMARY_COUNT; index++) {
            assertEquals(GeometrySummary.singleAir(7), cache.summary(index));
        }

        cache.invalidateBaseBrick(63);

        assertEquals(GeometrySummary.Kind.UNKNOWN, cache.baseSummary(63).kind());
        assertEquals(GeometrySummary.Kind.UNKNOWN, cache.octantSummary(7).kind());
        assertEquals(GeometrySummary.Kind.UNKNOWN, cache.sectionSummary().kind());
        assertEquals(GeometrySummary.singleAir(7), cache.baseSummary(62));
        assertEquals(GeometrySummary.singleAir(7), cache.octantSummary(0));

        cache.setBaseSummary(63, GeometrySummary.singleAir(7));
        assertEquals(GeometrySummary.singleAir(7), cache.octantSummary(7));
        assertEquals(GeometrySummary.singleAir(7), cache.sectionSummary());
    }

    @Test
    void incompatibleChildPreventsCoarseHomogeneousProof() {
        GeometrySummaryCache cache = new GeometrySummaryCache();
        cache.installAllAirProof(1);

        cache.setBaseSummary(0, GeometrySummary.singleMedium(2));

        assertEquals(GeometrySummary.Kind.MIXED, cache.octantSummary(0).kind());
        assertEquals(GeometrySummary.Kind.MIXED, cache.sectionSummary().kind());
    }

    @Test
    void summaryContractRejectsAmbiguousMediumAndUnknownFlags() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeometrySummary(GeometrySummary.Kind.UNKNOWN, 1, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new GeometrySummary(GeometrySummary.Kind.SINGLE_AIR, 1, 0));
        assertThrows(IllegalArgumentException.class, () ->
                GeometrySummary.mixed(1 << 20));
    }
}
