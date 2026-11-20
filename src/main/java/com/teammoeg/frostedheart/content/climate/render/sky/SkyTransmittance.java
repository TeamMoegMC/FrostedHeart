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
 * Transmittance from a point in the atmosphere to the top of the atmosphere, in the parameterisation of Bruneton 2008.
 * Rows are spaced evenly in the distance to the horizon below, inside, and above the ice band, with half of them
 * inside it, because the transmittance through a thick ice layer changes too fast between even rows.
 */
final class SkyTransmittance {
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int WIDTH = 256;
    private static final int HEIGHT = 128;
    private static final int ROWS_BELOW_ICE_BAND = 24;
    private static final int ROWS_IN_ICE_BAND = 64;
    private static final int STEP_COUNT = 40;
    private static final int MAXIMUM_STEPS = SkyRaySteps.maximumSteps(STEP_COUNT, SkyRaySteps.ICE_BAND_STEPS);
    private static final double HORIZON_DISTANCE = Math.sqrt(
            SkyAtmosphere.TOP_RADIUS * SkyAtmosphere.TOP_RADIUS
                    - SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS);
    private static final double ICE_BAND_LOW_HORIZONTAL = Math.sqrt(
            SkyAtmosphere.ICE_BAND_LOW_RADIUS * SkyAtmosphere.ICE_BAND_LOW_RADIUS
                    - SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS);
    private static final double ICE_BAND_HIGH_HORIZONTAL = Math.sqrt(
            SkyAtmosphere.ICE_BAND_HIGH_RADIUS * SkyAtmosphere.ICE_BAND_HIGH_RADIUS
                    - SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS);
    /**
     * Rows by the distance from the ground's horizon point to a point of the row, sqrt(r^2 - R^2) for radius r and
     * ground radius R.
     */
    private static final BandRows ROWS = new BandRows(HEIGHT, ROWS_BELOW_ICE_BAND, ROWS_IN_ICE_BAND,
            ICE_BAND_LOW_HORIZONTAL, ICE_BAND_HIGH_HORIZONTAL, HORIZON_DISTANCE);

    private final float[] values = new float[WIDTH * HEIGHT * CHANNELS];

    SkyTransmittance(SkyAtmosphere atmosphere) {
        this(atmosphere, atmosphere.totalExtinction);
    }

    /**
     * Builds the table from extinction coefficients stored like those of SkyAtmosphere.
     */
    SkyTransmittance(SkyAtmosphere atmosphere, double[] extinction) {
        double[] opticalDepth = new double[CHANNELS];
        double[] boundaries = new double[SkyRaySteps.MAXIMUM_BOUNDARIES];
        double[] travelled = new double[MAXIMUM_STEPS];
        double[] stepLengths = new double[MAXIMUM_STEPS];
        for (int row = 0; row < HEIGHT; row++) {
            double horizontal = ROWS.value(row);
            double radius = Math.sqrt(horizontal * horizontal
                    + SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS);
            double minimumDistance = SkyAtmosphere.TOP_RADIUS - radius;
            double maximumDistance = horizontal + HORIZON_DISTANCE;
            for (int column = 0; column < WIDTH; column++) {
                double distance = minimumDistance + (maximumDistance - minimumDistance) * column / (WIDTH - 1);
                double cosine = 1.0;
                if (distance > 0.0) {
                    cosine = (HORIZON_DISTANCE * HORIZON_DISTANCE - horizontal * horizontal - distance * distance)
                            / (2.0 * radius * distance);
                    cosine = Math.max(-1.0, Math.min(1.0, cosine));
                }
                int stepCount = SkyRaySteps.fill(radius, cosine, distance, STEP_COUNT, SkyRaySteps.ICE_BAND_STEPS,
                        boundaries, travelled, stepLengths, 0);
                Arrays.fill(opticalDepth, 0.0);
                for (int step = 0; step < stepCount; step++) {
                    int slot = atmosphere.altitudeSlot(SkyAtmosphere.radiusAlongRay(radius, cosine, travelled[step]));
                    for (int channel = 0; channel < CHANNELS; channel++) {
                        opticalDepth[channel] += extinction[slot + channel] * stepLengths[step];
                    }
                }
                int texel = (column + row * WIDTH) * CHANNELS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    values[texel + channel] = (float) Math.exp(-opticalDepth[channel]);
                }
            }
        }
    }

    void sample(double radius, double cosine, double[] result) {
        double clampedRadius = Math.max(SkyAtmosphere.GROUND_RADIUS, Math.min(SkyAtmosphere.TOP_RADIUS, radius));
        double groundDiscriminant = clampedRadius * clampedRadius * (cosine * cosine - 1.0)
                + SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS;
        if (cosine < 0.0 && groundDiscriminant >= 0.0) {
            Arrays.fill(result, 0.0);
            return;
        }
        double horizontal = Math.sqrt(Math.max(0.0, clampedRadius * clampedRadius
                - SkyAtmosphere.GROUND_RADIUS * SkyAtmosphere.GROUND_RADIUS));
        double distance = SkyAtmosphere.distanceToTop(clampedRadius, cosine);
        double minimumDistance = SkyAtmosphere.TOP_RADIUS - clampedRadius;
        double maximumDistance = horizontal + HORIZON_DISTANCE;
        double columnPosition = (distance - minimumDistance) / (maximumDistance - minimumDistance) * (WIDTH - 1);
        double rowPosition = ROWS.position(horizontal);
        columnPosition = Math.max(0.0, Math.min(WIDTH - 1.0, columnPosition));
        rowPosition = Math.max(0.0, Math.min(HEIGHT - 1.0, rowPosition));

        int column = Math.min((int) columnPosition, WIDTH - 2);
        int row = Math.min((int) rowPosition, HEIGHT - 2);
        double columnFraction = columnPosition - column;
        double rowFraction = rowPosition - row;
        int topLeft = (column + row * WIDTH) * CHANNELS;
        int bottomLeft = (column + (row + 1) * WIDTH) * CHANNELS;
        for (int channel = 0; channel < CHANNELS; channel++) {
            double top = values[topLeft + channel] * (1.0 - columnFraction)
                    + values[topLeft + CHANNELS + channel] * columnFraction;
            double bottom = values[bottomLeft + channel] * (1.0 - columnFraction)
                    + values[bottomLeft + CHANNELS + channel] * columnFraction;
            result[channel] = top * (1.0 - rowFraction) + bottom * rowFraction;
        }
    }
}
