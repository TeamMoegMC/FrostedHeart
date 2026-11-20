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
 * Lookup tables of Hillaire 2020 for one set of settings, built on the CPU.
 * Multiple scattering is computed against a black ground. Light reflected between snow and atmosphere is added
 * afterwards as L_ground = (A / pi) * E_0 / (1 - A * S), where S is the spherical albedo of the atmosphere.
 * The multiple scattering table uses the diffuse coefficients of SkyAtmosphere, lit by the sun through a transmittance
 * built from them, so that the light ice scatters forward lights deeper layers like the sun's beam.
 * The tables do not change after construction, so other threads may read them while a view is computed. Computing a
 * view reuses scratch buffers of the instance, so one thread at a time computes views.
 */
public final class SkyTables {
    public static final int VIEW_WIDTH = 200;
    public static final int VIEW_HEIGHT = 100;
    /**
     * Half the side of the sun's square on the plane one unit in front of the camera.
     * It matches the 8 texel core of vanilla's 32 texel sun texture, which vanilla draws 60 wide at distance 100.
     */
    public static final double SUN_HALF_TANGENT = 0.075;
    /**
     * Solid angle of the sun's square, over which the sun's irradiance of 1 is spread.
     */
    public static final double SUN_SOLID_ANGLE = 4.0 * Math.atan(SUN_HALF_TANGENT * SUN_HALF_TANGENT
            / Math.sqrt(1.0 + 2.0 * SUN_HALF_TANGENT * SUN_HALF_TANGENT));
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int VIEW_STEPS = 30;
    private static final int MAXIMUM_VIEW_STEPS = SkyRaySteps.maximumSteps(VIEW_STEPS, SkyRaySteps.ICE_BAND_STEPS);
    /**
     * Rows of the multiple scattering table are spaced evenly below, inside, and above the ice band, with a third of
     * them inside it, because the light inside a thick ice layer changes over a few hundred meters.
     */
    private static final int MULTIPLE_SCATTERING_ROWS = 48;
    private static final int MULTIPLE_SCATTERING_COLUMNS = 32;
    private static final int ROWS_BELOW_ICE_BAND = 8;
    private static final int ROWS_IN_ICE_BAND = 16;
    private static final double ICE_BAND_LOW_ALTITUDE = SkyAtmosphere.ICE_BAND_LOW_RADIUS - SkyAtmosphere.GROUND_RADIUS;
    private static final double ICE_BAND_HIGH_ALTITUDE = SkyAtmosphere.ICE_BAND_HIGH_RADIUS - SkyAtmosphere.GROUND_RADIUS;
    /**
     * Rows of the multiple scattering table by altitude.
     */
    private static final BandRows MULTIPLE_SCATTERING_ROW_ALTITUDES = new BandRows(MULTIPLE_SCATTERING_ROWS,
            ROWS_BELOW_ICE_BAND, ROWS_IN_ICE_BAND, ICE_BAND_LOW_ALTITUDE, ICE_BAND_HIGH_ALTITUDE,
            SkyAtmosphere.ATMOSPHERE_HEIGHT);
    private static final int MULTIPLE_SCATTERING_STEPS = 20;
    private static final int MAXIMUM_STEPS = Math.max(MAXIMUM_VIEW_STEPS,
            SkyRaySteps.maximumSteps(MULTIPLE_SCATTERING_STEPS, SkyRaySteps.ICE_BAND_STEPS));
    private static final int DIRECTION_ROOT = 8;
    private static final int GLOW_VIEW_SAMPLES = 64;
    private static final int GLOW_ALTITUDE_SAMPLES = 64;
    private static final int IRRADIANCE_AZIMUTH_SAMPLES = 64;
    private static final int IRRADIANCE_COSINE_SAMPLES = 32;
    private static final int HORIZON_AZIMUTH_SAMPLES = 64;
    private static final double MAXIMUM_TRANSFER = 0.9999;
    private static final double[] SPHERE_DIRECTION_X = new double[DIRECTION_ROOT * DIRECTION_ROOT];
    private static final double[] SPHERE_DIRECTION_Y = new double[DIRECTION_ROOT * DIRECTION_ROOT];
    private static final int COMPUTED_COLUMNS = VIEW_WIDTH / 2;
    private static final double[] ROW_SINES = new double[VIEW_HEIGHT];
    private static final double[] ROW_COSINES = new double[VIEW_HEIGHT];
    private static final double[] COLUMN_SINES = new double[COMPUTED_COLUMNS];
    private static final double[] COLUMN_COSINES = new double[COMPUTED_COLUMNS];

    static {
        for (int azimuthIndex = 0; azimuthIndex < DIRECTION_ROOT; azimuthIndex++) {
            for (int cosineIndex = 0; cosineIndex < DIRECTION_ROOT; cosineIndex++) {
                double azimuth = 2.0 * Math.PI * (azimuthIndex + 0.5) / DIRECTION_ROOT;
                double cosine = 1.0 - 2.0 * (cosineIndex + 0.5) / DIRECTION_ROOT;
                double sine = Math.sqrt(1.0 - cosine * cosine);
                SPHERE_DIRECTION_X[azimuthIndex * DIRECTION_ROOT + cosineIndex] = sine * Math.cos(azimuth);
                SPHERE_DIRECTION_Y[azimuthIndex * DIRECTION_ROOT + cosineIndex] = cosine;
            }
        }
        for (int row = 0; row < VIEW_HEIGHT; row++) {
            double elevation = rowElevation(row);
            ROW_SINES[row] = Math.sin(elevation);
            ROW_COSINES[row] = Math.cos(elevation);
        }
        for (int index = 0; index < COMPUTED_COLUMNS; index++) {
            double azimuth = -Math.PI + 2.0 * Math.PI * (VIEW_WIDTH / 2 + index + 0.5) / VIEW_WIDTH;
            COLUMN_SINES[index] = Math.sin(azimuth);
            COLUMN_COSINES[index] = Math.cos(azimuth);
        }
    }

    private final SkySettings settings;
    private final SkyAtmosphere atmosphere;
    private final SkyTransmittance transmittance;
    private final SkyTransmittance diffuseTransmittance;
    private final double[] multipleScattering =
            new double[MULTIPLE_SCATTERING_ROWS * MULTIPLE_SCATTERING_COLUMNS * CHANNELS];
    private final double[] glowResponse = new double[(GLOW_VIEW_SAMPLES + 1) * CHANNELS];
    private final double[] sphericalAlbedo = new double[CHANNELS];
    private final int[] rayStepCount = new int[VIEW_HEIGHT];
    private final double[] rayTravelled = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS];
    private final double[] rayStepLength = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS];
    private final double[] rayRadius = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS];
    private final double[] rayHeight = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS];
    private final int[] raySlot = new int[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS];
    private final double[] rayTransmittance = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS * CHANNELS];
    private final double[] rayIntegral = new double[VIEW_HEIGHT * MAXIMUM_VIEW_STEPS * CHANNELS];
    private final double[] scratchBoundaries = new double[SkyRaySteps.MAXIMUM_BOUNDARIES];
    private final double[] scratchTravelled = new double[MAXIMUM_STEPS];
    private final double[] scratchStepLengths = new double[MAXIMUM_STEPS];
    private final float[] sunRadiance = new float[VIEW_HEIGHT * CHANNELS];
    private final float[] diffractionWeights = new float[VIEW_HEIGHT * CHANNELS];
    private final double[] scratchSun = new double[3];
    private final double[] scratchRadiance = new double[CHANNELS];
    private final double[] scratchIceRadiance = new double[CHANNELS];
    private final double[] scratchSunTransmittance = new double[CHANNELS];
    private final double[] scratchNanobotPhase = new double[CHANNELS];
    private final double[] scratchViewTransmittance = new double[CHANNELS];
    private final double[] scratchStepSunTransmittance = new double[CHANNELS];
    private final double[] scratchMultiple = new double[CHANNELS];
    private final double[] scratchProfileIrradiance = new double[CHANNELS];
    private final double[] scratchMapIrradiance = new double[CHANNELS];
    private final double[] scratchRingSums = new double[CHANNELS];
    private final double[] scratchSamples = new double[CHANNELS];

    public SkyTables(SkySettings settings) {
        this.settings = settings;
        this.atmosphere = new SkyAtmosphere(settings);
        this.transmittance = new SkyTransmittance(atmosphere);
        this.diffuseTransmittance = new SkyTransmittance(atmosphere, atmosphere.diffuseExtinction);
        buildMultipleScattering();
        buildGlowResponse();
        buildViewRays();
    }

    /**
     * Fills the steps of the view rays of every row, which do not depend on the azimuth or the sun, and the sun's
     * radiance and the diffraction weight through each row. The transmittance of a step is the transmittance from the
     * camera to where the step begins. Rows that hit the ground take the diffraction weight of the lowest row of sky,
     * so that it stays smooth where the shader interpolates across the horizon.
     */
    private void buildViewRays() {
        double radius = SkyAtmosphere.CAMERA_RADIUS;
        double[] viewTransmittance = new double[CHANNELS];
        int lowestSkyRow = -1;
        for (int row = 0; row < VIEW_HEIGHT; row++) {
            double cosine = ROW_SINES[row];
            double groundDistance = SkyAtmosphere.distanceToGroundOrInfinity(radius, cosine);
            boolean hitsGround = groundDistance < Double.POSITIVE_INFINITY;
            double length = hitsGround ? groundDistance : SkyAtmosphere.distanceToTop(radius, cosine);
            int stepCount = SkyRaySteps.fill(radius, cosine, length, VIEW_STEPS, SkyRaySteps.ICE_BAND_STEPS,
                    scratchBoundaries, rayTravelled, rayStepLength, row * MAXIMUM_VIEW_STEPS);
            rayStepCount[row] = stepCount;
            Arrays.fill(viewTransmittance, 1.0);
            double iceDepth = 0.0;
            for (int step = 0; step < stepCount; step++) {
                int sample = row * MAXIMUM_VIEW_STEPS + step;
                double travelled = rayTravelled[sample];
                double stepLength = rayStepLength[sample];
                double sampleRadius = SkyAtmosphere.radiusAlongRay(radius, cosine, travelled);
                int slot = atmosphere.altitudeSlot(sampleRadius);
                rayRadius[sample] = sampleRadius;
                rayHeight[sample] = radius + cosine * travelled;
                raySlot[sample] = slot;
                iceDepth += atmosphere.iceScattering[slot + SkyAtmosphere.GREEN_CHANNEL] * stepLength;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double extinction = atmosphere.totalExtinction[slot + channel];
                    double stepTransmittance = Math.exp(-extinction * stepLength);
                    rayTransmittance[sample * CHANNELS + channel] = viewTransmittance[channel];
                    rayIntegral[sample * CHANNELS + channel] =
                            extinction > 0.0 ? (1.0 - stepTransmittance) / extinction : stepLength;
                    viewTransmittance[channel] *= stepTransmittance;
                }
            }
            double diffracted = SkyAtmosphere.ICE_DIFFRACTION_FRACTION * iceDepth;
            double undiffracted = Math.exp(-diffracted);
            double diffractionWeight = diffracted > 1.0e-9 ? -Math.expm1(-diffracted) / diffracted : 1.0;
            if (!hitsGround && lowestSkyRow < 0) {
                lowestSkyRow = row;
            }
            for (int channel = 0; channel < CHANNELS; channel++) {
                sunRadiance[row * CHANNELS + channel] =
                        hitsGround ? 0.0F : (float) (viewTransmittance[channel] * undiffracted / SUN_SOLID_ANGLE);
                diffractionWeights[row * CHANNELS + channel] = (float) diffractionWeight;
            }
        }
        if (lowestSkyRow > 0) {
            for (int row = 0; row < lowestSkyRow; row++) {
                System.arraycopy(diffractionWeights, lowestSkyRow * CHANNELS, diffractionWeights, row * CHANNELS,
                        CHANNELS);
            }
        }
    }

    public SkySettings settings() {
        return settings;
    }

    /**
     * Radiance of the sun's square seen through each row of the view tables, as interleaved RGB.
     * It is the transmittance from the camera to the top of the atmosphere with the full ice extinction, over
     * SUN_SOLID_ANGLE, and zero for rows that hit the ground. The light that ice diffracts out of the square is drawn
     * by the diffraction peak of IceHaloProfile. calculated_sky.fsh reads it with the row coordinate of the sky view.
     */
    public float[] sunRadiance() {
        return sunRadiance;
    }

    /**
     * Weight of the diffraction peak of IceHaloProfile through each row of the view tables, as interleaved RGB.
     * The ice table uses delta scaled transmittance, which keeps light that is diffracted again in the beam, so light
     * diffracted n times would count n times in the aureole. The weight (1 - exp(-f tau)) / (f tau) scales the
     * aureole to the light diffracted at least once, where tau is the ice optical depth along the row and f is
     * ICE_DIFFRACTION_FRACTION. The sun's square and the aureole then carry the delta scaled beam between them.
     * calculated_sky.fsh reads it with the row coordinate of the sky view.
     */
    public float[] diffractionWeights() {
        return diffractionWeights;
    }

    public double nanobotMegatons() {
        return settings.nanobotOpticalDepth() * atmosphere.nanobotBands.megatonsForUnitOpticalDepth();
    }

    private void buildMultipleScattering() {
        double[] secondOrder = new double[CHANNELS];
        double[] transfer = new double[CHANNELS];
        double[] groundReach = new double[CHANNELS];
        for (int row = 0; row < MULTIPLE_SCATTERING_ROWS; row++) {
            double altitude = Math.max(1.0, MULTIPLE_SCATTERING_ROW_ALTITUDES.value(row));
            double radius = SkyAtmosphere.GROUND_RADIUS + altitude;
            for (int column = 0; column < MULTIPLE_SCATTERING_COLUMNS; column++) {
                double sunCosine = column / (MULTIPLE_SCATTERING_COLUMNS - 1.0) * 2.0 - 1.0;
                double sunSine = Math.sqrt(Math.max(0.0, 1.0 - sunCosine * sunCosine));
                integrateIsotropic(radius, sunSine, sunCosine, true, atmosphere.diffuseScattering,
                        atmosphere.diffuseExtinction, secondOrder, transfer, groundReach);
                int texel = (column + row * MULTIPLE_SCATTERING_COLUMNS) * CHANNELS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    multipleScattering[texel + channel] =
                            secondOrder[channel] / (1.0 - Math.min(MAXIMUM_TRANSFER, transfer[channel]));
                }
            }
        }
    }

    /**
     * Sums over directions from a point, with the given coefficients, the share of isotropic light scattered again,
     * the share that reaches the ground, and with the sun the sunlight scattered once toward the point. The sun lights
     * the samples through the diffuse transmittance.
     */
    private void integrateIsotropic(double radius, double sunSine, double sunCosine, boolean withSun,
            double[] scatteringCoefficients, double[] extinctionCoefficients, double[] secondOrder, double[] transfer,
            double[] groundReach) {
        Arrays.fill(secondOrder, 0.0);
        Arrays.fill(transfer, 0.0);
        Arrays.fill(groundReach, 0.0);
        double[] sun = scratchStepSunTransmittance;
        double[] viewTransmittance = scratchViewTransmittance;
        double directionCount = DIRECTION_ROOT * DIRECTION_ROOT;
        for (int azimuthIndex = 0; azimuthIndex < DIRECTION_ROOT; azimuthIndex++) {
            for (int cosineIndex = 0; cosineIndex < DIRECTION_ROOT; cosineIndex++) {
                double directionX = SPHERE_DIRECTION_X[azimuthIndex * DIRECTION_ROOT + cosineIndex];
                double cosine = SPHERE_DIRECTION_Y[azimuthIndex * DIRECTION_ROOT + cosineIndex];
                double groundDistance = SkyAtmosphere.distanceToGroundOrInfinity(radius, cosine);
                boolean hitsGround = groundDistance < Double.POSITIVE_INFINITY;
                double length = hitsGround ? groundDistance : SkyAtmosphere.distanceToTop(radius, cosine);
                int stepCount = SkyRaySteps.fill(radius, cosine, length, MULTIPLE_SCATTERING_STEPS,
                        SkyRaySteps.ICE_BAND_STEPS, scratchBoundaries, scratchTravelled, scratchStepLengths, 0);
                Arrays.fill(viewTransmittance, 1.0);
                for (int step = 0; step < stepCount; step++) {
                    double travelled = scratchTravelled[step];
                    double stepLength = scratchStepLengths[step];
                    double sampleRadius = SkyAtmosphere.radiusAlongRay(radius, cosine, travelled);
                    int slot = atmosphere.altitudeSlot(sampleRadius);
                    if (withSun) {
                        double sampleX = directionX * travelled;
                        double sampleY = radius + cosine * travelled;
                        double sampleSunCosine = (sampleX * sunSine + sampleY * sunCosine) / sampleRadius;
                        diffuseTransmittance.sample(sampleRadius, sampleSunCosine, sun);
                    }
                    for (int channel = 0; channel < CHANNELS; channel++) {
                        double scattering = scatteringCoefficients[slot + channel];
                        double extinction = extinctionCoefficients[slot + channel];
                        double stepTransmittance = Math.exp(-extinction * stepLength);
                        double integral = extinction > 0.0 ? (1.0 - stepTransmittance) / extinction : stepLength;
                        if (withSun) {
                            secondOrder[channel] += viewTransmittance[channel] * scattering * sun[channel]
                                    / (4.0 * Math.PI) * integral;
                        }
                        transfer[channel] += viewTransmittance[channel] * scattering * integral;
                        viewTransmittance[channel] *= stepTransmittance;
                    }
                }
                if (hitsGround) {
                    for (int channel = 0; channel < CHANNELS; channel++) {
                        groundReach[channel] += viewTransmittance[channel];
                    }
                }
            }
        }
        for (int channel = 0; channel < CHANNELS; channel++) {
            secondOrder[channel] /= directionCount;
            transfer[channel] /= directionCount;
            groundReach[channel] /= directionCount;
        }
    }

    private void buildGlowResponse() {
        double[] secondOrder = new double[CHANNELS];
        double[] transfer = new double[CHANNELS];
        double[] groundReach = new double[CHANNELS];
        double[] sourceByAltitude = new double[(GLOW_ALTITUDE_SAMPLES + 1) * CHANNELS];
        for (int sample = 0; sample <= GLOW_ALTITUDE_SAMPLES; sample++) {
            double altitude = Math.max(1.0, sample / (double) GLOW_ALTITUDE_SAMPLES * SkyAtmosphere.ATMOSPHERE_HEIGHT);
            integrateIsotropic(SkyAtmosphere.GROUND_RADIUS + altitude, 0.0, 1.0, false, atmosphere.totalScattering,
                    atmosphere.totalExtinction, secondOrder, transfer, groundReach);
            for (int channel = 0; channel < CHANNELS; channel++) {
                sourceByAltitude[sample * CHANNELS + channel] =
                        groundReach[channel] / (1.0 - Math.min(MAXIMUM_TRANSFER, transfer[channel]));
            }
        }

        double radius = SkyAtmosphere.CAMERA_RADIUS;
        double[] viewTransmittance = new double[CHANNELS];
        for (int sample = 0; sample <= GLOW_VIEW_SAMPLES; sample++) {
            double cosine = sample / (double) GLOW_VIEW_SAMPLES;
            double length = SkyAtmosphere.distanceToTop(radius, cosine);
            int stepCount = SkyRaySteps.fill(radius, cosine, length, VIEW_STEPS, SkyRaySteps.ICE_BAND_STEPS,
                    scratchBoundaries, scratchTravelled, scratchStepLengths, 0);
            Arrays.fill(viewTransmittance, 1.0);
            for (int step = 0; step < stepCount; step++) {
                double travelled = scratchTravelled[step];
                double stepLength = scratchStepLengths[step];
                double sampleRadius = SkyAtmosphere.radiusAlongRay(radius, cosine, travelled);
                int slot = atmosphere.altitudeSlot(sampleRadius);
                double position = (sampleRadius - SkyAtmosphere.GROUND_RADIUS) / SkyAtmosphere.ATMOSPHERE_HEIGHT
                        * GLOW_ALTITUDE_SAMPLES;
                int lower = Math.max(0, Math.min(GLOW_ALTITUDE_SAMPLES - 1, (int) position));
                double fraction = Math.max(0.0, Math.min(1.0, position - lower));
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double extinction = atmosphere.totalExtinction[slot + channel];
                    double stepTransmittance = Math.exp(-extinction * stepLength);
                    double integral = extinction > 0.0 ? (1.0 - stepTransmittance) / extinction : stepLength;
                    double source = sourceByAltitude[lower * CHANNELS + channel] * (1.0 - fraction)
                            + sourceByAltitude[(lower + 1) * CHANNELS + channel] * fraction;
                    glowResponse[sample * CHANNELS + channel] += viewTransmittance[channel]
                            * atmosphere.totalScattering[slot + channel] * source * integral;
                    viewTransmittance[channel] *= stepTransmittance;
                }
            }
        }

        for (int channel = 0; channel < CHANNELS; channel++) {
            double response = 0.0;
            for (int sample = 0; sample < GLOW_VIEW_SAMPLES; sample++) {
                double cosineLow = sample / (double) GLOW_VIEW_SAMPLES;
                double cosineHigh = (sample + 1.0) / GLOW_VIEW_SAMPLES;
                double middle = 0.5 * (cosineLow + cosineHigh);
                double value = 0.5 * (glowResponse[sample * CHANNELS + channel]
                        + glowResponse[(sample + 1) * CHANNELS + channel]);
                response += value * middle * (cosineHigh - cosineLow);
            }
            sphericalAlbedo[channel] = 2.0 * response;
        }
    }

    private void sampleMultipleScattering(double radius, double sunCosine, double[] result) {
        double columnPosition = (0.5 + 0.5 * sunCosine) * (MULTIPLE_SCATTERING_COLUMNS - 1);
        double rowPosition = MULTIPLE_SCATTERING_ROW_ALTITUDES.position(radius - SkyAtmosphere.GROUND_RADIUS);
        columnPosition = Math.max(0.0, Math.min(MULTIPLE_SCATTERING_COLUMNS - 1.0, columnPosition));
        rowPosition = Math.max(0.0, Math.min(MULTIPLE_SCATTERING_ROWS - 1.0, rowPosition));
        int column = Math.min((int) columnPosition, MULTIPLE_SCATTERING_COLUMNS - 2);
        int row = Math.min((int) rowPosition, MULTIPLE_SCATTERING_ROWS - 2);
        double columnFraction = columnPosition - column;
        double rowFraction = rowPosition - row;
        int topLeft = (column + row * MULTIPLE_SCATTERING_COLUMNS) * CHANNELS;
        int bottomLeft = (column + (row + 1) * MULTIPLE_SCATTERING_COLUMNS) * CHANNELS;
        for (int channel = 0; channel < CHANNELS; channel++) {
            double top = multipleScattering[topLeft + channel] * (1.0 - columnFraction)
                    + multipleScattering[topLeft + CHANNELS + channel] * columnFraction;
            double bottom = multipleScattering[bottomLeft + channel] * (1.0 - columnFraction)
                    + multipleScattering[bottomLeft + CHANNELS + channel] * columnFraction;
            result[channel] = top * (1.0 - rowFraction) + bottom * rowFraction;
        }
    }

    /**
     * Computes the sky view for a sun elevation. The halo profile and map give the light that ice scatters onto the
     * ground, and the map may be built for a nearby sun elevation.
     */
    public SkyView computeView(double sunElevationRadians, IceHaloProfile haloProfile, IceHaloMap haloMap) {
        SkyView view = new SkyView();
        computeView(sunElevationRadians, haloProfile, haloMap, view);
        return view;
    }

    /**
     * Fills an existing view for a sun elevation, without allocating. The sky table holds the sky without light
     * reflected from the ground until the ground radiance is known, and is then completed in place.
     */
    public void computeView(double sunElevationRadians, IceHaloProfile haloProfile, IceHaloMap haloMap,
            SkyView view) {
        double[] sun = scratchSun;
        sun[0] = Math.cos(sunElevationRadians);
        sun[1] = Math.sin(sunElevationRadians);
        sun[2] = 0.0;
        float[] sky = view.sky();
        float[] ice = view.ice();
        double[] radiance = scratchRadiance;
        double[] iceRadiance = scratchIceRadiance;
        for (int row = 0; row < VIEW_HEIGHT; row++) {
            double rowCosine = ROW_COSINES[row];
            for (int column = VIEW_WIDTH / 2; column < VIEW_WIDTH; column++) {
                int index = column - VIEW_WIDTH / 2;
                integrateView(row, rowCosine * COLUMN_COSINES[index], rowCosine * COLUMN_SINES[index], sun, radiance,
                        iceRadiance);
                int texel = (column + row * VIEW_WIDTH) * CHANNELS;
                int mirroredTexel = (VIEW_WIDTH - 1 - column + row * VIEW_WIDTH) * CHANNELS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    sky[texel + channel] = (float) radiance[channel];
                    sky[mirroredTexel + channel] = (float) radiance[channel];
                    ice[texel + channel] = (float) iceRadiance[channel];
                    ice[mirroredTexel + channel] = (float) iceRadiance[channel];
                }
            }
        }

        double[] groundRadiance = view.groundRadiance();
        double[] sunTransmittance = scratchSunTransmittance;
        transmittance.sample(SkyAtmosphere.GROUND_RADIUS, sun[1], sunTransmittance);
        transmittance.sample(SkyAtmosphere.CAMERA_RADIUS, sun[1], view.lightTransmittance());
        double[] profileIrradiance = scratchProfileIrradiance;
        double[] mapIrradiance = scratchMapIrradiance;
        haloProfile.irradiance(sun, ice, profileIrradiance, scratchRingSums, scratchSamples);
        haloMap.irradiance(ice, mapIrradiance, scratchSamples);
        for (int channel = 0; channel < CHANNELS; channel++) {
            double direct = sun[1] > 0.0 ? sun[1] * sunTransmittance[channel] : 0.0;
            double iceIrradiance = profileIrradiance[channel] + mapIrradiance[channel];
            double downward = direct + skyIrradiance(sky, channel) + iceIrradiance;
            groundRadiance[channel] = SkyAtmosphere.GROUND_ALBEDO / Math.PI * downward
                    / (1.0 - SkyAtmosphere.GROUND_ALBEDO * sphericalAlbedo[channel]);
        }

        for (int row = 0; row < VIEW_HEIGHT; row++) {
            double directionY = ROW_SINES[row];
            boolean hitsGround = SkyAtmosphere.distanceToGroundOrInfinity(SkyAtmosphere.CAMERA_RADIUS, directionY)
                    < Double.POSITIVE_INFINITY;
            double position = Math.max(0.0, directionY) * GLOW_VIEW_SAMPLES;
            int lower = Math.min(GLOW_VIEW_SAMPLES - 1, (int) position);
            double fraction = position - lower;
            for (int column = 0; column < VIEW_WIDTH; column++) {
                int texel = (column + row * VIEW_WIDTH) * CHANNELS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    if (hitsGround) {
                        sky[texel + channel] = (float) groundRadiance[channel];
                        ice[texel + channel] = 0.0F;
                        continue;
                    }
                    double response = glowResponse[lower * CHANNELS + channel] * (1.0 - fraction)
                            + glowResponse[(lower + 1) * CHANNELS + channel] * fraction;
                    sky[texel + channel] = (float) (sky[texel + channel] + groundRadiance[channel] * response);
                }
            }
        }

        double[] meanHorizonRadiance = view.meanHorizonRadiance();
        double[] brightestHorizonRadiance = view.brightestHorizonRadiance();
        Arrays.fill(meanHorizonRadiance, 0.0);
        double brightestLuminance = -1.0;
        for (int index = 0; index < HORIZON_AZIMUTH_SAMPLES; index++) {
            double azimuth = -Math.PI + 2.0 * Math.PI * (index + 0.5) / HORIZON_AZIMUTH_SAMPLES;
            double red = SkyView.sample(sky, 0, azimuth, 0.0);
            double green = SkyView.sample(sky, 1, azimuth, 0.0);
            double blue = SkyView.sample(sky, 2, azimuth, 0.0);
            meanHorizonRadiance[0] += red / HORIZON_AZIMUTH_SAMPLES;
            meanHorizonRadiance[1] += green / HORIZON_AZIMUTH_SAMPLES;
            meanHorizonRadiance[2] += blue / HORIZON_AZIMUTH_SAMPLES;
            double luminance = SkyAdaptation.luminance(red, green, blue);
            if (luminance > brightestLuminance) {
                brightestLuminance = luminance;
                brightestHorizonRadiance[0] = red;
                brightestHorizonRadiance[1] = green;
                brightestHorizonRadiance[2] = blue;
            }
        }
        double[] adaptationRadiance = view.adaptationRadiance();
        for (int channel = 0; channel < CHANNELS; channel++) {
            adaptationRadiance[channel] = (skyIrradiance(sky, channel) + profileIrradiance[channel]
                    + mapIrradiance[channel]) / Math.PI;
        }
        view.setSunElevationRadians(sunElevationRadians);
    }

    static double rowElevation(int row) {
        double position = 2.0 * (row + 0.5) / VIEW_HEIGHT - 1.0;
        double latitude = Math.signum(position) * position * position * Math.PI / 2.0;
        return SkyView.latitudeToElevation(latitude);
    }

    /**
     * Integrates the view ray of a row toward the given horizontal direction components, with the steps that
     * buildViewRays filled for the row.
     */
    private void integrateView(int row, double directionX, double directionZ, double[] sun, double[] radiance,
            double[] iceRadiance) {
        double directionY = ROW_SINES[row];
        double viewSunCosine = directionX * sun[0] + directionY * sun[1] + directionZ * sun[2];
        double rayleighPhase = SkyPhase.rayleigh(viewSunCosine);
        double aerosolPhase = SkyPhase.henyeyGreenstein(viewSunCosine, SkyAtmosphere.AEROSOL_ASYMMETRY);
        double[] nanobotPhase = scratchNanobotPhase;
        double[] sunTransmittance = scratchStepSunTransmittance;
        double[] multiple = scratchMultiple;
        for (int channel = 0; channel < CHANNELS; channel++) {
            nanobotPhase[channel] = SkyPhase.henyeyGreenstein(viewSunCosine,
                    atmosphere.nanobotBands.asymmetry()[channel]);
            radiance[channel] = 0.0;
            iceRadiance[channel] = 0.0;
        }

        int stepCount = rayStepCount[row];
        for (int step = 0; step < stepCount; step++) {
            int sample = row * MAXIMUM_VIEW_STEPS + step;
            double travelled = rayTravelled[sample];
            double sampleRadius = rayRadius[sample];
            int slot = raySlot[sample];
            double sampleX = directionX * travelled;
            double sampleY = rayHeight[sample];
            double sampleZ = directionZ * travelled;
            double sampleSunCosine = (sampleX * sun[0] + sampleY * sun[1] + sampleZ * sun[2]) / sampleRadius;
            transmittance.sample(sampleRadius, sampleSunCosine, sunTransmittance);
            sampleMultipleScattering(sampleRadius, sampleSunCosine, multiple);
            for (int channel = 0; channel < CHANNELS; channel++) {
                double scattering = atmosphere.totalScattering[slot + channel];
                double viewTransmittance = rayTransmittance[sample * CHANNELS + channel];
                double integral = rayIntegral[sample * CHANNELS + channel];
                double phaseWeighted = atmosphere.rayleighScattering[slot + channel] * rayleighPhase
                        + atmosphere.aerosolScattering[slot + channel] * aerosolPhase
                        + atmosphere.nanobotScattering[slot + channel] * nanobotPhase[channel];
                double source = phaseWeighted * sunTransmittance[channel] + scattering * multiple[channel];
                radiance[channel] += viewTransmittance * source * integral;
                iceRadiance[channel] += viewTransmittance * atmosphere.iceScattering[slot + channel]
                        * sunTransmittance[channel] * integral;
            }
        }
    }

    private static double skyIrradiance(float[] table, int channel) {
        double sum = 0.0;
        for (int cosineIndex = 0; cosineIndex < IRRADIANCE_COSINE_SAMPLES; cosineIndex++) {
            double cosine = (cosineIndex + 0.5) / IRRADIANCE_COSINE_SAMPLES;
            double elevation = Math.asin(cosine);
            for (int azimuthIndex = 0; azimuthIndex < IRRADIANCE_AZIMUTH_SAMPLES; azimuthIndex++) {
                double azimuth = -Math.PI + 2.0 * Math.PI * (azimuthIndex + 0.5) / IRRADIANCE_AZIMUTH_SAMPLES;
                sum += SkyView.sample(table, channel, azimuth, elevation) * cosine;
            }
        }
        return sum * 2.0 * Math.PI / (IRRADIANCE_COSINE_SAMPLES * IRRADIANCE_AZIMUTH_SAMPLES);
    }
}
