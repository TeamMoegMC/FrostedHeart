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

/**
 * Geometry of a station ring before any section is placed.
 * <p>
 * The station is a flat ring. Layers are concentric circles from the inside ({@code layer == 0}) to the outside
 * ({@code layer == layerCount - 1}). A layer at {@link #layerRadius(int) radius} {@code r} has circumference
 * {@code 2πr}; the layer holds {@code floor(2πr / sectionArcLength)} sections, so an outer layer always has at least
 * as many slots as an inner one. Sections are laid out by arc length, not by a fixed count, which keeps their
 * footprint comparable between layers.
 * <p>
 * Coordinates are station-local blocks: the ring centre is {@code (0, 0, 0)}, {@code x}/{@code z} lie in the ring
 * plane and {@code y} is the deck level of the ring. World placement is a future concern; this record only builds
 * the local coordinate system.
 *
 * @param innerRadius      distance in blocks from the ring centre to the centreline of the innermost layer
 * @param layerCount       number of concentric layers, at least {@code 1}
 * @param layerDepth       radial distance in blocks between the centrelines of two adjacent layers
 * @param sectionArcLength target arc length in blocks occupied by one section
 * @param phaseRadians     rotation of the first slot centre around the ring, in radians
 */
public record StationShape(int innerRadius, int layerCount, int layerDepth, double sectionArcLength,
                           double phaseRadians) {
    public static final double TAU = Math.PI * 2.0;

    public StationShape {
        if (innerRadius < 1) {
            throw new IllegalArgumentException("innerRadius must be positive, was " + innerRadius);
        }
        if (layerCount < 1) {
            throw new IllegalArgumentException("layerCount must be positive, was " + layerCount);
        }
        if (layerDepth < 1) {
            throw new IllegalArgumentException("layerDepth must be positive, was " + layerDepth);
        }
        if (!Double.isFinite(sectionArcLength) || sectionArcLength <= 0.0D) {
            throw new IllegalArgumentException("sectionArcLength must be finite and positive, was " + sectionArcLength);
        }
        if (!Double.isFinite(phaseRadians)) {
            throw new IllegalArgumentException("phaseRadians must be finite, was " + phaseRadians);
        }
    }

    /**
     * Creates a ring whose first slot starts at angle zero.
     */
    public static StationShape of(int innerRadius, int layerCount, int layerDepth, double sectionArcLength) {
        return new StationShape(innerRadius, layerCount, layerDepth, sectionArcLength, 0.0D);
    }

    /**
     * Centreline radius of {@code layer}, in blocks.
     */
    public int layerRadius(int layer) {
        checkLayer(layer);
        return innerRadius + layer * layerDepth;
    }

    /**
     * Radius of the outermost layer, in blocks.
     */
    public int outerRadius() {
        return layerRadius(layerCount - 1);
    }

    /**
     * Circumference of {@code layer}, in blocks.
     */
    public double layerCircumference(int layer) {
        return TAU * layerRadius(layer);
    }

    /**
     * Number of sections that fit on {@code layer}.
     * <p>
     * Always at least one so a tiny or short-arc ring still has a valid layer. The result is derived from the
     * circumference and never stored separately.
     */
    public int sectionCount(int layer) {
        return Math.max(1, (int) Math.floor(layerCircumference(layer) / sectionArcLength));
    }

    /**
     * Total number of section slots across every layer.
     */
    public int totalSectionCount() {
        int total = 0;
        for (int layer = 0; layer < layerCount; layer++) {
            total += sectionCount(layer);
        }
        return total;
    }

    private void checkLayer(int layer) {
        if (layer < 0 || layer >= layerCount) {
            throw new IllegalArgumentException("layer out of range [0," + layerCount + "): " + layer);
        }
    }
}
