/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.chunkheat;

/** Integer-lattice spherical heat-field geometry shared by game and simulator. */
public final class SphericalHeatFieldModel {
    private SphericalHeatFieldModel() {
    }

    public static boolean contains(
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            int x,
            int y,
            int z
    ) {
        if (radius < 0) return false;
        long dx = (long) x - centerX;
        long dy = (long) y - centerY;
        long dz = (long) z - centerZ;
        if (dx < -radius || dx > radius
                || dy < -radius || dy > radius
                || dz < -radius || dz > radius) return false;
        return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
    }

    /** Exact number of integer block coordinates inside a sphere, boundary included. */
    public static long latticeVolume(int radius) {
        if (radius < 0) return 0L;
        long count = 0L;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (contains(0, 0, 0, radius, x, y, z)) count++;
                }
            }
        }
        return count;
    }

    /**
     * Largest centered footprint whose every voxel in {@code interiorHeight}
     * consecutive layers is inside the sphere. This is a geometric upper bound,
     * not a buildable floor plan: walls, tower blocks, access and furniture are excluded.
     */
    public static long centeredFootprintUpperBound(int radius, int interiorHeight) {
        if (radius < 0 || interiorHeight <= 0) return 0L;
        int lower = -(interiorHeight - 1) / 2;
        int upper = lower + interiorHeight - 1;
        int maximumAbsoluteY = Math.max(Math.abs(lower), Math.abs(upper));
        long count = 0L;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if ((long) x * x + (long) z * z
                        + (long) maximumAbsoluteY * maximumAbsoluteY
                        <= (long) radius * radius) count++;
            }
        }
        return count;
    }
}
