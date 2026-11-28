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

package com.teammoeg.frostedheart.content.steamenergy.charger;

/**
 * Converts station points into item points. The charger has already converted incoming FH network
 * heat into station points; price is the station points owed per point stored in this item.
 */
public final class ChargerTransfer {
    private ChargerTransfer() {}

    /**
     * Returns the highest whole stored point reachable with the available station points.
     * Existing fractional charge counts toward the next point; no mutation is made here.
     */
    public static int wholePointsAfterCharge(double stored, int capacity, int price, float available) {
        if (!(stored >= 0D) || !(available > 0F) || !Float.isFinite(available)) return 0;
        return (int) Math.min(capacity, stored + (double) available / price);
    }

    /** Exact net cost of a successful whole-point top-up, in the station's float storage format. */
    public static float stationDebit(double before, int after, int price) {
        return (float) ((after - before) * price);
    }
}
