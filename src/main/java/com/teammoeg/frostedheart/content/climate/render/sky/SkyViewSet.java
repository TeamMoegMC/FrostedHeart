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

import java.util.Arrays;

/**
 * The four sky views the calculated sky blends, for the two grid ice depths and the two grid sun elevations around the
 * drawn ones. Slot depthEnd * 2 + sunEnd holds the view of the low end (0) or the high end (1) of each, so slot 0 has
 * the low depth and the low sun. The radiance of the views is blended as calculated_sky.fsh blends them, by the high
 * sun's weight between the two sun elevations and then by the high depth's weight between the two depths.
 * The build that creates a set fills every slot, and afterwards the set is only read.
 */
public final class SkyViewSet {
    /**
     * The ends of each grid the views span, the ice depth and the sun elevation.
     */
    public static final int ENDS = 2;
    public static final int VIEW_COUNT = ENDS * ENDS;
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    /**
     * The lowest sky darkness that ClientLevel.getSkyDarken returns, at night.
     */
    private static final float NIGHT_SKY_DARKEN = 0.2F;
    /**
     * A light scale of 1 in every channel. Never written.
     */
    private static final double[] FULL_LIGHT = {1.0, 1.0, 1.0};
    /**
     * A night sky floor of 0 in every channel. Never written.
     */
    private static final double[] NO_FLOOR = {0.0, 0.0, 0.0};

    private final SkyView[] views = new SkyView[VIEW_COUNT];

    SkyViewSet() {
    }

    /**
     * A set that holds the same view in every slot.
     */
    public static SkyViewSet of(SkyView view) {
        SkyViewSet set = new SkyViewSet();
        Arrays.fill(set.views, view);
        return set;
    }

    public static int slot(int depthEnd, int sunEnd) {
        return depthEnd * ENDS + sunEnd;
    }

    public SkyView view(int slot) {
        return views[slot];
    }

    void setView(int slot, SkyView view) {
        views[slot] = view;
    }

    public boolean holds(SkyView view) {
        for (SkyView held : views) {
            if (held == view) {
                return true;
            }
        }
        return false;
    }

    /**
     * The sun elevation of the views at the low sun end, which both depths share.
     */
    public double lowSunElevationRadians() {
        return views[slot(0, 0)].sunElevationRadians();
    }

    /**
     * The sun elevation of the views at the high sun end, which both depths share.
     */
    public double highSunElevationRadians() {
        return views[slot(0, 1)].sunElevationRadians();
    }

    /**
     * Writes the displayed sky color at the horizon into the result for a light at full strength, as the other form
     * does with a light scale of 1 in every channel.
     */
    public void horizonDisplayColor(double sunWeight, double depthWeight, double azimuth, double horizontalFraction,
            double exposure, float rainLevel, float thunderLevel, float[] result) {
        horizonDisplayColor(sunWeight, depthWeight, azimuth, horizontalFraction, exposure, FULL_LIGHT, rainLevel,
                thunderLevel, result);
    }

    /**
     * Writes the displayed sky color at the horizon into the result with no night sky floor, as the other form does.
     */
    public void horizonDisplayColor(double sunWeight, double depthWeight, double azimuth, double horizontalFraction,
            double exposure, double[] lightScale, float rainLevel, float thunderLevel, float[] result) {
        horizonDisplayColor(sunWeight, depthWeight, azimuth, horizontalFraction, exposure, lightScale, NO_FLOOR,
                rainLevel, thunderLevel, result);
    }

    /**
     * Writes the displayed sky color at the horizon into the result, tone mapped, lifted by the night sky floor, and
     * grayed as calculated_sky.fsh does. Single scattering by ice is left out, so the color stays smooth near the sun
     * and across halo arcs. A horizontal fraction of 1 uses the given azimuth, and 0 uses the mean over all azimuths.
     * The light scale multiplies each channel of the radiance before the tone map, as SkyLight.radianceScale gives it.
     * The floor is linear per channel, as NightSkyDimming.floorColor gives it.
     */
    public void horizonDisplayColor(double sunWeight, double depthWeight, double azimuth, double horizontalFraction,
            double exposure, double[] lightScale, double[] floor, float rainLevel, float thunderLevel,
            float[] result) {
        for (int channel = 0; channel < CHANNELS; channel++) {
            double radiance = blend(views[slot(0, 0)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(0, 1)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(1, 0)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(1, 1)].horizonRadiance(channel, azimuth, horizontalFraction), sunWeight, depthWeight);
            double toneMapped = SkyDisplay.toneMap(exposure, radiance * lightScale[channel]);
            result[channel] = (float) SkyDisplay.encodeSrgb(SkyDisplay.withFloor(toneMapped, floor[channel]));
        }
        greyByWeather(result, 0.6F, rainLevel);
        greyByWeather(result, 0.2F, thunderLevel);
    }

    /**
     * The displayed luminance of the sky straight overhead, tone mapped as calculated_sky.fsh does, without the night
     * sky floor and the weather. The ice scatters by the tumbling crystals of the profile at the angle between the
     * zenith and the view's light, but at least 45 degrees, so the light's own glare does not count.
     */
    public double zenithDisplayLuminance(double sunWeight, double depthWeight, double exposure, double[] lightScale,
            IceHaloProfile haloProfile) {
        double red = zenithDisplayValue(0, sunWeight, depthWeight, exposure, lightScale, haloProfile);
        double green = zenithDisplayValue(1, sunWeight, depthWeight, exposure, lightScale, haloProfile);
        double blue = zenithDisplayValue(2, sunWeight, depthWeight, exposure, lightScale, haloProfile);
        return NightSkyDimming.displayLuminance(red, green, blue);
    }

    private double zenithDisplayValue(int channel, double sunWeight, double depthWeight, double exposure,
            double[] lightScale, IceHaloProfile haloProfile) {
        double radiance = blend(zenithRadiance(views[slot(0, 0)], channel, haloProfile),
                zenithRadiance(views[slot(0, 1)], channel, haloProfile),
                zenithRadiance(views[slot(1, 0)], channel, haloProfile),
                zenithRadiance(views[slot(1, 1)], channel, haloProfile), sunWeight, depthWeight);
        return SkyDisplay.displayValue(exposure, radiance * lightScale[channel]);
    }

    private static double zenithRadiance(SkyView view, int channel, IceHaloProfile haloProfile) {
        double zenith = Math.PI / 2.0;
        double lightCosine = Math.min(Math.sin(view.sunElevationRadians()), NightSkyDimming.STAR_PATH_SINE);
        return SkyView.sample(view.sky(), channel, 0.0, zenith)
                + haloProfile.phase(channel, lightCosine, 0.0) * SkyView.sample(view.ice(), channel, 0.0, zenith);
    }

    /**
     * Writes the sky light on the terrain into the result for a light at full strength, as the other form does with a
     * light scale of 1 in every channel.
     */
    public void daylightColor(double sunWeight, double depthWeight, double exposure, float rainLevel, float skyDarken,
            float[] result) {
        daylightColor(sunWeight, depthWeight, exposure, FULL_LIGHT, rainLevel, skyDarken, result);
    }

    /**
     * Writes the sky light on the terrain into the result, as the lightmap color at full sky light.
     * In clear weather it is the ground as the sky's exposure displays it, relative to snow under a sun of unit
     * irradiance at the zenith, scaled down where a channel would pass 1, and at least vanilla's sky light at night.
     * The ground radiance of the views is blended as in horizonDisplayColor, and rain blends the result into vanilla's
     * sky light for the given sky darkness. The light scale multiplies the ground radiance but not the snow it is
     * relative to.
     */
    public void daylightColor(double sunWeight, double depthWeight, double exposure, double[] lightScale,
            float rainLevel, float skyDarken, float[] result) {
        double brightest = SkyDisplay.encodeSrgb(1.0 - Math.exp(-exposure * SkyAtmosphere.GROUND_ALBEDO / Math.PI));
        for (int channel = 0; channel < CHANNELS; channel++) {
            double radiance = blend(views[slot(0, 0)].groundRadiance()[channel],
                    views[slot(0, 1)].groundRadiance()[channel], views[slot(1, 0)].groundRadiance()[channel],
                    views[slot(1, 1)].groundRadiance()[channel], sunWeight, depthWeight);
            result[channel] = (float) SkyDisplay.displayValue(exposure, radiance * lightScale[channel]);
            brightest = Math.max(brightest, result[channel]);
        }
        for (int channel = 0; channel < CHANNELS; channel++) {
            float clear = Math.max((float) (result[channel] / brightest),
                    SkyView.vanillaSkyLight(channel, NIGHT_SKY_DARKEN));
            result[channel] = clear + (SkyView.vanillaSkyLight(channel, skyDarken) - clear) * rainLevel;
        }
    }

    /**
     * The luminance in candela per square meter that the exposure adapts to, the larger of the sky's adaptation
     * radiance and its brightest radiance at the horizon, each blended as horizonDisplayColor blends the radiance and
     * times the light scale.
     */
    public double adaptationLuminance(double sunWeight, double depthWeight, double[] lightScale) {
        return Math.max(skyAdaptationLuminance(sunWeight, depthWeight, lightScale),
                horizonAdaptationLuminance(sunWeight, depthWeight, lightScale));
    }

    /**
     * The luminance in candela per square meter of the views' adaptation radiance, blended and times the light scale.
     */
    public double skyAdaptationLuminance(double sunWeight, double depthWeight, double[] lightScale) {
        double red = blend(views[slot(0, 0)].adaptationRadiance()[0], views[slot(0, 1)].adaptationRadiance()[0],
                views[slot(1, 0)].adaptationRadiance()[0], views[slot(1, 1)].adaptationRadiance()[0], sunWeight,
                depthWeight);
        double green = blend(views[slot(0, 0)].adaptationRadiance()[1], views[slot(0, 1)].adaptationRadiance()[1],
                views[slot(1, 0)].adaptationRadiance()[1], views[slot(1, 1)].adaptationRadiance()[1], sunWeight,
                depthWeight);
        double blue = blend(views[slot(0, 0)].adaptationRadiance()[2], views[slot(0, 1)].adaptationRadiance()[2],
                views[slot(1, 0)].adaptationRadiance()[2], views[slot(1, 1)].adaptationRadiance()[2], sunWeight,
                depthWeight);
        return SkyAdaptation.luminance(red * lightScale[0], green * lightScale[1], blue * lightScale[2]);
    }

    /**
     * The luminance in candela per square meter of the views' brightest radiance at the horizon, blended and times the
     * light scale.
     */
    public double horizonAdaptationLuminance(double sunWeight, double depthWeight, double[] lightScale) {
        double red = blend(views[slot(0, 0)].brightestHorizonRadiance()[0],
                views[slot(0, 1)].brightestHorizonRadiance()[0], views[slot(1, 0)].brightestHorizonRadiance()[0],
                views[slot(1, 1)].brightestHorizonRadiance()[0], sunWeight, depthWeight);
        double green = blend(views[slot(0, 0)].brightestHorizonRadiance()[1],
                views[slot(0, 1)].brightestHorizonRadiance()[1], views[slot(1, 0)].brightestHorizonRadiance()[1],
                views[slot(1, 1)].brightestHorizonRadiance()[1], sunWeight, depthWeight);
        double blue = blend(views[slot(0, 0)].brightestHorizonRadiance()[2],
                views[slot(0, 1)].brightestHorizonRadiance()[2], views[slot(1, 0)].brightestHorizonRadiance()[2],
                views[slot(1, 1)].brightestHorizonRadiance()[2], sunWeight, depthWeight);
        return SkyAdaptation.luminance(red * lightScale[0], green * lightScale[1], blue * lightScale[2]);
    }

    /**
     * Writes the share of the light's beam that reaches the eye into the result, blended as horizonDisplayColor blends
     * the radiance, times the light scale.
     */
    public void lightTransmittance(double sunWeight, double depthWeight, double[] lightScale, double[] result) {
        for (int channel = 0; channel < CHANNELS; channel++) {
            double transmittance = blend(views[slot(0, 0)].lightTransmittance()[channel],
                    views[slot(0, 1)].lightTransmittance()[channel], views[slot(1, 0)].lightTransmittance()[channel],
                    views[slot(1, 1)].lightTransmittance()[channel], sunWeight, depthWeight);
            result[channel] = transmittance * lightScale[channel];
        }
    }

    private static double blend(double lowDepthLowSun, double lowDepthHighSun, double highDepthLowSun,
            double highDepthHighSun, double sunWeight, double depthWeight) {
        double lowDepth = lowDepthLowSun + (lowDepthHighSun - lowDepthLowSun) * sunWeight;
        double highDepth = highDepthLowSun + (highDepthHighSun - highDepthLowSun) * sunWeight;
        return lowDepth + (highDepth - lowDepth) * depthWeight;
    }

    private static void greyByWeather(float[] color, float greyScale, float weatherLevel) {
        float grey = (0.3F * color[0] + 0.59F * color[1] + 0.11F * color[2]) * greyScale;
        float kept = 1.0F - weatherLevel * 0.75F;
        for (int channel = 0; channel < CHANNELS; channel++) {
            color[channel] = grey + (color[channel] - grey) * kept;
        }
    }
}
