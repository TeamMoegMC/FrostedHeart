/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import java.util.Arrays;

/**
 * Immutable worker-to-main geometry and phase publication for one Page.
 * The worker Page slot is an opaque QueryPublication index and is never wired.
 */
public final class PagePublication {
    private static final int NO_COVERAGE = -1;
    public static final int NO_AIR_POINT = -1;
    public static final PagePublication EMPTY = new PagePublication(
            -1, -1L, -1L, emptyBricks());

    private final int workerPageSlot;
    private final long geometryRevision;
    private final long topologyGeneration;
    private final Brick[] bricks;

    private PagePublication(
            int workerPageSlot,
            long geometryRevision,
            long topologyGeneration,
            Brick[] bricks
    ) {
        validateIdentities(workerPageSlot, geometryRevision, topologyGeneration);
        if (bricks == null || bricks.length != ThermalPageHandle.BASE_BRICK_COUNT) {
            throw new IllegalArgumentException("Page publication requires 64 Bricks");
        }
        this.workerPageSlot = workerPageSlot;
        this.geometryRevision = geometryRevision;
        this.topologyGeneration = topologyGeneration;
        this.bricks = bricks;
        for (Brick brick : this.bricks) {
            if (brick == null) {
                throw new IllegalArgumentException("Page publication contains a null Brick");
            }
        }
    }

    private PagePublication(
            long geometryRevision,
            long topologyGeneration,
            PagePublication previous
    ) {
        validateIdentities(
                previous.workerPageSlot, geometryRevision, topologyGeneration);
        this.workerPageSlot = previous.workerPageSlot;
        this.geometryRevision = geometryRevision;
        this.topologyGeneration = topologyGeneration;
        bricks = previous.bricks;
    }

    public static PagePublication owned(
            int workerPageSlot,
            long geometryRevision,
            long topologyGeneration,
            Brick[] bricks
    ) {
        return new PagePublication(
                workerPageSlot, geometryRevision, topologyGeneration, bricks);
    }

    public PagePublication withIdentities(
            long geometryRevision,
            long topologyGeneration
    ) {
        if (this.geometryRevision == geometryRevision
                && this.topologyGeneration == topologyGeneration) {
            return this;
        }
        return new PagePublication(
                geometryRevision, topologyGeneration, this);
    }

    public long geometryRevision() {
        return geometryRevision;
    }

    public int workerPageSlot() {
        return workerPageSlot;
    }

    public long topologyGeneration() {
        return topologyGeneration;
    }

    public Brick[] copyBricks() {
        return bricks.clone();
    }

    public Brick brick(int index) {
        if (index < 0 || index >= bricks.length) {
            throw new IllegalArgumentException("Brick index must be within [0, 63]");
        }
        return bricks[index];
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

    public int resolveAirPoint(int localX, int localY, int localZ) {
        Brick brick=brickAt(localX,localY,localZ);
        if (!brick.resolved || brick.firstSlot<0 || brick.transportNodeCount==0) return NO_AIR_POINT;
        if (brick.blockLayout==null) return brick.firstSlot;
        int node=brick.blockLayout.transportAt((localX&3)|(localZ&3)<<2|(localY&3)<<4);
        return node<0 ? NO_AIR_POINT : brick.firstSlot+node;
    }

    private static Brick[] emptyBricks() {
        Brick[] result = new Brick[ThermalPageHandle.BASE_BRICK_COUNT];
        Arrays.fill(result, Brick.EMPTY);
        return result;
    }

    private static void validateIdentities(
            int workerPageSlot,
            long geometryRevision,
            long topologyGeneration
    ) {
        if (workerPageSlot < -1
                || geometryRevision < -1L
                || topologyGeneration < -1L) {
            throw new IllegalArgumentException(
                    "Page publication identities are invalid");
        }
    }

    /** One immutable Brick's query-facing coverage and geometry. */
    public record Brick(
            int firstSlot,
            int arenaGeneration,
            Object signaturePayload,
            BlockBrickLayout blockLayout,
            int transportNodeCount,
            boolean resolved
    ) {
        public int signatureAtBlock(int block) {
            return signaturePayload == null ? ThermalSignatureTable.UNRESOLVED
                    : PageSignatures.valueAt(signaturePayload, block);
        }
        public static final Brick EMPTY = new Brick(
                NO_COVERAGE,
                0,
                null,
                null,
                0,
                false);

        public Brick {
            if (firstSlot < NO_COVERAGE || arenaGeneration < 0) {
                throw new IllegalArgumentException("Brick coverage identity is invalid");
            }
            if (signaturePayload != null
                    && !(signaturePayload instanceof Integer)
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
        }
    }

}
