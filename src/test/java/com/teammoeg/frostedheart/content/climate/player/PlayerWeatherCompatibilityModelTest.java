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

package com.teammoeg.frostedheart.content.climate.player;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;

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
