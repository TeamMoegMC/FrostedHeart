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
 * The extinction of the diamond dust near the ground, per kilometer. With dustFollowsWeather it follows DiamondDustLaw
 * from the climate the server sends for the player's position, smoothed over the client's climate clock, and otherwise
 * it is the configured extinction. Render thread only.
 */
@OnlyIn(Dist.CLIENT)
public final class DiamondDustExtinction {
    private static final double SECONDS_PER_CLIMATE_HOUR = 50.0;

    private static double extinction = Double.NaN;
    private static double targetExtinction = Double.NaN;
    private static double updatedSeconds;
    private static boolean snowing;
    private static double hoursSinceSnow = Double.POSITIVE_INFINITY;
    private static double lastValue;

    private DiamondDustExtinction() {
    }

    /**
     * The extinction the last update returned, or 0 before the first. Code outside the frame reads this instead of
     * calling update, because an update with an earlier clock than the last sets the extinction to its target at once.
     */
    public static double lastValue() {
        return lastValue;
    }

    public static boolean followsWeather() {
        return SkySetting.DUST_FOLLOWS_WEATHER.isOn();
    }

    /**
     * Brings the extinction up to the client's climate clock and returns it. Calls with the same clock time return the
     * same extinction. A clock that went back sets the extinction to the target at once.
     */
    public static double update(float partialTick) {
        if (!followsWeather()) {
            extinction = Double.NaN;
            lastValue = SkySetting.DUST_EXTINCTION.get();
            return lastValue;
        }
        double seconds = ClientWeatherState.INSTANCE.frameClimateSeconds(partialTick);
        double hours = seconds / SECONDS_PER_CLIMATE_HOUR;
        ClimateType climate = ClientClimateData.climate;
        snowing = climate != null && climate.isSnowyOrBlizzard();
        hoursSinceSnow = ClientClimateData.lastSnowEndHour >= 0
                ? hours - ClientClimateData.lastSnowEndHour : Double.POSITIVE_INFINITY;
        targetExtinction = DiamondDustLaw.targetExtinction(snowing, hoursSinceSnow);
        if (Double.isNaN(extinction) || seconds < updatedSeconds) {
            extinction = targetExtinction;
        } else {
            extinction = DiamondDustLaw.smoothedExtinction(extinction, targetExtinction, seconds - updatedSeconds);
        }
        updatedSeconds = seconds;
        lastValue = extinction;
        return extinction;
    }

    /**
     * A line about the extinction as the last update left it, for the client command.
     */
    public static String describe(float partialTick) {
        double current = update(partialTick);
        if (!followsWeather()) {
            return String.format(Locale.ROOT, "Diamond dust extinction %.3f per km, fixed by dustExtinction", current);
        }
        String lastSnow = Double.isInfinite(hoursSinceSnow)
                ? "no snow in the last " + WorldClimate.SNOW_SEARCH_HOURS + " h"
                : String.format(Locale.ROOT, "last snow ended %.2f h ago", hoursSinceSnow);
        return String.format(Locale.ROOT,
                "Diamond dust extinction %.3f per km toward %.3f, following the weather: snowing %s, %s",
                current, targetExtinction, snowing, lastSnow);
    }
}
