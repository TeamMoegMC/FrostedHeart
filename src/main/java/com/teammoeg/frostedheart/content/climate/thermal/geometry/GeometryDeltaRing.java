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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-capacity primitive SPSC ring. Queue occupancy is transport state only;
 * page revision and page-owned resync state remain the geometry authority.
 */
public final class GeometryDeltaRing {
    private final int capacity;
    private final long[] sectionKeys;
    private final long[] lifecycleGenerations;
    private final long[] geometryRevisions;
    private final long[] effectiveTicks;
    private final int[] baseBrickIndices;
    private final long[] changedVoxelMasks;
    private final AtomicLong readSequence = new AtomicLong();
    private final AtomicLong writeSequence = new AtomicLong();

    public GeometryDeltaRing(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        sectionKeys = new long[capacity];
        lifecycleGenerations = new long[capacity];
        geometryRevisions = new long[capacity];
        effectiveTicks = new long[capacity];
        baseBrickIndices = new int[capacity];
        changedVoxelMasks = new long[capacity];
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        long size = writeSequence.get() - readSequence.get();
        return (int) Math.min(capacity, Math.max(0L, size));
    }

    public int remainingCapacity() {
        return capacity - size();
    }

    public boolean offer(GeometryDelta delta) {
        if (delta == null) {
            throw new IllegalArgumentException("delta is required");
        }
        return offer(
                delta.sectionKey(),
                delta.lifecycleGeneration(),
                delta.geometryRevision(),
                delta.effectiveTick(),
                delta.baseBrickIndex(),
                delta.changedVoxelMask()
        );
    }

    public boolean offer(
            long sectionKey,
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick,
            int baseBrickIndex,
            long changedVoxelMask
    ) {
        validate(lifecycleGeneration, geometryRevision, effectiveTick,
                baseBrickIndex, changedVoxelMask);
        long write = writeSequence.get();
        if (write - readSequence.get() >= capacity) {
            return false;
        }
        int slot = (int) (write % capacity);
        sectionKeys[slot] = sectionKey;
        lifecycleGenerations[slot] = lifecycleGeneration;
        geometryRevisions[slot] = geometryRevision;
        effectiveTicks[slot] = effectiveTick;
        baseBrickIndices[slot] = baseBrickIndex;
        changedVoxelMasks[slot] = changedVoxelMask;
        writeSequence.lazySet(write + 1L);
        return true;
    }

    /** Allocation-free hot-path poll into a caller-owned holder. */
    public boolean poll(MutableGeometryDelta target) {
        if (target == null) {
            throw new IllegalArgumentException("target is required");
        }
        long read = readSequence.get();
        if (read >= writeSequence.get()) {
            return false;
        }
        int slot = (int) (read % capacity);
        target.set(
                sectionKeys[slot],
                lifecycleGenerations[slot],
                geometryRevisions[slot],
                effectiveTicks[slot],
                baseBrickIndices[slot],
                changedVoxelMasks[slot]
        );
        readSequence.lazySet(read + 1L);
        return true;
    }

    /** Polls only entries belonging to the sealed tick cut. */
    public boolean pollThroughTick(long maximumEffectiveTick, MutableGeometryDelta target) {
        if (maximumEffectiveTick < 0L || target == null) {
            throw new IllegalArgumentException("sealed tick and target are required");
        }
        long read = readSequence.get();
        if (read >= writeSequence.get()) {
            return false;
        }
        int slot = (int) (read % capacity);
        if (effectiveTicks[slot] > maximumEffectiveTick) {
            return false;
        }
        return poll(target);
    }

    /** Cold/debug convenience wrapper. */
    public GeometryDelta poll() {
        MutableGeometryDelta target = new MutableGeometryDelta();
        return poll(target) ? target.snapshot() : null;
    }

    private static void validate(
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick,
            int baseBrickIndex,
            long changedVoxelMask
    ) {
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

    public static final class MutableGeometryDelta {
        private long sectionKey;
        private long lifecycleGeneration;
        private long geometryRevision;
        private long effectiveTick;
        private int baseBrickIndex;
        private long changedVoxelMask;

        public long sectionKey() {
            return sectionKey;
        }

        public long lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public long geometryRevision() {
            return geometryRevision;
        }

        public long effectiveTick() {
            return effectiveTick;
        }

        public int baseBrickIndex() {
            return baseBrickIndex;
        }

        public long changedVoxelMask() {
            return changedVoxelMask;
        }

        public GeometryDelta snapshot() {
            return new GeometryDelta(
                    sectionKey,
                    lifecycleGeneration,
                    geometryRevision,
                    effectiveTick,
                    baseBrickIndex,
                    changedVoxelMask
            );
        }

        private void set(
                long sectionKey,
                long lifecycleGeneration,
                long geometryRevision,
                long effectiveTick,
                int baseBrickIndex,
                long changedVoxelMask
        ) {
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            this.geometryRevision = geometryRevision;
            this.effectiveTick = effectiveTick;
            this.baseBrickIndex = baseBrickIndex;
            this.changedVoxelMask = changedVoxelMask;
        }
    }
}
