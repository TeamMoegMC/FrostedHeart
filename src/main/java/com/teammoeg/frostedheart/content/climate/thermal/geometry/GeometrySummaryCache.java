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

import java.util.Arrays;

/** Fixed primitive cache for the 64 world-aligned 4-cubed Brick summaries. */
public final class GeometrySummaryCache {
    public static final int BASE_SUMMARY_COUNT = 64;

    private final byte[] kinds = new byte[BASE_SUMMARY_COUNT];
    private final int[] mediumIds = new int[BASE_SUMMARY_COUNT];
    private final int[] topologyFlags = new int[BASE_SUMMARY_COUNT];

    public GeometrySummaryCache() {
        Arrays.fill(mediumIds, GeometrySummary.NO_MEDIUM);
    }

    public GeometrySummary summary(int summaryIndex) {
        requireBaseIndex(summaryIndex);
        return new GeometrySummary(
                GeometrySummary.Kind.values()[kinds[summaryIndex]],
                mediumIds[summaryIndex],
                topologyFlags[summaryIndex]
        );
    }

    public void setBaseSummary(int baseIndex, GeometrySummary summary) {
        requireBaseIndex(baseIndex);
        set(baseIndex, summary);
    }

    public void replaceAll(GeometrySummary[] summaries) {
        if (summaries == null || summaries.length != BASE_SUMMARY_COUNT) {
            throw new IllegalArgumentException("summaries must contain exactly 64 entries");
        }
        for (int index = 0; index < summaries.length; index++) {
            set(index, summaries[index]);
        }
    }

    /** Invalidates only the changed Brick summary. */
    public void invalidateBaseBrick(int baseIndex) {
        requireBaseIndex(baseIndex);
        setUnknown(baseIndex);
    }

    public static int baseIndex(int localX, int localY, int localZ) {
        requireLocalCoordinate("localX", localX);
        requireLocalCoordinate("localY", localY);
        requireLocalCoordinate("localZ", localZ);
        int brickX = localX >>> 2;
        int brickY = localY >>> 2;
        int brickZ = localZ >>> 2;
        return brickX | (brickZ << 2) | (brickY << 4);
    }

    private void set(int index, GeometrySummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary is required");
        }
        setRaw(index, (byte) summary.kind().ordinal(), summary.mediumId(), summary.topologyFlags());
    }

    private void setUnknown(int index) {
        setRaw(index, (byte) GeometrySummary.Kind.UNKNOWN.ordinal(), GeometrySummary.NO_MEDIUM, 0);
    }

    private void setRaw(int index, byte kind, int mediumId, int flags) {
        kinds[index] = kind;
        mediumIds[index] = mediumId;
        topologyFlags[index] = flags;
    }

    private static void requireBaseIndex(int index) {
        if (index < 0 || index >= BASE_SUMMARY_COUNT) {
            throw new IllegalArgumentException("baseIndex must be within [0, 63]");
        }
    }

    private static void requireLocalCoordinate(String name, int value) {
        if (value < 0 || value >= 16) {
            throw new IllegalArgumentException(name + " must be within [0, 15]");
        }
    }
}
