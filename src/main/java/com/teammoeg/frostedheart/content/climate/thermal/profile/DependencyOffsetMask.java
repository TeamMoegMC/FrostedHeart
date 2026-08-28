/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Immutable resolver dependency mask inside the target block's 3x3x3 cube.
 * The target block is always included because every thermal signature is at
 * least a function of its own block and fluid state.
 */
public final class DependencyOffsetMask {
    public static final int MAXIMUM_OFFSET_COUNT = 27;
    private static final int VALID_BITS = (1 << MAXIMUM_OFFSET_COUNT) - 1;

    public static final Offset SELF = new Offset(0, 0, 0);
    public static final DependencyOffsetMask SELF_ONLY = explicit(List.of());
    public static final DependencyOffsetMask NEIGHBOR_6 = createNeighbor6();
    public static final DependencyOffsetMask NEIGHBOR_26 = new DependencyOffsetMask(VALID_BITS);

    private final int encodedBits;
    private final List<Offset> offsets;

    private DependencyOffsetMask(int encodedBits) {
        this.encodedBits = encodedBits;
        List<Offset> decoded = new ArrayList<>(Integer.bitCount(encodedBits));
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    Offset offset = new Offset(x, y, z);
                    if ((encodedBits & bit(offset)) != 0) {
                        decoded.add(offset);
                    }
                }
            }
        }
        this.offsets = List.copyOf(decoded);
    }

    /** Creates an explicit finite neighbor subset, with {@link #SELF} added implicitly. */
    public static DependencyOffsetMask explicit(Offset... neighborOffsets) {
        Objects.requireNonNull(neighborOffsets, "neighborOffsets");
        return explicit(List.of(neighborOffsets));
    }

    /** Creates an explicit finite neighbor subset, with {@link #SELF} added implicitly. */
    public static DependencyOffsetMask explicit(Collection<Offset> neighborOffsets) {
        Objects.requireNonNull(neighborOffsets, "neighborOffsets");
        int bits = bit(SELF);
        for (Offset offset : neighborOffsets) {
            bits |= bit(Objects.requireNonNull(offset, "neighborOffsets must not contain null"));
        }
        return new DependencyOffsetMask(bits);
    }

    public boolean contains(Offset offset) {
        Objects.requireNonNull(offset, "offset");
        return (encodedBits & bit(offset)) != 0;
    }

    public boolean contains(int x, int y, int z) {
        if (!Offset.isInRange(x, y, z)) {
            return false;
        }
        return (encodedBits & bit(x, y, z)) != 0;
    }

    public int offsetCount() {
        return offsets.size();
    }

    /** Returns offsets in stable y/z/x bit order. */
    public List<Offset> offsets() {
        return offsets;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof DependencyOffsetMask mask && encodedBits == mask.encodedBits;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(encodedBits);
    }

    @Override
    public String toString() {
        return "DependencyOffsetMask" + offsets;
    }

    private static DependencyOffsetMask createNeighbor6() {
        List<Offset> offsets = new ArrayList<>(6);
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 1) {
                        offsets.add(new Offset(x, y, z));
                    }
                }
            }
        }
        return explicit(offsets);
    }

    private static int bit(Offset offset) {
        return bit(offset.x(), offset.y(), offset.z());
    }

    private static int bit(int x, int y, int z) {
        int index = ((y + 1) * 9) + ((z + 1) * 3) + x + 1;
        return 1 << index;
    }

    /** Relative block coordinate constrained to one block in each direction. */
    public record Offset(int x, int y, int z) {
        public Offset {
            if (!isInRange(x, y, z)) {
                throw new IllegalArgumentException("dependency offset must be inside [-1, 1]^3");
            }
        }

        static boolean isInRange(int x, int y, int z) {
            return x >= -1 && x <= 1
                    && y >= -1 && y <= 1
                    && z >= -1 && z <= 1;
        }
    }
}
