/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.chorda.util.noise;

/**
 * Important to note: all distances are concerning square distances!
 */
public enum CellularNoiseType {
    VALUE,
    DISTANCE,
    DISTANCE_2,
    DISTANCE_SUM,
    DISTANCE_DIFFERENCE,
    DISTANCE_PRODUCT,
    DISTANCE_QUOTIENT,
    OTHER;

    public float calculate(float distance0, float distance1, int closestHash) {
        switch (this) {
            case VALUE:
                return closestHash * (1 / 2147483648.0f);
            case DISTANCE:
                return distance0;
            case DISTANCE_2:
                return distance1;
            case DISTANCE_SUM:
                return (distance1 + distance0) * 0.5f - 1;
            case DISTANCE_DIFFERENCE:
                return distance1 - distance0 - 1;
            case DISTANCE_PRODUCT:
                return distance1 * distance0 * 0.5f - 1;
            case DISTANCE_QUOTIENT:
                return distance0 / distance1 - 1;
            default:
                return 0;
        }
    }
}
