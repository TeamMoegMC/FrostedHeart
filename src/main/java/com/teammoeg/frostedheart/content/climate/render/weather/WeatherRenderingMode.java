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
