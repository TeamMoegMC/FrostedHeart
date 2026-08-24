/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

/** Player-selected weather backend. It never changes automatically due to measured FPS. */
public enum WeatherRenderingMode {
    COMPATIBILITY(null),
    SPATIAL_V1_FAST(WeatherQualityProfile.FAST),
    SPATIAL_V1_FANCY(WeatherQualityProfile.FANCY);

    private final WeatherQualityProfile profile;

    WeatherRenderingMode(WeatherQualityProfile profile) {
        this.profile = profile;
    }

    public boolean isSpatial() {
        return profile != null;
    }

    public WeatherQualityProfile profile() {
        return profile;
    }
}
