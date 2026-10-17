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

package com.teammoeg.frostedheart.content.space.station;

import net.minecraft.core.Vec3i;

/**
 * One section position on a station ring.
 * <p>
 * A slot is identified by its layer and its index inside that layer. The index is ordered by increasing angle, so
 * walking a layer means walking one contiguous ring. The anchor is the rounded block position of the slot centre at
 * deck level {@code y = 0}; it is the position a section is placed at.
 *
 * @param layer             layer index, {@code 0} is the innermost ring
 * @param index             slot index inside the layer, in {@code [0, layerSectionCount)}
 * @param layerSectionCount number of slots on this layer
 * @param angleRadians      angle of the slot centre, in {@code [0, 2π)}
 * @param centerX           exact centreline x coordinate, in blocks
 * @param centerZ           exact centreline z coordinate, in blocks
 * @param anchor            rounded block position of the slot centre
 */
public record StationSlot(int layer, int index, int layerSectionCount, double angleRadians, double centerX,
                          double centerZ, Vec3i anchor) {
    public StationSlot {
        if (layer < 0) {
            throw new IllegalArgumentException("layer must not be negative, was " + layer);
        }
        if (layerSectionCount < 1) {
            throw new IllegalArgumentException("layerSectionCount must be positive, was " + layerSectionCount);
        }
        if (index < 0 || index >= layerSectionCount) {
            throw new IllegalArgumentException(
                    "index out of range [0," + layerSectionCount + "): " + index);
        }
        if (!Double.isFinite(angleRadians) || !Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("slot coordinates must be finite");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("anchor must not be null");
        }
    }

    /**
     * Exact distance of the slot centre from the ring centre, in blocks.
     */
    public double centerRadius() {
        return Math.hypot(centerX, centerZ);
    }

    /**
     * Anchor position at the given deck height.
     */
    public Vec3i anchorAt(int y) {
        return new Vec3i(anchor.getX(), y, anchor.getZ());
    }
}
