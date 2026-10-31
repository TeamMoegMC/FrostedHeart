/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

/** Loaded-only neighboring geometry. These positions own no solver slots. */
public record GeometryHalo(long[] positions, int[] signatures) {
    public static final GeometryHalo EMPTY = new GeometryHalo(new long[0], new int[0]);
    public int size() { return positions.length; }
    /** Bounds distinct Sections without allocating a temporary set before scratch admission. */
    public long sectionCountBound() {
        if (positions.length == 0) return 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long position : positions) {
            int x = net.minecraft.core.BlockPos.getX(position) >> 4;
            int y = net.minecraft.core.BlockPos.getY(position) >> 4;
            int z = net.minecraft.core.BlockPos.getZ(position) >> 4;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        long count = Math.min(positions.length, (long) maxX - minX + 1);
        count = Math.min(positions.length, count * ((long) maxY - minY + 1));
        return Math.min(positions.length, count * ((long) maxZ - minZ + 1));
    }
}
