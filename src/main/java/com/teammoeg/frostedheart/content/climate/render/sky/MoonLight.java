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
 * Chooses the light of the calculated sky. The sun lights it while it stands at or above the switch elevation, and the
 * moon, exactly opposite, while the sun is below it. The sun fades out over SUN_FADE_DEGREES above the switch and the
 * moon fades in over the fade degrees below it, so the sky is black at the switch and needs no views for both lights.
 * The moon's light is a share of the sun's irradiance, dimmed by the phase and tinted between the moon's own color and
 * the blue that the dark adapted eye sees.
 */
public final class MoonLight {
    /**
     * Degrees above the switch elevation over which the sun's light fades out.
     */
    public static final double SUN_FADE_DEGREES = 3.0;
    public static final int PHASE_COUNT = 8;
    private static final double PHASE_ANGLE_DEGREES_PER_PHASE = 45.0;
    /**
     * Moonlight relative to sunlight in the sky's channels near full moon, from the lunar reflectance of Kieffer and
     * Stone 2005.
     */
    private static final double[] OWN_COLOR = {1.20, 1.00, 0.82};
    /**
     * The hue of night vision, chromaticity (0.25, 0.25) in Jensen et al. 2000, as linear sRGB of luminance 1.
     */
    private static final double[] BLUE_COLOR = {0.706, 0.990, 1.966};

    private MoonLight() {
    }

    /**
     * Sets the light for the angle of the time of day, which is 2 pi times DimensionType.timeOfDay, and the moon phase
     * of DimensionType.moonPhase. The brightness is the full moon's irradiance as a share of the sun's.
     */
    public static void chooseLight(SkyLight light, double timeOfDayAngle, int moonPhase, double switchDegrees,
            double fadeDegrees, double brightness, double blueShift) {
        double sunElevationDegrees = Math.toDegrees(Math.asin(Math.cos(timeOfDayAngle)));
        if (sunElevationDegrees >= switchDegrees) {
            light.set(SkyLight.Kind.SUN, timeOfDayAngle, sunFade(sunElevationDegrees, switchDegrees));
            return;
        }
        double scale = brightness * phaseBrightness(moonPhase)
                * moonFade(sunElevationDegrees, switchDegrees, fadeDegrees);
        light.set(SkyLight.Kind.MOON, timeOfDayAngle, scale, color(0, blueShift), color(1, blueShift),
                color(2, blueShift));
    }

    /**
     * The share of the sun's light that shows, 0 at the switch elevation and 1 from SUN_FADE_DEGREES above it.
     */
    public static double sunFade(double sunElevationDegrees, double switchDegrees) {
        return smoothStep((sunElevationDegrees - switchDegrees) / SUN_FADE_DEGREES);
    }

    /**
     * The share of the moon's light that shows, 0 with the sun at the switch elevation and 1 from the fade degrees
     * below it. A fade of 0 shows all of it below the switch.
     */
    public static double moonFade(double sunElevationDegrees, double switchDegrees, double fadeDegrees) {
        if (fadeDegrees <= 0.0) {
            return sunElevationDegrees < switchDegrees ? 1.0 : 0.0;
        }
        return smoothStep((switchDegrees - sunElevationDegrees) / fadeDegrees);
    }

    /**
     * The moon's brightness relative to the full moon for a phase from 0 (full) to 7, with 4 the new moon.
     * The magnitude follows the phase angle, 45 degrees per phase, by Krisciunas and Schaefer 1991, which leaves out
     * the brightening within a few degrees of full.
     */
    public static double phaseBrightness(int moonPhase) {
        int phase = Math.floorMod(moonPhase, PHASE_COUNT);
        double phaseAngleDegrees = PHASE_ANGLE_DEGREES_PER_PHASE * Math.min(phase, PHASE_COUNT - phase);
        double magnitude = 0.026 * phaseAngleDegrees + 4.0e-9 * Math.pow(phaseAngleDegrees, 4.0);
        return Math.pow(10.0, -0.4 * magnitude);
    }

    /**
     * The moon's color in a channel, from its own color at a blue shift of 0 to the blue of night vision at 1.
     */
    public static double color(int channel, double blueShift) {
        double shift = Math.max(0.0, Math.min(1.0, blueShift));
        return OWN_COLOR[channel] + (BLUE_COLOR[channel] - OWN_COLOR[channel]) * shift;
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
