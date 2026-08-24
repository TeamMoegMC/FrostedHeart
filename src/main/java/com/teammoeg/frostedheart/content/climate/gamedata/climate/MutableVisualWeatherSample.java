/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
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
