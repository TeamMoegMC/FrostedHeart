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

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Checks how the calculated sky dims vanilla's stars and moon sprite, and the floor its night sky does not go below.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class NightSkyGameTests {
    private static final double EXPOSURE = SkySetting.EXPOSURE.defaultValue();
    private static final double[] FULL_LIGHT = {1.0, 1.0, 1.0};
    private static final double[] NO_LIGHT = {0.0, 0.0, 0.0};
    private static final double[] NO_FLOOR = {0.0, 0.0, 0.0};
    /**
     * The display value of the sky overhead at which vanilla's stars have faded away.
     */
    private static final double TWILIGHT_DISPLAY = 0.25;
    private static final double[] STAR_DEPTHS = {0.02, 0.4, 1.0, 3.0};
    private static final double[] STAR_TWILIGHT_DISPLAYS = {0.0, 0.05, 0.125, 0.2};
    private static final double[] NO_STAR_TWILIGHT_DISPLAYS = {0.25, 0.5, 1.0};
    private static final double NIGHT_SUN_DEGREES = -18.0;
    private static final double DAY_SUN_DEGREES = 30.0;
    private static final double[] SINKING_SUN_DEGREES = {10.0, 0.0, -4.0};
    private static final double[] SET_MOON_DEGREES = {-5.0, 0.0};
    private static final double HIGH_MOON_DEGREES = 60.0;
    /**
     * The sine of the moon's elevation at which the sprite has faded halfway into the fog.
     */
    private static final double HALF_FADED_MOON_SINE = 0.04;
    private static final double BLEND_MOON_DEGREES = 30.0;
    private static final double BLEND_DEPTH_WEIGHT = 0.5;
    private static final double LOW_MOON_DEGREES = 18.0;
    private static final double RED_MOON_DEGREES = 5.0;
    private static final double[] FLOORS = {0.0, 0.003, 0.2};
    private static final double[] TONE_MAPPED_VALUES = {0.0, 0.3, 1.0};
    private static final int SRGB_STEPS = 100;
    private static final double FLOOR_DISPLAY = 0.04;
    private static final double FLOOR_BLUE_SHIFT = 0.5;
    private static final double WHITE_BLUE_SHIFT = 0.0;
    private static final double LIT_SUN_DEGREES = 4.5;
    /**
     * The largest lift the night sky floor may add to the fog of a lit sky.
     */
    private static final double LIT_FOG_LIFT_LIMIT = 0.01;
    private static final double EXACT_TOLERANCE = 1.0e-12;
    private static final double SRGB_TOLERANCE = 1.0e-9;
    private static final double MOON_TOLERANCE = 1.0e-6;
    private static final double FOG_TOLERANCE = 1.0e-6;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void starsDimThroughIceAndAgainstABrightSky(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables clear = new SkyTables(SkySettings.DEFAULT);

        double overheadScale = NightSkyDimming.starScale(0.0, 0.0, TWILIGHT_DISPLAY);
        helper.assertTrue(overheadScale == 1.0,
                "Stars overhead under a black sky have scale " + overheadScale + " instead of 1");
        double negativeScale = NightSkyDimming.starScale(-1.0, -0.1, TWILIGHT_DISPLAY);
        helper.assertTrue(negativeScale == 1.0,
                "Stars with an ice depth of -1 and a sky display of -0.1 have scale " + negativeScale
                        + " instead of 1");

        double previousScale = Double.POSITIVE_INFINITY;
        for (double depth : STAR_DEPTHS) {
            double scale = NightSkyDimming.starScale(depth, 0.0, TWILIGHT_DISPLAY);
            double expected = Math.sqrt(Math.exp(-depth / Math.sqrt(0.5)));
            helper.assertTrue(Math.abs(scale - expected) < EXACT_TOLERANCE,
                    "Stars through an ice depth of " + depth + " have scale " + scale + ", expected " + expected);
            helper.assertTrue(scale < previousScale,
                    "Stars through an ice depth of " + depth + " have scale " + scale + ", not below the "
                            + previousScale + " of the depth before");
            helper.assertTrue(scale < 1.0,
                    "Stars through an ice depth of " + depth + " have scale " + scale + ", not below 1");
            previousScale = scale;
        }

        for (double skyDisplay : STAR_TWILIGHT_DISPLAYS) {
            double scale = NightSkyDimming.starScale(0.0, skyDisplay, TWILIGHT_DISPLAY);
            double expected = Math.sqrt(1.0 - skyDisplay / TWILIGHT_DISPLAY);
            helper.assertTrue(Math.abs(scale - expected) < EXACT_TOLERANCE,
                    "Stars under a sky of display " + skyDisplay + " have scale " + scale + ", expected "
                            + expected);
        }
        for (double skyDisplay : NO_STAR_TWILIGHT_DISPLAYS) {
            double scale = NightSkyDimming.starScale(0.0, skyDisplay, TWILIGHT_DISPLAY);
            helper.assertTrue(scale == 0.0,
                    "Stars under a sky of display " + skyDisplay + " have scale " + scale + " instead of 0");
        }

        double nightElevation = Math.toRadians(NIGHT_SUN_DEGREES);
        SkyViewSet nightViews = SkyViewSet.of(clear.computeView(nightElevation, profile,
                new IceHaloMap(data, nightElevation, blurSigma)));
        double nightZenith = nightViews.zenithDisplayLuminance(0.0, 0.0, EXPOSURE, NO_LIGHT, profile);
        helper.assertTrue(nightZenith == 0.0,
                "The sky overhead at sun " + NIGHT_SUN_DEGREES + " degrees has luminance " + nightZenith
                        + " instead of 0");
        double nightScale = NightSkyDimming.starScale(0.0, nightZenith, TWILIGHT_DISPLAY);
        helper.assertTrue(nightScale == 1.0,
                "Vanilla's stars at sun " + NIGHT_SUN_DEGREES + " degrees have scale " + nightScale
                        + " instead of 1");

        double dayElevation = Math.toRadians(DAY_SUN_DEGREES);
        SkyViewSet dayViews = SkyViewSet.of(clear.computeView(dayElevation, profile,
                new IceHaloMap(data, dayElevation, blurSigma)));
        double dayZenith = dayViews.zenithDisplayLuminance(0.0, 0.0, EXPOSURE, FULL_LIGHT, profile);
        helper.assertTrue(dayZenith > TWILIGHT_DISPLAY && dayZenith <= 1.0,
                "The sky overhead at sun " + DAY_SUN_DEGREES + " degrees has luminance " + dayZenith
                        + ", outside the range from " + TWILIGHT_DISPLAY + " to 1");
        double dayScale = NightSkyDimming.starScale(0.0, dayZenith, TWILIGHT_DISPLAY);
        helper.assertTrue(dayScale == 0.0,
                "Stars at sun " + DAY_SUN_DEGREES + " degrees have scale " + dayScale + " instead of 0");

        double[] zenithLuminances = new double[SINKING_SUN_DEGREES.length];
        for (int index = 0; index < SINKING_SUN_DEGREES.length; index++) {
            double elevation = Math.toRadians(SINKING_SUN_DEGREES[index]);
            SkyViewSet views = SkyViewSet.of(clear.computeView(elevation, profile,
                    new IceHaloMap(data, elevation, blurSigma)));
            zenithLuminances[index] = views.zenithDisplayLuminance(0.0, 0.0, EXPOSURE, FULL_LIGHT, profile);
        }
        String sinkingValues = "The sky overhead at sun " + SINKING_SUN_DEGREES[0] + ", " + SINKING_SUN_DEGREES[1]
                + " and " + SINKING_SUN_DEGREES[2] + " degrees has luminance " + zenithLuminances[0] + ", "
                + zenithLuminances[1] + " and " + zenithLuminances[2];
        helper.assertTrue(zenithLuminances[0] > zenithLuminances[1],
                sinkingValues + ", so it does not darken from the first sun to the second");
        helper.assertTrue(zenithLuminances[1] > zenithLuminances[2],
                sinkingValues + ", so it does not darken from the second sun to the third");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void moonSpriteTakesTheSunsSquareAtTheMoonsPlace(GameTestHelper helper) {
        SkyTables clear = new SkyTables(SkySettings.DEFAULT);
        SkyTables thick = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(3.0));
        float[] moon = new float[SkyAtmosphere.CHANNEL_COUNT];

        for (double elevationDegrees : SET_MOON_DEGREES) {
            NightSkyDimming.moonColor(clear, clear, 0.0, Math.toRadians(elevationDegrees), EXPOSURE, moon);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                helper.assertTrue(moon[channel] == 0.0F,
                        "The moon at " + elevationDegrees + " degrees has channel " + channel + " at "
                                + moon[channel] + " instead of 0");
            }
        }

        double highElevation = Math.toRadians(HIGH_MOON_DEGREES);
        NightSkyDimming.moonColor(clear, clear, 0.0, highElevation, EXPOSURE, moon);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double expected = sunSquareDisplay(clear, channel, highElevation);
            helper.assertTrue(Math.abs(moon[channel] - expected) < MOON_TOLERANCE,
                    "The moon at " + HIGH_MOON_DEGREES + " degrees has channel " + channel + " at " + moon[channel]
                            + ", the sun's square there at " + expected);
            helper.assertTrue(moon[channel] > 0.0F && moon[channel] <= 1.0F,
                    "The moon at " + HIGH_MOON_DEGREES + " degrees has channel " + channel + " at " + moon[channel]
                            + ", outside the range from 0 to 1");
        }

        double fadedElevation = Math.asin(HALF_FADED_MOON_SINE);
        NightSkyDimming.moonColor(clear, clear, 0.0, fadedElevation, EXPOSURE, moon);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double expected = 0.5 * sunSquareDisplay(clear, channel, fadedElevation);
            helper.assertTrue(Math.abs(moon[channel] - expected) < MOON_TOLERANCE,
                    "The moon at " + Math.toDegrees(fadedElevation) + " degrees has channel " + channel + " at "
                            + moon[channel] + ", half the sun's square there at " + expected);
        }

        double blendElevation = Math.toRadians(BLEND_MOON_DEGREES);
        NightSkyDimming.moonColor(clear, thick, BLEND_DEPTH_WEIGHT, blendElevation, EXPOSURE, moon);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double clearRadiance = SkyView.sampleRow(clear.sunRadiance(), channel, blendElevation);
            double thickRadiance = SkyView.sampleRow(thick.sunRadiance(), channel, blendElevation);
            double expected = SkyDisplay.displayValue(EXPOSURE,
                    BLEND_DEPTH_WEIGHT * (clearRadiance + thickRadiance));
            helper.assertTrue(Math.abs(moon[channel] - expected) < MOON_TOLERANCE,
                    "The moon at " + BLEND_MOON_DEGREES + " degrees with depth weight " + BLEND_DEPTH_WEIGHT
                            + " has channel " + channel + " at " + moon[channel] + ", expected " + expected);
        }

        double lowElevation = Math.toRadians(LOW_MOON_DEGREES);
        float[] clearLowMoon = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] thickLowMoon = new float[SkyAtmosphere.CHANNEL_COUNT];
        NightSkyDimming.moonColor(clear, clear, 0.0, lowElevation, EXPOSURE, clearLowMoon);
        NightSkyDimming.moonColor(thick, thick, 0.0, lowElevation, EXPOSURE, thickLowMoon);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(thickLowMoon[channel] < clearLowMoon[channel],
                    "Under snow the moon at " + LOW_MOON_DEGREES + " degrees has channel " + channel + " at "
                            + thickLowMoon[channel] + ", not below the " + clearLowMoon[channel] + " of clear air");
        }

        float[] thickHighMoon = new float[SkyAtmosphere.CHANNEL_COUNT];
        NightSkyDimming.moonColor(thick, thick, 0.0, highElevation, EXPOSURE, thickHighMoon);
        helper.assertTrue(thickHighMoon[1] > 0.5F,
                "Under snow the moon at " + HIGH_MOON_DEGREES + " degrees has green " + thickHighMoon[1]
                        + ", not above 0.5");

        double redElevation = Math.toRadians(RED_MOON_DEGREES);
        NightSkyDimming.moonColor(clear, clear, 0.0, redElevation, EXPOSURE, moon);
        helper.assertTrue(moon[0] > moon[2],
                "The moon at " + RED_MOON_DEGREES + " degrees has red " + moon[0] + ", not above the blue "
                        + moon[2]);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void nightFloorLiftsTheBlackSkyAndTheFogAlike(GameTestHelper helper) {
        for (double floor : FLOORS) {
            double blackSky = SkyDisplay.withFloor(0.0, floor);
            helper.assertTrue(Math.abs(blackSky - floor) < EXACT_TOLERANCE,
                    "A black sky lifted by a floor of " + floor + " shows " + blackSky);
            double whiteSky = SkyDisplay.withFloor(1.0, floor);
            helper.assertTrue(Math.abs(whiteSky - 1.0) < EXACT_TOLERANCE,
                    "A white sky lifted by a floor of " + floor + " shows " + whiteSky);
        }
        for (double toneMapped : TONE_MAPPED_VALUES) {
            double noFloor = SkyDisplay.withFloor(toneMapped, 0.0);
            helper.assertTrue(Math.abs(noFloor - toneMapped) < EXACT_TOLERANCE,
                    "A sky of " + toneMapped + " lifted by a floor of 0 shows " + noFloor);
        }

        for (int step = 0; step <= SRGB_STEPS; step++) {
            double encoded = step / (double) SRGB_STEPS;
            double roundTrip = SkyDisplay.decodeSrgb(SkyDisplay.encodeSrgb(encoded));
            helper.assertTrue(Math.abs(roundTrip - encoded) < SRGB_TOLERANCE,
                    "The sRGB round trip of " + encoded + " gives " + roundTrip);
        }
        double darkDecode = SkyDisplay.decodeSrgb(0.04);
        helper.assertTrue(Math.abs(darkDecode - 0.04 / 12.92) < EXACT_TOLERANCE,
                "The linear value of the sRGB 0.04 is " + darkDecode + ", expected " + 0.04 / 12.92);

        double[] zeroFloor = new double[SkyAtmosphere.CHANNEL_COUNT];
        NightSkyDimming.floorColor(0.0, FLOOR_BLUE_SHIFT, zeroFloor);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(zeroFloor[channel] == 0.0,
                    "A floor of display 0 has channel " + channel + " at " + zeroFloor[channel] + " instead of 0");
        }

        double[] blueFloor = new double[SkyAtmosphere.CHANNEL_COUNT];
        NightSkyDimming.floorColor(FLOOR_DISPLAY, FLOOR_BLUE_SHIFT, blueFloor);
        assertFloorTint(helper, blueFloor, FLOOR_BLUE_SHIFT);
        helper.assertTrue(blueFloor[2] > blueFloor[1] && blueFloor[1] > blueFloor[0],
                "The floor at blue shift " + FLOOR_BLUE_SHIFT + " is red " + blueFloor[0] + ", green "
                        + blueFloor[1] + " and blue " + blueFloor[2] + ", not blue over green over red");

        double[] whiteFloor = new double[SkyAtmosphere.CHANNEL_COUNT];
        NightSkyDimming.floorColor(FLOOR_DISPLAY, WHITE_BLUE_SHIFT, whiteFloor);
        assertFloorTint(helper, whiteFloor, WHITE_BLUE_SHIFT);
        helper.assertTrue(whiteFloor[0] > whiteFloor[1] && whiteFloor[1] > whiteFloor[2],
                "The floor at blue shift " + WHITE_BLUE_SHIFT + " is red " + whiteFloor[0] + ", green "
                        + whiteFloor[1] + " and blue " + whiteFloor[2] + ", not red over green over blue");

        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables clear = new SkyTables(SkySettings.DEFAULT);

        double nightElevation = Math.toRadians(NIGHT_SUN_DEGREES);
        SkyViewSet nightViews = SkyViewSet.of(clear.computeView(nightElevation, profile,
                new IceHaloMap(data, nightElevation, blurSigma)));
        float[] nightFog = new float[SkyAtmosphere.CHANNEL_COUNT];
        nightViews.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, EXPOSURE, NO_LIGHT, blueFloor, 0.0F, 0.0F, nightFog);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double expected = SkyDisplay.encodeSrgb(blueFloor[channel]);
            helper.assertTrue(Math.abs(nightFog[channel] - expected) < FOG_TOLERANCE,
                    "The fog at night with the floor has channel " + channel + " at " + nightFog[channel]
                            + ", the encoded floor " + expected);
        }
        float[] bareNightFog = new float[SkyAtmosphere.CHANNEL_COUNT];
        nightViews.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, EXPOSURE, NO_LIGHT, 0.0F, 0.0F, bareNightFog);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(bareNightFog[channel] == 0.0F,
                    "The fog at night without a floor has channel " + channel + " at " + bareNightFog[channel]
                            + " instead of 0");
        }

        double litElevation = Math.toRadians(LIT_SUN_DEGREES);
        SkyViewSet litViews = SkyViewSet.of(clear.computeView(litElevation, profile,
                new IceHaloMap(data, litElevation, blurSigma)));
        float[] litFog = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] zeroFloorFog = new float[SkyAtmosphere.CHANNEL_COUNT];
        float[] floorFog = new float[SkyAtmosphere.CHANNEL_COUNT];
        litViews.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, EXPOSURE, FULL_LIGHT, 0.0F, 0.0F, litFog);
        litViews.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, EXPOSURE, FULL_LIGHT, NO_FLOOR, 0.0F, 0.0F, zeroFloorFog);
        litViews.horizonDisplayColor(0.0, 0.0, 0.0, 1.0, EXPOSURE, FULL_LIGHT, blueFloor, 0.0F, 0.0F, floorFog);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            helper.assertTrue(litFog[channel] == zeroFloorFog[channel],
                    "The lit fog at sun " + LIT_SUN_DEGREES + " degrees has channel " + channel + " at "
                            + litFog[channel] + " without a floor and " + zeroFloorFog[channel] + " with a floor of 0");
            helper.assertTrue(floorFog[channel] >= litFog[channel],
                    "The floor darkens the lit fog at sun " + LIT_SUN_DEGREES + " degrees, channel " + channel
                            + " is " + floorFog[channel] + " against " + litFog[channel] + " without a floor");
            helper.assertTrue(floorFog[channel] <= litFog[channel] + LIT_FOG_LIFT_LIMIT,
                    "The floor lifts the lit fog at sun " + LIT_SUN_DEGREES + " degrees, channel " + channel
                            + " is " + floorFog[channel] + " against " + litFog[channel] + " without a floor");
        }
        helper.succeed();
    }

    /**
     * The displayed value of the sun's square through the tables at the elevation, which the moon's sprite takes.
     */
    private static double sunSquareDisplay(SkyTables tables, int channel, double elevationRadians) {
        return SkyDisplay.displayValue(EXPOSURE, SkyView.sampleRow(tables.sunRadiance(), channel, elevationRadians));
    }

    /**
     * Checks that the floor keeps the luminance of its display value and the tint of the moonlight in every channel.
     */
    private static void assertFloorTint(GameTestHelper helper, double[] floor, double blueShift) {
        double luminance = 0.2126 * floor[0] + 0.7152 * floor[1] + 0.0722 * floor[2];
        helper.assertTrue(Math.abs(luminance - FLOOR_DISPLAY / 12.92) < SRGB_TOLERANCE,
                "The floor of display " + FLOOR_DISPLAY + " at blue shift " + blueShift + " has luminance "
                        + luminance + ", expected " + FLOOR_DISPLAY / 12.92);
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double ratio = floor[channel] / floor[1];
            double expected = MoonLight.color(channel, blueShift) / MoonLight.color(1, blueShift);
            helper.assertTrue(Math.abs(ratio - expected) < SRGB_TOLERANCE,
                    "The floor at blue shift " + blueShift + " has channel " + channel + " at " + ratio
                            + " of the green, expected " + expected);
        }
    }
}
