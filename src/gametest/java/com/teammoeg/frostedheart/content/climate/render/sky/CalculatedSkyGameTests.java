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

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.DoubleUnaryOperator;

/**
 * Checks the sky optics against published values and against the offline preview in the workspace.
 * Reference values come from the preview's datasets mode, which also wrote the ice halo datasets.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class CalculatedSkyGameTests {
    private static final double[][] DIRECTIONS_DEGREES = {
            {0.0, 90.0}, {0.0, 10.0}, {180.0, 10.0}, {90.0, 45.0}, {0.0, 30.0}, {0.0, -5.0}
    };
    private static final double[][] PREVIEW_RADIANCE_SUN_30 = {
            {3.743975e-02, 4.762229e-02, 6.666073e-02},
            {1.515355e-01, 1.451203e-01, 1.525993e-01},
            {8.861074e-02, 1.029520e-01, 1.294570e-01},
            {4.589578e-02, 5.743036e-02, 8.011560e-02},
            {1.386137e+02, 1.055828e+02, 7.590167e+01},
            {1.184124e-01, 1.094150e-01, 1.109586e-01}
    };
    private static final double[][] PREVIEW_RADIANCE_SUN_8 = {
            {1.319047e-02, 1.437835e-02, 1.866422e-02},
            {1.434313e+00, 6.463899e-01, 2.637266e-01},
            {3.176577e-02, 2.753153e-02, 2.841744e-02},
            {1.670649e-02, 1.783067e-02, 2.248554e-02},
            {9.278304e-02, 6.041685e-02, 4.310171e-02},
            {2.269845e-02, 1.908241e-02, 2.028031e-02}
    };
    private static final int UNIFORM_STEPS = 30;
    private static final double[] RAY_START_ALTITUDES = {100.0, 5000.0, 11000.0, 20000.0, 60000.0};
    private static final double AUREOLE_ICE_OPTICAL_DEPTH = 0.5;
    private static final int[] AUREOLE_ROWS = {60, 65, 85};
    private static final double AUREOLE_SQUARE_TOLERANCE = 0.02;
    private static final double AUREOLE_BEAM_TOLERANCE = 0.05;
    private static final double[] PREVIEW_PROFILE_TOTAL = {7.001337e-01, 7.001327e-01, 7.001303e-01};
    private static final double[] PREVIEW_MAP_TOTAL_SUN_30 = {2.486358e-01, 2.481081e-01, 2.472797e-01};
    private static final double[] PREVIEW_MAP_TOTAL_SUN_8 = {1.982199e-01, 1.981322e-01, 1.984010e-01};
    private static final double PREVIEW_TEXEL_DEGREES = 0.5;
    private static final double DEFAULT_EXPOSURE = SkySetting.EXPOSURE.defaultValue();
    private static final double[] DAYLIGHT_SUN_DEGREES = {
            90.0, 45.0, 30.0, 20.0, 12.0, 5.0, 2.0, 0.0, -2.0, -6.0, -12.0, -18.0
    };
    private static final double[] SKY_LIGHT_ANGLES = {
            0.0, 0.3, 1.0, Math.PI / 2.0, 2.0, Math.PI, 4.0, 3.0 * Math.PI / 2.0, 5.5, 2.0 * Math.PI - 0.01
    };
    /**
     * Covers the day time rounded to the whole tick, which moves the sun by at most half of the 0.018 degrees it
     * travels in a tick.
     */
    private static final double ROUND_TRIP_TOLERANCE_DEGREES = 0.01;
    private static final double ROUND_TRIP_LOWEST_SUN_DEGREES = -18.0;
    private static final double ROUND_TRIP_SUN_STEP_DEGREES = 0.25;
    private static final int ROUND_TRIP_STEPS = 432;
    private static final double MIRROR_LOWEST_SUN_DEGREES = -90.0;
    private static final double MIRROR_SUN_STEP_DEGREES = 0.5;
    private static final int MIRROR_STEPS = 360;
    private static final long NOON_DAY_TIME = 6000;
    private static final long MIDNIGHT_DAY_TIME = 18000;
    private static final long SUNSET_DAY_TIME = 12785;
    private static final long SUNRISE_DAY_TIME = 23215;
    /**
     * The day time that the setting and the rising time of one elevation add up to, each rounded to a tick.
     */
    private static final long MIRROR_DAY_TIME = 12000;
    private static final long MIRROR_DAY_TIME_TOLERANCE = 1;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void mieMatchesPublishedAndRayleighLimits(GameTestHelper helper) {
        double extinction = SkyParticleOptics.mie(10.0, 1.5).extinction();
        helper.assertTrue(Math.abs(extinction - 2.8820) < 5.0e-4,
                "Extinction efficiency for m=1.5 and x=10 is " + extinction + ", published 2.8820");
        double sizeParameter = 0.01;
        double index = 1.5;
        double lorentz = (index * index - 1.0) / (index * index + 2.0);
        double rayleighLimit = 8.0 / 3.0 * Math.pow(sizeParameter, 4.0) * lorentz * lorentz;
        SkyParticleOptics.Efficiencies small = SkyParticleOptics.mie(sizeParameter, index);
        helper.assertTrue(Math.abs(small.scattering() / rayleighLimit - 1.0) < 1.0e-3,
                "Small particles scatter as in the Rayleigh limit");
        helper.assertTrue(Math.abs(small.asymmetry()) < 1.0e-3, "Small particles scatter symmetrically");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void phaseFunctionsIntegrateToOne(GameTestHelper helper) {
        assertNormalized(helper, "Rayleigh", SkyPhase::rayleigh, 1.0e-3);
        assertNormalized(helper, "Henyey-Greenstein", cosine -> SkyPhase.henyeyGreenstein(cosine, 0.8), 2.0e-3);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void iceHaloDatasetsAreNormalized(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double worstSideFaceError = 0.0;
        for (int row = 0; row < data.sideFaceIndexRows(); row++) {
            double integral = 0.0;
            for (int column = 0; column < data.sideFaceDeviationColumns(); column++) {
                integral += data.sideFaceDensity(row, column) * data.sideFaceDeviationStep();
            }
            worstSideFaceError = Math.max(worstSideFaceError, Math.abs(integral - 1.0));
        }
        helper.assertTrue(worstSideFaceError < 1.0e-3,
                "Side face deviation densities integrate to 1, worst row off by " + worstSideFaceError);
        double shareSum = 0.0;
        for (IceCrystalData.Crystal crystal : data.crystals()) {
            shareSum += data.shareOf(crystal);
            if (crystal.orientation() == IceCrystalData.Orientation.RANDOM) {
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double[] angles = data.scattering(crystal, channel);
                    double total = 0.0;
                    for (int bin = 0; bin < angles.length; bin++) {
                        total += angles[bin] * IceHaloProfile.binSolidAngle(bin);
                    }
                    helper.assertTrue(Math.abs(total - 1.0) < 1.0e-3,
                            crystal.name() + " scattering integrates to " + total + " in channel " + channel);
                }
                continue;
            }
            for (int row = 0; row < data.pathShareRows(crystal); row++) {
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double sum = 0.0;
                    for (int path = 0; path < IceCrystalData.PATH_COUNT; path++) {
                        sum += data.pathShare(crystal, row, channel, path);
                    }
                    helper.assertTrue(sum > 0.99 && sum < 1.0 + 1.0e-5,
                            crystal.name() + " path shares add up to " + sum + " in row " + row + ", channel "
                                    + channel + ", where light still inside after the tracer's last interaction "
                                    + "is lost");
                }
            }
        }
        helper.assertTrue(Math.abs(shareSum - 1.0) < 1.0e-9, "Crystal shares add up to " + shareSum);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void iceHalosMatchOfflinePreview(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(PREVIEW_TEXEL_DEGREES);
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double total = profile.total(channel);
            helper.assertTrue(Math.abs(total / PREVIEW_PROFILE_TOTAL[channel] - 1.0) < 1.0e-3,
                    "Halo profile total in channel " + channel + " is " + total + ", preview "
                            + PREVIEW_PROFILE_TOTAL[channel]);
        }
        assertHaloMap(helper, new IceHaloMap(data, Math.toRadians(30.0), blurSigma), 30.0, PREVIEW_MAP_TOTAL_SUN_30);
        assertHaloMap(helper, new IceHaloMap(data, Math.toRadians(8.0), blurSigma), 8.0, PREVIEW_MAP_TOTAL_SUN_8);
        helper.succeed();
    }

    /**
     * Checks the light a halo map holds against the preview, and that its sun dogs sit just outside the minimum
     * deviation that Bravais' effective index predicts, where the blur moves the brightest bin.
     */
    private static void assertHaloMap(GameTestHelper helper, IceHaloMap map, double sunElevationDegrees,
            double[] expectedTotals) {
        int sunRow = IceHaloMap.rowOfElevation(Math.toRadians(sunElevationDegrees));
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double total = map.total(channel);
            helper.assertTrue(Math.abs(total / expectedTotals[channel] - 1.0) < 5.0e-3,
                    "Halo map total at sun " + sunElevationDegrees + " in channel " + channel + " is " + total
                            + ", preview " + expectedTotals[channel]);
            int brightest = -1;
            for (int column = 0; column < IceHaloMap.AZIMUTH_BINS; column++) {
                double degrees = Math.toDegrees((column + 0.5) * IceHaloMap.BIN_STEP);
                if (degrees > 15.0 && degrees < 40.0 && (brightest < 0
                        || map.value(channel, column, sunRow) > map.value(channel, brightest, sunRow))) {
                    brightest = column;
                }
            }
            double index = IceHaloFormulas.refractiveIndex(channel);
            double along = Math.sin(Math.toRadians(sunElevationDegrees));
            double effectiveIndex = Math.sqrt(index * index - along * along) / Math.sqrt(1.0 - along * along);
            double bravaisDegrees = Math.toDegrees(2.0 * Math.asin(effectiveIndex * 0.5)) - 60.0;
            double sunDogDegrees = Math.toDegrees((brightest + 0.5) * IceHaloMap.BIN_STEP);
            helper.assertTrue(sunDogDegrees > bravaisDegrees - 0.25 && sunDogDegrees < bravaisDegrees + 1.0,
                    "Sun dog at sun " + sunElevationDegrees + " in channel " + channel + " is at " + sunDogDegrees
                            + " degrees from the sun, Bravais minimum deviation " + bravaisDegrees);
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void defaultSkyMatchesOfflinePreview(GameTestHelper helper) {
        SkySettings settings = SkySettings.DEFAULT.with(SkySetting.TEXEL_DEGREES, PREVIEW_TEXEL_DEGREES);
        SkyTables tables = new SkyTables(settings);
        helper.assertTrue(Math.abs(tables.nanobotMegatons() - 0.25 * 120.5512) < 0.01,
                "Default nanobot mass is " + tables.nanobotMegatons() + " Mt");
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(settings.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        for (double sunElevationDegrees : new double[] {30.0, 8.0}) {
            double sunElevation = Math.toRadians(sunElevationDegrees);
            IceHaloMap map = new IceHaloMap(data, sunElevation, blurSigma);
            SkyView view = tables.computeView(sunElevation, profile, map);
            double[][] expected = sunElevationDegrees == 30.0 ? PREVIEW_RADIANCE_SUN_30 : PREVIEW_RADIANCE_SUN_8;
            assertMatchesPreview(helper, view, profile, map, tables.diffractionWeights(), expected,
                    sunElevationDegrees);
        }
        helper.succeed();
    }

    /**
     * With the sun in the middle of a row of the view tables, the sun's square carries the transmittance toward it
     * with the full ice extinction. Together with the diffraction peak drawn around it, it carries the transmittance
     * with delta scaled ice, which counts diffracted light as transmitted. The peak is taken as if all of it lay in
     * the sun's direction.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void sunSquareAndAureoleCarryTheDeltaScaledBeam(GameTestHelper helper) {
        SkySettings settings = SkySettings.DEFAULT.withIceOpticalDepth(AUREOLE_ICE_OPTICAL_DEPTH);
        SkyTables tables = new SkyTables(settings);
        SkyAtmosphere atmosphere = new SkyAtmosphere(settings);
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(settings.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        for (int row : AUREOLE_ROWS) {
            double sunElevation = SkyTables.rowElevation(row);
            double cosine = Math.sin(sunElevation);
            SkyView view = tables.computeView(sunElevation, profile, new IceHaloMap(data, sunElevation, blurSigma));
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                double deltaScaledDepth = opticalDepthToTop(atmosphere, atmosphere.totalExtinction, channel, cosine);
                double diffractedDepth = SkyAtmosphere.ICE_DIFFRACTION_FRACTION
                        * opticalDepthToTop(atmosphere, atmosphere.iceScattering, channel, cosine);
                double square = tables.sunRadiance()[row * SkyAtmosphere.CHANNEL_COUNT + channel]
                        * SkyTables.SUN_SOLID_ANGLE;
                double aureole = SkyAtmosphere.ICE_DIFFRACTION_FRACTION
                        * tables.diffractionWeights()[row * SkyAtmosphere.CHANNEL_COUNT + channel]
                        * SkyView.sample(view.ice(), channel, 0.0, sunElevation);
                double squareExpected = Math.exp(-deltaScaledDepth - diffractedDepth);
                double beamExpected = Math.exp(-deltaScaledDepth);
                String where = "Sun at " + Math.toDegrees(sunElevation) + " degrees, channel " + channel;
                helper.assertTrue(Math.abs(square / squareExpected - 1.0) < AUREOLE_SQUARE_TOLERANCE,
                        where + ", the sun's square carries " + square + ", full ice extinction gives "
                                + squareExpected);
                helper.assertTrue(Math.abs((square + aureole) / beamExpected - 1.0) < AUREOLE_BEAM_TOLERANCE,
                        where + ", the square and the aureole carry " + square + " and " + aureole
                                + ", delta scaled ice gives " + beamExpected);
            }
        }
        helper.succeed();
    }

    private static double opticalDepthToTop(SkyAtmosphere atmosphere, double[] coefficients, int channel,
            double cosine) {
        double radius = SkyAtmosphere.CAMERA_RADIUS;
        double length = SkyAtmosphere.distanceToTop(radius, cosine);
        int fineSteps = 20000;
        double depth = 0.0;
        for (int step = 0; step < fineSteps; step++) {
            double distance = (step + 0.5) * length / fineSteps;
            depth += coefficients[atmosphere.altitudeSlot(SkyAtmosphere.radiusAlongRay(radius, cosine, distance))
                    + channel] * length / fineSteps;
        }
        return depth;
    }

    /**
     * Rays from the camera along every row of the sky view, and rays from other altitudes in every vertical direction,
     * must see the whole ice layer, which uniform steps over the ray left to one or two samples.
     * Rays from the camera cross the whole layer. Rays from other altitudes may start inside it or graze it, where the
     * twelve steps of a crossing spread over a longer chord, so they get a wider tolerance.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void rayStepsCoverTheIceLayerOnEveryPath(GameTestHelper helper) {
        SkyAtmosphere atmosphere = new SkyAtmosphere(
                SkySettings.DEFAULT.with(SkySetting.NANOBOT_OPTICAL_DEPTH, 0.0).withIceOpticalDepth(1.0));
        assertStepsCoverIce(helper, atmosphere, SkyAtmosphere.CAMERA_RADIUS, 1.0, 0.01);
        for (int row = 0; row < SkyTables.VIEW_HEIGHT; row++) {
            assertStepsCoverIce(helper, atmosphere, SkyAtmosphere.CAMERA_RADIUS,
                    Math.sin(SkyTables.rowElevation(row)), 0.01);
        }
        for (double altitude : RAY_START_ALTITUDES) {
            for (int direction = 0; direction <= 40; direction++) {
                assertStepsCoverIce(helper, atmosphere, SkyAtmosphere.GROUND_RADIUS + altitude,
                        -1.0 + 2.0 * direction / 40.0, 0.02);
            }
        }
        helper.succeed();
    }

    /**
     * Checks that the steps of one ray follow each other without gaps or overlaps up to its end, and that their ice
     * optical depth matches a fine integration within the relative tolerance.
     */
    private static void assertStepsCoverIce(GameTestHelper helper, SkyAtmosphere atmosphere, double radius,
            double cosine, double tolerance) {
        double groundDistance = SkyAtmosphere.distanceToGroundOrInfinity(radius, cosine);
        double length = groundDistance < Double.POSITIVE_INFINITY ? groundDistance
                : SkyAtmosphere.distanceToTop(radius, cosine);
        int maximumSteps = SkyRaySteps.maximumSteps(UNIFORM_STEPS, SkyRaySteps.ICE_BAND_STEPS);
        double[] travelled = new double[maximumSteps];
        double[] lengths = new double[maximumSteps];
        int count = SkyRaySteps.fill(radius, cosine, length, UNIFORM_STEPS, SkyRaySteps.ICE_BAND_STEPS,
                new double[SkyRaySteps.MAXIMUM_BOUNDARIES], travelled, lengths, 0);
        String ray = "ray from radius " + radius + " with cosine " + cosine;
        double end = 0.0;
        double depth = 0.0;
        for (int step = 0; step < count; step++) {
            double start = travelled[step] - 0.5 * lengths[step];
            helper.assertTrue(Math.abs(start - end) < 1.0e-6 * length,
                    "Step " + step + " of the " + ray + " starts at " + start + " where the step before ended at "
                            + end);
            end = travelled[step] + 0.5 * lengths[step];
            depth += iceExtinction(atmosphere, radius, cosine, travelled[step]) * lengths[step];
        }
        helper.assertTrue(Math.abs(end - length) < 1.0e-6 * length,
                "The steps of the " + ray + " end at " + end + " instead of " + length);
        double exactDepth = 0.0;
        int fineSteps = 20000;
        for (int step = 0; step < fineSteps; step++) {
            exactDepth += iceExtinction(atmosphere, radius, cosine, (step + 0.5) * length / fineSteps) * length
                    / fineSteps;
        }
        if (exactDepth > 0.01) {
            helper.assertTrue(Math.abs(depth / exactDepth - 1.0) < tolerance,
                    "Ice optical depth along the steps of the " + ray + " is " + depth + ", fine integration "
                            + exactDepth);
        }
    }

    private static double iceExtinction(SkyAtmosphere atmosphere, double radius, double cosine, double distance) {
        return atmosphere.iceScattering[atmosphere.altitudeSlot(SkyAtmosphere.radiusAlongRay(radius, cosine, distance))
                + SkyAtmosphere.GREEN_CHANNEL];
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void fogColorFollowsTheLowSun(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        double sunElevation = Math.toRadians(4.5);
        SkyView view = new SkyTables(SkySettings.DEFAULT).computeView(sunElevation,
                new IceHaloProfile(data, blurSigma), new IceHaloMap(data, sunElevation, blurSigma));
        SkyViewSet views = SkyViewSet.of(view);
        float[] towardSun = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] awayFromSun = new float[SkyAtmosphere.CHANNEL_COUNT];
        views.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, towardSun);
        views.horizonDisplayColor(0.0, 0.0, Math.PI, 1.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, awayFromSun);
        helper.assertTrue(towardSun[0] / towardSun[2] > 1.1F * awayFromSun[0] / awayFromSun[2],
                "Fog toward a sun at 4.5 degrees is warmer than away from it, red over blue "
                        + towardSun[0] / towardSun[2] + " against " + awayFromSun[0] / awayFromSun[2]);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(towardSun[channel] > 0.0F && towardSun[channel] <= 1.0F,
                    "Fog channel " + channel + " toward the sun is " + towardSun[channel]);
        }

        float[] upTowardSun = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] upAwayFromSun = new float[SkyAtmosphere.CHANNEL_COUNT];
        views.horizonDisplayColor(0.0, 0.0, 0.0, 0.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, upTowardSun);
        views.horizonDisplayColor(0.0, 0.0, Math.PI, 0.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, upAwayFromSun);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(upTowardSun[channel] == upAwayFromSun[channel],
                    "Looking straight up uses the mean horizon in every direction, channel " + channel);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void daylightDimsWithTheSunIntoVanillaNightAndRain(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        SkyTables tables = new SkyTables(SkySettings.DEFAULT);
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        float[] daylight = new float[SkyAtmosphere.CHANNEL_COUNT];
        double previousLuminance = Double.POSITIVE_INFINITY;
        for (double sunElevationDegrees : DAYLIGHT_SUN_DEGREES) {
            double sunElevation = Math.toRadians(sunElevationDegrees);
            IceHaloMap map = new IceHaloMap(data, Math.toRadians(Math.max(-6.0, sunElevationDegrees)), blurSigma);
            SkyViewSet views = SkyViewSet.of(tables.computeView(sunElevation, profile, map));
            views.daylightColor(0.0, 0.0, DEFAULT_EXPOSURE, 0.0F,
                    vanillaSkyDarken(sunElevationDegrees, 0.0F), daylight);
            double luminance = 0.2126 * daylight[0] + 0.7152 * daylight[1] + 0.0722 * daylight[2];
            helper.assertTrue(luminance <= previousLuminance + 1.0e-6,
                    "Daylight at sun " + sunElevationDegrees + " has luminance " + luminance
                            + ", brighter than " + previousLuminance + " at the higher sun before");
            previousLuminance = luminance;
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                if (sunElevationDegrees == 90.0) {
                    helper.assertTrue(daylight[channel] > 0.95F && daylight[channel] <= 1.0F,
                            "Daylight at noon is close to white, channel " + channel + " is " + daylight[channel]);
                }
                if (sunElevationDegrees == -18.0) {
                    float night = SkyView.vanillaSkyLight(channel, 0.2F);
                    helper.assertTrue(Math.abs(daylight[channel] - night) < 1.0e-6F,
                            "Daylight at sun -18 is vanilla's night, channel " + channel + " is " + daylight[channel]
                                    + ", vanilla " + night);
                }
            }
            if (sunElevationDegrees == 30.0) {
                float rainSkyDarken = vanillaSkyDarken(sunElevationDegrees, 1.0F);
                views.daylightColor(0.0, 0.0, DEFAULT_EXPOSURE, 1.0F, rainSkyDarken, daylight);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    float vanilla = SkyView.vanillaSkyLight(channel, rainSkyDarken);
                    helper.assertTrue(Math.abs(daylight[channel] - vanilla) < 1.0e-6F,
                            "Daylight in full rain is vanilla's sky light, channel " + channel + " is "
                                    + daylight[channel] + ", vanilla " + vanilla);
                }
            }
        }
        helper.succeed();
    }

    /**
     * The sky darkness that ClientLevel.getSkyDarken gives for a sun elevation and rain level without thunder.
     */
    private static float vanillaSkyDarken(double sunElevationDegrees, float rainLevel) {
        float sunHeight = (float) Math.sin(Math.toRadians(sunElevationDegrees));
        float skyDarken = 1.0F - Math.max(0.0F, Math.min(1.0F, 1.0F - (sunHeight * 2.0F + 0.2F)));
        skyDarken *= 1.0F - rainLevel * 5.0F / 16.0F;
        return skyDarken * 0.8F + 0.2F;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sunReddensTowardTheHorizonAndSetsBelowIt(GameTestHelper helper) {
        float[] sunRadiance = new SkyTables(SkySettings.DEFAULT).sunRadiance();
        double redOverBlueAt2 = Double.NaN;
        double redOverBlueAt30 = Double.NaN;
        for (int row = 0; row < SkyTables.VIEW_HEIGHT; row++) {
            double elevationDegrees = Math.toDegrees(SkyTables.rowElevation(row));
            float red = sunRadiance[row * SkyAtmosphere.CHANNEL_COUNT];
            float green = sunRadiance[row * SkyAtmosphere.CHANNEL_COUNT + 1];
            float blue = sunRadiance[row * SkyAtmosphere.CHANNEL_COUNT + 2];
            if (elevationDegrees < -Math.toDegrees(SkyAtmosphere.HORIZON_DIP_RADIANS)) {
                helper.assertTrue(red == 0.0F && green == 0.0F && blue == 0.0F,
                        "The sun is hidden below the horizon at elevation " + elevationDegrees);
                continue;
            }
            helper.assertTrue(red > 0.0F && red >= green && green >= blue && blue >= 0.0F,
                    "The sun at elevation " + elevationDegrees + " is " + red + ", " + green + ", " + blue
                            + ", where red should be at least green and green at least blue");
            if (Double.isNaN(redOverBlueAt2) && elevationDegrees >= 2.0) {
                redOverBlueAt2 = red / blue;
            }
            if (Double.isNaN(redOverBlueAt30) && elevationDegrees >= 30.0) {
                redOverBlueAt30 = red / blue;
            }
        }
        helper.assertTrue(redOverBlueAt2 > 10.0 * redOverBlueAt30,
                "Red over blue of the sun is " + redOverBlueAt2 + " at 2 degrees and " + redOverBlueAt30
                        + " at 30 degrees");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void skyLightPutsTheMoonOppositeTheSun(GameTestHelper helper) {
        SkyLight sun = new SkyLight();
        SkyLight moon = new SkyLight();
        for (double angle : SKY_LIGHT_ANGLES) {
            sun.set(SkyLight.Kind.SUN, angle, 1.0);
            moon.set(SkyLight.Kind.MOON, angle, 0.25);
            String where = "At the time of day angle " + angle;
            helper.assertTrue(sun.kind() == SkyLight.Kind.SUN, where + " the sun's kind is " + sun.kind()
                    + " instead of SUN");
            helper.assertTrue(moon.kind() == SkyLight.Kind.MOON, where + " the moon's kind is " + moon.kind()
                    + " instead of MOON");
            helper.assertTrue(sun.scale() == 1.0, where + " the sun's scale is " + sun.scale() + " instead of 1");
            helper.assertTrue(moon.scale() == 0.25, where + " the moon's scale is " + moon.scale()
                    + " instead of 0.25");
            helper.assertTrue(sun.elevationRadians() == Math.asin(Math.cos(angle)), where + " the sun's elevation is "
                    + sun.elevationRadians() + " radians instead of the " + Math.asin(Math.cos(angle))
                    + " that the cosine of the angle gives");
            helper.assertTrue(sun.x() == (float) -Math.sin(angle), where + " the sun's x is " + sun.x()
                    + " instead of " + (float) -Math.sin(angle));
            helper.assertTrue(sun.y() == (float) Math.cos(angle), where + " the sun's y is " + sun.y()
                    + " instead of " + (float) Math.cos(angle));
            helper.assertTrue(sun.side() == (Math.sin(angle) > 0.0 ? -1.0F : 1.0F), where + " the sun's side is "
                    + sun.side() + " instead of " + (Math.sin(angle) > 0.0 ? -1.0F : 1.0F));
            helper.assertTrue(moon.elevationRadians() == -sun.elevationRadians(), where + " the moon's elevation is "
                    + moon.elevationRadians() + " radians instead of the sun's " + sun.elevationRadians()
                    + " negated");
            helper.assertTrue(moon.x() == -sun.x(), where + " the moon's x is " + moon.x() + " instead of the sun's "
                    + sun.x() + " negated");
            helper.assertTrue(moon.y() == -sun.y(), where + " the moon's y is " + moon.y() + " instead of the sun's "
                    + sun.y() + " negated");
            helper.assertTrue(moon.side() == -sun.side(), where + " the moon's side is " + moon.side()
                    + " instead of the sun's " + sun.side() + " negated");
        }
        sun.set(SkyLight.Kind.SUN, 2.0, 1.0);
        moon.set(SkyLight.Kind.SUN, 2.0, 1.0);
        helper.assertTrue(sun.kind() == SkyLight.Kind.SUN, "The sun set again has the kind " + sun.kind()
                + " instead of SUN");
        helper.assertTrue(moon.kind() == sun.kind(), "A light reused as the sun has the kind " + moon.kind()
                + " instead of the sun's " + sun.kind());
        helper.assertTrue(moon.scale() == sun.scale(), "A light reused as the sun has the scale " + moon.scale()
                + " instead of the sun's " + sun.scale());
        helper.assertTrue(moon.elevationRadians() == sun.elevationRadians(), "A light reused as the sun has the "
                + "elevation " + moon.elevationRadians() + " radians instead of the sun's " + sun.elevationRadians());
        helper.assertTrue(moon.x() == sun.x(), "A light reused as the sun has the x " + moon.x()
                + " instead of the sun's " + sun.x());
        helper.assertTrue(moon.y() == sun.y(), "A light reused as the sun has the y " + moon.y()
                + " instead of the sun's " + sun.y());
        helper.assertTrue(moon.side() == sun.side(), "A light reused as the sun has the side " + moon.side()
                + " instead of the sun's " + sun.side());
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sunElevationTimesRoundTripOnBothBranches(GameTestHelper helper) {
        double largestError = 0.0;
        for (int step = 0; step <= ROUND_TRIP_STEPS; step++) {
            double elevationDegrees = ROUND_TRIP_LOWEST_SUN_DEGREES + ROUND_TRIP_SUN_STEP_DEGREES * step;
            for (SunElevationTimes.Branch branch : SunElevationTimes.Branch.values()) {
                long time = SunElevationTimes.dayTime(elevationDegrees, branch);
                String where = "The sun at " + elevationDegrees + " degrees on the " + branch + " branch";
                helper.assertTrue(time >= 0 && time < Level.TICKS_PER_DAY, where + " stands at day time " + time
                        + ", outside the day of " + Level.TICKS_PER_DAY + " ticks");
                float timeOfDay = helper.getLevel().dimensionType().timeOfDay(time);
                double back = Math.toDegrees(Math.asin(Math.cos(timeOfDay * Math.PI * 2.0)));
                double error = Math.abs(back - elevationDegrees);
                helper.assertTrue(error <= ROUND_TRIP_TOLERANCE_DEGREES, where + " comes back as " + back
                        + " degrees at day time " + time + ", off by " + error + " degrees");
                largestError = Math.max(largestError, error);
                if (elevationDegrees <= 89.0) {
                    double sine = Math.sin(timeOfDay * Math.PI * 2.0);
                    boolean setting = branch == SunElevationTimes.Branch.SETTING;
                    helper.assertTrue(setting ? sine > 0.0 : sine < 0.0, where + " has the sine " + sine
                            + " of the time of day angle at day time " + time + ", on the wrong side of 0");
                }
            }
        }
        LogUtils.getLogger().info("SunElevationTimes largest round trip error " + largestError + " degrees");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sunElevationTimesAreSymmetricAroundMidnight(GameTestHelper helper) {
        long noon = SunElevationTimes.dayTime(90.0, SunElevationTimes.Branch.SETTING);
        helper.assertTrue(noon == NOON_DAY_TIME, "The sun at 90 degrees sets at day time " + noon + " instead of the "
                + "noon " + NOON_DAY_TIME);
        long risingNoon = SunElevationTimes.dayTime(90.0, SunElevationTimes.Branch.RISING);
        helper.assertTrue(risingNoon == NOON_DAY_TIME, "The sun at 90 degrees rises at day time " + risingNoon
                + " instead of the noon " + NOON_DAY_TIME);
        long midnight = SunElevationTimes.dayTime(-90.0, SunElevationTimes.Branch.SETTING);
        helper.assertTrue(midnight == MIDNIGHT_DAY_TIME, "The sun at -90 degrees sets at day time " + midnight
                + " instead of the midnight " + MIDNIGHT_DAY_TIME);
        long risingMidnight = SunElevationTimes.dayTime(-90.0, SunElevationTimes.Branch.RISING);
        helper.assertTrue(risingMidnight == MIDNIGHT_DAY_TIME, "The sun at -90 degrees rises at day time "
                + risingMidnight + " instead of the midnight " + MIDNIGHT_DAY_TIME);
        long sunset = SunElevationTimes.dayTime(0.0, SunElevationTimes.Branch.SETTING);
        long sunrise = SunElevationTimes.dayTime(0.0, SunElevationTimes.Branch.RISING);
        LogUtils.getLogger().info("SunElevationTimes sunset " + sunset + " sunrise " + sunrise);
        helper.assertTrue(sunset == SUNSET_DAY_TIME, "The sun at 0 degrees sets at day time " + sunset
                + " instead of " + SUNSET_DAY_TIME);
        helper.assertTrue(sunrise == SUNRISE_DAY_TIME, "The sun at 0 degrees rises at day time " + sunrise
                + " instead of " + SUNRISE_DAY_TIME);
        for (int step = 0; step <= MIRROR_STEPS; step++) {
            double elevationDegrees = MIRROR_LOWEST_SUN_DEGREES + MIRROR_SUN_STEP_DEGREES * step;
            long setting = SunElevationTimes.dayTime(elevationDegrees, SunElevationTimes.Branch.SETTING);
            long rising = SunElevationTimes.dayTime(elevationDegrees, SunElevationTimes.Branch.RISING);
            long mirror = Math.floorMod(setting + rising, Level.TICKS_PER_DAY);
            helper.assertTrue(Math.abs(mirror - MIRROR_DAY_TIME) <= MIRROR_DAY_TIME_TOLERANCE, "The sun at "
                    + elevationDegrees + " degrees sets at " + setting + " and rises at " + rising + ", which add up "
                    + "to " + mirror + " instead of " + MIRROR_DAY_TIME + " within a tick");
        }
        long highSetting = SunElevationTimes.dayTime(120.0, SunElevationTimes.Branch.SETTING);
        helper.assertTrue(highSetting == noon, "The sun at 120 degrees sets at day time " + highSetting
                + " instead of at the noon " + noon + " of 90 degrees");
        long lowRising = SunElevationTimes.dayTime(-120.0, SunElevationTimes.Branch.RISING);
        helper.assertTrue(lowRising == midnight, "The sun at -120 degrees rises at day time " + lowRising
                + " instead of at the midnight " + midnight + " of -90 degrees");
        helper.assertTrue(SunElevationTimes.Branch.byKey("setting") == SunElevationTimes.Branch.SETTING,
                "The branch key setting gives " + SunElevationTimes.Branch.byKey("setting") + " instead of SETTING");
        helper.assertTrue(SunElevationTimes.Branch.byKey("rising") == SunElevationTimes.Branch.RISING,
                "The branch key rising gives " + SunElevationTimes.Branch.byKey("rising") + " instead of RISING");
        helper.assertTrue(SunElevationTimes.Branch.byKey("noon") == null,
                "The branch key noon gives " + SunElevationTimes.Branch.byKey("noon") + " instead of none");
        helper.succeed();
    }

    /**
     * A change of the texel size keeps the tables and builds the halo profile, the halo maps, and the views again.
     * A change of the nanobots then keeps the halo profile and maps, builds the tables again, and fills the views that
     * the first state dropped. A change of any setting that rebuilds nothing, such as the exposure, starts no build.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void settingsRebuildOnlyWhatDependsOnThem(GameTestHelper helper) {
        SkyBuilder builder = new SkyBuilder();
        int haloIndex = SkyBuilder.haloGridIndex(Math.toRadians(20.0));
        int depthIndex = IceDepthGrid.index(0.1);
        int viewIndex = SkyBuilder.viewGridIndex(Math.toRadians(20.0));
        SkySettings texelChanged = SkySettings.DEFAULT.with(SkySetting.TEXEL_DEGREES, 1.0);
        SkySettings nanobotsChanged = texelChanged.with(SkySetting.NANOBOT_OPTICAL_DEPTH, 0.5);
        SkyState[] states = new SkyState[3];
        helper.startSequence()
                .thenExecute(() -> builder.request(SkySettings.DEFAULT, haloIndex, depthIndex, viewIndex))
                .thenWaitUntil(() -> states[0] = installFinished(builder))
                .thenExecute(() -> builder.request(texelChanged, haloIndex, depthIndex, viewIndex))
                .thenWaitUntil(() -> states[1] = installFinished(builder))
                .thenExecute(() -> {
                    SkyState before = states[0];
                    SkyState after = states[1];
                    helper.assertTrue(after.lowDepthTables() == before.lowDepthTables()
                                    && after.highDepthTables() == before.highDepthTables(),
                            "A change of the texel size built the tables again");
                    helper.assertTrue(after.haloProfile() != before.haloProfile(),
                            "A change of the texel size kept the halo profile");
                    for (int slot = 0; slot < SkyState.HALO_COUNT; slot++) {
                        helper.assertTrue(!before.holdsHalo(after.halo(slot)),
                                "A change of the texel size kept halo map " + slot);
                    }
                    for (int slot = 0; slot < SkyViewSet.VIEW_COUNT; slot++) {
                        helper.assertTrue(!before.views().holds(after.views().view(slot)),
                                "A change of the texel size kept view " + slot);
                    }
                    builder.request(nanobotsChanged, haloIndex, depthIndex, viewIndex);
                })
                .thenWaitUntil(() -> states[2] = installFinished(builder))
                .thenExecute(() -> {
                    SkyState before = states[1];
                    SkyState after = states[2];
                    helper.assertTrue(after.lowDepthTables() != before.lowDepthTables()
                                    && after.highDepthTables() != before.highDepthTables(),
                            "A change of the nanobots kept the tables");
                    helper.assertTrue(after.haloProfile() == before.haloProfile(),
                            "A change of the nanobots built the halo profile again");
                    for (int slot = 0; slot < SkyState.HALO_COUNT; slot++) {
                        helper.assertTrue(after.halo(slot) == before.halo(slot),
                                "A change of the nanobots built halo map " + slot + " again");
                    }
                    for (int slot = 0; slot < SkyViewSet.VIEW_COUNT; slot++) {
                        SkyView view = after.views().view(slot);
                        helper.assertTrue(!before.views().holds(view),
                                "A change of the nanobots kept view " + slot);
                        helper.assertTrue(states[0].views().holds(view),
                                "View " + slot + " after the nanobot change is not one the first state dropped");
                    }
                    for (SkySetting setting : SkySetting.values()) {
                        if (setting.rebuilds() != SkySetting.Rebuilds.NOTHING) {
                            continue;
                        }
                        double changed = setting.isSwitch() ? 1.0 - setting.defaultValue()
                                : setting.defaultValue() == setting.maximum() ? setting.minimum() : setting.maximum();
                        builder.request(nanobotsChanged.with(setting, changed), haloIndex, depthIndex, viewIndex);
                        helper.assertTrue(!builder.isBuilding() && builder.state() == after,
                                "A change of " + setting.key() + " started a build");
                    }
                })
                .thenSucceed();
    }

    /**
     * Installs the build that finished and returns its state, or throws to wait another tick.
     */
    private static SkyState installFinished(SkyBuilder builder) {
        SkyState finished = builder.takeFinished();
        if (finished == null) {
            throw new GameTestAssertException("Waiting for the build");
        }
        builder.install(finished);
        return finished;
    }

    private static void assertMatchesPreview(GameTestHelper helper, SkyView view, IceHaloProfile profile,
            IceHaloMap map, float[] diffractionWeights, double[][] expected, double sunElevationDegrees) {
        for (int index = 0; index < DIRECTIONS_DEGREES.length; index++) {
            double azimuth = Math.toRadians(DIRECTIONS_DEGREES[index][0]);
            double elevation = Math.toRadians(DIRECTIONS_DEGREES[index][1]);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                double actual = view.radiance(channel, azimuth, elevation, profile, map, diffractionWeights);
                double relativeError = Math.abs(actual / expected[index][channel] - 1.0);
                helper.assertTrue(relativeError < 0.01,
                        "Sun " + sunElevationDegrees + ", azimuth " + DIRECTIONS_DEGREES[index][0] + ", elevation "
                                + DIRECTIONS_DEGREES[index][1] + ", channel " + channel + " is " + actual
                                + ", preview " + expected[index][channel]);
            }
        }
    }

    private static void assertNormalized(GameTestHelper helper, String name, DoubleUnaryOperator phase,
            double tolerance) {
        double integral = integrate(phase);
        helper.assertTrue(Math.abs(integral - 1.0) < tolerance, name + " phase integrates to " + integral);
    }

    private static double integrate(DoubleUnaryOperator phase) {
        int steps = 2000000;
        double sum = 0.0;
        for (int step = 0; step < steps; step++) {
            sum += phase.applyAsDouble(-1.0 + 2.0 * (step + 0.5) / steps);
        }
        return sum * 2.0 / steps * 2.0 * Math.PI;
    }
}
