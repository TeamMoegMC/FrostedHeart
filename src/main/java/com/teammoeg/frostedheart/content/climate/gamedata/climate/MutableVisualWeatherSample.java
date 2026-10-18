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

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

/** Caller-owned scratch output for allocation-free white-curtain field sampling. */
public final class MutableVisualWeatherSample {
    public boolean insideAffectedCorridor;
    public float signedDistanceToActiveFrontBlocks;
    public float snowIntensity;
    public float whiteoutIntensity;
    public float windIntensity;
    public float windX;
    public float windZ;
    public float visibilityBlocks;

    public MutableVisualWeatherSample clear() {
        insideAffectedCorridor = false;
        signedDistanceToActiveFrontBlocks = Float.POSITIVE_INFINITY;
        snowIntensity = 0.0F;
        whiteoutIntensity = 0.0F;
        windIntensity = 0.0F;
        windX = 0.0F;
        windZ = 0.0F;
        visibilityBlocks = Float.POSITIVE_INFINITY;
        return this;
    }
}
