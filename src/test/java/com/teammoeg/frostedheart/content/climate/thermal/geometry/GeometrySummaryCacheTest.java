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
    void fixedLayoutContainsSixtyFourBrickSummaries() {
        assertEquals(64, GeometrySummaryCache.BASE_SUMMARY_COUNT);
        assertEquals(0, GeometrySummaryCache.baseIndex(0, 0, 0));
        assertEquals(1, GeometrySummaryCache.baseIndex(4, 0, 0));
        assertEquals(4, GeometrySummaryCache.baseIndex(0, 0, 4));
        assertEquals(16, GeometrySummaryCache.baseIndex(0, 4, 0));
        assertEquals(63, GeometrySummaryCache.baseIndex(15, 15, 15));
    }

    @Test
    void targetedInvalidationChangesOnlyOneBrick() {
        GeometrySummaryCache cache = new GeometrySummaryCache();
        for (int index = 0; index < GeometrySummaryCache.BASE_SUMMARY_COUNT; index++) {
            cache.setBaseSummary(index, GeometrySummary.singleAir(7));
        }

        cache.invalidateBaseBrick(63);

        assertEquals(GeometrySummary.Kind.UNKNOWN, cache.summary(63).kind());
        assertEquals(GeometrySummary.singleAir(7), cache.summary(62));
        cache.setBaseSummary(63, GeometrySummary.singleAir(7));
        assertEquals(GeometrySummary.singleAir(7), cache.summary(63));
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
