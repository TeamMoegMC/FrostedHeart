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

/**
 * World-stable identity of one axis-aligned thermal face patch.
 *
 * <p>The packed coordinate is the minimum world corner of the patch on its
 * interface plane. The axis is kept separately because equal corners on
 * perpendicular interfaces are different physical patches.</p>
 */
public record CanonicalFacePatchKey(
        FacePatchIterator.Axis axis,
        long packedWorldCoordinate
) implements Comparable<CanonicalFacePatchKey> {
    private static final int HORIZONTAL_BITS = 26;
    private static final int VERTICAL_BITS = 12;
    private static final int Z_SHIFT = VERTICAL_BITS;
    private static final int X_SHIFT = HORIZONTAL_BITS + VERTICAL_BITS;
    private static final long HORIZONTAL_MASK = (1L << HORIZONTAL_BITS) - 1L;
    private static final long VERTICAL_MASK = (1L << VERTICAL_BITS) - 1L;

    public static final int MIN_HORIZONTAL_COORDINATE = -(1 << (HORIZONTAL_BITS - 1));
    public static final int MAX_HORIZONTAL_COORDINATE = (1 << (HORIZONTAL_BITS - 1)) - 1;
    public static final int MIN_VERTICAL_COORDINATE = -(1 << (VERTICAL_BITS - 1));
    public static final int MAX_VERTICAL_COORDINATE = (1 << (VERTICAL_BITS - 1)) - 1;

    public CanonicalFacePatchKey {
        if (axis == null) {
            throw new IllegalArgumentException("axis is required");
        }
    }

    public static CanonicalFacePatchKey of(
            FacePatchIterator.Axis axis,
            int worldX,
            int worldY,
            int worldZ
    ) {
        if (axis == null) {
            throw new IllegalArgumentException("axis is required");
        }
        requirePackableCoordinate(worldX, worldY, worldZ);
        long packed = ((long) worldX & HORIZONTAL_MASK) << X_SHIFT
                | ((long) worldZ & HORIZONTAL_MASK) << Z_SHIFT
                | ((long) worldY & VERTICAL_MASK);
        return new CanonicalFacePatchKey(axis, packed);
    }

    public int worldX() {
        return unpackSigned(packedWorldCoordinate, X_SHIFT, HORIZONTAL_BITS);
    }

    public int worldY() {
        return unpackSigned(packedWorldCoordinate, 0, VERTICAL_BITS);
    }

    public int worldZ() {
        return unpackSigned(packedWorldCoordinate, Z_SHIFT, HORIZONTAL_BITS);
    }

    @Override
    public int compareTo(CanonicalFacePatchKey other) {
        if (other == null) {
            throw new IllegalArgumentException("other key is required");
        }
        int comparison = Integer.compare(axis.ordinal(), other.axis.ordinal());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(worldX(), other.worldX());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(worldY(), other.worldY());
        if (comparison != 0) {
            return comparison;
        }
        return Integer.compare(worldZ(), other.worldZ());
    }

    static void requirePackableCoordinate(int worldX, int worldY, int worldZ) {
        requireRange("worldX", worldX, MIN_HORIZONTAL_COORDINATE, MAX_HORIZONTAL_COORDINATE);
        requireRange("worldY", worldY, MIN_VERTICAL_COORDINATE, MAX_VERTICAL_COORDINATE);
        requireRange("worldZ", worldZ, MIN_HORIZONTAL_COORDINATE, MAX_HORIZONTAL_COORDINATE);
    }

    private static int unpackSigned(long packed, int shift, int bits) {
        int leftShift = Long.SIZE - shift - bits;
        return (int) (packed << leftShift >> (Long.SIZE - bits));
    }

    private static void requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be within [" + minimum + ", " + maximum + "]");
        }
    }
}
