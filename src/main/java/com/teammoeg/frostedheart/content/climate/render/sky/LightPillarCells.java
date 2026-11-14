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
 * The square cells that light pillars are drawn in while SkySetting.PILLAR_TEXEL_DEGREES is above 0, and the atlas
 * that holds their values, as light_pillars.glsl lays them out. Each ribbon has its own grid: columns by the lateral
 * angle from the pillar's axis, with the middle column on the axis, and rows by the elevation up from cellOrigin, so
 * the cells stay on the pillar as the eye moves and on the sky as it turns. The atlas holds both ribbons of
 * SLOTS_ACROSS drawn pillars side by side in each band of rows, the plates first and then the wobbling plates of
 * crystals.csv, in the order LightPillarRenderer draws the pillars, with as many bands as the most pillars drawn need.
 * The shader computes the cells beyond a ribbon's half columns or rows, or beyond the atlas's bands, at each of their
 * pixels instead.
 */
final class LightPillarCells {
    static final int SLOTS_ACROSS = 16;
    static final int PLATE_HALF_COLUMNS = 16;
    static final int WOBBLING_HALF_COLUMNS = 64;
    static final int PAIR_COLUMNS = 2 * PLATE_HALF_COLUMNS + 2 * WOBBLING_HALF_COLUMNS + 2;
    static final int ATLAS_COLUMNS = SLOTS_ACROSS * PAIR_COLUMNS;
    static final int MOST_BANDS = bandsFor(LightPillarSelection.SLOT_COUNT);
    /**
     * The atlas grows by this many bands at a time.
     */
    static final int BAND_STEP = 2;
    /**
     * The most rows of each ribbon, which keeps a band within 1024 texels at small cells.
     */
    static final int MOST_ROWS = 1024;
    /**
     * The tilt in standard deviations of the faces whose mirror image of a light below the eye the cells stand on, as
     * CELL_ORIGIN_SIGMAS in light_pillars.glsl.
     */
    static final double CELL_ORIGIN_SIGMAS = 6.0;

    private LightPillarCells() {
    }

    static int halfColumns(int kind) {
        return kind == 0 ? PLATE_HALF_COLUMNS : WOBBLING_HALF_COLUMNS;
    }

    /**
     * The atlas column of the ribbon's cells on the pillar's axis, for the ribbon index drawn pillar * 2 + plate kind.
     */
    static int axisColumn(int ribbon) {
        int pillar = ribbon / 2;
        int kind = ribbon - 2 * pillar;
        return (pillar % SLOTS_ACROSS) * PAIR_COLUMNS + (kind == 0 ? 0 : 2 * PLATE_HALF_COLUMNS + 1)
                + halfColumns(kind);
    }

    /**
     * The atlas row of the ribbon's bottom row of cells, with the rows of each ribbon.
     */
    static int bottomRow(int ribbon, int rows) {
        return (ribbon / 2 / SLOTS_ACROSS) * rows;
    }

    /**
     * The rows of each ribbon for the side of a cell in radians, from the lowest cell origin of the widest tilt spread
     * up past the highest elevation that ribbons reach, at most MOST_ROWS. The lowest cell origin lies half
     * CELL_ORIGIN_SIGMAS below the horizon.
     */
    static int atlasRows(double cellAngle, double widestTiltSigma) {
        double height = LightPillarOptics.HIGHEST_RIBBON_ELEVATION + 0.5 * CELL_ORIGIN_SIGMAS * widestTiltSigma;
        return (int) Math.min(MOST_ROWS, Math.ceil(height / cellAngle) + 2.0);
    }

    static int atlasHeight(int rows, int bands) {
        return bands * rows;
    }

    /**
     * The bands that hold the ribbons of the count of drawn pillars.
     */
    static int bandsFor(int pillars) {
        return (pillars + SLOTS_ACROSS - 1) / SLOTS_ACROSS;
    }

    /**
     * The bands an atlas of the rows gets when it has to hold the bands needed, from BAND_STEP up in steps of it, at
     * most MOST_BANDS and at most as many as the largest texture side holds, and at least 1.
     */
    static int grownBands(int bandsNeeded, int rows, int largestTextureSide) {
        int stepped = (Math.max(1, bandsNeeded) + BAND_STEP - 1) / BAND_STEP * BAND_STEP;
        int fitting = largestTextureSide / Math.max(1, rows);
        return Math.max(1, Math.min(Math.min(stepped, MOST_BANDS), fitting));
    }

    /**
     * The elevation in radians that the bottom row of cells stands on, the light itself or for a light below the eye
     * the lowest elevation that faces tilted by CELL_ORIGIN_SIGMAS mirror it to.
     */
    static double cellOrigin(double lightElevation, double tiltSigma) {
        return Math.max(lightElevation, -lightElevation - CELL_ORIGIN_SIGMAS * tiltSigma);
    }

    /**
     * The column of the cell at the lateral angle, 0 on the pillar's axis, for the side of a cell in radians.
     */
    static int column(double lateralAngle, double cellAngle) {
        return (int) Math.floor(lateralAngle / cellAngle + 0.5);
    }

    /**
     * The row of the cell at the elevation, below 0 under the cell origin, for the side of a cell in radians.
     */
    static int row(double elevation, double cellOrigin, double cellAngle) {
        return (int) Math.floor((elevation - cellOrigin) / cellAngle);
    }
}
