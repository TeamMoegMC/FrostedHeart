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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.Locale;

/**
 * Checks the ice depth and the diamond dust that the weather calls for, the grid of depths the sky is built for, that
 * blending the tables of two grid depths matches the tables of the depth between them, and the light on the ground
 * under ice.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class IceWeatherGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double RELATIVE_TOLERANCE = 1.0e-9;
    private static final double[] SUN_ELEVATIONS_DEGREES = {5.0, 20.0, 45.0};
    private static final double[] AZIMUTHS_DEGREES = {0.0, 5.0, 15.0, 30.0, 60.0, 90.0, 135.0, 180.0};
    private static final double[] ELEVATIONS_DEGREES = {-3.0, 1.0, 5.0, 15.0, 30.0, 50.0, 70.0, 88.0};
    private static final double[] SUN_OFFSET_ELEVATIONS_DEGREES = {-5.0, 3.0, 5.0, 10.0, 22.0, 46.0};
    private static final double MAXIMUM_DISPLAY_DIFFERENCE = 2.0;
    private static final double MAXIMUM_GROUND_RELATIVE_DIFFERENCE = 0.02;
    private static final double CLEAR_DEPTH = 0.02;
    private static final double[] GROUND_DEPTHS = {1.0, 2.0, 3.0};
    private static final double[] GROUND_SUN_ELEVATIONS_DEGREES = {10.0, 30.0, 45.0, 70.0};
    /**
     * Ground radiance under ice over that under CLEAR_DEPTH, by sun elevation, depth, and channel, from the path tracer
     * of the offline sky preview as the mean of three seeds, which differ by up to 0.1.
     * Its ice phase is generic, half of it diffraction, and the other half has an asymmetry of 0.57 against 0.54 for the
     * tumbling crystals.
     */
    private static final double[][][] PATH_TRACED_GROUND_OVER_CLEAR = {
            {{0.833, 0.862, 0.911}, {0.744, 0.810, 0.876}, {0.729, 0.778, 0.842}},
            {{0.922, 0.930, 0.936}, {0.877, 0.886, 0.883}, {0.842, 0.848, 0.862}},
            {{0.977, 0.988, 0.974}, {0.953, 0.955, 0.939}, {0.922, 0.918, 0.918}},
            {{1.016, 1.009, 0.991}, {1.015, 1.003, 0.993}, {0.996, 0.972, 0.974}}
    };
    /**
     * Covers the noise of the mean of three seeds, about 3%, and the generic phase, which the preview's fast sky with the
     * mod's crystals exceeds by up to 8% under a high sun and falls short of by up to 3% under a low sun.
     */
    private static final double GROUND_OVER_CLEAR_TOLERANCE = 0.12;
    private static final double SUN_SQUARE_MARGIN_DEGREES = 0.5;
    private static final double SUN_SQUARE_HALF_TANGENT =
            Math.tan(Math.atan(SkyTables.SUN_HALF_TANGENT) + Math.toRadians(SUN_SQUARE_MARGIN_DEGREES));

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void iceDepthLawFollowsHumidityAndSnow(GameTestHelper helper) {
        assertClearDepth(helper);
        assertRampBeforeSnow(helper);
        assertRecoveryAfterSnow(helper);
        assertSmoothing(helper);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void diamondDustLawFollowsSnow(GameTestHelper helper) {
        double full = DiamondDustLaw.FULL_EXTINCTION;
        for (double hoursSinceSnow : new double[] {0.0, 0.5, 6.0, 18.0, 30.0, Double.POSITIVE_INFINITY}) {
            assertClose(helper, "dust while snowing " + hoursSinceSnow + " hours after snow",
                    DiamondDustLaw.targetExtinction(true, hoursSinceSnow), 0.0, 0.0);
        }
        assertClose(helper, "dust with no snow known", dustAfterSnow(Double.POSITIVE_INFINITY), 0.0, 0.0);
        assertClose(helper, "dust with the snow end ahead of the clock", dustAfterSnow(-1.0), 0.0, 0.0);
        assertClose(helper, "dust when the snow ends", dustAfterSnow(0.0), 0.0, 0.0);
        assertRelative(helper, "dust half an hour after snow", dustAfterSnow(0.5), 0.5 * full);
        assertRelative(helper, "dust 1 hour after snow", dustAfterSnow(1.0), full);
        assertRelative(helper, "dust 6 hours after snow", dustAfterSnow(6.0), full);
        assertRelative(helper, "dust 12 hours after snow", dustAfterSnow(12.0), full);
        assertRelative(helper, "dust 18 hours after snow", dustAfterSnow(18.0), 0.5 * full);
        assertClose(helper, "dust 24 hours after snow", dustAfterSnow(24.0), 0.0, 0.0);
        assertClose(helper, "dust 30 hours after snow", dustAfterSnow(30.0), 0.0, 0.0);
        double previous = 0.0;
        for (double hours = 0.0; hours <= 30.0; hours += 0.25) {
            double extinction = dustAfterSnow(hours);
            helper.assertTrue(extinction >= 0.0 && extinction <= full,
                    "Dust " + hours + " hours after snow is " + extinction + ", outside the range from 0 to " + full);
            boolean rising = hours <= DiamondDustLaw.HOLD_END_HOURS;
            helper.assertTrue(rising ? extinction >= previous : extinction <= previous,
                    "Dust " + hours + " hours after snow is " + extinction + " after " + previous
                            + " at the quarter hour before");
            previous = extinction;
        }
        assertRelative(helper, "smoothed dust with no elapsed time",
                DiamondDustLaw.smoothedExtinction(0.2, full, 0.0), 0.2);
        assertRelative(helper, "smoothed dust after a very long time",
                DiamondDustLaw.smoothedExtinction(0.2, full, 1.0e6), full);
        assertRelative(helper, "gap left after one smoothing time",
                full - DiamondDustLaw.smoothedExtinction(0.0, full, DiamondDustLaw.SMOOTHING_SECONDS),
                full / Math.E);
        for (double elapsed : new double[] {0.1, 1.0, 10.0, 49.0, 50.0, 51.0, 100.0, 500.0}) {
            for (double[] pair : new double[][] {{0.0, 1.0}, {1.0, 0.0}, {0.3, 0.7}}) {
                double result = DiamondDustLaw.smoothedExtinction(pair[0], pair[1], elapsed);
                double low = Math.min(pair[0], pair[1]);
                double high = Math.max(pair[0], pair[1]);
                helper.assertTrue(result >= low && result <= high,
                        "Smoothed dust from " + pair[0] + " toward " + pair[1] + " after " + elapsed
                                + " seconds is " + result + ", outside the range from " + low + " to " + high);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void iceDepthGridCoversTheDepths(GameTestHelper helper) {
        helper.assertTrue(IceDepthGrid.depth(0) == 0.0,
                "Grid depth 0 is " + IceDepthGrid.depth(0) + " instead of 0");
        for (int index = 0; index <= IceDepthGrid.HIGHEST_LOW_INDEX; index++) {
            double low = IceDepthGrid.depth(index);
            double high = IceDepthGrid.depth(index + 1);
            helper.assertTrue(high > low,
                    "Grid depth " + (index + 1) + " is " + high + ", not above grid depth " + index + " at " + low);
        }
        for (int index = 0; index <= IceDepthGrid.HIGHEST_LOW_INDEX; index++) {
            double depth = IceDepthGrid.depth(index);
            helper.assertTrue(IceDepthGrid.index(depth) == index,
                    "Index of grid depth " + depth + " is " + IceDepthGrid.index(depth) + " instead of " + index);
            double above = depth * (1.0 + 1.0e-9) + 1.0e-12;
            helper.assertTrue(IceDepthGrid.index(above) == index,
                    "Index of " + above + ", just above grid depth " + depth + ", is " + IceDepthGrid.index(above)
                            + " instead of " + index);
        }
        double maximumDepth = SkySetting.ICE_OPTICAL_DEPTH.maximum();
        helper.assertTrue(IceDepthGrid.index(maximumDepth) == IceDepthGrid.HIGHEST_LOW_INDEX,
                "Index of the maximum ice optical depth " + maximumDepth + " is "
                        + IceDepthGrid.index(maximumDepth) + " instead of " + IceDepthGrid.HIGHEST_LOW_INDEX);
        double highest = IceDepthGrid.depth(IceDepthGrid.HIGHEST_LOW_INDEX + 1);
        helper.assertTrue(highest >= maximumDepth * (1.0 - RELATIVE_TOLERANCE),
                "Grid depth " + (IceDepthGrid.HIGHEST_LOW_INDEX + 1) + " is " + highest
                        + ", below the maximum ice optical depth " + maximumDepth);
        helper.assertTrue(IceDepthGrid.index(-1.0) == 0,
                "Index of a negative depth is " + IceDepthGrid.index(-1.0) + " instead of 0");
        helper.assertTrue(IceDepthGrid.index(1.0e6) == IceDepthGrid.HIGHEST_LOW_INDEX,
                "Index of a depth far above the maximum is " + IceDepthGrid.index(1.0e6) + " instead of "
                        + IceDepthGrid.HIGHEST_LOW_INDEX);
        for (int index = 0; index <= IceDepthGrid.HIGHEST_LOW_INDEX; index++) {
            double low = IceDepthGrid.depth(index);
            double high = IceDepthGrid.depth(index + 1);
            double middle = 0.5 * (low + high);
            assertClose(helper, "blend weight at grid depth " + index, IceDepthGrid.blendWeight(low, index), 0.0,
                    1.0e-12);
            assertClose(helper, "blend weight at grid depth " + (index + 1), IceDepthGrid.blendWeight(high, index),
                    1.0, 1.0e-12);
            assertClose(helper, "blend weight halfway through cell " + index, IceDepthGrid.blendWeight(middle, index),
                    0.5, 1.0e-12);
            assertClose(helper, "blend weight below cell " + index, IceDepthGrid.blendWeight(low - 1.0, index), 0.0,
                    1.0e-12);
            assertClose(helper, "blend weight above cell " + index, IceDepthGrid.blendWeight(high + 1.0, index), 1.0,
                    1.0e-12);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void blendedDepthMatchesExactTables(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        IceHaloMap[] maps = new IceHaloMap[SUN_ELEVATIONS_DEGREES.length];
        for (int sun = 0; sun < maps.length; sun++) {
            maps[sun] = new IceHaloMap(data, Math.toRadians(SUN_ELEVATIONS_DEGREES[sun]), blurSigma);
        }
        WorstDifference worstDisplay = new WorstDifference();
        WorstDifference worstGround = new WorstDifference();
        WorstDifference worstUnderSun = new WorstDifference();
        SkyTables lowTables = tablesAt(IceDepthGrid.depth(0));
        for (int index = 0; index <= IceDepthGrid.HIGHEST_LOW_INDEX; index++) {
            double lowDepth = IceDepthGrid.depth(index);
            double highDepth = IceDepthGrid.depth(index + 1);
            double middleDepth = 0.5 * (lowDepth + highDepth);
            SkyTables highTables = tablesAt(highDepth);
            SkyTables middleTables = tablesAt(middleDepth);
            String cell = "cell " + lowDepth + " to " + highDepth;
            WorstDifference cellWorst = new WorstDifference();
            WorstDifference cellUnderSun = new WorstDifference();
            for (int sun = 0; sun < maps.length; sun++) {
                double sunDegrees = SUN_ELEVATIONS_DEGREES[sun];
                IceHaloMap map = maps[sun];
                DepthBlend blend = new DepthBlend(lowTables, highTables, middleTables, Math.toRadians(sunDegrees),
                        profile, map);
                for (double azimuthDegrees : AZIMUTHS_DEGREES) {
                    for (double elevationDegrees : ELEVATIONS_DEGREES) {
                        compareDirection(cellWorst, cellUnderSun, blend, profile, map, cell, sunDegrees,
                                azimuthDegrees, elevationDegrees);
                    }
                }
                for (double offsetDegrees : SUN_OFFSET_ELEVATIONS_DEGREES) {
                    double elevationDegrees = sunDegrees + offsetDegrees;
                    if (elevationDegrees >= 90.0) {
                        continue;
                    }
                    compareDirection(cellWorst, cellUnderSun, blend, profile, map, cell, sunDegrees, 0.0,
                            elevationDegrees);
                }
                compareGround(worstGround, blend, cell + ", sun " + sunDegrees + " degrees");
            }
            LOGGER.info("Cell " + lowDepth + " to " + highDepth + ", worst display difference " + cellWorst.value()
                    + " at " + cellWorst.where() + ", under the sun's square " + cellUnderSun.value() + " at "
                    + cellUnderSun.where());
            worstDisplay.record(cellWorst.value(), cellWorst.where());
            worstUnderSun.record(cellUnderSun.value(), cellUnderSun.where());
            lowTables = highTables;
        }
        String summary = "Worst display difference " + worstDisplay.value() + " at " + worstDisplay.where()
                + ", worst ground relative difference " + worstGround.value() + " at " + worstGround.where();
        LOGGER.info(summary);
        LOGGER.info("Worst display difference under the sun's square " + worstUnderSun.value() + " at "
                + worstUnderSun.where() + ", not asserted");
        helper.assertTrue(worstDisplay.value() <= MAXIMUM_DISPLAY_DIFFERENCE,
                "The sky blended between two grid depths differs from the exact depth by more than "
                        + MAXIMUM_DISPLAY_DIFFERENCE + " display steps. " + summary);
        helper.assertTrue(worstGround.value() <= MAXIMUM_GROUND_RELATIVE_DIFFERENCE,
                "The ground radiance blended between two grid depths differs from the exact depth by more than "
                        + MAXIMUM_GROUND_RELATIVE_DIFFERENCE + ". " + summary);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void groundUnderIceFollowsThePathTracer(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables clearTables = tablesAt(CLEAR_DEPTH);
        SkyTables[] tables = new SkyTables[GROUND_DEPTHS.length];
        for (int depth = 0; depth < tables.length; depth++) {
            tables[depth] = tablesAt(GROUND_DEPTHS[depth]);
        }
        float[] daylight = new float[SkyAtmosphere.CHANNEL_COUNT];
        WorstDifference worst = new WorstDifference();
        for (int sun = 0; sun < GROUND_SUN_ELEVATIONS_DEGREES.length; sun++) {
            double sunDegrees = GROUND_SUN_ELEVATIONS_DEGREES[sun];
            double sunElevation = Math.toRadians(sunDegrees);
            IceHaloMap map = new IceHaloMap(data, sunElevation, blurSigma);
            double[] clear = clearTables.computeView(sunElevation, profile, map).groundRadiance();
            StringBuilder line = new StringBuilder("Sun " + sunDegrees + " degrees, ground radiance over depth "
                    + CLEAR_DEPTH + " in red, green, and blue against the path tracer, and the daylight color:");
            for (int depth = 0; depth < tables.length; depth++) {
                SkyView view = tables[depth].computeView(sunElevation, profile, map);
                double[] ground = view.groundRadiance();
                SkyViewSet.of(view).daylightColor(0.0, 0.0, SkySetting.EXPOSURE.defaultValue(), 0.0F, 1.0F,
                        daylight);
                line.append(String.format(Locale.ROOT, " depth %.1f ground", GROUND_DEPTHS[depth]));
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double ratio = ground[channel] / clear[channel];
                    double expected = PATH_TRACED_GROUND_OVER_CLEAR[sun][depth][channel];
                    line.append(String.format(Locale.ROOT, " %.3f/%.3f", ratio, expected));
                    worst.record(Math.abs(ratio / expected - 1.0), "sun " + sunDegrees + " degrees, ice optical depth "
                            + GROUND_DEPTHS[depth] + ", channel " + channel);
                }
                line.append(String.format(Locale.ROOT, " daylight %.3f %.3f %.3f", daylight[0], daylight[1],
                        daylight[2]));
            }
            LOGGER.info(line.toString());
        }
        LOGGER.info("Ground over clear differs from the path tracer by at most " + worst.value() + " at "
                + worst.where());
        helper.assertTrue(worst.value() <= GROUND_OVER_CLEAR_TOLERANCE,
                "Ground over clear differs from the path tracer by " + worst.value() + " at " + worst.where()
                        + ", more than " + GROUND_OVER_CLEAR_TOLERANCE);
        helper.succeed();
    }

    private static void assertClearDepth(GameTestHelper helper) {
        assertRelative(helper, "clear depth at humidity 0", IceWeatherLaw.clearDepth(0.0), 0.02);
        assertRelative(helper, "clear depth at humidity 50", IceWeatherLaw.clearDepth(50.0), 0.15);
        assertRelative(helper, "clear depth at humidity 25", IceWeatherLaw.clearDepth(25.0), 0.085);
        assertRelative(helper, "clear depth at humidity -10", IceWeatherLaw.clearDepth(-10.0), 0.02);
        assertRelative(helper, "clear depth at humidity 100", IceWeatherLaw.clearDepth(100.0), 0.15);
    }

    private static void assertRampBeforeSnow(GameTestHelper helper) {
        for (double humidity : new double[] {0.0, 50.0}) {
            double clear = IceWeatherLaw.clearDepth(humidity);
            String at = " at humidity " + humidity;
            assertRelative(helper, "depth with no snow known" + at,
                    IceWeatherLaw.targetDepth(false, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, humidity),
                    clear);
            assertRelative(helper, "depth 30 hours before snow" + at, depthBeforeSnow(30.0, humidity), clear);
            assertRelative(helper, "depth 24 hours before snow" + at, depthBeforeSnow(24.0, humidity), clear);
            assertRelative(helper, "depth 12 hours before snow" + at, depthBeforeSnow(12.0, humidity), 0.4);
            assertRelative(helper, "depth 6 hours before snow" + at, depthBeforeSnow(6.0, humidity), 1.0);
            assertRelative(helper, "depth when the snow starts" + at, depthBeforeSnow(0.0, humidity), 3.0);
            assertRelative(helper, "depth with the snow overdue" + at, depthBeforeSnow(-5.0, humidity), 3.0);
            assertRelative(helper, "depth 18 hours before snow" + at, depthBeforeSnow(18.0, humidity),
                    Math.sqrt(clear * 0.4));
            for (double hoursUntilSnow : new double[] {30.0, 12.0, 0.0}) {
                for (double hoursSinceSnow : new double[] {30.0, 0.0}) {
                    assertRelative(helper, "depth while snowing " + hoursUntilSnow + " hours before and "
                            + hoursSinceSnow + " hours after snow" + at,
                            IceWeatherLaw.targetDepth(true, hoursUntilSnow, hoursSinceSnow, humidity), 3.0);
                }
            }
            double previous = 0.0;
            for (double hours = 30.0; hours >= 0.0; hours -= 0.25) {
                double depth = depthBeforeSnow(hours, humidity);
                helper.assertTrue(depth >= previous * (1.0 - RELATIVE_TOLERANCE),
                        "Depth " + hours + " hours before snow" + at + " is " + depth + ", below " + previous
                                + " at the hour before");
                previous = depth;
            }
        }
    }

    private static void assertRecoveryAfterSnow(GameTestHelper helper) {
        for (double humidity : new double[] {0.0, 50.0}) {
            double clear = IceWeatherLaw.clearDepth(humidity);
            String at = " at humidity " + humidity;
            assertRelative(helper, "depth when the snow ends" + at, depthAfterSnow(0.0, humidity), 3.0);
            assertRelative(helper, "depth 6 hours after snow" + at, depthAfterSnow(6.0, humidity), clear);
            assertRelative(helper, "depth 30 hours after snow" + at, depthAfterSnow(30.0, humidity), clear);
            double previous = Double.POSITIVE_INFINITY;
            for (double hours = 0.0; hours <= 8.0; hours += 0.25) {
                double depth = depthAfterSnow(hours, humidity);
                helper.assertTrue(depth <= previous * (1.0 + RELATIVE_TOLERANCE),
                        "Depth " + hours + " hours after snow" + at + " is " + depth + ", above " + previous
                                + " at the hour before");
                previous = depth;
            }
            for (double[] pair : new double[][] {{3.0, 1.0}, {10.0, 2.0}, {20.0, 0.5}, {0.0, 5.0}}) {
                double combined = IceWeatherLaw.targetDepth(false, pair[0], pair[1], humidity);
                double expected = Math.max(depthBeforeSnow(pair[0], humidity), depthAfterSnow(pair[1], humidity));
                assertRelative(helper, "depth " + pair[0] + " hours before and " + pair[1] + " hours after snow" + at,
                        combined, expected);
            }
        }
    }

    private static void assertSmoothing(GameTestHelper helper) {
        double current = 0.5;
        double target = 2.0;
        assertRelative(helper, "smoothed depth with no elapsed time",
                IceWeatherLaw.smoothedDepth(current, target, 0.0), current);
        assertRelative(helper, "smoothed depth after a very long time",
                IceWeatherLaw.smoothedDepth(current, target, 1.0e6), target);
        double closed = IceWeatherLaw.smoothedDepth(current, target, IceWeatherLaw.SMOOTHING_SECONDS);
        double closedFraction = (Math.log(closed) - Math.log(target)) / (Math.log(current) - Math.log(target));
        assertRelative(helper, "fraction of the gap left after one smoothing time", closedFraction, 1.0 / Math.E);
        for (double elapsed : new double[] {0.1, 1.0, 10.0, 49.0, 50.0, 51.0, 100.0, 500.0}) {
            for (double[] pair : new double[][] {{0.5, 2.0}, {3.0, 0.02}, {0.02, 3.0}}) {
                double result = IceWeatherLaw.smoothedDepth(pair[0], pair[1], elapsed);
                double low = Math.min(pair[0], pair[1]);
                double high = Math.max(pair[0], pair[1]);
                helper.assertTrue(result >= low * (1.0 - RELATIVE_TOLERANCE)
                                && result <= high * (1.0 + RELATIVE_TOLERANCE),
                        "Smoothed depth from " + pair[0] + " toward " + pair[1] + " after " + elapsed
                                + " seconds is " + result + ", outside the range from " + low + " to " + high);
            }
        }
    }

    private static double dustAfterSnow(double hoursSinceSnow) {
        return DiamondDustLaw.targetExtinction(false, hoursSinceSnow);
    }

    private static double depthBeforeSnow(double hoursUntilSnow, double humidity) {
        return IceWeatherLaw.targetDepth(false, hoursUntilSnow, Double.POSITIVE_INFINITY, humidity);
    }

    private static double depthAfterSnow(double hoursSinceSnow, double humidity) {
        return IceWeatherLaw.targetDepth(false, Double.POSITIVE_INFINITY, hoursSinceSnow, humidity);
    }

    private static SkyTables tablesAt(double depth) {
        return new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(depth));
    }

    /**
     * Records the difference toward one direction in worst, or in worstUnderSun when the sun's square covers the
     * direction, since SkyView.radiance leaves the square out.
     */
    private static void compareDirection(WorstDifference worst, WorstDifference worstUnderSun, DepthBlend blend,
            IceHaloProfile profile, IceHaloMap map, String cell, double sunDegrees, double azimuthDegrees,
            double elevationDegrees) {
        double azimuth = Math.toRadians(azimuthDegrees);
        double elevation = Math.toRadians(elevationDegrees);
        WorstDifference target = underSunSquare(azimuth, elevation, Math.toRadians(sunDegrees)) ? worstUnderSun : worst;
        String where = cell + ", sun " + sunDegrees + " degrees, azimuth " + azimuthDegrees + " degrees, elevation "
                + elevationDegrees + " degrees";
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double blended = displaySteps(blend.blendedRadiance(channel, azimuth, elevation, profile, map));
            double exact = displaySteps(blend.exactRadiance(channel, azimuth, elevation, profile, map));
            target.record(Math.abs(blended - exact), where + ", channel " + channel);
        }
    }

    private static void compareGround(WorstDifference worst, DepthBlend blend, String where) {
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            worst.record(relativeDifference(blend.blendedGround(channel), blend.exactGround(channel)),
                    where + ", channel " + channel);
        }
    }

    /**
     * Whether the direction is within SUN_SQUARE_MARGIN_DEGREES of the sun's square, measured like sunCoverage in
     * calculated_sky.fsh.
     */
    private static boolean underSunSquare(double azimuth, double elevation, double sunElevation) {
        double directionX = Math.cos(elevation) * Math.cos(azimuth);
        double directionY = Math.sin(elevation);
        double directionZ = Math.cos(elevation) * Math.sin(azimuth);
        double forward = directionX * Math.cos(sunElevation) + directionY * Math.sin(sunElevation);
        if (forward <= 0.0) {
            return false;
        }
        double alongPath = (directionX * Math.sin(sunElevation) - directionY * Math.cos(sunElevation)) / forward;
        double across = directionZ / forward;
        return Math.max(Math.abs(alongPath), Math.abs(across)) < SUN_SQUARE_HALF_TANGENT;
    }

    private static double relativeDifference(double actual, double expected) {
        if (expected == 0.0) {
            return actual == 0.0 ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return Math.abs(actual / expected - 1.0);
    }

    private static double displaySteps(double radiance) {
        return SkyDisplay.displaySteps(SkySetting.EXPOSURE.defaultValue(), radiance);
    }

    private static void assertRelative(GameTestHelper helper, String what, double actual, double expected) {
        double scale = Math.max(Math.abs(expected), 1.0e-12);
        helper.assertTrue(Math.abs(actual - expected) / scale <= RELATIVE_TOLERANCE,
                what + " is " + actual + " instead of " + expected);
    }

    private static void assertClose(GameTestHelper helper, String what, double actual, double expected,
            double tolerance) {
        helper.assertTrue(Math.abs(actual - expected) <= tolerance,
                what + " is " + actual + " instead of " + expected);
    }

    /**
     * The views of one cell of the grid at one sun elevation, with the diffraction weights of their tables.
     */
    private static final class DepthBlend {
        private final SkyView lowView;
        private final SkyView highView;
        private final SkyView middleView;
        private final float[] lowWeights;
        private final float[] highWeights;
        private final float[] middleWeights;

        private DepthBlend(SkyTables lowTables, SkyTables highTables, SkyTables middleTables, double sunElevation,
                IceHaloProfile profile, IceHaloMap map) {
            lowView = lowTables.computeView(sunElevation, profile, map);
            highView = highTables.computeView(sunElevation, profile, map);
            middleView = middleTables.computeView(sunElevation, profile, map);
            lowWeights = lowTables.diffractionWeights();
            highWeights = highTables.diffractionWeights();
            middleWeights = middleTables.diffractionWeights();
        }

        private double blendedRadiance(int channel, double azimuth, double elevation, IceHaloProfile profile,
                IceHaloMap map) {
            return 0.5 * lowView.radiance(channel, azimuth, elevation, profile, map, lowWeights)
                    + 0.5 * highView.radiance(channel, azimuth, elevation, profile, map, highWeights);
        }

        private double exactRadiance(int channel, double azimuth, double elevation, IceHaloProfile profile,
                IceHaloMap map) {
            return middleView.radiance(channel, azimuth, elevation, profile, map, middleWeights);
        }

        private double blendedGround(int channel) {
            return 0.5 * lowView.groundRadiance()[channel] + 0.5 * highView.groundRadiance()[channel];
        }

        private double exactGround(int channel) {
            return middleView.groundRadiance()[channel];
        }
    }

    /**
     * The largest difference seen so far and where it was seen.
     */
    private static final class WorstDifference {
        private double value = -1.0;
        private String where = "nowhere";

        private void record(double difference, String location) {
            if (difference > value) {
                value = difference;
                where = location;
            }
        }

        private double value() {
            return value;
        }

        private String where() {
            return where;
        }
    }
}
