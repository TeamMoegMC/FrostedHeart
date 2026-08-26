/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed-envelope SPSC ring consumed by the geometry applier. */
public final class ResolvedGeometryInputRing {
    public static final int BLOCKS_PER_PAGE = 16 * 16 * 16;
    private static final Kind[] KINDS = Kind.values();
    private static final ThermalResolution.Status[] STATUSES =
            ThermalResolution.Status.values();
    private static final ThermalResolution.Reason[] REASONS =
            ThermalResolution.Reason.values();
    private static final ThermalPage.GeometryResyncReason[] RESYNC_REASONS =
            ThermalPage.GeometryResyncReason.values();

    public enum Kind {
        RESOLVED_CENTER,
        FULL_RESYNC_REQUIRED
    }

    private final int capacity;
    private final int fullSnapshotCapacity;
    private final byte[] kinds;
    private final long[] watermarks;
    private final long[] sectionKeys;
    private final long[] lifecycleGenerations;
    private final long[] geometryRevisions;
    private final long[] effectiveTicks;
    private final int[] blockIndices;
    private final byte[] statuses;
    private final byte[] reasons;
    private final int[] signatureIds;
    private final byte[] resyncReasons;
    private final int[][] fullPageSignatureIds;
    private final AtomicLong readSequence = new AtomicLong();
    private final AtomicLong writeSequence = new AtomicLong();
    private final AtomicInteger fullSnapshotCount = new AtomicInteger();
    private volatile long nextWatermark;

    public ResolvedGeometryInputRing(int capacity) {
        this(capacity, Math.min(capacity, 4));
    }

    public ResolvedGeometryInputRing(int capacity, int fullSnapshotCapacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (fullSnapshotCapacity <= 0 || fullSnapshotCapacity > capacity) {
            throw new IllegalArgumentException(
                    "fullSnapshotCapacity must be within [1, capacity]");
        }
        this.capacity = capacity;
        this.fullSnapshotCapacity = fullSnapshotCapacity;
        kinds = new byte[capacity];
        watermarks = new long[capacity];
        sectionKeys = new long[capacity];
        lifecycleGenerations = new long[capacity];
        geometryRevisions = new long[capacity];
        effectiveTicks = new long[capacity];
        blockIndices = new int[capacity];
        statuses = new byte[capacity];
        reasons = new byte[capacity];
        signatureIds = new int[capacity];
        resyncReasons = new byte[capacity];
        fullPageSignatureIds = new int[capacity][];
    }

    public long latestOfferedWatermark() {
        return nextWatermark;
    }

    public boolean canOfferFullResync() {
        return writeSequence.get() - readSequence.get() < capacity
                && fullSnapshotCount.get() < fullSnapshotCapacity;
    }

    public boolean offerResolvedCenter(
            long sectionKey,
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick,
            int blockIndex,
            ThermalSignatureResolution resolution
    ) {
        Objects.requireNonNull(resolution, "resolution");
        requireCommonFields(lifecycleGeneration, geometryRevision, effectiveTick);
        if (blockIndex < 0 || blockIndex >= 4096) {
            throw new IllegalArgumentException("blockIndex must be within [0, 4095]");
        }
        return offer(
                Kind.RESOLVED_CENTER,
                sectionKey,
                lifecycleGeneration,
                geometryRevision,
                effectiveTick,
                blockIndex,
                resolution.status(),
                resolution.reason(),
                resolution.signatureId(),
                -1,
                null);
    }

    public boolean offerFullResync(
            long sectionKey,
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick,
            ThermalPage.GeometryResyncReason reason,
            int[] pageSignatureIds
    ) {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(pageSignatureIds, "pageSignatureIds");
        if (pageSignatureIds.length != BLOCKS_PER_PAGE) {
            throw new IllegalArgumentException("full Page snapshot must contain 4096 IDs");
        }
        requireCommonFields(lifecycleGeneration, geometryRevision, effectiveTick);
        if (writeSequence.get() - readSequence.get() >= capacity
                || fullSnapshotCount.get() >= fullSnapshotCapacity) {
            return false;
        }
        return offer(
                Kind.FULL_RESYNC_REQUIRED,
                sectionKey,
                lifecycleGeneration,
                geometryRevision,
                effectiveTick,
                -1,
                ThermalResolution.Status.UNRESOLVED,
                ThermalResolution.Reason.SNAPSHOT_DATA_MISSING,
                ThermalSignatureResolution.NO_SIGNATURE_ID,
                reason.ordinal(),
                pageSignatureIds.clone());
    }

    public boolean poll(MutableInput target) {
        Objects.requireNonNull(target, "target");
        long read = readSequence.get();
        if (read >= writeSequence.get()) {
            return false;
        }
        int slot = (int) (read % capacity);
        target.set(
                KINDS[Byte.toUnsignedInt(kinds[slot])],
                watermarks[slot],
                sectionKeys[slot],
                lifecycleGenerations[slot],
                geometryRevisions[slot],
                effectiveTicks[slot],
                blockIndices[slot],
                STATUSES[Byte.toUnsignedInt(statuses[slot])],
                REASONS[Byte.toUnsignedInt(reasons[slot])],
                signatureIds[slot],
                resyncReasons[slot] < 0
                        ? null
                        : RESYNC_REASONS[
                                Byte.toUnsignedInt(resyncReasons[slot])],
                fullPageSignatureIds[slot]);
        if (fullPageSignatureIds[slot] != null) {
            fullSnapshotCount.decrementAndGet();
        }
        fullPageSignatureIds[slot] = null;
        readSequence.lazySet(read + 1L);
        return true;
    }

    /** Polls only entries covered by one sealed geometry watermark. */
    public boolean pollThroughWatermark(long maximumWatermark, MutableInput target) {
        if (maximumWatermark < 0L || target == null) {
            throw new IllegalArgumentException("sealed watermark and target are required");
        }
        long read = readSequence.get();
        if (read >= writeSequence.get()) {
            return false;
        }
        int slot = (int) (read % capacity);
        if (watermarks[slot] > maximumWatermark) {
            return false;
        }
        return poll(target);
    }

    private boolean offer(
            Kind kind,
            long sectionKey,
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick,
            int blockIndex,
            ThermalResolution.Status status,
            ThermalResolution.Reason reason,
            int signatureId,
            int resyncReason,
            int[] pageSignatureIds
    ) {
        long write = writeSequence.get();
        if (write - readSequence.get() >= capacity) {
            return false;
        }
        if (pageSignatureIds != null
                && fullSnapshotCount.get() >= fullSnapshotCapacity) {
            return false;
        }
        nextWatermark = Math.incrementExact(nextWatermark);
        long watermark = nextWatermark;
        int slot = (int) (write % capacity);
        kinds[slot] = (byte) kind.ordinal();
        watermarks[slot] = watermark;
        sectionKeys[slot] = sectionKey;
        lifecycleGenerations[slot] = lifecycleGeneration;
        geometryRevisions[slot] = geometryRevision;
        effectiveTicks[slot] = effectiveTick;
        blockIndices[slot] = blockIndex;
        statuses[slot] = (byte) status.ordinal();
        reasons[slot] = (byte) reason.ordinal();
        signatureIds[slot] = signatureId;
        resyncReasons[slot] = (byte) resyncReason;
        fullPageSignatureIds[slot] = pageSignatureIds;
        if (pageSignatureIds != null) {
            fullSnapshotCount.incrementAndGet();
        }
        writeSequence.lazySet(write + 1L);
        return true;
    }

    private static void requireCommonFields(
            long lifecycleGeneration,
            long geometryRevision,
            long effectiveTick
    ) {
        if (lifecycleGeneration < 0L
                || geometryRevision <= 0L
                || effectiveTick < 0L) {
            throw new IllegalArgumentException(
                    "generation/tick must be non-negative and revision positive");
        }
    }

    /** Caller-owned allocation-free poll result. */
    public static final class MutableInput {
        private Kind kind;
        private long watermark;
        private long sectionKey;
        private long lifecycleGeneration;
        private long geometryRevision;
        private long effectiveTick;
        private int blockIndex;
        private ThermalResolution.Status status;
        private ThermalResolution.Reason reason;
        private int signatureId;
        private ThermalPage.GeometryResyncReason geometryResyncReason;
        private int[] fullPageSignatureIds;

        public Kind kind() {
            return kind;
        }

        public long watermark() {
            return watermark;
        }

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

        public int blockIndex() {
            return blockIndex;
        }

        public ThermalResolution.Status status() {
            return status;
        }

        public ThermalResolution.Reason reason() {
            return reason;
        }

        public int signatureId() {
            return signatureId;
        }

        public ThermalPage.GeometryResyncReason geometryResyncReason() {
            return geometryResyncReason;
        }

        /** Ownership transfers to the consumer when the ring entry is polled. */
        public int[] fullPageSignatureIds() {
            return fullPageSignatureIds;
        }

        private void set(
                Kind kind,
                long watermark,
                long sectionKey,
                long lifecycleGeneration,
                long geometryRevision,
                long effectiveTick,
                int blockIndex,
                ThermalResolution.Status status,
                ThermalResolution.Reason reason,
                int signatureId,
                ThermalPage.GeometryResyncReason geometryResyncReason,
                int[] fullPageSignatureIds
        ) {
            this.kind = kind;
            this.watermark = watermark;
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            this.geometryRevision = geometryRevision;
            this.effectiveTick = effectiveTick;
            this.blockIndex = blockIndex;
            this.status = status;
            this.reason = reason;
            this.signatureId = signatureId;
            this.geometryResyncReason = geometryResyncReason;
            this.fullPageSignatureIds = fullPageSignatureIds;
        }
    }
}
