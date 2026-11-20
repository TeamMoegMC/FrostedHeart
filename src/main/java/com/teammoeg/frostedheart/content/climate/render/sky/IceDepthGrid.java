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

package com.teammoeg.frostedheart.content.climate.render.sky;

/**
 * Ice optical depths for which the calculated sky builds tables. The sky for any other depth blends the tables of the
 * two grid depths around it, linearly in depth, so the sky changes smoothly as the depth changes.
 * Grid depths are SCALE * (exp(index * STEP) - 1), close to even steps of SCALE * STEP below SCALE and to steps of a
 * constant ratio exp(STEP) above it. Index 0 is depth 0 and index CELL_COUNT is the maximum of
 * SkySetting.ICE_OPTICAL_DEPTH.
 */
public final class IceDepthGrid {
    public static final double STEP = 0.2;
    public static final int CELL_COUNT = 21;
    public static final double SCALE = SkySetting.ICE_OPTICAL_DEPTH.maximum() / Math.expm1(CELL_COUNT * STEP);
    /**
     * Added to the grid coordinate before rounding down, so that a grid depth maps to its own index despite rounding.
     */
    private static final double INDEX_ROUNDING = 1.0e-9;
    /**
     * The highest index that blends with the next one.
     */
    public static final int HIGHEST_LOW_INDEX = CELL_COUNT - 1;

    private IceDepthGrid() {
    }

    public static double depth(int index) {
        return SCALE * Math.expm1(index * STEP);
    }

    /**
     * The index of the grid depth at or below the given depth, clamped so that the next index is still on the grid.
     */
    public static int index(double depth) {
        int index = (int) Math.floor(Math.log1p(Math.max(0.0, depth) / SCALE) / STEP + INDEX_ROUNDING);
        return Math.max(0, Math.min(HIGHEST_LOW_INDEX, index));
    }

    /**
     * How far the depth is from the grid depth of the index toward the next one, clamped to the range from 0 to 1.
     */
    public static double blendWeight(double depth, int index) {
        double low = depth(index);
        double high = depth(index + 1);
        return Math.max(0.0, Math.min(1.0, (depth - low) / (high - low)));
    }
}
