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
     * Writes the displayed sky color at the horizon into the result, tone mapped and grayed as calculated_sky.fsh does.
     * Single scattering by ice is left out, so the color stays smooth near the sun and across halo arcs.
     * A horizontal fraction of 1 uses the given azimuth, and 0 uses the mean over all azimuths.
     * The light scale multiplies each channel of the radiance before the tone map, as SkyLight.radianceScale gives it.
     */
    public void horizonDisplayColor(double sunWeight, double depthWeight, double azimuth, double horizontalFraction,
            double exposure, double[] lightScale, float rainLevel, float thunderLevel, float[] result) {
        for (int channel = 0; channel < CHANNELS; channel++) {
            double radiance = blend(views[slot(0, 0)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(0, 1)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(1, 0)].horizonRadiance(channel, azimuth, horizontalFraction),
                    views[slot(1, 1)].horizonRadiance(channel, azimuth, horizontalFraction), sunWeight, depthWeight);
            result[channel] = (float) SkyDisplay.displayValue(exposure, radiance * lightScale[channel]);
        }
        greyByWeather(result, 0.6F, rainLevel);
        greyByWeather(result, 0.2F, thunderLevel);
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
