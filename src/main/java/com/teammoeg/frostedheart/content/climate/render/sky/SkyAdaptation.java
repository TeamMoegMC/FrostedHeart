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

/**
 * The exposure of the calculated sky as the eye adapts to the sky's luminance.
 * The key of Krawczyk, Myszkowski, and Seidel 2005, a(L) = 1.03 - 2 / (2 + log10(L + 1)), gives the shape. The
 * exposure is the day exposure at and above the day luminance and the night exposure at and below the night
 * luminance, and between them it goes from one to the other as the logarithm of a(L) / L does.
 */
public final class SkyAdaptation {
    /**
     * Luminance in candela per square meter of a radiance of 1, the sun's illuminance above the air of about 128000
     * lux per steradian.
     */
    public static final double CANDELA_PER_RADIANCE = 1.28e5;
    private static final double[] LUMINANCE_WEIGHTS = {0.2126, 0.7152, 0.0722};

    private SkyAdaptation() {
    }

    /**
     * The luminance in candela per square meter of a linear radiance per channel.
     */
    public static double luminance(double red, double green, double blue) {
        return CANDELA_PER_RADIANCE
                * (LUMINANCE_WEIGHTS[0] * red + LUMINANCE_WEIGHTS[1] * green + LUMINANCE_WEIGHTS[2] * blue);
    }

    /**
     * The key of Krawczyk, Myszkowski, and Seidel 2005 for a luminance in candela per square meter.
     */
    public static double key(double luminance) {
        return 1.03 - 2.0 / (2.0 + Math.log10(Math.max(0.0, luminance) + 1.0));
    }

    /**
     * The exposure for a sky of the given luminance in candela per square meter. Limits given the wrong way round
     * are swapped, and a luminance that is not a positive number gives the night exposure.
     */
    public static double exposure(double luminance, double dayExposure, double nightExposure, double dayLuminance,
            double nightLuminance) {
        double upperLuminance = Math.max(dayLuminance, nightLuminance);
        double lowerLuminance = Math.min(dayLuminance, nightLuminance);
        if (luminance >= upperLuminance) {
            return dayExposure;
        }
        if (!(luminance > lowerLuminance) || !(luminance > 0.0)) {
            return nightExposure;
        }
        double dayLog = logKeyOverLuminance(upperLuminance);
        double nightLog = logKeyOverLuminance(lowerLuminance);
        double share = (logKeyOverLuminance(luminance) - dayLog) / (nightLog - dayLog);
        return dayExposure * Math.pow(nightExposure / dayExposure, share);
    }

    /**
     * The exposure while the eye does not adapt, the day exposure under the sun and the night exposure under the
     * moon. The sun's light fades out above the switch to the moon, so the change of exposure does not show.
     */
    public static double fixedExposure(SkyLight.Kind kind, double dayExposure, double nightExposure) {
        return kind == SkyLight.Kind.MOON ? nightExposure : dayExposure;
    }

    private static double logKeyOverLuminance(double luminance) {
        return Math.log(key(luminance)) - Math.log(luminance);
    }
}
