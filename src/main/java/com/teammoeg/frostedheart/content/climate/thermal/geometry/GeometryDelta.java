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

/** One tick-coalesced base-Brick invalidation passed from the main thread to a worker. */
public record GeometryDelta(
        long sectionKey,
        long lifecycleGeneration,
        long geometryRevision,
        long effectiveTick,
        int baseBrickIndex,
        long changedVoxelMask
) {
    public GeometryDelta {
        if (lifecycleGeneration < 0L) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
        if (geometryRevision <= 0L) {
            throw new IllegalArgumentException("geometryRevision must be positive");
        }
        if (effectiveTick < 0L) {
            throw new IllegalArgumentException("effectiveTick must be non-negative");
        }
        if (baseBrickIndex < 0 || baseBrickIndex >= GeometrySummaryCache.BASE_SUMMARY_COUNT) {
            throw new IllegalArgumentException("baseBrickIndex must be within [0, 63]");
        }
        if (changedVoxelMask == 0L) {
            throw new IllegalArgumentException("changedVoxelMask must not be empty");
        }
    }
}
