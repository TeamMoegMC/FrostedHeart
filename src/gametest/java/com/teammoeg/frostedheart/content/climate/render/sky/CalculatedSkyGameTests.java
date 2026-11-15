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

import java.util.Arrays;
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
    /**
     * The moon's brightness relative to the full moon for phases 0 to 4, from Krisciunas and Schaefer 1991.
     */
    private static final double[] MOON_PHASE_BRIGHTNESS = {1.0, 0.33529, 0.09100, 0.011603, 0.0002808};
    private static final double MOON_PHASE_BRIGHTNESS_TOLERANCE = 0.003;
    private static final double[] FADE_TEST_SWITCHES_DEGREES = {-9.5, -4.0, -16.0};
    private static final double[] FADE_TEST_FADES_DEGREES = {4.0, 1.0, 10.0};
    /**
     * The sun fade scan starts one degree below the switch and steps by a hundredth of a degree.
     */
    private static final double SUN_FADE_SCAN_LOWEST_BELOW_DEGREES = 1.0;
    private static final double SUN_FADE_SCAN_STEP_DEGREES = 0.01;
    private static final int SUN_FADE_SCAN_STEPS = 500;
    /**
     * The first and last steps of the sun fade scan whose elevation and the one before it lie from 0.01 to 2.99
     * degrees above the switch.
     */
    private static final int SUN_FADE_STRICT_FIRST_STEP = 102;
    private static final int SUN_FADE_STRICT_LAST_STEP = 399;
    /**
     * The settings of the light scan, fixed so that the scan's elevations below stay right when the defaults change.
     */
    private static final double MOONLIGHT_SWITCH_DEGREES = -9.5;
    private static final double MOONLIGHT_FADE_DEGREES = 4.0;
    private static final double MOONLIGHT_BRIGHTNESS = 0.025;
    private static final double MOONLIGHT_BLUE_SHIFT = 0.5;
    private static final double MOONLIGHT_SCAN_LOWEST_DEGREES = -20.0;
    private static final double MOONLIGHT_SCAN_STEP_DEGREES = 0.05;
    private static final int MOONLIGHT_SCAN_STEPS = 600;
    /**
     * The step of the scan whose sun stands at the switch elevation.
     */
    private static final int MOONLIGHT_SWITCH_STEP = 210;
    private static final double MOONLIGHT_SCAN_LARGEST_STEP = 0.03;
    private static final double MOONLIGHT_SWITCH_STEP_LIMIT = 1.0e-6;
    private static final double MOONLIGHT_SUN_SIDE_DEGREES = -9.49;
    private static final double MOONLIGHT_MOON_SIDE_DEGREES = -9.51;
    private static final double MOONLIGHT_FULL_SUN_DEGREES = -6.49;
    private static final double MOONLIGHT_FULL_MOON_DEGREES = -13.51;
    private static final double MOONLIGHT_TOLERANCE = 1.0e-12;
    private static final double MOON_COLOR_TOLERANCE = 1.0e-12;
    private static final double MOON_LUMINANCE_TOLERANCE = 0.05;
    private static final double[] MOON_BLUE_SHIFTS = {0.0, 0.5, 1.0};
    /**
     * A moonlight scale that dims and tints each channel, and a scale of 1 and of 0 in every channel.
     */
    private static final double[] MOONLIGHT_LIGHT_SCALE = {0.03, 0.025, 0.02};
    private static final double[] FULL_LIGHT_SCALE = {1.0, 1.0, 1.0};
    private static final double[] NO_LIGHT_SCALE = {0.0, 0.0, 0.0};
    private static final double[] FOG_AZIMUTHS = {0.0, 1.0, Math.PI};
    private static final double[] FOG_HORIZONTAL_FRACTIONS = {1.0, 0.4, 0.0};
    private static final float[][] FOG_WEATHERS = {{0.0F, 0.0F}, {0.8F, 0.5F}};
    /**
     * The fog color at zero weather can miss the tone mapped radiance by one float step, since the weather greying
     * rounds even when it keeps the whole color.
     */
    private static final double FOG_DISPLAY_TOLERANCE = 1.0e-6;
    private static final float[] DAYLIGHT_RAINS = {0.0F, 0.8F};
    private static final float[] DAYLIGHT_SKY_DARKENS = {0.2F, 1.0F};
    /**
     * The sky darkness that ClientLevel.getSkyDarken returns at night.
     */
    private static final float NIGHT_SKY_DARKEN = 0.2F;
    private static final double[] SWITCH_DEPTHS = {IceWeatherLaw.CLEAR_MINIMUM_DEPTH, 0.5, 3.0};
    private static final int SWITCH_AZIMUTH_STEPS = 72;
    private static final int SWITCH_ELEVATION_STEPS = 180;
    private static final double SWITCH_AZIMUTH_STEP_DEGREES = 5.0;
    private static final double SWITCH_ELEVATION_STEP_DEGREES = 0.5;
    private static final double SWITCH_DISPLAY_STEPS = 0.5;
    /**
     * The phases whose night sky is checked, the full moon, the half moon, and the new moon.
     */
    private static final int[] MOON_NIGHT_PHASES = {0, 2, 4};
    private static final int MOON_NIGHT_AZIMUTH_COUNT = 4;
    private static final double MOON_NIGHT_AZIMUTH_STEP_DEGREES = 90.0;
    /**
     * The sun stands at the nadir, which puts the moon overhead, and the sky is read 30 degrees from the moon.
     */
    private static final double MOON_NIGHT_SUN_ELEVATION_DEGREES = -90.0;
    private static final double MOON_NIGHT_SKY_ELEVATION_DEGREES = 60.0;
    private static final double FULL_MOON_GREEN_SKY_STEPS_MINIMUM = 6.0;
    private static final double FULL_MOON_GREEN_SKY_STEPS_MAXIMUM = 35.0;
    private static final double FULL_MOON_GREEN_FOG_STEPS_MINIMUM = 15.0;
    private static final double FULL_MOON_GREEN_FOG_STEPS_MAXIMUM = 55.0;
    private static final double NIGHT_SKY_STEPS_LIMIT = 64.0;
    private static final double NEW_MOON_STEPS_LIMIT = 0.5;
    /**
     * The sun elevations whose halo maps are checked for negative values, from below the horizon to well above it.
     */
    private static final double[] MIRROR_PILLAR_SUN_DEGREES = {
            -6.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 5.0, 8.0, 15.0, 30.0
    };
    private static final double MIRROR_PILLAR_MAP_SUN_DEGREES = -3.0;
    private static final double MIRROR_PILLAR_ROW_LOWEST_DEGREES = 0.0;
    private static final double MIRROR_PILLAR_ROW_HIGHEST_DEGREES = 12.0;
    /**
     * The pillar of plates spreads over about half a degree across, so its peak stays within the row that holds the
     * sun's mirror image.
     */
    private static final double MIRROR_PILLAR_PEAK_TOLERANCE_DEGREES = 0.75;
    private static final int MIRROR_PILLAR_SIDE_COLUMN = 8;
    private static final double MIRROR_PILLAR_SIDE_RATIO = 4.0;
    private static final String LEVEL_PLATE_NAME = "plates";
    private static final String WOBBLING_PLATE_NAME = "wobbling plates";
    private static final double[] MIRROR_PILLAR_CLOSED_FORM_SUN_DEGREES = {5.0, 10.0, 30.0};
    /**
     * The mean side area over the basal area of a level plate per unit of half height over radius, as the closed form
     * of the level plate pillar holds it.
     */
    private static final double LEVEL_PLATE_SIDE_AREA = 1.47;
    private static final double MIRROR_PILLAR_CLOSED_FORM_TOLERANCE = 0.03;
    private static final double MIRROR_PILLAR_SUNSET_MINIMUM = 0.1;
    // The traced mirrored share is 0.84 to 0.96 times the slab formula from sun -3 to 30 degrees, so 0.75 leaves room:
    private static final double MIRROR_PILLAR_TRACED_LEAST_SHARE = 0.75;
    private static final double[] MIRROR_PILLAR_INSIDE_SUN_DEGREES = {-3.0, 3.0, 5.0};
    private static final double MIRROR_PILLAR_INSIDE_WEIGHT = 0.1;
    // The map sums the pillar at row centers, so its part inside the map differs a little from the integral:
    private static final double MIRROR_PILLAR_INSIDE_TOLERANCE = 0.005;
    private static final double MIRROR_PILLAR_INSIDE_STEP_DEGREES = 0.001;
    private static final double[] MIRROR_PILLAR_PRINT_SUN_DEGREES = {
            -3.0, 0.0, 1.0, 2.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0
    };
    private static final double MIRROR_PILLAR_SPHERE_STEP_DEGREES = 0.01;
    private static final double MIRROR_PILLAR_PHASE_SUN_DEGREES = 3.0;
    private static final double MIRROR_PILLAR_PHASE_STEP_DEGREES = 0.02;
    private static final double MIRROR_PILLAR_PHASE_ELEVATION_LIMIT_DEGREES = 60.0;
    private static final double MIRROR_PILLAR_PHASE_SIDE_LIMIT_DEGREES = 15.0;
    private static final double MIRROR_PILLAR_EXTRA_VARIANCE_DEGREES = 0.5;
    private static final double MIRROR_PILLAR_PHASE_TOLERANCE = 0.03;

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

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void moonPhaseBrightnessFollowsThePhaseAngle(GameTestHelper helper) {
        double[] brightness = new double[MoonLight.PHASE_COUNT];
        for (int phase = 0; phase < MoonLight.PHASE_COUNT; phase++) {
            brightness[phase] = MoonLight.phaseBrightness(phase);
        }
        LogUtils.getLogger().info("MoonLight phase brightness " + brightness[0] + " " + brightness[1] + " "
                + brightness[2] + " " + brightness[3] + " " + brightness[4] + " " + brightness[5] + " "
                + brightness[6] + " " + brightness[7]);
        helper.assertTrue(brightness[0] == 1.0, "The full moon has the brightness " + brightness[0]
                + " instead of 1");
        for (int phase = 1; phase < MOON_PHASE_BRIGHTNESS.length; phase++) {
            double published = MOON_PHASE_BRIGHTNESS[phase];
            helper.assertTrue(Math.abs(brightness[phase] / published - 1.0) < MOON_PHASE_BRIGHTNESS_TOLERANCE,
                    "Phase " + phase + " has the brightness " + brightness[phase] + ", published " + published);
        }
        for (int phase = 1; phase < MoonLight.PHASE_COUNT - 1; phase++) {
            int mirroredPhase = MoonLight.PHASE_COUNT - phase;
            helper.assertTrue(brightness[phase] == brightness[mirroredPhase], "Phase " + phase + " has the brightness "
                    + brightness[phase] + ", phase " + mirroredPhase + " has " + brightness[mirroredPhase]);
        }
        for (int phase = 1; phase <= 4; phase++) {
            helper.assertTrue(brightness[phase] < brightness[phase - 1], "Phase " + phase + " has the brightness "
                    + brightness[phase] + ", not below the " + brightness[phase - 1] + " of phase " + (phase - 1));
        }
        double wrappedFull = MoonLight.phaseBrightness(MoonLight.PHASE_COUNT);
        helper.assertTrue(wrappedFull == brightness[0], "Phase " + MoonLight.PHASE_COUNT + " has the brightness "
                + wrappedFull + " instead of the full moon's " + brightness[0]);
        double beforeFull = MoonLight.phaseBrightness(-1);
        helper.assertTrue(beforeFull == brightness[7], "Phase -1 has the brightness " + beforeFull
                + " instead of the " + brightness[7] + " of phase 7");
        double beyondLast = MoonLight.phaseBrightness(13);
        helper.assertTrue(beyondLast == brightness[5], "Phase 13 has the brightness " + beyondLast
                + " instead of the " + brightness[5] + " of phase 5");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void sunAndMoonFadeToBlackAtTheSwitch(GameTestHelper helper) {
        for (double switchDegrees : FADE_TEST_SWITCHES_DEGREES) {
            for (double fadeDegrees : FADE_TEST_FADES_DEGREES) {
                String where = "With the switch at " + switchDegrees + " degrees and the fade at " + fadeDegrees
                        + " degrees";
                double atSwitch = MoonLight.sunFade(switchDegrees, switchDegrees);
                helper.assertTrue(atSwitch == 0.0, where + ", the sun at the switch fades to " + atSwitch
                        + " instead of 0");
                double fullSun = MoonLight.sunFade(switchDegrees + MoonLight.SUN_FADE_DEGREES, switchDegrees);
                helper.assertTrue(fullSun == 1.0, where + ", the sun " + MoonLight.SUN_FADE_DEGREES
                        + " degrees above the switch fades to " + fullSun + " instead of 1");
                double highSun = MoonLight.sunFade(switchDegrees + 20.0, switchDegrees);
                helper.assertTrue(highSun == 1.0, where + ", the sun 20 degrees above the switch fades to " + highSun
                        + " instead of 1");
                double belowSun = MoonLight.sunFade(switchDegrees - 1.0, switchDegrees);
                helper.assertTrue(belowSun == 0.0, where + ", the sun one degree below the switch fades to " + belowSun
                        + " instead of 0");
                double previousSunFade = belowSun;
                for (int step = 1; step <= SUN_FADE_SCAN_STEPS; step++) {
                    double elevationDegrees = switchDegrees - SUN_FADE_SCAN_LOWEST_BELOW_DEGREES
                            + SUN_FADE_SCAN_STEP_DEGREES * step;
                    double sunFade = MoonLight.sunFade(elevationDegrees, switchDegrees);
                    helper.assertTrue(sunFade >= previousSunFade, where + ", the sun at " + elevationDegrees
                            + " degrees fades to " + sunFade + ", below the " + previousSunFade + " of the step below");
                    if (step >= SUN_FADE_STRICT_FIRST_STEP && step <= SUN_FADE_STRICT_LAST_STEP) {
                        helper.assertTrue(sunFade > previousSunFade, where + ", the sun at " + elevationDegrees
                                + " degrees fades to " + sunFade + ", not above the " + previousSunFade
                                + " of the step below");
                    }
                    previousSunFade = sunFade;
                }
                double moonAtSwitch = MoonLight.moonFade(switchDegrees, switchDegrees, fadeDegrees);
                helper.assertTrue(moonAtSwitch == 0.0, where + ", the moon with the sun at the switch fades to "
                        + moonAtSwitch + " instead of 0");
                double fullMoon = MoonLight.moonFade(switchDegrees - fadeDegrees, switchDegrees, fadeDegrees);
                helper.assertTrue(fullMoon == 1.0, where + ", the moon with the sun " + fadeDegrees
                        + " degrees below the switch fades to " + fullMoon + " instead of 1");
                double deepMoon = MoonLight.moonFade(switchDegrees - fadeDegrees - 20.0, switchDegrees, fadeDegrees);
                helper.assertTrue(deepMoon == 1.0, where + ", the moon with the sun " + (fadeDegrees + 20.0)
                        + " degrees below the switch fades to " + deepMoon + " instead of 1");
                double aboveMoon = MoonLight.moonFade(switchDegrees + 1.0, switchDegrees, fadeDegrees);
                helper.assertTrue(aboveMoon == 0.0, where + ", the moon with the sun one degree above the switch fades "
                        + "to " + aboveMoon + " instead of 0");
                double previousMoonFade = aboveMoon;
                int moonSteps = (int) Math.round((fadeDegrees + 2.0) * 100.0);
                for (int step = 1; step <= moonSteps; step++) {
                    double elevationDegrees = switchDegrees + 1.0 - SUN_FADE_SCAN_STEP_DEGREES * step;
                    double moonFade = MoonLight.moonFade(elevationDegrees, switchDegrees, fadeDegrees);
                    helper.assertTrue(moonFade >= previousMoonFade, where + ", the moon with the sun at "
                            + elevationDegrees + " degrees fades to " + moonFade + ", below the " + previousMoonFade
                            + " of the step above");
                    previousMoonFade = moonFade;
                }
                double noFadeBelow = MoonLight.moonFade(switchDegrees - 0.001, switchDegrees, 0.0);
                helper.assertTrue(noFadeBelow == 1.0, where + ", a fade of 0 with the sun 0.001 degrees below the "
                        + "switch fades to " + noFadeBelow + " instead of 1");
                double noFadeAt = MoonLight.moonFade(switchDegrees, switchDegrees, 0.0);
                helper.assertTrue(noFadeAt == 0.0, where + ", a fade of 0 with the sun at the switch fades to "
                        + noFadeAt + " instead of 0");
                double noFadeAbove = MoonLight.moonFade(switchDegrees + 1.0, switchDegrees, 0.0);
                helper.assertTrue(noFadeAbove == 0.0, where + ", a fade of 0 with the sun one degree above the switch "
                        + "fades to " + noFadeAbove + " instead of 0");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void chooseLightTakesTheSunOrTheMoon(GameTestHelper helper) {
        SkyLight light = new SkyLight();
        for (boolean rising : new boolean[] {false, true}) {
            String where = "On the " + (rising ? "rising" : "setting") + " branch";
            MoonLight.chooseLight(light, timeOfDayAngle(30.0, rising), 0, MOONLIGHT_SWITCH_DEGREES,
                    MOONLIGHT_FADE_DEGREES, MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
            helper.assertTrue(light.kind() == SkyLight.Kind.SUN, where + " the sun at 30 degrees has the kind "
                    + light.kind() + " instead of SUN");
            helper.assertTrue(Math.abs(light.elevationRadians() - Math.toRadians(30.0)) < MOONLIGHT_TOLERANCE, where
                    + " the sun at 30 degrees has the elevation " + light.elevationRadians() + " radians instead of "
                    + Math.toRadians(30.0));
            helper.assertTrue(light.scale() == 1.0, where + " the sun at 30 degrees has the scale " + light.scale()
                    + " instead of 1");
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(light.color(channel) == 1.0, where + " the sun at 30 degrees has the color "
                        + light.color(channel) + " in channel " + channel + " instead of 1");
                helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.SUN) == 1.0, where + " the sun at 30 "
                        + "degrees has the radiance scale " + light.radianceScale(channel, SkyLight.Kind.SUN)
                        + " for a sun sky in channel " + channel + " instead of 1");
                helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.MOON) == 0.0, where + " the sun at 30 "
                        + "degrees has the radiance scale " + light.radianceScale(channel, SkyLight.Kind.MOON)
                        + " for a moon sky in channel " + channel + " instead of 0");
            }
            MoonLight.chooseLight(light, timeOfDayAngle(-30.0, rising), 0, MOONLIGHT_SWITCH_DEGREES,
                    MOONLIGHT_FADE_DEGREES, MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
            helper.assertTrue(light.kind() == SkyLight.Kind.MOON, where + " the sun at -30 degrees has the kind "
                    + light.kind() + " instead of MOON");
            helper.assertTrue(Math.abs(light.elevationRadians() - Math.toRadians(30.0)) < MOONLIGHT_TOLERANCE, where
                    + " the moon with the sun at -30 degrees has the elevation " + light.elevationRadians()
                    + " radians instead of the " + Math.toRadians(30.0) + " that the moon being up gives");
            double expectedScale = MOONLIGHT_BRIGHTNESS * MoonLight.phaseBrightness(0);
            helper.assertTrue(Math.abs(light.scale() / expectedScale - 1.0) < MOONLIGHT_TOLERANCE, where + " the full "
                    + "moon has the scale " + light.scale() + " instead of the " + expectedScale
                    + " of the brightness times the phase");
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(light.color(channel) == MoonLight.color(channel, MOONLIGHT_BLUE_SHIFT), where
                        + " the moon has the color " + light.color(channel) + " in channel " + channel + " instead of "
                        + MoonLight.color(channel, MOONLIGHT_BLUE_SHIFT));
                helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.MOON) == light.scale()
                                * light.color(channel), where + " the moon has the radiance scale "
                        + light.radianceScale(channel, SkyLight.Kind.MOON) + " for a moon sky in channel " + channel
                        + " instead of the " + light.scale() * light.color(channel) + " of its scale and color");
                helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.SUN) == 0.0, where + " the moon has the "
                        + "radiance scale " + light.radianceScale(channel, SkyLight.Kind.SUN) + " for a sun sky in "
                        + "channel " + channel + " instead of 0");
            }
            MoonLight.chooseLight(light, timeOfDayAngle(-30.0, rising), 2, MOONLIGHT_SWITCH_DEGREES,
                    MOONLIGHT_FADE_DEGREES, MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
            double expectedPhaseTwoScale = MOONLIGHT_BRIGHTNESS * MoonLight.phaseBrightness(2);
            helper.assertTrue(Math.abs(light.scale() / expectedPhaseTwoScale - 1.0) < MOONLIGHT_TOLERANCE, where
                    + " the moon at phase 2 has the scale " + light.scale() + " instead of the "
                    + expectedPhaseTwoScale + " of the brightness times the phase");

            double previousGreen = Double.NaN;
            SkyLight.Kind previousKind = null;
            double largestSunStep = 0.0;
            double largestMoonStep = 0.0;
            for (int step = 0; step <= MOONLIGHT_SCAN_STEPS; step++) {
                double elevationDegrees = MOONLIGHT_SCAN_LOWEST_DEGREES + MOONLIGHT_SCAN_STEP_DEGREES * step;
                MoonLight.chooseLight(light, timeOfDayAngle(elevationDegrees, rising), 0, MOONLIGHT_SWITCH_DEGREES,
                        MOONLIGHT_FADE_DEGREES, MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
                SkyLight.Kind kind = light.kind();
                double green = light.radianceScale(SkyAtmosphere.GREEN_CHANNEL, kind);
                String at = where + " with the sun at " + elevationDegrees + " degrees";
                helper.assertTrue(green >= 0.0, at + " the green radiance scale is " + green + ", below 0");
                if (step > 0) {
                    double difference = Math.abs(green - previousGreen);
                    helper.assertTrue(difference <= MOONLIGHT_SCAN_LARGEST_STEP, at + " the green radiance scale "
                            + "moves by " + difference + " from " + previousGreen + " to " + green + ", more than "
                            + MOONLIGHT_SCAN_LARGEST_STEP);
                    if (kind == previousKind) {
                        if (kind == SkyLight.Kind.SUN) {
                            largestSunStep = Math.max(largestSunStep, difference);
                        } else {
                            largestMoonStep = Math.max(largestMoonStep, difference);
                        }
                    }
                }
                if (step == MOONLIGHT_SWITCH_STEP) {
                    helper.assertTrue(green <= MOONLIGHT_SWITCH_STEP_LIMIT, at + " the sun stands at the switch, where "
                            + "the green radiance scale is " + green + " instead of at most "
                            + MOONLIGHT_SWITCH_STEP_LIMIT);
                }
                if (elevationDegrees > MOONLIGHT_SUN_SIDE_DEGREES) {
                    helper.assertTrue(kind == SkyLight.Kind.SUN, at + " the kind is " + kind + " instead of SUN");
                }
                if (elevationDegrees < MOONLIGHT_MOON_SIDE_DEGREES) {
                    helper.assertTrue(kind == SkyLight.Kind.MOON, at + " the kind is " + kind + " instead of MOON");
                }
                if (elevationDegrees >= MOONLIGHT_FULL_SUN_DEGREES) {
                    helper.assertTrue(green == 1.0, at + " the green radiance scale is " + green
                            + " instead of the full sun's 1");
                }
                if (elevationDegrees <= MOONLIGHT_FULL_MOON_DEGREES) {
                    double expectedGreen = MOONLIGHT_BRIGHTNESS * MoonLight.color(SkyAtmosphere.GREEN_CHANNEL,
                            MOONLIGHT_BLUE_SHIFT);
                    helper.assertTrue(Math.abs(green / expectedGreen - 1.0) < MOONLIGHT_TOLERANCE, at
                            + " the green radiance scale is " + green + " instead of the full moon's " + expectedGreen);
                }
                previousGreen = green;
                previousKind = kind;
            }
            LogUtils.getLogger().info("MoonLight largest step sun " + largestSunStep);
            LogUtils.getLogger().info("MoonLight largest step moon " + largestMoonStep);
        }
        MoonLight.chooseLight(light, timeOfDayAngle(30.0, false), 0, MOONLIGHT_SWITCH_DEGREES,
                MOONLIGHT_FADE_DEGREES, MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(light.color(channel) == 1.0, "A light reused as the sun after the moon has the color "
                    + light.color(channel) + " in channel " + channel + " instead of 1");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void moonColorBlendsFromItsOwnColorToBlue(GameTestHelper helper) {
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double ownColor = MoonLight.color(channel, 0.0);
            double blueColor = MoonLight.color(channel, 1.0);
            double halfShift = MoonLight.color(channel, 0.5);
            helper.assertTrue(Math.abs(halfShift - (ownColor + blueColor) / 2.0) < MOON_COLOR_TOLERANCE, "Channel "
                    + channel + " at a blue shift of 0.5 is " + halfShift + " instead of the " + (ownColor + blueColor)
                    / 2.0 + " halfway between " + ownColor + " and " + blueColor);
            double belowZero = MoonLight.color(channel, -1.0);
            helper.assertTrue(belowZero == ownColor, "Channel " + channel + " at a blue shift of -1 is " + belowZero
                    + " instead of the " + ownColor + " at 0");
            double aboveOne = MoonLight.color(channel, 2.0);
            helper.assertTrue(aboveOne == blueColor, "Channel " + channel + " at a blue shift of 2 is " + aboveOne
                    + " instead of the " + blueColor + " at 1");
        }
        double ownRed = MoonLight.color(0, 0.0);
        double ownBlue = MoonLight.color(2, 0.0);
        helper.assertTrue(ownRed > ownBlue, "The moon's own color has red " + ownRed + " and blue " + ownBlue
                + ", where red should be above blue");
        double shiftedRed = MoonLight.color(0, 1.0);
        double shiftedBlue = MoonLight.color(2, 1.0);
        helper.assertTrue(shiftedBlue > shiftedRed, "The moon's color at a blue shift of 1 has blue " + shiftedBlue
                + " and red " + shiftedRed + ", where blue should be above red");
        for (double blueShift : MOON_BLUE_SHIFTS) {
            double luminance = 0.2126 * MoonLight.color(0, blueShift) + 0.7152 * MoonLight.color(1, blueShift)
                    + 0.0722 * MoonLight.color(2, blueShift);
            helper.assertTrue(Math.abs(luminance - 1.0) < MOON_LUMINANCE_TOLERANCE, "The moon's color at a blue shift "
                    + "of " + blueShift + " has the luminance " + luminance + ", not within " + MOON_LUMINANCE_TOLERANCE
                    + " of 1");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void skyLightScalesOnlyTheKindItWasBuiltFor(GameTestHelper helper) {
        SkyLight light = new SkyLight();
        double[] expectedColor = {1.1, 1.0, 0.9};
        double[] expectedScale = {0.02 * 1.1, 0.02 * 1.0, 0.02 * 0.9};
        light.set(SkyLight.Kind.MOON, 2.0, 0.02, 1.1, 1.0, 0.9);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(light.color(channel) == expectedColor[channel], "The moon has the color "
                    + light.color(channel) + " in channel " + channel + " instead of " + expectedColor[channel]);
            helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.MOON) == expectedScale[channel], "The moon "
                    + "has the radiance scale " + light.radianceScale(channel, SkyLight.Kind.MOON) + " for a moon sky "
                    + "in channel " + channel + " instead of " + expectedScale[channel]);
            helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.SUN) == 0.0, "The moon has the radiance "
                    + "scale " + light.radianceScale(channel, SkyLight.Kind.SUN) + " for a sun sky in channel " + channel
                    + " instead of 0");
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.MOON) == 0.0, "The moon has the terrain "
                    + "scale " + light.terrainScale(channel, SkyLight.Kind.MOON) + " for a moon sky in channel "
                    + channel + " instead of 0");
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.SUN) == 0.0, "The moon has the terrain "
                    + "scale " + light.terrainScale(channel, SkyLight.Kind.SUN) + " for a sun sky in channel " + channel
                    + " instead of 0");
        }
        light.set(SkyLight.Kind.SUN, 2.0, 1.0);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(light.color(channel) == 1.0, "The sun set after the moon has the color "
                    + light.color(channel) + " in channel " + channel + " instead of 1");
            helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.SUN) == 1.0, "The sun set after the moon has "
                    + "the radiance scale " + light.radianceScale(channel, SkyLight.Kind.SUN) + " for a sun sky in "
                    + "channel " + channel + " instead of 1");
            helper.assertTrue(light.radianceScale(channel, SkyLight.Kind.MOON) == 0.0, "The sun set after the moon has "
                    + "the radiance scale " + light.radianceScale(channel, SkyLight.Kind.MOON) + " for a moon sky in "
                    + "channel " + channel + " instead of 0");
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.SUN) == 1.0, "The sun set after the moon has "
                    + "the terrain scale " + light.terrainScale(channel, SkyLight.Kind.SUN) + " for a sun sky in "
                    + "channel " + channel + " instead of 1");
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.MOON) == 0.0, "The sun set after the moon has "
                    + "the terrain scale " + light.terrainScale(channel, SkyLight.Kind.MOON) + " for a moon sky in "
                    + "channel " + channel + " instead of 0");
        }
        light.set(SkyLight.Kind.SUN, 2.0, 0.5);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.SUN) == 0.5, "The half strength sun has the "
                    + "terrain scale " + light.terrainScale(channel, SkyLight.Kind.SUN) + " for a sun sky in channel "
                    + channel + " instead of 0.5");
            helper.assertTrue(light.terrainScale(channel, SkyLight.Kind.MOON) == 0.0, "The half strength sun has the "
                    + "terrain scale " + light.terrainScale(channel, SkyLight.Kind.MOON) + " for a moon sky in channel "
                    + channel + " instead of 0");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void scaledFogAndDaylightFollowTheLightScale(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        double sunElevation = Math.toRadians(20.0);
        SkyView view = new SkyTables(SkySettings.DEFAULT).computeView(sunElevation,
                new IceHaloProfile(data, blurSigma), new IceHaloMap(data, sunElevation, blurSigma));
        SkyViewSet views = SkyViewSet.of(view);
        float[] scaled = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] unscaled = new float[SkyAtmosphere.CHANNEL_COUNT];
        for (double azimuth : FOG_AZIMUTHS) {
            for (double horizontalFraction : FOG_HORIZONTAL_FRACTIONS) {
                views.horizonDisplayColor(0.0, 0.0, azimuth, horizontalFraction, DEFAULT_EXPOSURE,
                        MOONLIGHT_LIGHT_SCALE, 0.0F, 0.0F, scaled);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double radiance = view.horizonRadiance(channel, azimuth, horizontalFraction)
                            * MOONLIGHT_LIGHT_SCALE[channel];
                    float expected = (float) SkyDisplay.displayValue(DEFAULT_EXPOSURE, radiance);
                    helper.assertTrue(Math.abs(scaled[channel] - expected) <= FOG_DISPLAY_TOLERANCE, "Fog at azimuth "
                            + azimuth + " and horizontal fraction " + horizontalFraction + " has " + scaled[channel]
                            + " in channel " + channel + " instead of the " + expected + " of the radiance "
                            + radiance + ", off by " + Math.abs(scaled[channel] - expected) + " and not within "
                            + FOG_DISPLAY_TOLERANCE);
                }
                for (float[] weather : FOG_WEATHERS) {
                    views.horizonDisplayColor(0.0, 0.0, azimuth, horizontalFraction, DEFAULT_EXPOSURE,
                            FULL_LIGHT_SCALE, weather[0], weather[1], scaled);
                    views.horizonDisplayColor(0.0, 0.0, azimuth, horizontalFraction, DEFAULT_EXPOSURE, weather[0],
                            weather[1], unscaled);
                    for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                        helper.assertTrue(Float.floatToRawIntBits(scaled[channel])
                                        == Float.floatToRawIntBits(unscaled[channel]), "Fog at azimuth " + azimuth
                                + ", horizontal fraction " + horizontalFraction + ", rain " + weather[0]
                                + " and thunder " + weather[1] + " has " + scaled[channel] + " in channel " + channel
                                + " with a light scale of 1 instead of the " + unscaled[channel] + " without one");
                    }
                }
            }
        }
        for (float rainLevel : DAYLIGHT_RAINS) {
            for (float skyDarken : DAYLIGHT_SKY_DARKENS) {
                views.daylightColor(0.0, 0.0, DEFAULT_EXPOSURE, FULL_LIGHT_SCALE, rainLevel, skyDarken, scaled);
                views.daylightColor(0.0, 0.0, DEFAULT_EXPOSURE, rainLevel, skyDarken, unscaled);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    helper.assertTrue(Float.floatToRawIntBits(scaled[channel])
                                    == Float.floatToRawIntBits(unscaled[channel]), "Daylight in rain " + rainLevel
                            + " and sky darkness " + skyDarken + " has " + scaled[channel] + " in channel " + channel
                            + " with a light scale of 1 instead of the " + unscaled[channel] + " without one");
                }
            }
        }
        views.daylightColor(0.0, 0.0, DEFAULT_EXPOSURE, NO_LIGHT_SCALE, 0.0F, 1.0F, scaled);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            float night = SkyView.vanillaSkyLight(channel, NIGHT_SKY_DARKEN);
            helper.assertTrue(scaled[channel] == night, "Daylight without a light scale has " + scaled[channel]
                    + " in channel " + channel + " instead of vanilla's night of " + night);
        }
        views.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, DEFAULT_EXPOSURE, NO_LIGHT_SCALE, 0.0F, 0.0F, scaled);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(scaled[channel] == 0.0F, "Fog without a light scale has " + scaled[channel]
                    + " in channel " + channel + " instead of 0");
        }
        helper.succeed();
    }

    /**
     * The sun below the moonlight switch lights nothing, so the sky and the fog at the switch are black in every
     * direction and at every depth, while the sun three degrees higher, where its fade reaches 1, is not.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void sunSkyIsDarkAtTheMoonlightSwitch(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        double switchDegrees = SkySetting.MOONLIGHT_SWITCH_DEGREES.defaultValue();
        IceHaloMap map = new IceHaloMap(data, Math.toRadians(Math.max(-6.0, switchDegrees)), blurSigma);
        SkyTables[] depthTables = new SkyTables[SWITCH_DEPTHS.length];
        for (int depthIndex = 0; depthIndex < SWITCH_DEPTHS.length; depthIndex++) {
            double depth = SWITCH_DEPTHS[depthIndex];
            SkyTables tables = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(depth));
            depthTables[depthIndex] = tables;
            SkyView view = tables.computeView(Math.toRadians(switchDegrees), profile, map);
            double[] largestRadiance = new double[SkyAtmosphere.CHANNEL_COUNT];
            for (int azimuthStep = 0; azimuthStep < SWITCH_AZIMUTH_STEPS; azimuthStep++) {
                double azimuth = Math.toRadians(-180.0 + SWITCH_AZIMUTH_STEP_DEGREES * azimuthStep);
                for (int elevationStep = 0; elevationStep <= SWITCH_ELEVATION_STEPS; elevationStep++) {
                    double elevation = Math.toRadians(SWITCH_ELEVATION_STEP_DEGREES * elevationStep);
                    for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                        largestRadiance[channel] = Math.max(largestRadiance[channel], view.radiance(channel, azimuth,
                                elevation, profile, map, tables.diffractionWeights()));
                    }
                }
            }
            SkyViewSet views = SkyViewSet.of(view);
            float[] fog = new float[SkyAtmosphere.CHANNEL_COUNT];
            double[] largestFog = new double[SkyAtmosphere.CHANNEL_COUNT];
            for (int azimuthStep = 0; azimuthStep < SWITCH_AZIMUTH_STEPS; azimuthStep++) {
                double azimuth = Math.toRadians(-180.0 + SWITCH_AZIMUTH_STEP_DEGREES * azimuthStep);
                views.horizonDisplayColor(0.0, 0.0, azimuth, 1.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, fog);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    largestFog[channel] = Math.max(largestFog[channel], 255.0 * fog[channel]);
                }
            }
            views.horizonDisplayColor(0.0, 0.0, 0.0, 0.0, DEFAULT_EXPOSURE, 0.0F, 0.0F, fog);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                largestFog[channel] = Math.max(largestFog[channel], 255.0 * fog[channel]);
            }
            double[] skySteps = new double[SkyAtmosphere.CHANNEL_COUNT];
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                skySteps[channel] = SkyDisplay.displaySteps(DEFAULT_EXPOSURE, largestRadiance[channel]);
            }
            LogUtils.getLogger().info("MoonLight switch depth " + depth + " sky " + skySteps[0] + "," + skySteps[1]
                    + "," + skySteps[2] + " fog " + largestFog[0] + "," + largestFog[1] + "," + largestFog[2]);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(skySteps[channel] < SWITCH_DISPLAY_STEPS, "At depth " + depth + " the brightest "
                        + "sky in channel " + channel + " is at " + skySteps[channel] + " steps, not below "
                        + SWITCH_DISPLAY_STEPS);
                helper.assertTrue(largestFog[channel] < SWITCH_DISPLAY_STEPS, "At depth " + depth + " the brightest "
                        + "fog in channel " + channel + " is at " + largestFog[channel] + " steps, not below "
                        + SWITCH_DISPLAY_STEPS);
            }
        }
        double fadedDegrees = switchDegrees + MoonLight.SUN_FADE_DEGREES;
        IceHaloMap fadedMap = new IceHaloMap(data, Math.toRadians(Math.max(-6.0, fadedDegrees)), blurSigma);
        SkyTables clearTables = depthTables[0];
        SkyView fadedView = clearTables.computeView(Math.toRadians(fadedDegrees), profile, fadedMap);
        double largestRed = 0.0;
        for (int azimuthStep = 0; azimuthStep < SWITCH_AZIMUTH_STEPS; azimuthStep++) {
            double azimuth = Math.toRadians(-180.0 + SWITCH_AZIMUTH_STEP_DEGREES * azimuthStep);
            for (int elevationStep = 0; elevationStep <= SWITCH_ELEVATION_STEPS; elevationStep++) {
                double elevation = Math.toRadians(SWITCH_ELEVATION_STEP_DEGREES * elevationStep);
                largestRed = Math.max(largestRed, fadedView.radiance(0, azimuth, elevation, profile, fadedMap,
                        clearTables.diffractionWeights()));
            }
        }
        double fadedSteps = SkyDisplay.displaySteps(DEFAULT_EXPOSURE, largestRed);
        helper.assertTrue(fadedSteps > SWITCH_DISPLAY_STEPS, "The sun at " + fadedDegrees + " degrees, "
                + MoonLight.SUN_FADE_DEGREES + " above the switch, lights the red sky to " + fadedSteps
                + " steps, not above " + SWITCH_DISPLAY_STEPS);
        helper.succeed();
    }

    /**
     * A build for the other light keeps the halo maps, the halo profile, the tables, and the views, since they depend
     * only on the sun elevation and the depth, and a request for the light the state is already built for starts no
     * build.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void builderBuildsAgainForTheOtherLight(GameTestHelper helper) {
        SkyBuilder builder = new SkyBuilder();
        int haloIndex = SkyBuilder.haloGridIndex(Math.toRadians(20.0));
        int depthIndex = IceDepthGrid.index(0.1);
        int viewIndex = SkyBuilder.viewGridIndex(Math.toRadians(20.0));
        SkyState[] states = new SkyState[2];
        helper.startSequence()
                .thenExecute(() -> builder.request(SkySettings.DEFAULT, SkyLight.Kind.SUN, haloIndex, depthIndex,
                        viewIndex))
                .thenWaitUntil(() -> states[0] = installFinished(builder))
                .thenExecute(() -> {
                    SkyState sun = states[0];
                    helper.assertTrue(sun.lightKind() == SkyLight.Kind.SUN, "The first build has the light kind "
                            + sun.lightKind() + " instead of SUN");
                    helper.assertTrue(sun.isFor(SkyLight.Kind.SUN, haloIndex, depthIndex, viewIndex), "The first build "
                            + "is not for the indices " + haloIndex + ", " + depthIndex + " and " + viewIndex
                            + " of the sun");
                    helper.assertTrue(!sun.isFor(SkyLight.Kind.MOON, haloIndex, depthIndex, viewIndex), "The first "
                            + "build is for the moon at the indices " + haloIndex + ", " + depthIndex + " and "
                            + viewIndex);
                    builder.request(SkySettings.DEFAULT, SkyLight.Kind.MOON, haloIndex, depthIndex, viewIndex);
                    helper.assertTrue(builder.isBuilding(), "A request for the moon started no build");
                })
                .thenWaitUntil(() -> states[1] = installFinished(builder))
                .thenExecute(() -> {
                    SkyState sun = states[0];
                    SkyState moon = states[1];
                    helper.assertTrue(moon.lightKind() == SkyLight.Kind.MOON, "The build for the moon has the light "
                            + "kind " + moon.lightKind() + " instead of MOON");
                    helper.assertTrue(moon.isFor(SkyLight.Kind.MOON, haloIndex, depthIndex, viewIndex), "The build for "
                            + "the moon is not for the indices " + haloIndex + ", " + depthIndex + " and " + viewIndex);
                    for (int slot = 0; slot < SkyState.HALO_COUNT; slot++) {
                        helper.assertTrue(moon.halo(slot) == sun.halo(slot), "The build for the moon built halo map "
                                + slot + " again");
                    }
                    helper.assertTrue(moon.haloProfile() == sun.haloProfile(), "The build for the moon built the halo "
                            + "profile again");
                    helper.assertTrue(moon.lowDepthTables() == sun.lowDepthTables(), "The build for the moon built the "
                            + "low depth tables again");
                    helper.assertTrue(moon.highDepthTables() == sun.highDepthTables(), "The build for the moon built "
                            + "the high depth tables again");
                    for (int slot = 0; slot < SkyViewSet.VIEW_COUNT; slot++) {
                        helper.assertTrue(moon.views().view(slot) == sun.views().view(slot), "The build for the moon "
                                + "built view " + slot + " again");
                    }
                    builder.request(SkySettings.DEFAULT, SkyLight.Kind.MOON, haloIndex, depthIndex, viewIndex);
                    helper.assertTrue(!builder.isBuilding(), "A request for the moon the state is built for started a "
                            + "build");
                    helper.assertTrue(builder.state() == moon, "A request for the moon the state is built for changed "
                            + "the state");
                })
                .thenSucceed();
    }

    /**
     * The full moon at the default settings lights the sky 30 degrees from it and the fog to a faint blue night sky,
     * and the half moon lights both dimmer in every channel, down to the new moon, which lights neither.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void fullMoonDisplaysAsANightSky(GameTestHelper helper) {
        SkyLight light = new SkyLight();
        double[][] lightScales = new double[MOON_NIGHT_PHASES.length][SkyAtmosphere.CHANNEL_COUNT];
        for (int phaseIndex = 0; phaseIndex < MOON_NIGHT_PHASES.length; phaseIndex++) {
            int phase = MOON_NIGHT_PHASES[phaseIndex];
            MoonLight.chooseLight(light, timeOfDayAngle(MOON_NIGHT_SUN_ELEVATION_DEGREES, false), phase,
                    SkySetting.MOONLIGHT_SWITCH_DEGREES.defaultValue(),
                    SkySetting.MOONLIGHT_FADE_DEGREES.defaultValue(),
                    SkySetting.MOONLIGHT_BRIGHTNESS.defaultValue(),
                    SkySetting.MOONLIGHT_BLUE_SHIFT.defaultValue());
            helper.assertTrue(light.kind() == SkyLight.Kind.MOON, "The moon at phase " + phase + " with the sun at "
                    + MOON_NIGHT_SUN_ELEVATION_DEGREES + " degrees has the kind " + light.kind()
                    + " instead of MOON");
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                lightScales[phaseIndex][channel] = light.radianceScale(channel, SkyLight.Kind.MOON);
            }
        }
        SkyTables tables = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(IceWeatherLaw.CLEAR_MINIMUM_DEPTH));
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        IceHaloMap map = new IceHaloMap(data, Math.toRadians(90.0), blurSigma);
        SkyView view = tables.computeView(Math.toRadians(90.0), profile, map);
        SkyViewSet views = SkyViewSet.of(view);
        double[][][] skySteps = new double[MOON_NIGHT_PHASES.length][MOON_NIGHT_AZIMUTH_COUNT]
                [SkyAtmosphere.CHANNEL_COUNT];
        double[][][] fogSteps = new double[MOON_NIGHT_PHASES.length][MOON_NIGHT_AZIMUTH_COUNT]
                [SkyAtmosphere.CHANNEL_COUNT];
        float[] fog = new float[SkyAtmosphere.CHANNEL_COUNT];
        for (int phaseIndex = 0; phaseIndex < MOON_NIGHT_PHASES.length; phaseIndex++) {
            int phase = MOON_NIGHT_PHASES[phaseIndex];
            for (int azimuthIndex = 0; azimuthIndex < MOON_NIGHT_AZIMUTH_COUNT; azimuthIndex++) {
                double azimuthDegrees = MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex;
                double azimuth = Math.toRadians(azimuthDegrees);
                views.horizonDisplayColor(0.0, 0.0, azimuth, 1.0, DEFAULT_EXPOSURE, lightScales[phaseIndex], 0F, 0F,
                        fog);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double radiance = view.radiance(channel, azimuth,
                            Math.toRadians(MOON_NIGHT_SKY_ELEVATION_DEGREES), profile, map,
                            tables.diffractionWeights());
                    skySteps[phaseIndex][azimuthIndex][channel] = SkyDisplay.displaySteps(DEFAULT_EXPOSURE,
                            radiance * lightScales[phaseIndex][channel]);
                    fogSteps[phaseIndex][azimuthIndex][channel] = 255.0 * fog[channel];
                }
                LogUtils.getLogger().info("MoonLight night phase " + phase + " azimuth " + azimuthDegrees + " sky "
                        + skySteps[phaseIndex][azimuthIndex][0] + "," + skySteps[phaseIndex][azimuthIndex][1] + ","
                        + skySteps[phaseIndex][azimuthIndex][2] + " fog " + fogSteps[phaseIndex][azimuthIndex][0] + ","
                        + fogSteps[phaseIndex][azimuthIndex][1] + "," + fogSteps[phaseIndex][azimuthIndex][2]);
            }
        }
        double[][] fullMoonSky = skySteps[0];
        double[][] fullMoonFog = fogSteps[0];
        for (int azimuthIndex = 0; azimuthIndex < MOON_NIGHT_AZIMUTH_COUNT; azimuthIndex++) {
            String where = "The full moon at azimuth " + MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex
                    + " degrees";
            double greenSky = fullMoonSky[azimuthIndex][SkyAtmosphere.GREEN_CHANNEL];
            helper.assertTrue(greenSky >= FULL_MOON_GREEN_SKY_STEPS_MINIMUM
                            && greenSky <= FULL_MOON_GREEN_SKY_STEPS_MAXIMUM, where + " has the green sky at "
                    + greenSky + " steps, not within " + FULL_MOON_GREEN_SKY_STEPS_MINIMUM + " to "
                    + FULL_MOON_GREEN_SKY_STEPS_MAXIMUM);
            double greenFog = fullMoonFog[azimuthIndex][SkyAtmosphere.GREEN_CHANNEL];
            helper.assertTrue(greenFog >= FULL_MOON_GREEN_FOG_STEPS_MINIMUM
                            && greenFog <= FULL_MOON_GREEN_FOG_STEPS_MAXIMUM, where + " has the green fog at "
                    + greenFog + " steps, not within " + FULL_MOON_GREEN_FOG_STEPS_MINIMUM + " to "
                    + FULL_MOON_GREEN_FOG_STEPS_MAXIMUM);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(fullMoonSky[azimuthIndex][channel] < NIGHT_SKY_STEPS_LIMIT, where + " has the sky "
                        + "at " + fullMoonSky[azimuthIndex][channel] + " steps in channel " + channel + ", not below "
                        + NIGHT_SKY_STEPS_LIMIT);
                helper.assertTrue(fullMoonFog[azimuthIndex][channel] < NIGHT_SKY_STEPS_LIMIT, where + " has the fog "
                        + "at " + fullMoonFog[azimuthIndex][channel] + " steps in channel " + channel + ", not below "
                        + NIGHT_SKY_STEPS_LIMIT);
            }
            helper.assertTrue(fullMoonSky[azimuthIndex][2] > fullMoonSky[azimuthIndex][0], where + " has the sky blue "
                    + fullMoonSky[azimuthIndex][2] + " and red " + fullMoonSky[azimuthIndex][0]
                    + " steps, where blue should be above red");
            helper.assertTrue(fullMoonFog[azimuthIndex][2] > fullMoonFog[azimuthIndex][0], where + " has the fog blue "
                    + fullMoonFog[azimuthIndex][2] + " and red " + fullMoonFog[azimuthIndex][0]
                    + " steps, where blue should be above red");
        }
        for (int azimuthIndex = 0; azimuthIndex < MOON_NIGHT_AZIMUTH_COUNT; azimuthIndex++) {
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(skySteps[1][azimuthIndex][channel] < skySteps[0][azimuthIndex][channel], "The half "
                        + "moon at azimuth " + MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex + " degrees has the sky at "
                        + skySteps[1][azimuthIndex][channel] + " steps in channel " + channel + ", not below the full "
                        + "moon's " + skySteps[0][azimuthIndex][channel]);
                helper.assertTrue(fogSteps[1][azimuthIndex][channel] < fogSteps[0][azimuthIndex][channel], "The half "
                        + "moon at azimuth " + MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex + " degrees has the fog at "
                        + fogSteps[1][azimuthIndex][channel] + " steps in channel " + channel + ", not below the full "
                        + "moon's " + fogSteps[0][azimuthIndex][channel]);
            }
        }
        for (int azimuthIndex = 0; azimuthIndex < MOON_NIGHT_AZIMUTH_COUNT; azimuthIndex++) {
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(skySteps[2][azimuthIndex][channel] < NEW_MOON_STEPS_LIMIT, "The new moon at azimuth "
                        + MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex + " degrees has the sky at "
                        + skySteps[2][azimuthIndex][channel] + " steps in channel " + channel + ", not below "
                        + NEW_MOON_STEPS_LIMIT);
                helper.assertTrue(fogSteps[2][azimuthIndex][channel] < NEW_MOON_STEPS_LIMIT, "The new moon at azimuth "
                        + MOON_NIGHT_AZIMUTH_STEP_DEGREES * azimuthIndex + " degrees has the fog at "
                        + fogSteps[2][azimuthIndex][channel] + " steps in channel " + channel + ", not below "
                        + NEW_MOON_STEPS_LIMIT);
            }
        }
        helper.succeed();
    }

    /**
     * The pillar that plates mirror into the halo map moves light out of the sun's bin, which must never take more
     * than the data set gives the other paths, at any sun elevation from below the horizon to high above it.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void mirrorPillarKeepsTheHaloMapNonNegative(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        for (double sunElevationDegrees : MIRROR_PILLAR_SUN_DEGREES) {
            IceHaloMap map = new IceHaloMap(data, Math.toRadians(sunElevationDegrees), blurSigma);
            double lowest = Double.POSITIVE_INFINITY;
            double highest = Double.NEGATIVE_INFINITY;
            int lowestChannel = 0;
            int lowestColumn = 0;
            int lowestRow = 0;
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                for (int row = 0; row < IceHaloMap.ELEVATION_BINS; row++) {
                    for (int column = 0; column < IceHaloMap.AZIMUTH_BINS; column++) {
                        double value = map.value(channel, column, row);
                        if (!(value >= lowest)) {
                            lowest = value;
                            lowestChannel = channel;
                            lowestColumn = column;
                            lowestRow = row;
                        }
                        highest = Math.max(highest, value);
                    }
                }
            }
            helper.assertTrue(Double.isFinite(lowest) && lowest >= 0.0 && Double.isFinite(highest),
                    "The halo map at sun " + sunElevationDegrees + " degrees holds the lowest value " + lowest
                            + " in channel " + lowestChannel + ", column " + lowestColumn + " and row " + lowestRow
                            + " and the highest value " + highest + ", where every value should be finite and at "
                            + "least 0");
        }
        helper.succeed();
    }

    /**
     * The plates' mirror pillar stands at the sun's mirror image, three degrees up for a sun three degrees down, and
     * falls off sideways far faster than it falls off along the vertical.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void mirrorPillarPeaksAtTheSunsMirrorImage(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloMap map = new IceHaloMap(data, Math.toRadians(MIRROR_PILLAR_MAP_SUN_DEGREES), blurSigma);
        int peakRow = -1;
        for (int row = 0; row < IceHaloMap.ELEVATION_BINS; row++) {
            double centerDegrees = Math.toDegrees(IceHaloMap.LOWEST_ELEVATION + (row + 0.5) * IceHaloMap.BIN_STEP);
            if (centerDegrees < MIRROR_PILLAR_ROW_LOWEST_DEGREES
                    || centerDegrees > MIRROR_PILLAR_ROW_HIGHEST_DEGREES) {
                continue;
            }
            if (peakRow < 0 || map.value(SkyAtmosphere.GREEN_CHANNEL, 0, row)
                    > map.value(SkyAtmosphere.GREEN_CHANNEL, 0, peakRow)) {
                peakRow = row;
            }
        }
        helper.assertTrue(peakRow >= 0, "The halo map at sun " + MIRROR_PILLAR_MAP_SUN_DEGREES + " degrees holds no "
                + "row between " + MIRROR_PILLAR_ROW_LOWEST_DEGREES + " and " + MIRROR_PILLAR_ROW_HIGHEST_DEGREES
                + " degrees of elevation");
        double peakDegrees = Math.toDegrees(IceHaloMap.LOWEST_ELEVATION + (peakRow + 0.5) * IceHaloMap.BIN_STEP);
        double mirrorDegrees = -MIRROR_PILLAR_MAP_SUN_DEGREES;
        helper.assertTrue(Math.abs(peakDegrees - mirrorDegrees) <= MIRROR_PILLAR_PEAK_TOLERANCE_DEGREES,
                "The mirror pillar of the sun at " + MIRROR_PILLAR_MAP_SUN_DEGREES + " degrees peaks at elevation "
                        + peakDegrees + " degrees in row " + peakRow + ", not within "
                        + MIRROR_PILLAR_PEAK_TOLERANCE_DEGREES + " degrees of the sun's mirror image at "
                        + mirrorDegrees + " degrees");
        double peakValue = map.value(SkyAtmosphere.GREEN_CHANNEL, 0, peakRow);
        double sideValue = map.value(SkyAtmosphere.GREEN_CHANNEL, MIRROR_PILLAR_SIDE_COLUMN, peakRow);
        helper.assertTrue(peakValue > MIRROR_PILLAR_SIDE_RATIO * sideValue, "The mirror pillar of the sun at "
                + MIRROR_PILLAR_MAP_SUN_DEGREES + " degrees has the green value " + peakValue + " in column 0 of row "
                + peakRow + " and " + sideValue + " in column " + MIRROR_PILLAR_SIDE_COLUMN + ", not more than "
                + MIRROR_PILLAR_SIDE_RATIO + " times as much");
        helper.succeed();
    }

    /**
     * The pillar's shape from IceMirror adds up to the share that level plates reflect as a slab, and the phase that
     * spreads it over the sky integrates back to that share. Wobbling plates keep a pillar at sunset, where level
     * plates reflect nothing. The traced share of the data set lies a little below it, since part of the light inside
     * a plate leaves through a side face.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void mirrorPillarMatchesTheLevelPlateClosedForm(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        IceCrystalData.Crystal plates = plateCrystal(data, LEVEL_PLATE_NAME);
        IceCrystalData.Crystal wobblingPlates = plateCrystal(data, WOBBLING_PLATE_NAME);
        for (double sunElevationDegrees : MIRROR_PILLAR_CLOSED_FORM_SUN_DEGREES) {
            double sunElevation = Math.toRadians(sunElevationDegrees);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                double sphere = mirrorPillarSphereTotal(plates, channel, sunElevation);
                double reflectance = IceMirror.slabReflectance(Math.sin(sunElevation),
                        IceHaloFormulas.refractiveIndex(channel));
                double closedForm = Math.sin(sunElevation) * reflectance / (Math.sin(sunElevation)
                        + LEVEL_PLATE_SIDE_AREA * plates.halfHeightOverRadius() * Math.cos(sunElevation));
                helper.assertTrue(Math.abs(sphere / closedForm - 1.0) < MIRROR_PILLAR_CLOSED_FORM_TOLERANCE,
                        "The " + LEVEL_PLATE_NAME + " pillar at sun " + sunElevationDegrees + " degrees holds "
                                + sphere + " of the light in channel " + channel + ", the level plate closed form "
                                + closedForm);
            }
        }
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double sphere = mirrorPillarSphereTotal(wobblingPlates, channel, 0.0);
            helper.assertTrue(sphere > MIRROR_PILLAR_SUNSET_MINIMUM, "The " + WOBBLING_PLATE_NAME + " pillar at sun 0 "
                    + "degrees holds " + sphere + " of the light in channel " + channel + ", not above "
                    + MIRROR_PILLAR_SUNSET_MINIMUM);
        }
        double[][] pathShares = new double[SkyAtmosphere.CHANNEL_COUNT][IceCrystalData.PATH_COUNT];
        for (IceCrystalData.Crystal crystal : new IceCrystalData.Crystal[] {plates, wobblingPlates}) {
            for (double sunElevationDegrees : MIRROR_PILLAR_PRINT_SUN_DEGREES) {
                double sunElevation = Math.toRadians(sunElevationDegrees);
                data.pathShares(crystal, sunElevationDegrees, pathShares);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double sphere = mirrorPillarSphereTotal(crystal, channel, sunElevation);
                    double traced = pathShares[channel][IceCrystalData.END_MIRROR_PATH];
                    LogUtils.getLogger().info("MirrorPillar share " + crystal.name() + " sun " + sunElevationDegrees
                            + " channel " + channel + " sphere " + sphere + " traced " + traced + " other "
                            + pathShares[channel][IceCrystalData.OTHER_PATH]);
                    helper.assertTrue(traced <= sphere && traced >= MIRROR_PILLAR_TRACED_LEAST_SHARE * sphere,
                            "The " + crystal.name() + " pillar at sun " + sunElevationDegrees + " degrees holds "
                                    + sphere + " of the light in channel " + channel + " from IceMirror and "
                                    + traced + " in the data set, outside " + MIRROR_PILLAR_TRACED_LEAST_SHARE
                                    + " to 1 times");
                }
            }
        }
        double sunElevation = Math.toRadians(MIRROR_PILLAR_PHASE_SUN_DEGREES);
        double extraVariance = Math.pow(Math.toRadians(MIRROR_PILLAR_EXTRA_VARIANCE_DEGREES), 2.0);
        double[] integrated = new double[2];
        for (IceCrystalData.Crystal crystal : new IceCrystalData.Crystal[] {wobblingPlates, plates}) {
            double sphere = mirrorPillarSphereTotal(crystal, SkyAtmosphere.GREEN_CHANNEL, sunElevation);
            integrateMirrorPillarPhase(crystal, SkyAtmosphere.GREEN_CHANNEL, sunElevation, 0.0, integrated);
            double withoutExtra = integrated[0];
            double peakWithoutExtra = integrated[1];
            integrateMirrorPillarPhase(crystal, SkyAtmosphere.GREEN_CHANNEL, sunElevation, extraVariance, integrated);
            double withExtra = integrated[0];
            double peakWithExtra = integrated[1];
            String where = "The " + crystal.name() + " pillar at sun " + MIRROR_PILLAR_PHASE_SUN_DEGREES + " degrees";
            helper.assertTrue(Math.abs(withoutExtra / sphere - 1.0) < MIRROR_PILLAR_PHASE_TOLERANCE, where
                    + " has the phase integral " + withoutExtra + " without extra variance, the sphere total "
                    + sphere);
            helper.assertTrue(Math.abs(withExtra / sphere - 1.0) < MIRROR_PILLAR_PHASE_TOLERANCE, where
                    + " has the phase integral " + withExtra + " with the extra variance of "
                    + MIRROR_PILLAR_EXTRA_VARIANCE_DEGREES + " degrees, the sphere total " + sphere);
            helper.assertTrue(peakWithExtra < peakWithoutExtra, where + " has the phase peak " + peakWithExtra
                    + " with the extra variance of " + MIRROR_PILLAR_EXTRA_VARIANCE_DEGREES
                    + " degrees, not below the " + peakWithoutExtra + " without it");
        }
        helper.succeed();
    }

    /**
     * With all of a plate kind's light on the mirrored path, the pillar takes the part of IceMirror's shape that lies
     * inside the map and the sun's bin keeps the rest, so the map holds all of the light while the sun is in it. With
     * the sun below the map only the pillar's part inside the map is there.
     */
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void mirrorPillarMovesThePartOfItsShapeInsideTheMap(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        IceHaloFormulas formulas = new IceHaloFormulas(data);
        double[][] maps = new double[SkyAtmosphere.CHANNEL_COUNT][IceHaloMap.AZIMUTH_BINS * IceHaloMap.ELEVATION_BINS];
        double[][] pathShares = new double[SkyAtmosphere.CHANNEL_COUNT][IceCrystalData.PATH_COUNT];
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            pathShares[channel][IceCrystalData.END_MIRROR_PATH] = 1.0;
        }
        for (String name : new String[] {LEVEL_PLATE_NAME, WOBBLING_PLATE_NAME}) {
            IceCrystalData.Crystal crystal = plateCrystal(data, name);
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double sunElevationDegrees : MIRROR_PILLAR_INSIDE_SUN_DEGREES) {
                double sunElevation = Math.toRadians(sunElevationDegrees);
                double[] sun = {Math.cos(sunElevation), Math.sin(sunElevation), 0.0};
                for (double[] map : maps) {
                    Arrays.fill(map, 0.0);
                }
                formulas.addCrystal(maps, crystal, sun, MIRROR_PILLAR_INSIDE_WEIGHT, pathShares);
                int sunBin = IceHaloMap.binOf(sun[0], sun[1], sun[2]);
                double crossSection = IceMirror.crossSection(sunElevation, tiltSigma, crystal.halfHeightOverRadius());
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double total = 0.0;
                    double atSun = 0.0;
                    for (int bin = 0; bin < maps[channel].length; bin++) {
                        double mass = maps[channel][bin] * IceHaloMap.binSolidAngle(bin);
                        total += mass;
                        if (bin == sunBin) {
                            atSun = mass;
                        }
                    }
                    double index = IceHaloFormulas.refractiveIndex(channel);
                    double inside = mirrorPillarShapeInsideMap(crystal, sunElevation, crossSection, index)
                            / IceMirror.pillarShare(sunElevation, tiltSigma, crossSection, index);
                    double expectedPillar = MIRROR_PILLAR_INSIDE_WEIGHT * inside;
                    String where = "The " + name + " pillar at sun " + sunElevationDegrees + " degrees in channel "
                            + channel;
                    helper.assertTrue(Math.abs(total - atSun - expectedPillar)
                                    <= MIRROR_PILLAR_INSIDE_TOLERANCE * MIRROR_PILLAR_INSIDE_WEIGHT,
                            where + " puts " + (total - atSun) + " outside the sun's bin, where its shape puts "
                                    + expectedPillar + " inside the map");
                    double expectedTotal = sunBin >= 0 ? MIRROR_PILLAR_INSIDE_WEIGHT : expectedPillar;
                    helper.assertTrue(Math.abs(total - expectedTotal)
                                    <= MIRROR_PILLAR_INSIDE_TOLERANCE * MIRROR_PILLAR_INSIDE_WEIGHT,
                            where + " leaves " + total + " in the map, expected " + expectedTotal);
                }
            }
        }
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
                .thenExecute(() -> builder.request(SkySettings.DEFAULT, SkyLight.Kind.SUN, haloIndex, depthIndex,
                        viewIndex))
                .thenWaitUntil(() -> states[0] = installFinished(builder))
                .thenExecute(() -> builder.request(texelChanged, SkyLight.Kind.SUN, haloIndex, depthIndex,
                        viewIndex))
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
                    builder.request(nanobotsChanged, SkyLight.Kind.SUN, haloIndex, depthIndex, viewIndex);
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
                        builder.request(nanobotsChanged.with(setting, changed), SkyLight.Kind.SUN, haloIndex,
                                depthIndex, viewIndex);
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

    /**
     * The time of day angle at which the sun stands at the elevation in degrees, on the setting branch or the rising
     * one.
     */
    private static double timeOfDayAngle(double sunElevationDegrees, boolean rising) {
        double setting = Math.acos(Math.sin(Math.toRadians(sunElevationDegrees)));
        return rising ? 2.0 * Math.PI - setting : setting;
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

    /**
     * The plate crystal kind with the name, which the closed form of the level plate pillar applies to.
     */
    /**
     * The part of the pillar's shape that lies inside the halo map, from IceMirror.pillarStrength over the vertical.
     */
    private static double mirrorPillarShapeInsideMap(IceCrystalData.Crystal crystal, double sunElevation,
            double crossSection, double refractiveIndex) {
        double heightSigma = IceMirror.pillarHeightSigma(Math.toRadians(crystal.tiltDegrees()));
        double lowest = Math.max(IceHaloMap.LOWEST_ELEVATION, -sunElevation - 6.0 * heightSigma);
        double highest = Math.min(0.5 * Math.PI, -sunElevation + 6.0 * heightSigma);
        double step = Math.toRadians(MIRROR_PILLAR_INSIDE_STEP_DEGREES);
        double sum = 0.0;
        for (double viewElevation = lowest + 0.5 * step; viewElevation < highest; viewElevation += step) {
            double offset = (viewElevation + sunElevation) / heightSigma;
            sum += IceMirror.pillarStrength(viewElevation, sunElevation, crossSection, refractiveIndex)
                    * Math.exp(-0.5 * offset * offset) / (Math.sqrt(2.0 * Math.PI) * heightSigma);
        }
        return sum * step;
    }

    private static IceCrystalData.Crystal plateCrystal(IceCrystalData data, String name) {
        for (IceCrystalData.Crystal crystal : data.crystals()) {
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE && crystal.name().equals(name)) {
                return crystal;
            }
        }
        throw new GameTestAssertException("The ice crystal data holds no plate crystal named " + name);
    }

    /**
     * The share of the light hitting plates that the pillar holds over the whole sphere at a sun elevation, from the
     * midpoint rule over the view elevation of the pillar's strength times its Gaussian along the vertical.
     */
    private static double mirrorPillarSphereTotal(IceCrystalData.Crystal crystal, int channel, double sunElevation) {
        double tiltSigma = Math.toRadians(crystal.tiltDegrees());
        double heightSigma = IceMirror.pillarHeightSigma(tiltSigma);
        double crossSection = IceMirror.crossSection(sunElevation, tiltSigma, crystal.halfHeightOverRadius());
        double refractiveIndex = IceHaloFormulas.refractiveIndex(channel);
        double step = Math.toRadians(MIRROR_PILLAR_SPHERE_STEP_DEGREES);
        int steps = (int) Math.round(Math.PI / step);
        double sum = 0.0;
        for (int index = 0; index < steps; index++) {
            double viewElevation = -0.5 * Math.PI + (index + 0.5) * step;
            double offset = (viewElevation + sunElevation) / heightSigma;
            double density = Math.exp(-0.5 * offset * offset) / (Math.sqrt(2.0 * Math.PI) * heightSigma);
            sum += IceMirror.pillarStrength(viewElevation, sunElevation, crossSection, refractiveIndex) * density;
        }
        return sum * step;
    }

    /**
     * Integrates the pillar's phase over the view elevation and the angle sideways from the vertical through the sun
     * with dv * dside as the solid angle, and writes the integral first and the peak of the grid second.
     */
    private static void integrateMirrorPillarPhase(IceCrystalData.Crystal crystal, int channel, double sunElevation,
            double extraVariance, double[] result) {
        double tiltSigma = Math.toRadians(crystal.tiltDegrees());
        double halfHeightOverRadius = crystal.halfHeightOverRadius();
        double refractiveIndex = IceHaloFormulas.refractiveIndex(channel);
        double step = Math.toRadians(MIRROR_PILLAR_PHASE_STEP_DEGREES);
        int elevationSteps = (int) Math.round(2.0 * MIRROR_PILLAR_PHASE_ELEVATION_LIMIT_DEGREES
                / MIRROR_PILLAR_PHASE_STEP_DEGREES);
        int sideSteps = (int) Math.round(2.0 * MIRROR_PILLAR_PHASE_SIDE_LIMIT_DEGREES
                / MIRROR_PILLAR_PHASE_STEP_DEGREES);
        double sum = 0.0;
        double peak = 0.0;
        for (int elevationIndex = 0; elevationIndex < elevationSteps; elevationIndex++) {
            double viewElevation = Math.toRadians(-MIRROR_PILLAR_PHASE_ELEVATION_LIMIT_DEGREES
                    + (elevationIndex + 0.5) * MIRROR_PILLAR_PHASE_STEP_DEGREES);
            for (int sideIndex = 0; sideIndex < sideSteps; sideIndex++) {
                double sideAngle = Math.toRadians(-MIRROR_PILLAR_PHASE_SIDE_LIMIT_DEGREES
                        + (sideIndex + 0.5) * MIRROR_PILLAR_PHASE_STEP_DEGREES);
                double phase = IceMirror.pillarPhase(viewElevation, sideAngle, sunElevation, tiltSigma,
                        halfHeightOverRadius, refractiveIndex, extraVariance);
                sum += phase;
                peak = Math.max(peak, phase);
            }
        }
        result[0] = sum * step * step;
        result[1] = peak;
    }
}
