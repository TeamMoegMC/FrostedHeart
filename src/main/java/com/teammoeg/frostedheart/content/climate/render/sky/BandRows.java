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
 * Rows of a table spaced evenly in a value that grows with altitude, separately below, inside, and above the ice band,
 * so that a thick ice layer gets rows of its own. Row 0 is at value 0, row belowRows at the lower edge of the band,
 * row belowRows + inRows at its upper edge, and the last row at the top value.
 */
final class BandRows {
    private final int belowRows;
    private final int inRows;
    private final int aboveRows;
    private final double low;
    private final double high;
    private final double top;

    BandRows(int rowCount, int belowRows, int inRows, double low, double high, double top) {
        this.belowRows = belowRows;
        this.inRows = inRows;
        this.aboveRows = rowCount - 1 - belowRows - inRows;
        this.low = low;
        this.high = high;
        this.top = top;
    }

    double value(int row) {
        if (row <= belowRows) {
            return low * row / belowRows;
        }
        if (row <= belowRows + inRows) {
            return low + (high - low) * (row - belowRows) / inRows;
        }
        return high + (top - high) * (row - belowRows - inRows) / aboveRows;
    }

    /**
     * The fractional row of a value, the inverse of value, not clamped to the rows of the table.
     */
    double position(double value) {
        if (value <= low) {
            return value / low * belowRows;
        }
        if (value <= high) {
            return belowRows + (value - low) / (high - low) * inRows;
        }
        return belowRows + inRows + (value - high) / (top - high) * aboveRows;
    }
}
