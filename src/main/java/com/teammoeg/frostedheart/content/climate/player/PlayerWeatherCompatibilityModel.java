/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;

/** Pure mapping from authoritative climate to the low-frequency Vanilla compatibility state. */
public final class PlayerWeatherCompatibilityModel {
    private PlayerWeatherCompatibilityModel() {
    }

    public static VanillaWeatherState fromClimate(ClimateType climate) {
        boolean raining = climate != null && climate.isSnowyOrBlizzard();
        boolean thundering = climate != null && climate.isBlizzard();
        return new VanillaWeatherState(raining, thundering,
                raining ? 0.8F : 0.0F, thundering ? 0.8F : 0.0F);
    }

    public record VanillaWeatherState(boolean raining, boolean thundering,
                                      float rainStrength, float thunderStrength) {
    }
}
