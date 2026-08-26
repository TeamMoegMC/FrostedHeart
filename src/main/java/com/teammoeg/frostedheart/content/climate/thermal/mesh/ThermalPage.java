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

    private final long sectionKey;
    private final long lifecycleGeneration;
    private final int[] coverageRefs = new int[BASE_BRICK_COUNT];
    private final long[] latestBrickMutationRevisions = new long[BASE_BRICK_COUNT];
    private final GeometrySummaryCache geometrySummaries = new GeometrySummaryCache();
    private final GeometryDeltaCoalescer deltaCoalescer = new GeometryDeltaCoalescer();
    private final AtomicLong liveGeometryRevision = new AtomicLong();
    private final AtomicReference<ResyncRequirement> resyncRequirement = new AtomicReference<>();

    private long mixedBrickMask;
    private long dirtyBrickMask;
    private long topologyGeneration;
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

    public synchronized int[] coverageSnapshot() {
        return coverageRefs.clone();
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
                publishedGeometryRevision,
                publishedTopologyGeneration,
                publishedSolveEpoch
        );
        return true;
    }

    /**
     * Resolves the installed geometry while the topology logical writer is held.
     * Unlike the public query path, this does not require the geometry to have
     * been published to gameplay readers yet.
     */
    public synchronized boolean tryQueryInstalledCoverage(
            int localX,
            int localY,
            int localZ,
            MutableCoverageQuery result
    ) {
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
        int baseIndex = GeometrySummaryCache.baseIndex(localX, localY, localZ);
        if (resyncRequirement.get() != null || dirtyBrickMask != 0L) {
            result.invalidate(sectionKey, lifecycleGeneration, liveGeometryRevision.get());
            return false;
        }
        result.set(
                sectionKey,
                lifecycleGeneration,
                baseIndex,
                coverageRefs[baseIndex],
                liveGeometryRevision.get(),
                topologyGeneration,
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

    /**
     * Records one block/fluid geometry mutation and immediately makes any old
     * publication stale. Repeated same-tick changes to the same Brick are
     * merged by the page-local coalescer.
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
        long revision = liveGeometryRevision.incrementAndGet();
        latestBrickMutationRevisions[baseIndex] = revision;

        long brickBit = 1L << baseIndex;
        dirtyBrickMask |= brickBit;
        coverageRefs[baseIndex] = NO_COVERAGE;
        geometrySummaries.invalidateBaseBrick(baseIndex);

        ResyncRequirement existingResync = resyncRequirement.get();
        if (existingResync != null) {
            advanceResyncRequirement(existingResync.reason(), revision);
            deltaCoalescer.reset();
            return new MutationObservation(revision, baseIndex, false, true);
        }

        GeometryDeltaCoalescer.RecordResult coalesced = deltaCoalescer.record(
                sectionKey,
                lifecycleGeneration,
                baseIndex,
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
            int dropped = deltaCoalescer.reset();
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

    /** Atomically installs one or more rebuilt width-4 Bricks from one revision cut. */
    public synchronized boolean tryInstallBrickBuilds(
            long capturedGeometryRevision,
            long rebuiltBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        requireBrickBuildPayload(rebuiltBrickMask, supportRefs, baseSummaries);
        if (resyncRequirement.get() != null
                || liveGeometryRevision.get() != capturedGeometryRevision
                || (dirtyBrickMask & rebuiltBrickMask) != rebuiltBrickMask) {
            return false;
        }
        installBrickBuildPayload(rebuiltBrickMask, supportRefs, baseSummaries);
        dirtyBrickMask &= ~rebuiltBrickMask;
        topologyGeneration++;
        return true;
    }

    /**
     * Restores Brick coverage invalidated by mutations whose final compiled
     * topology is identical to the installed topology. No topology generation
     * is advanced because no arena cell or adjacency changed.
     */
    public synchronized boolean tryAcknowledgeUnchangedBricks(
            long capturedGeometryRevision,
            long unchangedBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        requireBrickBuildPayload(unchangedBrickMask, supportRefs, baseSummaries);
        if (resyncRequirement.get() != null
                || liveGeometryRevision.get() != capturedGeometryRevision
                || (dirtyBrickMask & unchangedBrickMask) != unchangedBrickMask) {
            return false;
        }
        installBrickBuildPayload(unchangedBrickMask, supportRefs, baseSummaries);
        dirtyBrickMask &= ~unchangedBrickMask;
        return true;
    }

    /** Installs only changed width-4 Bricks and clears one exact full-resync token. */
    public synchronized boolean tryInstallBrickFullGeometryResync(
            GeometryResyncToken token,
            long rebuiltBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        requireBrickBuildPayload(rebuiltBrickMask, supportRefs, baseSummaries);
        ResyncRequirement current = matchingResyncRequirement(token);
        if (current == null || !resyncRequirement.compareAndSet(current, null)) {
            return false;
        }
        installBrickBuildPayload(rebuiltBrickMask, supportRefs, baseSummaries);
        dirtyBrickMask = 0L;
        deltaCoalescer.reset();
        topologyGeneration++;
        return true;
    }

    /** Clears one exact full-resync token whose snapshot matches current topology. */
    public synchronized boolean tryAcknowledgeUnchangedFullGeometryResync(
            GeometryResyncToken token
    ) {
        ResyncRequirement current = matchingResyncRequirement(token);
        if (current == null || !resyncRequirement.compareAndSet(current, null)) {
            return false;
        }
        dirtyBrickMask = 0L;
        deltaCoalescer.reset();
        return true;
    }

    /** Installs a complete geometry build only against its exact live revision. */
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

    private ResyncRequirement matchingResyncRequirement(GeometryResyncToken token) {
        if (token == null
                || token.sectionKey() != sectionKey
                || token.lifecycleGeneration() != lifecycleGeneration) {
            return null;
        }
        ResyncRequirement current = resyncRequirement.get();
        return current != null
                && current.requiredRevision() == token.requiredRevision()
                && current.reason() == token.reason()
                && liveGeometryRevision.get() == token.requiredRevision()
                ? current
                : null;
    }

    private static void requireBrickBuildPayload(
            long rebuiltBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        if (rebuiltBrickMask == 0L
                || supportRefs == null || supportRefs.length != BASE_BRICK_COUNT
                || baseSummaries == null || baseSummaries.length != BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("Brick build payload is incomplete");
        }
        long remaining = rebuiltBrickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            int supportRef = supportRefs[baseIndex];
            GeometrySummary summary = baseSummaries[baseIndex];
            if (summary == null || summary.kind() == GeometrySummary.Kind.UNKNOWN
                    || (supportRef < 0 && supportRef != NO_COVERAGE)) {
                throw new IllegalArgumentException("rebuilt Brick state is invalid");
            }
            remaining &= remaining - 1L;
        }
    }

    private void installBrickBuildPayload(
            long rebuiltBrickMask,
            int[] supportRefs,
            GeometrySummary[] baseSummaries
    ) {
        long remaining = rebuiltBrickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            int supportRef = supportRefs[baseIndex];
            GeometrySummary summary = baseSummaries[baseIndex];
            coverageRefs[baseIndex] = supportRef;
            geometrySummaries.setBaseSummary(baseIndex, summary);
            long brickBit = 1L << baseIndex;
            if (summary.kind() == GeometrySummary.Kind.MIXED) {
                mixedBrickMask |= brickBit;
            } else {
                mixedBrickMask &= ~brickBit;
            }
            remaining &= remaining - 1L;
        }
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
                && dirtyBrickMask == 0L
                && publishedGeometryRevision == liveGeometryRevision.get()
                && publishedTopologyGeneration == topologyGeneration;
    }

    public long publishedSolveEpoch() {
        return publishedSolveEpoch;
    }

    private void installGeometryState(FullGeometryState state) {
        System.arraycopy(state.coverageRefs, 0, coverageRefs, 0, BASE_BRICK_COUNT);
        geometrySummaries.replaceAll(state.summaries);
        mixedBrickMask = state.mixedBrickMask;
        dirtyBrickMask = 0L;
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
        OUT_OF_ORDER_MUTATION,
        SECTION_REPLACED,
        EXPLICIT_INVALIDATION
    }

    public record MutationObservation(
            long geometryRevision,
            int baseBrickIndex,
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
                long geometryRevision,
                long topologyGeneration,
                long solveEpoch
        ) {
            valid = true;
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            this.baseBrickIndex = baseBrickIndex;
            this.coverageRef = coverageRef;
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
        private final GeometrySummary[] summaries;
        private final long mixedBrickMask;

        public FullGeometryState(
                int[] coverageRefs,
                GeometrySummary[] summaries,
                long mixedBrickMask
        ) {
            if (coverageRefs == null || coverageRefs.length != BASE_BRICK_COUNT) {
                throw new IllegalArgumentException("coverage must contain exactly 64 entries");
            }
            if (summaries == null || summaries.length != BASE_BRICK_COUNT) {
                throw new IllegalArgumentException("summaries must contain exactly 64 entries");
            }
            this.coverageRefs = coverageRefs.clone();
            this.summaries = summaries.clone();
            this.mixedBrickMask = mixedBrickMask;
            validateStableState();
        }

        private void validateStableState() {
            for (int baseIndex = 0; baseIndex < BASE_BRICK_COUNT; baseIndex++) {
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
                    if (coverageRefs[baseIndex] != NO_COVERAGE) {
                        throw new IllegalArgumentException(
                                "no-air base Bricks must use NO_COVERAGE");
                    }
                } else {
                    requireSupportRef(coverageRefs[baseIndex]);
                }
            }
        }
    }

    private record ResyncRequirement(GeometryResyncReason reason, long requiredRevision) {
    }
}
