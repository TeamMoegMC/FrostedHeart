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

/** Main-thread, page-local coalescer that emits at most one delta per Brick per tick. */
public final class GeometryDeltaCoalescer {
    private static final long NO_PENDING_TICK = Long.MIN_VALUE;

    private final long[] latestBrickRevisions = new long[GeometrySummaryCache.BASE_SUMMARY_COUNT];
    private long pendingTick = NO_PENDING_TICK;
    private long pendingBrickMask;

    public RecordResult record(
            long sectionKey,
            long lifecycleGeneration,
            int baseBrickIndex,
            long geometryRevision,
            long effectiveTick,
            GeometryDeltaRing ring
    ) {
        requireBaseBrickIndex(baseBrickIndex);
        if (geometryRevision <= 0L) {
            throw new IllegalArgumentException("geometryRevision must be positive");
        }
        if (effectiveTick < 0L) {
            throw new IllegalArgumentException("effectiveTick must be non-negative");
        }
        if (ring == null) {
            throw new IllegalArgumentException("ring is required");
        }

        SealResult automaticSeal = SealResult.empty();
        boolean outOfOrder = false;
        if (pendingTick != NO_PENDING_TICK && pendingTick != effectiveTick) {
            outOfOrder = effectiveTick < pendingTick;
            automaticSeal = sealTo(sectionKey, lifecycleGeneration, ring);
        }
        if (pendingTick == NO_PENDING_TICK) {
            pendingTick = effectiveTick;
        }

        long brickBit = 1L << baseBrickIndex;
        boolean firstChangeForBrick = (pendingBrickMask & brickBit) == 0L;
        pendingBrickMask |= brickBit;
        latestBrickRevisions[baseBrickIndex] = geometryRevision;
        return new RecordResult(firstChangeForBrick, automaticSeal, outOfOrder);
    }

    public SealResult sealTo(
            long sectionKey,
            long lifecycleGeneration,
            GeometryDeltaRing ring
    ) {
        if (ring == null) {
            throw new IllegalArgumentException("ring is required");
        }
        int deltaCount = Long.bitCount(pendingBrickMask);
        if (deltaCount == 0) {
            pendingTick = NO_PENDING_TICK;
            return SealResult.empty();
        }
        if (ring.remainingCapacity() < deltaCount) {
            reset();
            return new SealResult(0, deltaCount, true);
        }

        int offered = 0;
        long remaining = pendingBrickMask;
        while (remaining != 0L) {
            int brickIndex = Long.numberOfTrailingZeros(remaining);
            boolean accepted = ring.offer(
                    sectionKey,
                    lifecycleGeneration,
                    latestBrickRevisions[brickIndex],
                    pendingTick,
                    brickIndex
            );
            if (!accepted) {
                int dropped = Long.bitCount(remaining);
                reset();
                return new SealResult(offered, dropped, true);
            }
            offered++;
            remaining &= remaining - 1L;
        }
        reset();
        return new SealResult(offered, 0, false);
    }

    public int reset() {
        int discardedBricks = Long.bitCount(pendingBrickMask);
        pendingBrickMask = 0L;
        pendingTick = NO_PENDING_TICK;
        return discardedBricks;
    }

    public record RecordResult(
            boolean firstChangeForBrick,
            SealResult automaticSeal,
            boolean outOfOrder
    ) {
        public boolean overflowed() {
            return automaticSeal.overflowed();
        }
    }

    public record SealResult(int offeredDeltas, int droppedDeltas, boolean overflowed) {
        public SealResult {
            if (offeredDeltas < 0 || droppedDeltas < 0) {
                throw new IllegalArgumentException("delta counts must be non-negative");
            }
            if (!overflowed && droppedDeltas != 0) {
                throw new IllegalArgumentException("only overflow may drop geometry deltas");
            }
        }

        public static SealResult empty() {
            return new SealResult(0, 0, false);
        }
    }

    private static void requireBaseBrickIndex(int baseBrickIndex) {
        if (baseBrickIndex < 0 || baseBrickIndex >= GeometrySummaryCache.BASE_SUMMARY_COUNT) {
            throw new IllegalArgumentException("baseBrickIndex must be within [0, 63]");
        }
    }
}
