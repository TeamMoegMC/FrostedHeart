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
 * Checks the exposure of the calculated sky as the eye adapts to the sky's luminance: the key and the luminance of
 * SkyAdaptation, the luminance the clear views give through dusk, and how much a full moon lowers the night exposure.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class SkyAdaptationGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double DAY_EXPOSURE = SkySetting.EXPOSURE.defaultValue();
    private static final double NIGHT_EXPOSURE = SkySetting.NIGHT_EXPOSURE.defaultValue();
    private static final double DAY_LUMINANCE = SkySetting.ADAPTATION_DAY_LUMINANCE.defaultValue();
    private static final double NIGHT_LUMINANCE = SkySetting.ADAPTATION_NIGHT_LUMINANCE.defaultValue();
    private static final double MOONLIGHT_BRIGHTNESS = SkySetting.MOONLIGHT_BRIGHTNESS.defaultValue();
    private static final double MOONLIGHT_SWITCH_DEGREES = SkySetting.MOONLIGHT_SWITCH_DEGREES.defaultValue();
    private static final double MOONLIGHT_FADE_DEGREES = SkySetting.MOONLIGHT_FADE_DEGREES.defaultValue();
    private static final double MOONLIGHT_BLUE_SHIFT = SkySetting.MOONLIGHT_BLUE_SHIFT.defaultValue();
    private static final double[] FULL_LIGHT = {1.0, 1.0, 1.0};
    private static final double[] HALF_LIGHT = {0.5, 0.5, 0.5};
    private static final double[] NO_LIGHT = {0.0, 0.0, 0.0};
    private static final double KEY_TOLERANCE = 1.0e-12;
    private static final double LUMINANCE_TOLERANCE = 1.0e-6;
    private static final double CLOSED_FORM_TOLERANCE = 1.0e-9;
    private static final double[] CLOSED_FORM_LUMINANCES = {100.0, 1.0, 0.1};
    private static final int MONOTONE_STEPS = 200;
    private static final double CONTINUITY_OFFSET = 1.0e-9;
    private static final double CONTINUITY_TOLERANCE = 1.0e-5;
    private static final double TUNED_NIGHT_PRODUCT = 6.0 * 0.0125;
    private static final double TUNED_NIGHT_TOLERANCE = 1.0e-12;
    /**
     * Sun elevations in degrees from a high day through dusk to deep night.
     */
    private static final double[] DUSK_SUN_DEGREES = {30.0, 10.0, 5.0, 2.0, 0.0, -2.0, -4.0, -6.0, -8.0, -10.0,
            -12.0};
    /**
     * The lowest sun elevation in degrees the halo map is built for, below which it takes this one.
     */
    private static final double LOWEST_HALO_SUN_DEGREES = -6.0;
    private static final double[] INTEGRAL_SUN_DEGREES = {30.0, 2.0, -2.0};
    private static final double INTEGRAL_TOLERANCE = 0.15;
    private static final double DUSK_LUMINANCE_MINIMUM = 400.0;
    private static final double DUSK_LUMINANCE_MAXIMUM = 2000.0;
    private static final double NIGHT_LUMINANCE_LIMIT = 1.0;
    private static final double LIGHT_SCALE_TOLERANCE = 1.0e-12;
    private static final int AZIMUTH_SAMPLES = 180;
    private static final double AZIMUTH_START_DEGREES = -179.0;
    private static final double AZIMUTH_STEP_DEGREES = 2.0;
    private static final int ELEVATION_SAMPLES = 90;
    private static final double ELEVATION_START_DEGREES = 0.5;
    private static final double ZENITH_GREEN_DEGREES = 89.5;
    private static final int FULL_MOON_PHASE = 0;
    private static final int HALF_MOON_PHASE = 2;
    private static final int NEW_MOON_PHASE = 4;
    private static final int[] MOON_PHASES = {FULL_MOON_PHASE, HALF_MOON_PHASE, NEW_MOON_PHASE};
    private static final double OVERHEAD_DEGREES = 90.0;
    private static final double LOW_MOON_DEGREES = 30.0;
    /**
     * The lowest share of the night exposure that a full moon may leave.
     */
    private static final double MOON_EXPOSURE_FLOOR = 0.75;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void adaptedExposureFollowsTheKeyBetweenItsLimits(GameTestHelper helper) {
        assertNear(helper, "The key of the luminance 0", SkyAdaptation.key(0.0), 0.03, KEY_TOLERANCE);
        assertNear(helper, "The key of the luminance 9", SkyAdaptation.key(9.0), 1.03 - 2.0 / 3.0, KEY_TOLERANCE);
        assertNear(helper, "The key of the luminance 99", SkyAdaptation.key(99.0), 0.53, KEY_TOLERANCE);
        double zeroKey = SkyAdaptation.key(0.0);
        double negativeKey = SkyAdaptation.key(-5.0);
        helper.assertTrue(negativeKey == zeroKey, "The key of the luminance -5 is " + negativeKey
                + " instead of the " + zeroKey + " of the luminance 0");

        assertNear(helper, "The luminance of the radiance 1, 1, 1", SkyAdaptation.luminance(1.0, 1.0, 1.0), 1.28e5,
                LUMINANCE_TOLERANCE);
        assertNear(helper, "The luminance of the radiance 0, 1, 0", SkyAdaptation.luminance(0.0, 1.0, 0.0),
                0.7152 * 1.28e5, LUMINANCE_TOLERANCE);

        for (double luminance : new double[] {DAY_LUMINANCE, 1.0e4, 1.0e6}) {
            double exposure = adaptedExposure(luminance);
            helper.assertTrue(exposure == DAY_EXPOSURE, "The exposure at the luminance " + luminance + " is " + exposure
                    + " instead of the day exposure " + DAY_EXPOSURE);
        }
        for (double luminance : new double[] {NIGHT_LUMINANCE, 0.001, 0.0, -1.0}) {
            double exposure = adaptedExposure(luminance);
            helper.assertTrue(exposure == NIGHT_EXPOSURE, "The exposure at the luminance " + luminance + " is "
                    + exposure + " instead of the night exposure " + NIGHT_EXPOSURE);
        }

        double dayLog = Math.log(SkyAdaptation.key(DAY_LUMINANCE) / DAY_LUMINANCE);
        double nightLog = Math.log(SkyAdaptation.key(NIGHT_LUMINANCE) / NIGHT_LUMINANCE);
        for (double luminance : CLOSED_FORM_LUMINANCES) {
            double logarithm = Math.log(SkyAdaptation.key(luminance) / luminance);
            double share = (logarithm - dayLog) / (nightLog - dayLog);
            double expected = DAY_EXPOSURE * Math.pow(NIGHT_EXPOSURE / DAY_EXPOSURE, share);
            assertRelative(helper, "The exposure at the luminance " + luminance, adaptedExposure(luminance), expected,
                    CLOSED_FORM_TOLERANCE);
        }

        double[] luminances = new double[MONOTONE_STEPS + 1];
        double[] exposures = new double[MONOTONE_STEPS + 1];
        for (int index = 0; index <= MONOTONE_STEPS; index++) {
            luminances[index] = DAY_LUMINANCE
                    * Math.pow(NIGHT_LUMINANCE / DAY_LUMINANCE, index / (double) MONOTONE_STEPS);
            exposures[index] = adaptedExposure(luminances[index]);
        }
        for (int index = 1; index <= MONOTONE_STEPS - 1; index++) {
            helper.assertTrue(exposures[index] > exposures[index - 1], "The exposure at the luminance "
                    + luminances[index] + " of step " + index + " is " + exposures[index] + ", not above the "
                    + exposures[index - 1] + " of the step before");
        }
        for (int index = 0; index <= MONOTONE_STEPS; index++) {
            helper.assertTrue(exposures[index] >= DAY_EXPOSURE && exposures[index] <= NIGHT_EXPOSURE,
                    "The exposure at the luminance " + luminances[index] + " of step " + index + " is "
                            + exposures[index] + ", outside the range from the day exposure " + DAY_EXPOSURE
                            + " to the night exposure " + NIGHT_EXPOSURE);
        }

        double belowDay = adaptedExposure(DAY_LUMINANCE * (1.0 - CONTINUITY_OFFSET));
        assertRelative(helper, "The exposure just below the day luminance", belowDay, DAY_EXPOSURE,
                CONTINUITY_TOLERANCE);
        double aboveNight = adaptedExposure(NIGHT_LUMINANCE * (1.0 + CONTINUITY_OFFSET));
        assertRelative(helper, "The exposure just above the night luminance", aboveNight, NIGHT_EXPOSURE,
                CONTINUITY_TOLERANCE);

        double equalLimits = 1.0;
        double aboveEqual = SkyAdaptation.exposure(2.0, DAY_EXPOSURE, NIGHT_EXPOSURE, equalLimits, equalLimits);
        helper.assertTrue(aboveEqual == DAY_EXPOSURE, "The exposure at the luminance 2 with both limits at "
                + equalLimits + " is " + aboveEqual + " instead of the day exposure " + DAY_EXPOSURE);
        double atEqual = SkyAdaptation.exposure(1.0, DAY_EXPOSURE, NIGHT_EXPOSURE, equalLimits, equalLimits);
        helper.assertTrue(atEqual == DAY_EXPOSURE, "The exposure at the luminance 1 with both limits at " + equalLimits
                + " is " + atEqual + " instead of the day exposure " + DAY_EXPOSURE);
        double belowEqual = SkyAdaptation.exposure(0.5, DAY_EXPOSURE, NIGHT_EXPOSURE, equalLimits, equalLimits);
        helper.assertTrue(belowEqual == NIGHT_EXPOSURE, "The exposure at the luminance 0.5 with both limits at "
                + equalLimits + " is " + belowEqual + " instead of the night exposure " + NIGHT_EXPOSURE);

        for (double luminance : CLOSED_FORM_LUMINANCES) {
            double swapped = SkyAdaptation.exposure(luminance, DAY_EXPOSURE, NIGHT_EXPOSURE, NIGHT_LUMINANCE,
                    DAY_LUMINANCE);
            double ordered = adaptedExposure(luminance);
            helper.assertTrue(swapped == ordered, "The exposure at the luminance " + luminance + " with the limits "
                    + "swapped is " + swapped + " instead of the " + ordered + " with them in order");
        }
        double notANumber = adaptedExposure(Double.NaN);
        helper.assertTrue(notANumber == NIGHT_EXPOSURE, "The exposure at a luminance that is not a number is "
                + notANumber + " instead of the night exposure " + NIGHT_EXPOSURE);

        double sunExposure = SkyAdaptation.fixedExposure(SkyLight.Kind.SUN, DAY_EXPOSURE, NIGHT_EXPOSURE);
        helper.assertTrue(sunExposure == DAY_EXPOSURE, "The fixed exposure under the sun is " + sunExposure
                + " instead of the day exposure " + DAY_EXPOSURE);
        double moonExposure = SkyAdaptation.fixedExposure(SkyLight.Kind.MOON, DAY_EXPOSURE, NIGHT_EXPOSURE);
        helper.assertTrue(moonExposure == NIGHT_EXPOSURE, "The fixed exposure under the moon is " + moonExposure
                + " instead of the night exposure " + NIGHT_EXPOSURE);

        double tunedNight = NIGHT_EXPOSURE * MOONLIGHT_BRIGHTNESS;
        assertNear(helper, "The night exposure times the moonlight brightness", tunedNight, TUNED_NIGHT_PRODUCT,
                TUNED_NIGHT_TOLERANCE);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void adaptationLuminanceFallsThroughDusk(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables tables = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(IceWeatherLaw.CLEAR_MINIMUM_DEPTH));
        float[] zeroWeights = new float[tables.diffractionWeights().length];

        SkyView[] views = new SkyView[DUSK_SUN_DEGREES.length];
        IceHaloMap[] maps = new IceHaloMap[DUSK_SUN_DEGREES.length];
        double[] luminances = new double[DUSK_SUN_DEGREES.length];
        double[] integrals = new double[DUSK_SUN_DEGREES.length];
        for (int index = 0; index < DUSK_SUN_DEGREES.length; index++) {
            double elevation = Math.toRadians(DUSK_SUN_DEGREES[index]);
            double mapDegrees = Math.max(LOWEST_HALO_SUN_DEGREES, DUSK_SUN_DEGREES[index]);
            maps[index] = new IceHaloMap(data, Math.toRadians(mapDegrees), blurSigma);
            views[index] = tables.computeView(elevation, profile, maps[index]);
            luminances[index] = adaptationLuminance(views[index], FULL_LIGHT);
        }
        for (double sunDegrees : INTEGRAL_SUN_DEGREES) {
            int index = duskIndex(sunDegrees);
            integrals[index] = integralLuminance(views[index], profile, maps[index], zeroWeights);
        }

        for (int index = 0; index < DUSK_SUN_DEGREES.length; index++) {
            double exposure = adaptedExposure(luminances[index]);
            double zenithGreenSteps = SkyDisplay.displaySteps(exposure, views[index].radiance(1, 0.0,
                    Math.toRadians(ZENITH_GREEN_DEGREES), profile, maps[index], tables.diffractionWeights()));
            double horizonGreenSteps = SkyDisplay.displaySteps(exposure, views[index].meanHorizonRadiance()[1]);
            float[] sunwardFog = new float[3];
            SkyViewSet.of(views[index]).horizonDisplayColor(0.0, 0.0, 0.0, 1.0, exposure, 0.0F, 0.0F, sunwardFog);
            LOGGER.info(String.format(Locale.ROOT,
                    "ADAPT dusk sun=%.1f luminance=%.4e sky=%.4e integral=%.4e exposure=%.4e zenithGreenSteps=%.2f "
                            + "horizonGreenSteps=%.2f sunwardFog=%.0f,%.0f,%.0f", DUSK_SUN_DEGREES[index],
                    luminances[index], SkyViewSet.of(views[index]).skyAdaptationLuminance(0.0, 0.0, FULL_LIGHT),
                    integrals[index], exposure, zenithGreenSteps, horizonGreenSteps, 255.0 * sunwardFog[0],
                    255.0 * sunwardFog[1], 255.0 * sunwardFog[2]));
        }

        for (int index = 1; index < DUSK_SUN_DEGREES.length; index++) {
            helper.assertTrue(luminances[index] < luminances[index - 1], "The sky at sun " + DUSK_SUN_DEGREES[index]
                    + " degrees has luminance " + luminances[index] + ", not below the " + luminances[index - 1]
                    + " at sun " + DUSK_SUN_DEGREES[index - 1] + " degrees");
        }
        helper.assertTrue(luminances[0] > DAY_LUMINANCE, "The sky at sun " + DUSK_SUN_DEGREES[0]
                + " degrees has luminance " + luminances[0] + ", not above the day luminance " + DAY_LUMINANCE);
        double sunsetLuminance = luminances[duskIndex(0.0)];
        helper.assertTrue(sunsetLuminance >= DUSK_LUMINANCE_MINIMUM
                && sunsetLuminance <= DUSK_LUMINANCE_MAXIMUM, "The sky at sun " + DUSK_SUN_DEGREES[duskIndex(0.0)]
                        + " degrees has luminance " + sunsetLuminance + ", outside the range from "
                        + DUSK_LUMINANCE_MINIMUM + " to " + DUSK_LUMINANCE_MAXIMUM);
        double deepestLuminance = luminances[DUSK_SUN_DEGREES.length - 1];
        helper.assertTrue(deepestLuminance < NIGHT_LUMINANCE_LIMIT, "The sky at sun "
                + DUSK_SUN_DEGREES[DUSK_SUN_DEGREES.length - 1] + " degrees has luminance " + deepestLuminance
                + ", not below " + NIGHT_LUMINANCE_LIMIT);

        int zeroDegreesIndex = duskIndex(0.0);
        double fullScaleLuminance = luminances[zeroDegreesIndex];
        double halfScaleLuminance = adaptationLuminance(views[zeroDegreesIndex], HALF_LIGHT);
        assertRelative(helper, "The sky at sun " + DUSK_SUN_DEGREES[zeroDegreesIndex] + " degrees with a light "
                + "scale of 0.5 in every channel", halfScaleLuminance, 0.5 * fullScaleLuminance,
                LIGHT_SCALE_TOLERANCE);
        double noScaleLuminance = adaptationLuminance(views[zeroDegreesIndex], NO_LIGHT);
        helper.assertTrue(noScaleLuminance == 0.0, "The sky at sun " + DUSK_SUN_DEGREES[zeroDegreesIndex]
                + " degrees with a light scale of 0 in every channel has luminance " + noScaleLuminance
                + " instead of 0");

        for (double sunDegrees : INTEGRAL_SUN_DEGREES) {
            int index = duskIndex(sunDegrees);
            double skyLuminance = SkyViewSet.of(views[index]).skyAdaptationLuminance(0.0, 0.0, FULL_LIGHT);
            assertRelative(helper, "The integral of the sky at sun " + sunDegrees + " degrees", integrals[index],
                    skyLuminance, INTEGRAL_TOLERANCE);
        }
        for (int index = 0; index < DUSK_SUN_DEGREES.length; index++) {
            SkyViewSet set = SkyViewSet.of(views[index]);
            double skyLuminance = set.skyAdaptationLuminance(0.0, 0.0, FULL_LIGHT);
            double horizonLuminance = set.horizonAdaptationLuminance(0.0, 0.0, FULL_LIGHT);
            double[] horizon = views[index].brightestHorizonRadiance();
            double expectedHorizon = SkyAdaptation.luminance(horizon[0], horizon[1], horizon[2]);
            double[] mean = views[index].meanHorizonRadiance();
            double meanLuminance = SkyAdaptation.luminance(mean[0], mean[1], mean[2]);
            helper.assertTrue(expectedHorizon >= meanLuminance, "The brightest horizon at sun "
                    + DUSK_SUN_DEGREES[index] + " degrees has luminance " + expectedHorizon + ", below the mean "
                    + meanLuminance);
            assertRelative(helper, "The horizon's luminance at sun " + DUSK_SUN_DEGREES[index] + " degrees",
                    horizonLuminance, expectedHorizon, LIGHT_SCALE_TOLERANCE);
            helper.assertTrue(luminances[index] == Math.max(skyLuminance, horizonLuminance), "The adaptation "
                    + "luminance at sun " + DUSK_SUN_DEGREES[index] + " degrees is " + luminances[index]
                    + " instead of the larger of the sky's " + skyLuminance + " and the horizon's "
                    + horizonLuminance);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void fullMoonLowersTheNightExposureALittle(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables tables = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(IceWeatherLaw.CLEAR_MINIMUM_DEPTH));
        IceHaloMap overheadMap = new IceHaloMap(data, Math.toRadians(OVERHEAD_DEGREES), blurSigma);
        SkyView overheadView = tables.computeView(Math.toRadians(OVERHEAD_DEGREES), profile, overheadMap);
        IceHaloMap lowMap = new IceHaloMap(data, Math.toRadians(LOW_MOON_DEGREES), blurSigma);
        SkyView lowView = tables.computeView(Math.toRadians(LOW_MOON_DEGREES), profile, lowMap);

        SkyLight light = new SkyLight();
        double[] exposures = new double[MOON_PHASES.length];
        for (int index = 0; index < MOON_PHASES.length; index++) {
            int phase = MOON_PHASES[index];
            chooseMoon(light, phase);
            helper.assertTrue(light.kind() == SkyLight.Kind.MOON, "The light at midnight with the moon of phase "
                    + phase + " is " + light.kind() + " instead of MOON");
            double luminance = adaptationLuminance(overheadView, moonLightScale(light));
            exposures[index] = adaptedExposure(luminance);
            LOGGER.info(String.format(Locale.ROOT, "ADAPT moon phase=%d elevation=%.0f luminance=%.4e exposure=%.4e",
                    phase, OVERHEAD_DEGREES, luminance, exposures[index]));
        }

        double fullMoonExposure = exposures[0];
        chooseMoon(light, FULL_MOON_PHASE);
        helper.assertTrue(light.kind() == SkyLight.Kind.MOON, "The light at midnight with the moon of phase "
                + FULL_MOON_PHASE + " is " + light.kind() + " instead of MOON");
        double lowLuminance = adaptationLuminance(lowView, moonLightScale(light));
        double lowExposure = adaptedExposure(lowLuminance);
        LOGGER.info(String.format(Locale.ROOT, "ADAPT moon phase=%d elevation=%.0f luminance=%.4e exposure=%.4e",
                FULL_MOON_PHASE, LOW_MOON_DEGREES, lowLuminance, lowExposure));

        helper.assertTrue(lowExposure >= MOON_EXPOSURE_FLOOR * NIGHT_EXPOSURE && lowExposure <= NIGHT_EXPOSURE,
                "The full moon at " + LOW_MOON_DEGREES + " degrees gives the exposure " + lowExposure
                        + ", outside the range from " + (MOON_EXPOSURE_FLOOR * NIGHT_EXPOSURE)
                        + " to the night exposure " + NIGHT_EXPOSURE);
        helper.assertTrue(lowExposure >= fullMoonExposure, "The full moon at " + LOW_MOON_DEGREES + " degrees gives "
                + "the exposure " + lowExposure + ", below the " + fullMoonExposure + " of the full moon overhead");

        for (int index = 1; index < MOON_PHASES.length; index++) {
            helper.assertTrue(exposures[index] == NIGHT_EXPOSURE, "The moon of phase " + MOON_PHASES[index]
                    + " overhead gives the exposure " + exposures[index] + " instead of the night exposure "
                    + NIGHT_EXPOSURE);
        }
        helper.assertTrue(fullMoonExposure >= MOON_EXPOSURE_FLOOR * NIGHT_EXPOSURE
                && fullMoonExposure < NIGHT_EXPOSURE, "The full moon overhead gives the exposure "
                        + fullMoonExposure + ", outside the range from " + (MOON_EXPOSURE_FLOOR * NIGHT_EXPOSURE)
                        + " up to the night exposure " + NIGHT_EXPOSURE);
        helper.succeed();
    }

    /**
     * The exposure with the four defaults of the day and night limits.
     */
    private static double adaptedExposure(double luminance) {
        return SkyAdaptation.exposure(luminance, DAY_EXPOSURE, NIGHT_EXPOSURE, DAY_LUMINANCE, NIGHT_LUMINANCE);
    }

    /**
     * The luminance the view adapts to with the light scale in every channel.
     */
    private static double adaptationLuminance(SkyView view, double[] lightScale) {
        return SkyViewSet.of(view).adaptationLuminance(0.0, 0.0, lightScale);
    }

    /**
     * The luminance of the mean radiance of the view weighted by the cosine to the zenith, over the half of the sky
     * above the horizon, with the diffraction peak of the ice left out.
     */
    private static double integralLuminance(SkyView view, IceHaloProfile haloProfile, IceHaloMap haloMap,
            float[] diffractionWeights) {
        double[] weightedSums = new double[SkyAtmosphere.CHANNEL_COUNT];
        double weightSum = 0.0;
        for (int azimuthIndex = 0; azimuthIndex < AZIMUTH_SAMPLES; azimuthIndex++) {
            double azimuth = Math.toRadians(AZIMUTH_START_DEGREES + AZIMUTH_STEP_DEGREES * azimuthIndex);
            for (int elevationIndex = 0; elevationIndex < ELEVATION_SAMPLES; elevationIndex++) {
                double elevation = Math.toRadians(ELEVATION_START_DEGREES + elevationIndex);
                double weight = Math.cos(elevation) * Math.sin(elevation);
                weightSum += weight;
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    weightedSums[channel] += view.radiance(channel, azimuth, elevation, haloProfile, haloMap,
                            diffractionWeights) * weight;
                }
            }
        }
        return SkyAdaptation.luminance(weightedSums[0] / weightSum, weightedSums[1] / weightSum,
                weightedSums[2] / weightSum);
    }

    /**
     * The scale of the light on each channel of a sky built for the moon.
     */
    private static double[] moonLightScale(SkyLight light) {
        double[] lightScale = new double[SkyAtmosphere.CHANNEL_COUNT];
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            lightScale[channel] = light.radianceScale(channel, SkyLight.Kind.MOON);
        }
        return lightScale;
    }

    /**
     * Sets the light to the moon of the phase at midnight, which stands overhead.
     */
    private static void chooseMoon(SkyLight light, int moonPhase) {
        MoonLight.chooseLight(light, Math.PI, moonPhase, MOONLIGHT_SWITCH_DEGREES, MOONLIGHT_FADE_DEGREES,
                MOONLIGHT_BRIGHTNESS, MOONLIGHT_BLUE_SHIFT);
    }

    /**
     * The place of a sun elevation in the dusk elevations.
     */
    private static int duskIndex(double elevationDegrees) {
        for (int index = 0; index < DUSK_SUN_DEGREES.length; index++) {
            if (DUSK_SUN_DEGREES[index] == elevationDegrees) {
                return index;
            }
        }
        throw new IllegalArgumentException("The dusk elevations do not hold the sun at " + elevationDegrees
                + " degrees");
    }

    private static void assertNear(GameTestHelper helper, String where, double found, double expected,
            double tolerance) {
        helper.assertTrue(Math.abs(found - expected) <= tolerance, where + " is " + found + ", expected " + expected
                + " within " + tolerance);
    }

    private static void assertRelative(GameTestHelper helper, String where, double found, double expected,
            double tolerance) {
        double error = Math.abs(found - expected) / Math.abs(expected);
        helper.assertTrue(error <= tolerance, where + " is " + found + ", expected " + expected
                + " within the relative tolerance " + tolerance + ", found the relative error " + error);
    }
}
