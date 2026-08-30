/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Arrays;

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

    public int get(int blockIndex) {
        requireBlockIndex(blockIndex);
        int brick = brickIndex(blockIndex);
        return valueAt(bricks[brick], indexWithinBrick(blockIndex));
    }

    public PageSignatures withBricks(int[] baseBrickIndexes, int[][] brickValues) {
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
            next[brick] = encodeBrick(brickValues[index]);
        }
        return new PageSignatures(next);
    }

    /** Immutable internal payload shared with worker and Page publication. */
    public Object brickPayload(int baseBrickIndex) {
        requireBrickIndex(baseBrickIndex);
        return bricks[baseBrickIndex];
    }

    static int valueAt(Object payload, int index) {
        if (payload instanceof char[] compact) {
            return compact[compact.length == 1 ? 0 : index]
                    - ENCODE_OFFSET;
        }
        int[] wide = (int[]) payload;
        return wide[wide.length == 1 ? 0 : index];
    }

    private static Object encodeBrick(int[] values) {
        int first = values[0];
        boolean uniform = true;
        boolean compact = fitsCompact(first);
        for (int index = 1; index < values.length; index++) {
            int value = values[index];
            uniform &= value == first;
            compact &= fitsCompact(value);
        }
        if (uniform) {
            return compact
                    ? new char[]{encode(first)}
                    : new int[]{first};
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
        private final int[] values = new int[ENTRY_COUNT];

        public Builder set(int blockIndex, int value) {
            requireBlockIndex(blockIndex);
            values[blockIndex] = value;
            return this;
        }

        public PageSignatures build() {
            Object[] bricks = new Object[BRICK_COUNT];
            int[] brick = new int[ENTRIES_PER_BRICK];
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
                bricks[baseBrick] = encodeBrick(brick);
            }
            return new PageSignatures(bricks);
        }
    }
}
