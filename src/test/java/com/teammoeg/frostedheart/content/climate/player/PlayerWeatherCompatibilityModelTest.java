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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerWeatherCompatibilityModelTest {
    @Test
    void ordinarySnowAndBlizzardMapToFiniteDiscreteStates() {
        var clear = PlayerWeatherCompatibilityModel.fromClimate(ClimateType.NONE);
        var snow = PlayerWeatherCompatibilityModel.fromClimate(ClimateType.SNOW);
        var mixedSnow = PlayerWeatherCompatibilityModel.fromClimate(ClimateType.SNOW_BLIZZARD);
        var blizzard = PlayerWeatherCompatibilityModel.fromClimate(ClimateType.BLIZZARD);

        assertFalse(clear.raining());
        assertTrue(snow.raining());
        assertFalse(snow.thundering());
        assertEquals(snow, mixedSnow);
        assertTrue(blizzard.raining());
        assertTrue(blizzard.thundering());
        assertEquals(0.8F, blizzard.rainStrength());
        assertEquals(0.8F, blizzard.thunderStrength());
    }
}
