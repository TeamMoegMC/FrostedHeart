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

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.render.weather.ClientWeatherState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

/**
 * The ice optical depth the calculated sky draws. With iceFollowsWeather it follows IceWeatherLaw from the climate the
 * server sends for the player's position, smoothed over the client's climate clock, and otherwise it is the configured
 * depth. Render thread only.
 */
@OnlyIn(Dist.CLIENT)
public final class IceWeatherDepth {
    private static final double SECONDS_PER_CLIMATE_HOUR = 50.0;

    private static double depth = Double.NaN;
    private static double targetDepth = Double.NaN;
    private static double updatedSeconds;
    private static boolean snowing;
    private static double hoursUntilSnow = Double.POSITIVE_INFINITY;
    private static double hoursSinceSnow = Double.POSITIVE_INFINITY;

    private IceWeatherDepth() {
    }

    public static boolean followsWeather() {
        return SkySetting.ICE_FOLLOWS_WEATHER.isOn();
    }

    /**
     * Brings the depth up to the client's climate clock for the frame and returns it. Calls with the same clock time
     * return the same depth. A clock that went back sets the depth to the target at once.
     */
    public static double update(float partialTick) {
        if (!followsWeather()) {
            depth = Double.NaN;
            return SkySetting.ICE_OPTICAL_DEPTH.get();
        }
        double seconds = ClientWeatherState.INSTANCE.frameClimateSeconds(partialTick);
        double hours = seconds / SECONDS_PER_CLIMATE_HOUR;
        ClimateType climate = ClientClimateData.climate;
        snowing = climate != null && climate.isSnowyOrBlizzard();
        hoursUntilSnow = ClientClimateData.nextSnowStartHour >= 0
                ? ClientClimateData.nextSnowStartHour - hours : Double.POSITIVE_INFINITY;
        hoursSinceSnow = ClientClimateData.lastSnowEndHour >= 0
                ? hours - ClientClimateData.lastSnowEndHour : Double.POSITIVE_INFINITY;
        targetDepth = IceWeatherLaw.targetDepth(snowing, hoursUntilSnow, hoursSinceSnow, ClientClimateData.humidity);
        if (Double.isNaN(depth) || seconds < updatedSeconds) {
            depth = targetDepth;
        } else {
            depth = IceWeatherLaw.smoothedDepth(depth, targetDepth, seconds - updatedSeconds);
        }
        updatedSeconds = seconds;
        return depth;
    }

    /**
     * A line about the depth as the last update left it, for the client command.
     */
    public static String describe(float partialTick) {
        double drawn = update(partialTick);
        if (!followsWeather()) {
            return String.format(Locale.ROOT, "Ice optical depth %.3f, fixed by iceOpticalDepth", drawn);
        }
        String nextSnow = Double.isInfinite(hoursUntilSnow)
                ? "no snow in the next " + WorldClimate.SNOW_SEARCH_HOURS + " h"
                : String.format(Locale.ROOT, "next snow in %.2f h", hoursUntilSnow);
        String lastSnow = Double.isInfinite(hoursSinceSnow)
                ? "no snow in the last " + WorldClimate.SNOW_SEARCH_HOURS + " h"
                : String.format(Locale.ROOT, "last snow ended %.2f h ago", hoursSinceSnow);
        return String.format(Locale.ROOT,
                "Ice optical depth %.3f toward %.3f, following the weather: snowing %s, %s, %s, humidity %.1f",
                drawn, targetDepth, snowing, nextSnow, lastSnow, ClientClimateData.humidity);
    }
}
