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
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummaryCache;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable runtime page aligned to one 16-cubed Minecraft ChunkSection.
 * Main-thread methods own coverage and summary mutation; live revision and
 * resync invalidation may also be advanced by a mutation hook off-thread.
 */
public final class ThermalPage {
    public static final int BASE_BRICK_COUNT = 64;
    public static final int NO_COVERAGE = -1;
    public static final int FULL_GEOMETRY_RESYNC_REQUIRED = 1;

    private final long sectionKey;
    private final long lifecycleGeneration;
    private final int[] coverageRefs = new int[BASE_BRICK_COUNT];
    private final byte[] coverageWidths = new byte[BASE_BRICK_COUNT];
    private final long[] latestBrickMutationRevisions = new long[BASE_BRICK_COUNT];
    private final GeometrySummaryCache geometrySummaries = new GeometrySummaryCache();
    private final GeometryDeltaCoalescer deltaCoalescer = new GeometryDeltaCoalescer();
    private final AtomicLong liveGeometryRevision = new AtomicLong();
    private final AtomicReference<ResyncRequirement> resyncRequirement = new AtomicReference<>();

    private long mixedBrickMask;
    private long dirtyBrickMask;
    private boolean coverageRepartitionRequired;
    private long topologyGeneration;
    private ArenaSpan cellSpan = ArenaSpan.EMPTY;
    private volatile long publishedGeometryRevision = -1L;
    private volatile long publishedTopologyGeneration = -1L;
    private volatile long publishedSolveEpoch = -1L;

    public ThermalPage(long sectionKey, long lifecycleGeneration) {
        if (lifecycleGeneration < 0L) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
        this.sectionKey = sectionKey;
        this.lifecycleGeneration = lifecycleGeneration;
        Arrays.fill(coverageRefs, NO_COVERAGE);
    }

    /** Builds a page directly from Minecraft's cheap empty-section proof. */
    public static ThermalPage allAir(
            long sectionKey,
            long lifecycleGeneration,
            int supportRef,
            int airMediumId
    ) {
        ThermalPage page = new ThermalPage(sectionKey, lifecycleGeneration);
        page.installInitialGeometry(FullGeometryState.uniformAllAir(
                supportRef, airMediumId, new ArenaSpan(supportRef, 1)));
        return page;
    }

    public long sectionKey() {
        return sectionKey;
    }

    public long lifecycleGeneration() {
        return lifecycleGeneration;
    }

    public long liveGeometryRevision() {
        return liveGeometryRevision.get();
    }

    public long publishedGeometryRevision() {
        return publishedGeometryRevision;
    }

    public synchronized long topologyGeneration() {
        return topologyGeneration;
    }

    public long publishedTopologyGeneration() {
        return publishedTopologyGeneration;
    }

    public synchronized int coverageRefAtBase(int baseIndex) {
        requireBaseIndex(baseIndex);
        return coverageRefs[baseIndex];
    }

    public synchronized int coverageRefAtBlock(int localX, int localY, int localZ) {
        return coverageRefs[GeometrySummaryCache.baseIndex(localX, localY, localZ)];
    }

    public synchronized int coverageWidthAtBase(int baseIndex) {
        requireBaseIndex(baseIndex);
        return Byte.toUnsignedInt(coverageWidths[baseIndex]);
    }

    public synchronized int[] coverageSnapshot() {
        return coverageRefs.clone();
    }

    public synchronized byte[] coverageWidthSnapshot() {
        return coverageWidths.clone();
    }

    /**
     * Resolves one local block through the current published geometry in O(1).
     * The caller owns and reuses {@code result}; a stale Page clears it and
     * requires the query compositor to use its Page-wide fallback.
     */
    public synchronized boolean tryQueryPublishedCoverage(
            int localX,
            int localY,
            int localZ,
            MutableCoverageQuery result
    ) {
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
        int baseIndex = GeometrySummaryCache.baseIndex(localX, localY, localZ);
        if (!publishedGeometryIsCurrentLocked()) {
            result.invalidate(sectionKey, lifecycleGeneration, liveGeometryRevision.get());
            return false;
        }
        result.set(
                sectionKey,
                lifecycleGeneration,
                baseIndex,
                coverageRefs[baseIndex],
                Byte.toUnsignedInt(coverageWidths[baseIndex]),
                publishedGeometryRevision,
                publishedTopologyGeneration,
                publishedSolveEpoch
        );
        return true;
    }

    public synchronized GeometrySummary geometrySummary(int summaryIndex) {
        return geometrySummaries.summary(summaryIndex);
    }

    public synchronized long mixedBrickMask() {
        return mixedBrickMask;
    }

    public synchronized long dirtyBrickMask() {
        return dirtyBrickMask;
    }

    public synchronized int dirtyBrickCount() {
        return Long.bitCount(dirtyBrickMask);
    }

    public synchronized boolean coverageRepartitionRequired() {
        return coverageRepartitionRequired;
    }

    /**
     * Records one block/fluid geometry mutation and immediately makes any old
     * publication stale. The returned Brick is materialized only once; repeated
     * same-tick changes are merged by the page-local coalescer.
     */
    public synchronized MutationObservation recordGeometryMutation(
            int localX,
            int localY,
            int localZ,
            long effectiveTick,
            GeometryDeltaRing ring
    ) {
        if (ring == null) {
            throw new IllegalArgumentException("ring is required");
        }
        if (effectiveTick < 0L) {
            throw new IllegalArgumentException("effectiveTick must be non-negative");
        }
        int baseIndex = GeometrySummaryCache.baseIndex(localX, localY, localZ);
        int brickVoxelIndex = GeometrySummaryCache.brickVoxelIndex(localX, localY, localZ);
        long revision = liveGeometryRevision.incrementAndGet();
        latestBrickMutationRevisions[baseIndex] = revision;

        long brickBit = 1L << baseIndex;
        boolean materializedBrick = (mixedBrickMask & brickBit) == 0L;
        boolean invalidatedCoarseSupport = Byte.toUnsignedInt(coverageWidths[baseIndex]) > 4;
        mixedBrickMask |= brickBit;
        dirtyBrickMask |= brickBit;
        coverageRefs[baseIndex] = NO_COVERAGE;
        coverageWidths[baseIndex] = 0;
        coverageRepartitionRequired |= invalidatedCoarseSupport;
        geometrySummaries.invalidateBaseBrick(baseIndex);

        ResyncRequirement existingResync = resyncRequirement.get();
        if (existingResync != null) {
            advanceResyncRequirement(existingResync.reason(), revision);
            deltaCoalescer.reset();
            return new MutationObservation(
                    revision, baseIndex, invalidatedCoarseSupport,
                    materializedBrick, false, true);
        }

        GeometryDeltaCoalescer.RecordResult coalesced = deltaCoalescer.record(
                sectionKey,
                lifecycleGeneration,
                baseIndex,
                brickVoxelIndex,
                revision,
                effectiveTick,
                ring
        );
        if (coalesced.overflowed()) {
            advanceResyncRequirement(GeometryResyncReason.RING_OVERFLOW, revision);
            deltaCoalescer.reset();
        } else if (coalesced.outOfOrder()) {
            advanceResyncRequirement(GeometryResyncReason.OUT_OF_ORDER_MUTATION, revision);
            deltaCoalescer.reset();
        }
        return new MutationObservation(
                revision,
                baseIndex,
                invalidatedCoarseSupport,
                materializedBrick,
                !coalesced.firstChangeForBrick(),
                fullGeometryResyncRequired()
        );
    }

    /** Seals the current tick's coalesced Brick records into the bounded SPSC ring. */
    public synchronized GeometryDeltaCoalescer.SealResult sealGeometryDeltas(
            GeometryDeltaRing ring
    ) {
        if (ring == null) {
            throw new IllegalArgumentException("ring is required");
        }
        if (resyncRequirement.get() != null) {
            int dropped = deltaCoalescer.pendingBrickCount();
            deltaCoalescer.reset();
            return new GeometryDeltaCoalescer.SealResult(0, dropped, dropped != 0);
        }
        GeometryDeltaCoalescer.SealResult result = deltaCoalescer.sealTo(
                sectionKey, lifecycleGeneration, ring);
        if (result.overflowed()) {
            advanceResyncRequirement(
                    GeometryResyncReason.RING_OVERFLOW,
                    liveGeometryRevision.get()
            );
        }
        return result;
    }

    /**
     * Installs one rebuilt 4-cubed Brick when its old coverage was already fine.
     * Coarse invalidation requires a complete repartition through tryInstallGeometryBuild.
     */
    public synchronized boolean acknowledgeBrickRebuild(
            int baseIndex,
            long capturedBrickRevision,
            int supportRef,
            GeometrySummary summary
    ) {
        requireBaseIndex(baseIndex);
        requireSupportRef(supportRef);
        if (summary == null || summary.kind() == GeometrySummary.Kind.UNKNOWN) {
            throw new IllegalArgumentException("rebuilt Brick summary must be known");
        }
        if (resyncRequirement.get() != null
                || coverageRepartitionRequired
                || latestBrickMutationRevisions[baseIndex] != capturedBrickRevision) {
            return false;
        }
        coverageRefs[baseIndex] = supportRef;
        coverageWidths[baseIndex] = 4;
        geometrySummaries.setBaseSummary(baseIndex, summary);
        long brickBit = 1L << baseIndex;
        dirtyBrickMask &= ~brickBit;
        if (summary.kind() == GeometrySummary.Kind.MIXED) {
            mixedBrickMask |= brickBit;
        } else {
            mixedBrickMask &= ~brickBit;
        }
        topologyGeneration++;
        return true;
    }

    /** Atomically installs one or more rebuilt width-4 Bricks from one revision cut. */
    public synchronized boolean tryInstallBrickBuilds(
            long capturedGeometryRevision,
            long rebuiltBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        if (rebuiltBrickMask == 0L
                || supportRefs == null || supportRefs.length != BASE_BRICK_COUNT
                || baseSummaries == null || baseSummaries.length != BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("Brick build payload is incomplete");
        }
        if (resyncRequirement.get() != null
                || coverageRepartitionRequired
                || liveGeometryRevision.get() != capturedGeometryRevision
                || (dirtyBrickMask & rebuiltBrickMask) != rebuiltBrickMask) {
            return false;
        }
        long remaining = rebuiltBrickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            int supportRef = supportRefs[baseIndex];
            GeometrySummary summary = baseSummaries[baseIndex];
            if (summary == null || summary.kind() == GeometrySummary.Kind.UNKNOWN
                    || (supportRef < 0
                    && supportRef != NO_COVERAGE)) {
                throw new IllegalArgumentException("rebuilt Brick state is invalid");
            }
            coverageRefs[baseIndex] = supportRef;
            coverageWidths[baseIndex] = 4;
            geometrySummaries.setBaseSummary(baseIndex, summary);
            long brickBit = 1L << baseIndex;
            if (summary.kind() == GeometrySummary.Kind.MIXED) {
                mixedBrickMask |= brickBit;
            } else {
                mixedBrickMask &= ~brickBit;
            }
            remaining &= remaining - 1L;
        }
        dirtyBrickMask &= ~rebuiltBrickMask;
        topologyGeneration++;
        return true;
    }

    /** Installs a complete worker geometry result only against its exact live revision. */
    public synchronized boolean tryInstallGeometryBuild(
            long capturedGeometryRevision,
            FullGeometryState state
    ) {
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        if (resyncRequirement.get() != null
                || liveGeometryRevision.get() != capturedGeometryRevision) {
            return false;
        }
        installGeometryState(state);
        return true;
    }

    /** Off-thread-safe sticky invalidation for a mutation that could not produce a delta. */
    public long requireFullGeometryResync(GeometryResyncReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason is required");
        }
        long revision = liveGeometryRevision.incrementAndGet();
        advanceResyncRequirement(reason, revision);
        return revision;
    }

    public boolean fullGeometryResyncRequired() {
        return resyncRequirement.get() != null;
    }

    public GeometryResyncToken beginFullGeometryResync() {
        ResyncRequirement requirement = resyncRequirement.get();
        return requirement == null ? null : new GeometryResyncToken(
                sectionKey,
                lifecycleGeneration,
                requirement.requiredRevision(),
                requirement.reason()
        );
    }

    /**
     * Atomically acknowledges the page-owned sticky requirement only if no newer
     * mutation superseded the resnapshot token.
     */
    public synchronized boolean tryInstallFullGeometryResync(
            GeometryResyncToken token,
            FullGeometryState state
    ) {
        if (token == null || state == null) {
            throw new IllegalArgumentException("token and state are required");
        }
        if (token.sectionKey() != sectionKey
                || token.lifecycleGeneration() != lifecycleGeneration) {
            return false;
        }
        ResyncRequirement current = resyncRequirement.get();
        if (current == null
                || current.requiredRevision() != token.requiredRevision()
                || current.reason() != token.reason()
                || liveGeometryRevision.get() != token.requiredRevision()) {
            return false;
        }
        if (!resyncRequirement.compareAndSet(current, null)) {
            return false;
        }
        installGeometryState(state);
        deltaCoalescer.reset();
        return true;
    }

    /** Publishes only a complete, current topology identity. */
    public synchronized boolean tryPublishGeometry(
            long capturedGeometryRevision,
            long capturedTopologyGeneration,
            long solveEpoch
    ) {
        if (solveEpoch < 0L) {
            throw new IllegalArgumentException("solveEpoch must be non-negative");
        }
        if (resyncRequirement.get() != null
                || coverageRepartitionRequired
                || dirtyBrickMask != 0L
                || liveGeometryRevision.get() != capturedGeometryRevision
                || topologyGeneration != capturedTopologyGeneration) {
            return false;
        }
        publishedTopologyGeneration = capturedTopologyGeneration;
        publishedSolveEpoch = solveEpoch;
        publishedGeometryRevision = capturedGeometryRevision;
        return true;
    }

    public synchronized boolean publishedGeometryIsCurrent() {
        return publishedGeometryIsCurrentLocked();
    }

    private boolean publishedGeometryIsCurrentLocked() {
        return resyncRequirement.get() == null
                && !coverageRepartitionRequired
                && dirtyBrickMask == 0L
                && publishedGeometryRevision == liveGeometryRevision.get()
                && publishedTopologyGeneration == topologyGeneration;
    }

    public long publishedSolveEpoch() {
        return publishedSolveEpoch;
    }

    public synchronized ArenaSpan cellSpan() {
        return cellSpan;
    }

    public synchronized void setCellSpan(ArenaSpan cellSpan) {
        if (cellSpan == null) {
            throw new IllegalArgumentException("cellSpan is required");
        }
        this.cellSpan = cellSpan;
    }

    public int flags() {
        return fullGeometryResyncRequired() ? FULL_GEOMETRY_RESYNC_REQUIRED : 0;
    }

    private synchronized void installInitialGeometry(FullGeometryState state) {
        if (topologyGeneration != 0L || liveGeometryRevision.get() != 0L) {
            throw new IllegalStateException("initial geometry can only be installed once");
        }
        installGeometryState(state);
    }

    private void installGeometryState(FullGeometryState state) {
        System.arraycopy(state.coverageRefs, 0, coverageRefs, 0, BASE_BRICK_COUNT);
        System.arraycopy(state.coverageWidths, 0, coverageWidths, 0, BASE_BRICK_COUNT);
        geometrySummaries.replaceAll(state.summaries);
        mixedBrickMask = state.mixedBrickMask;
        dirtyBrickMask = 0L;
        coverageRepartitionRequired = false;
        cellSpan = state.cellSpan;
        Arrays.fill(latestBrickMutationRevisions, 0L);
        topologyGeneration++;
    }

    private void advanceResyncRequirement(GeometryResyncReason reason, long revision) {
        resyncRequirement.getAndUpdate(current -> new ResyncRequirement(
                current == null ? reason : current.reason(),
                current == null ? revision : Math.max(revision, current.requiredRevision())
        ));
    }

    private static void requireBaseIndex(int baseIndex) {
        if (baseIndex < 0 || baseIndex >= BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("baseIndex must be within [0, 63]");
        }
    }

    private static void requireSupportRef(int supportRef) {
        if (supportRef < 0) {
            throw new IllegalArgumentException("supportRef must be a non-negative int ID");
        }
    }

    public enum GeometryResyncReason {
        RING_OVERFLOW,
        OFF_THREAD_MUTATION,
        OUT_OF_ORDER_MUTATION,
        SECTION_REPLACED,
        EXPLICIT_INVALIDATION
    }

    public record MutationObservation(
            long geometryRevision,
            int baseBrickIndex,
            boolean coarseSupportInvalidated,
            boolean materializedBrick,
            boolean coalescedWithExistingBrickDelta,
            boolean fullResyncRequired
    ) {
    }

    /** Mutable allocation-free result for the production-shaped coverage path. */
    public static final class MutableCoverageQuery {
        private boolean valid;
        private long sectionKey;
        private long lifecycleGeneration;
        private int baseBrickIndex = -1;
        private int coverageRef = NO_COVERAGE;
        private int coverageWidth;
        private long geometryRevision = -1L;
        private long topologyGeneration = -1L;
        private long solveEpoch = -1L;

        public boolean valid() {
            return valid;
        }

        public long sectionKey() {
            return sectionKey;
        }

        public long lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public int baseBrickIndex() {
            return baseBrickIndex;
        }

        public int coverageRef() {
            return coverageRef;
        }

        public int coverageWidth() {
            return coverageWidth;
        }

        public long geometryRevision() {
            return geometryRevision;
        }

        public long topologyGeneration() {
            return topologyGeneration;
        }

        public long solveEpoch() {
            return solveEpoch;
        }

        private void set(
                long sectionKey,
                long lifecycleGeneration,
                int baseBrickIndex,
                int coverageRef,
                int coverageWidth,
                long geometryRevision,
                long topologyGeneration,
                long solveEpoch
        ) {
            valid = true;
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            this.baseBrickIndex = baseBrickIndex;
            this.coverageRef = coverageRef;
            this.coverageWidth = coverageWidth;
            this.geometryRevision = geometryRevision;
            this.topologyGeneration = topologyGeneration;
            this.solveEpoch = solveEpoch;
        }

        private void invalidate(
                long sectionKey,
                long lifecycleGeneration,
                long liveGeometryRevision
        ) {
            valid = false;
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            baseBrickIndex = -1;
            coverageRef = NO_COVERAGE;
            coverageWidth = 0;
            geometryRevision = liveGeometryRevision;
            topologyGeneration = -1L;
            solveEpoch = -1L;
        }
    }

    public record GeometryResyncToken(
            long sectionKey,
            long lifecycleGeneration,
            long requiredRevision,
            GeometryResyncReason reason
    ) {
        public GeometryResyncToken {
            if (lifecycleGeneration < 0L || requiredRevision <= 0L || reason == null) {
                throw new IllegalArgumentException("resync token fields are invalid");
            }
        }
    }

    /** Immutable complete geometry install payload. */
    public static final class FullGeometryState {
        private final int[] coverageRefs;
        private final byte[] coverageWidths;
        private final GeometrySummary[] summaries;
        private final long mixedBrickMask;
        private final ArenaSpan cellSpan;

        public FullGeometryState(
                int[] coverageRefs,
                byte[] coverageWidths,
                GeometrySummary[] summaries,
                long mixedBrickMask,
                ArenaSpan cellSpan
        ) {
            if (coverageRefs == null || coverageRefs.length != BASE_BRICK_COUNT
                    || coverageWidths == null || coverageWidths.length != BASE_BRICK_COUNT) {
                throw new IllegalArgumentException("coverage arrays must contain exactly 64 entries");
            }
            if (summaries == null || summaries.length != GeometrySummaryCache.SUMMARY_COUNT) {
                throw new IllegalArgumentException("summaries must contain exactly 73 entries");
            }
            if (cellSpan == null) {
                throw new IllegalArgumentException("cellSpan is required");
            }
            this.coverageRefs = coverageRefs.clone();
            this.coverageWidths = coverageWidths.clone();
            this.summaries = summaries.clone();
            this.mixedBrickMask = mixedBrickMask;
            this.cellSpan = cellSpan;
            validateStableState();
        }

        public static FullGeometryState uniformAllAir(
                int supportRef,
                int airMediumId,
                ArenaSpan cellSpan
        ) {
            requireSupportRef(supportRef);
            int[] refs = new int[BASE_BRICK_COUNT];
            byte[] widths = new byte[BASE_BRICK_COUNT];
            GeometrySummary[] summaries = new GeometrySummary[GeometrySummaryCache.SUMMARY_COUNT];
            Arrays.fill(refs, supportRef);
            Arrays.fill(widths, (byte) 16);
            Arrays.fill(summaries, GeometrySummary.singleAir(airMediumId));
            return new FullGeometryState(refs, widths, summaries, 0L, cellSpan);
        }

        public int[] coverageRefs() {
            return coverageRefs.clone();
        }

        public byte[] coverageWidths() {
            return coverageWidths.clone();
        }

        public GeometrySummary[] summaries() {
            return summaries.clone();
        }

        public long mixedBrickMask() {
            return mixedBrickMask;
        }

        public ArenaSpan cellSpan() {
            return cellSpan;
        }

        private void validateStableState() {
            for (int baseIndex = 0; baseIndex < BASE_BRICK_COUNT; baseIndex++) {
                int width = Byte.toUnsignedInt(coverageWidths[baseIndex]);
                if (width != 4 && width != 8 && width != 16) {
                    throw new IllegalArgumentException("coverage width must be 4, 8, or 16");
                }
                if (summaries[baseIndex] == null
                        || summaries[baseIndex].kind() == GeometrySummary.Kind.UNKNOWN) {
                    throw new IllegalArgumentException("stable base summaries must be known");
                }
                boolean mixed = (mixedBrickMask & (1L << baseIndex)) != 0L;
                if (mixed != (summaries[baseIndex].kind() == GeometrySummary.Kind.MIXED)) {
                    throw new IllegalArgumentException(
                            "mixedBrickMask must exactly match MIXED base summaries");
                }
                boolean noAir = summaries[baseIndex].kind() == GeometrySummary.Kind.NO_AIR;
                if (noAir) {
                    if (coverageRefs[baseIndex] != NO_COVERAGE || width != 4) {
                        throw new IllegalArgumentException(
                                "no-air base Bricks must use width-4 NO_COVERAGE");
                    }
                } else {
                    requireSupportRef(coverageRefs[baseIndex]);
                }
                validateCoverageGroup(baseIndex, width);
            }
            for (GeometrySummary summary : summaries) {
                if (summary == null || summary.kind() == GeometrySummary.Kind.UNKNOWN) {
                    throw new IllegalArgumentException("stable summaries must all be known");
                }
            }
        }

        private void validateCoverageGroup(int baseIndex, int width) {
            int bricksPerAxis = width >>> 2;
            int brickX = baseIndex & 3;
            int brickZ = (baseIndex >>> 2) & 3;
            int brickY = (baseIndex >>> 4) & 3;
            int originX = (brickX / bricksPerAxis) * bricksPerAxis;
            int originZ = (brickZ / bricksPerAxis) * bricksPerAxis;
            int originY = (brickY / bricksPerAxis) * bricksPerAxis;
            int expectedRef = coverageRefs[baseIndex];
            for (int y = 0; y < bricksPerAxis; y++) {
                for (int z = 0; z < bricksPerAxis; z++) {
                    for (int x = 0; x < bricksPerAxis; x++) {
                        int other = (originX + x)
                                | ((originZ + z) << 2)
                                | ((originY + y) << 4);
                        if (coverageRefs[other] != expectedRef
                                || Byte.toUnsignedInt(coverageWidths[other]) != width) {
                            throw new IllegalArgumentException(
                                    "coverage entries must form aligned 4/8/16 support groups");
                        }
                    }
                }
            }
        }
    }

    private record ResyncRequirement(GeometryResyncReason reason, long requiredRevision) {
    }
}
