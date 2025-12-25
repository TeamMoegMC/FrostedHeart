/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.TemperatureFrame;

public class ClientClimateData {
    public static final TemperatureFrame[] forecastData = new TemperatureFrame[40];
    public static ClimateType climate;
    public static ClimateType lastClimate;//store last climate for transition
    public static long climateChange;//store climate change time for transition
    public static long secs = 0;
    public static int wind = 0;
    public static float humidity = 0;

    public static void clear() {
        secs = 0;
        Arrays.fill(forecastData, null);
        climate = ClimateType.NONE;
        lastClimate = ClimateType.NONE;
        climateChange = -1;
        wind = 0;
        humidity = 0;
    }

    public static long getDate() {
        return (secs / 50) / 24;
    }

    public static int getHourInDay() {
        return (int) ((secs / 50) % 24);
    }

    public static long getHours() {
        return (secs / 50);
    }

    public static long getMonth() {
        return (secs / 50) / 24 / 30;
    }

    public static long getTimeSecs() {
        return secs;
    }

    public static int getWind() {
        return wind;
    }

    public static float getHumidity() {
        return humidity;
    }

    public ClientClimateData() {
    }
}
