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
 * How the calculated sky dims vanilla's stars and moon, and the floor its night sky does not go below.
 * Stars lose their light to the ice crystals on their way, and fade as the sky overhead brightens. The moon's sprite
 * takes the display of the sun's square at the moon's place, so it dims through ice and dims and reddens toward the
 * horizon as the sun's square does. The floor is a display value tinted like moonlight.
 */
public final class NightSkyDimming {
    /**
     * Sine of the elevation of 45 degrees, at which the stars' path through the ice is taken.
     */
    static final double STAR_PATH_SINE = Math.sqrt(0.5);
    /**
     * Height of the direction above the horizon over which the sky fades into the fog, as HORIZON_FADE in
     * calculated_sky.fsh.
     */
    static final double HORIZON_FADE = 0.08;
    private static final double[] LUMINANCE_WEIGHTS = {0.2126, 0.7152, 0.0722};

    private NightSkyDimming() {
    }

    /**
     * The factor on vanilla's star brightness of ClientLevel.getStarBrightness. Vanilla adds the square of that
     * brightness, so the factor is the square root of the share of starlight that shows. That share is the
     * transmittance through the ice optical depth along a path at 45 degrees, times a twilight share that falls from 1
     * with a black sky overhead to 0 where the sky overhead displays the twilight display value.
     */
    public static double starScale(double iceOpticalDepth, double zenithDisplay, double twilightDisplay) {
        double transmittance = Math.exp(-Math.max(0.0, iceOpticalDepth) / STAR_PATH_SINE);
        double twilight = Math.max(0.0, 1.0 - Math.max(0.0, zenithDisplay) / Math.max(1.0e-6, twilightDisplay));
        return Math.sqrt(transmittance * twilight);
    }

    /**
     * Writes the factor on each channel of the moon's sprite into the result: the display value of the sun's square
     * through the tables at the moon's elevation, blended by the depth weight as calculated_sky.fsh blends it, and
     * faded into the fog near the horizon as the shader fades the sun. It is 0 while the moon is below the horizon.
     */
    public static void moonColor(SkyTables lowDepthTables, SkyTables highDepthTables, double depthWeight,
            double moonElevationRadians, double exposure, float[] result) {
        double horizonFade = smoothStep(Math.sin(moonElevationRadians) / HORIZON_FADE);
        for (int channel = 0; channel < result.length; channel++) {
            double low = SkyView.sampleRow(lowDepthTables.sunRadiance(), channel, moonElevationRadians);
            double high = SkyView.sampleRow(highDepthTables.sunRadiance(), channel, moonElevationRadians);
            double radiance = low + (high - low) * depthWeight;
            result[channel] = (float) (SkyDisplay.displayValue(exposure, radiance) * horizonFade);
        }
    }

    /**
     * Writes the floor of the night sky into the result as a linear value per channel, for calculated_sky.fsh and
     * SkyViewSet.horizonDisplayColor to lift the tone mapped sky by. The floor display value is the displayed
     * luminance, and the tint is the moonlight's color for the blue shift.
     */
    public static void floorColor(double floorDisplay, double blueShift, double[] result) {
        double tintLuminance = 0.0;
        for (int channel = 0; channel < result.length; channel++) {
            tintLuminance += LUMINANCE_WEIGHTS[channel] * MoonLight.color(channel, blueShift);
        }
        double floor = SkyDisplay.decodeSrgb(floorDisplay);
        for (int channel = 0; channel < result.length; channel++) {
            result[channel] = Math.min(1.0, floor * MoonLight.color(channel, blueShift) / tintLuminance);
        }
    }

    /**
     * The luma of a displayed color, the luminance weights of linear sRGB applied to the encoded channels.
     */
    public static double displayLuminance(double red, double green, double blue) {
        return LUMINANCE_WEIGHTS[0] * red + LUMINANCE_WEIGHTS[1] * green + LUMINANCE_WEIGHTS[2] * blue;
    }

    private static double smoothStep(double fraction) {
        if (fraction <= 0.0) {
            return 0.0;
        }
        if (fraction >= 1.0) {
            return 1.0;
        }
        return fraction * fraction * (3.0 - 2.0 * fraction);
    }
}
