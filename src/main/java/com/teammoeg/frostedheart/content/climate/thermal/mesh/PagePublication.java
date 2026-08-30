/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;

import java.util.Arrays;

/** Immutable worker-to-main geometry and phase publication for one Page. */
public final class PagePublication {
    private static final int NO_COVERAGE = -1;
    public static final int NO_AIR_POINT = -1;
    public static final PagePublication EMPTY = new PagePublication(
            -1L, -1L, emptyBricks());

    private final long geometryRevision;
    private final long topologyGeneration;
    private final Brick[] bricks;

    private PagePublication(
            long geometryRevision,
            long topologyGeneration,
            Brick[] bricks
    ) {
        if (geometryRevision < -1L || topologyGeneration < -1L) {
            throw new IllegalArgumentException("Page publication identities are invalid");
        }
        if (bricks == null || bricks.length != ThermalPageHandle.BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("Page publication requires 64 Bricks");
        }
        this.geometryRevision = geometryRevision;
        this.topologyGeneration = topologyGeneration;
        this.bricks = bricks;
        for (Brick brick : this.bricks) {
            if (brick == null) {
                throw new IllegalArgumentException("Page publication contains a null Brick");
            }
        }
    }

    public static PagePublication owned(
            long geometryRevision,
            long topologyGeneration,
            Brick[] bricks
    ) {
        return new PagePublication(
                geometryRevision, topologyGeneration, bricks);
    }

    public long geometryRevision() {
        return geometryRevision;
    }

    public long topologyGeneration() {
        return topologyGeneration;
    }

    public Brick[] copyBricks() {
        return bricks.clone();
    }

    public Brick brickAt(int localX, int localY, int localZ) {
        if (localX < 0 || localX >= 16
                || localY < 0 || localY >= 16
                || localZ < 0 || localZ >= 16) {
            throw new IllegalArgumentException(
                    "local coordinates must be within [0, 15]");
        }
        return bricks[(localX >>> 2)
                | (localZ >>> 2) << 2
                | (localY >>> 2) << 4];
    }

    public int resolveAirPoint(
            int localX,
            int localY,
            int localZ,
            int microcellIndex,
            ThermalSignatureRegistry signatureRegistry
    ) {
        if (microcellIndex < 0 || microcellIndex >= 64) {
            throw new IllegalArgumentException(
                    "microcellIndex must be within [0, 63]");
        }
        Brick brick = brickAt(localX, localY, localZ);
        int support = brick.coverageSlot;
        if (support == NO_COVERAGE) {
            return NO_AIR_POINT;
        }
        ComponentBrickCompiler.CompiledBrick mixed = brick.mixedGeometry;
        if (mixed == null) {
            return support;
        }
        if (brick.signaturePayload == null) {
            return NO_AIR_POINT;
        }
        int blockInBrick = (localX & 3)
                | (localZ & 3) << 2
                | (localY & 3) << 4;
        int signatureId = PageSignatures.valueAt(
                brick.signaturePayload, blockInBrick);
        int localRegion = signatureRegistry.componentOrdinal(
                signatureId, microcellIndex);
        if (localRegion == 0xff) {
            return NO_AIR_POINT;
        }
        int component = mixed.compiledComponentAt(
                blockInBrick, localRegion);
        return component < 0 ? NO_AIR_POINT : support + component;
    }

    public boolean hasPhaseCandidate(
            int blockX,
            int blockY,
            int blockZ,
            int materialProfileId
    ) {
        int localX = Math.floorMod(blockX, 16);
        int localY = Math.floorMod(blockY, 16);
        int localZ = Math.floorMod(blockZ, 16);
        int candidateBit = localX & 3
                | (localZ & 3) << 2
                | (localY & 3) << 4;
        return brickAt(localX, localY, localZ).phaseCandidates.contains(
                materialProfileId, candidateBit);
    }

    private static Brick[] emptyBricks() {
        Brick[] result = new Brick[ThermalPageHandle.BASE_BRICK_COUNT];
        Arrays.fill(result, Brick.EMPTY);
        return result;
    }

    /** One immutable Brick's query-facing coverage, geometry, and phase payload. */
    public record Brick(
            int coverageSlot,
            int arenaGeneration,
            Object signaturePayload,
            ComponentBrickCompiler.CompiledBrick mixedGeometry,
            PhaseCandidates phaseCandidates
    ) {
        public static final Brick EMPTY = new Brick(
                NO_COVERAGE,
                0,
                null,
                null,
                PhaseCandidates.EMPTY);

        public Brick {
            if (coverageSlot < NO_COVERAGE || arenaGeneration < 0) {
                throw new IllegalArgumentException("Brick coverage identity is invalid");
            }
            if (signaturePayload != null
                    && !(signaturePayload instanceof char[])
                    && !(signaturePayload instanceof int[])) {
                throw new IllegalArgumentException("Brick signature payload is invalid");
            }
            if (signaturePayload instanceof char[] values
                    && values.length != PageSignatures.ENTRIES_PER_BRICK) {
                throw new IllegalArgumentException("compact Brick signatures are invalid");
            }
            if (signaturePayload instanceof int[] values
                    && values.length != PageSignatures.ENTRIES_PER_BRICK) {
                throw new IllegalArgumentException("wide Brick signatures are invalid");
            }
            if (phaseCandidates == null) {
                throw new IllegalArgumentException("Brick phase candidates are required");
            }
        }
    }

    /** Exact profile/mask entries for one base Brick. */
    public static final class PhaseCandidates {
        public static final PhaseCandidates EMPTY = new PhaseCandidates(
                new int[0], new long[0]);

        private final int[] profileIds;
        private final long[] candidateMasks;

        private PhaseCandidates(
                int[] profileIds,
                long[] candidateMasks
        ) {
            if (profileIds == null || candidateMasks == null
                    || profileIds.length != candidateMasks.length) {
                throw new IllegalArgumentException("phase candidate arrays are invalid");
            }
            this.profileIds = profileIds;
            this.candidateMasks = candidateMasks;
        }

        public static PhaseCandidates owned(int[] profileIds, long[] candidateMasks) {
            return new PhaseCandidates(profileIds, candidateMasks);
        }

        public boolean contains(int profileId, int candidateBit) {
            if (candidateBit < 0 || candidateBit >= Long.SIZE) {
                return false;
            }
            for (int index = 0; index < profileIds.length; index++) {
                if (profileIds[index] == profileId) {
                    return (candidateMasks[index] & 1L << candidateBit) != 0L;
                }
            }
            return false;
        }
    }
}
