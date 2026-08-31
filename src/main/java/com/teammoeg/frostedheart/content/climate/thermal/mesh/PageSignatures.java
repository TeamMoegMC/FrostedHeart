/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import java.util.Objects;

/** Immutable Brick-addressed signature IDs for one 16-cubed thermal Page. */
public final class PageSignatures {
    public static final int ENTRY_COUNT = 16 * 16 * 16;
    private static final int BRICK_COUNT = ThermalPageHandle.BASE_BRICK_COUNT;
    public static final int ENTRIES_PER_BRICK = 4 * 4 * 4;

    private static final int ENCODE_OFFSET = 2;
    private static final int MIN_COMPACT_VALUE = -ENCODE_OFFSET;
    private static final int MAX_COMPACT_VALUE = Character.MAX_VALUE - ENCODE_OFFSET;

    private final Object[] bricks;

    private PageSignatures(Object[] bricks) {
        this.bricks = bricks;
    }

    public static PageSignatures unresolved(ThermalSignatureTable signatures) {
        Objects.requireNonNull(signatures, "signatures");
        Object[] bricks = new Object[BRICK_COUNT];
        java.util.Arrays.fill(
                bricks,
                signatures.uniformPayload(ThermalSignatureTable.UNRESOLVED));
        return new PageSignatures(bricks);
    }

    public int get(int blockIndex) {
        requireBlockIndex(blockIndex);
        int brick = brickIndex(blockIndex);
        return valueAt(bricks[brick], indexWithinBrick(blockIndex));
    }

    public PageSignatures withBricks(
            ThermalSignatureTable signatures,
            int[] baseBrickIndexes,
            int[][] brickValues
    ) {
        if (baseBrickIndexes == null || brickValues == null
                || baseBrickIndexes.length != brickValues.length) {
            throw new IllegalArgumentException("Brick signature replacements are invalid");
        }
        Object[] next = bricks.clone();
        long seen = 0L;
        for (int index = 0; index < baseBrickIndexes.length; index++) {
            int brick = baseBrickIndexes[index];
            requireBrickIndex(brick);
            requireBrick(brickValues[index]);
            long bit = 1L << brick;
            if ((seen & bit) != 0L) {
                throw new IllegalArgumentException("duplicate Brick signature replacement");
            }
            seen |= bit;
            next[brick] = encodeBrick(signatures, brickValues[index]);
        }
        return new PageSignatures(next);
    }

    /** Immutable internal payload shared with worker and Page publication. */
    public Object brickPayload(int baseBrickIndex) {
        requireBrickIndex(baseBrickIndex);
        return bricks[baseBrickIndex];
    }

    static int valueAt(Object payload, int index) {
        if (payload instanceof Integer uniform) {
            return uniform;
        }
        if (payload instanceof char[] compact) {
            return compact[index] - ENCODE_OFFSET;
        }
        int[] wide = (int[]) payload;
        return wide[index];
    }

    private static Object encodeBrick(
            ThermalSignatureTable signatures,
            int[] values
    ) {
        int first = values[0];
        boolean uniform = true;
        boolean compact = fitsCompact(first);
        for (int index = 1; index < values.length; index++) {
            int value = values[index];
            uniform &= value == first;
            compact &= fitsCompact(value);
        }
        if (uniform) {
            return signatures.uniformPayload(first);
        }
        if (!compact) {
            return values.clone();
        }
        char[] encoded = new char[ENTRIES_PER_BRICK];
        for (int index = 0; index < values.length; index++) {
            encoded[index] = encode(values[index]);
        }
        return encoded;
    }

    private static int brickIndex(int blockIndex) {
        int localX = blockIndex & 15;
        int localZ = blockIndex >>> 4 & 15;
        int localY = blockIndex >>> 8 & 15;
        return localX >>> 2 | (localZ >>> 2) << 2 | (localY >>> 2) << 4;
    }

    private static int indexWithinBrick(int blockIndex) {
        int localX = blockIndex & 15;
        int localZ = blockIndex >>> 4 & 15;
        int localY = blockIndex >>> 8 & 15;
        return localX & 3 | (localZ & 3) << 2 | (localY & 3) << 4;
    }

    private static boolean fitsCompact(int value) {
        return value >= MIN_COMPACT_VALUE && value <= MAX_COMPACT_VALUE;
    }

    private static char encode(int value) {
        return (char) (value + ENCODE_OFFSET);
    }

    private static void requireBlockIndex(int index) {
        if (index < 0 || index >= ENTRY_COUNT) {
            throw new IndexOutOfBoundsException("signature index: " + index);
        }
    }

    private static void requireBrickIndex(int index) {
        if (index < 0 || index >= BRICK_COUNT) {
            throw new IndexOutOfBoundsException("Brick index: " + index);
        }
    }

    private static void requireBrick(int[] values) {
        if (values == null || values.length != ENTRIES_PER_BRICK) {
            throw new IllegalArgumentException("Brick signatures must contain 64 IDs");
        }
    }

    /** Main-thread or worker-preparation scratch; a built value never aliases it. */
    public static final class Builder {
        private final ThermalSignatureTable signatures;
        private final int[] values = new int[ENTRY_COUNT];
        private final int[] brick = new int[ENTRIES_PER_BRICK];
        private final Object[] brickPayloads = new Object[BRICK_COUNT];

        public Builder(ThermalSignatureTable signatures) {
            this.signatures = Objects.requireNonNull(signatures, "signatures");
        }

        public Builder set(int blockIndex, int value) {
            requireBlockIndex(blockIndex);
            values[blockIndex] = value;
            return this;
        }

        public Builder reset(PageSignatures base) {
            Objects.requireNonNull(base, "base");
            System.arraycopy(base.bricks, 0, brickPayloads, 0, BRICK_COUNT);
            return this;
        }

        public Builder setBrick(int brickIndex, int[] values) {
            requireBrickIndex(brickIndex);
            requireBrick(values);
            brickPayloads[brickIndex] = encodeBrick(signatures, values);
            return this;
        }

        public Builder setUniformBrick(int brickIndex, int signatureId) {
            requireBrickIndex(brickIndex);
            brickPayloads[brickIndex] = signatures.uniformPayload(signatureId);
            return this;
        }

        public PageSignatures buildBricks() {
            return new PageSignatures(brickPayloads.clone());
        }

        public PageSignatures build() {
            Object[] bricks = new Object[BRICK_COUNT];
            for (int baseBrick = 0; baseBrick < BRICK_COUNT; baseBrick++) {
                int write = 0;
                int minX = (baseBrick & 3) << 2;
                int minZ = (baseBrick >>> 2 & 3) << 2;
                int minY = (baseBrick >>> 4 & 3) << 2;
                for (int y = minY; y < minY + 4; y++) {
                    for (int z = minZ; z < minZ + 4; z++) {
                        int first = y << 8 | z << 4 | minX;
                        for (int x = 0; x < 4; x++) {
                            brick[write++] = values[first + x];
                        }
                    }
                }
                bricks[baseBrick] = encodeBrick(signatures, brick);
            }
            return new PageSignatures(bricks);
        }
    }
}
