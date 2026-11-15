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

import java.util.Arrays;

/**
 * Checks the glints that diamond_dust.glsl draws against the shader's math in DustGlitterField: the cubes that repeat
 * over the world, the lives that each point lives one after another, the lit window of a life and its stretch with the
 * dust's lit time, the fall of a life, the decisions taken at the middle of the lit window, the nested share of points
 * that the dust draws, the fade before the copies switch, the light of a glint, the size of a glint and the spread of
 * its light, and the plates' mirror lobe below the halo map.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class DustGlitterGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int AXIS_COUNT = 3;
    /**
     * Eye coordinates that the box tests use for each axis in turn, with the other two at 0.
     */
    private static final double[] EYE_COORDINATES = {
            -1.0e6 - 0.3, -40000.7, -17.25, -0.001, 0.0, 5.5, 31.999, 32.0, 1000.4, 2.9e7 + 0.6
    };
    private static final int BOX_POINT_COUNT = 4096;
    private static final int[] BOX_LIVES = {0, 5};
    private static final float[] BOX_AGES = {0.0F, 0.7F, 1.9F};
    private static final float FALL_SPEED = 0.5F;
    private static final double BOX_TOLERANCE = 1.0e-3;
    private static final double MOVING_EYE_START = 0.0;
    private static final double MOVING_EYE_HIGHEST = 20.0;
    private static final double MOVING_EYE_STEP = 0.25;
    /**
     * The offset from the eye within which a point stays in its box with half a block to spare.
     */
    private static final double MOVING_EYE_INNER_HALF = DustGlitterField.CELL_SIZE / 2.0 - 0.5;
    private static final double MOVING_EYE_TOLERANCE = 1.0e-3;
    private static final int MOVING_EYE_LEAST_COMPARISONS = 10000;
    private static final float[] DRAWN_SHARES = {0.1F, 0.3F, 0.6F, 1.0F};
    private static final int SHARE_POINT_COUNT = 65536;
    private static final double SHARE_TOLERANCE = 0.01;
    private static final double DRAWN_HEIGHT_TOLERANCE = 1.0e-6;
    private static final double FADE_SCAN_STEP = 0.01;
    private static final double LIFE_SECONDS_TOLERANCE = 1.0e-6;
    private static final int SMALLEST_CYCLES_LIMIT = 2060;
    private static final int LARGEST_CYCLES_LIMIT = 4085;
    private static final double[] CLOCK_SECONDS = {0.0, 0.37, 1.0, 17.25, 1000.123, 2048.5, 4095.9};
    private static final int CLOCK_POINT_COUNT = 4096;
    private static final double CLOCK_AGE_TOLERANCE = 1.0e-5;
    private static final double CLOCK_EDGE_SHARE = 1.0e-5;
    private static final double CLOCK_SKIP_SHARE = 0.01;
    private static final double WRAP_BEFORE_SECONDS = 1.0e-4;
    private static final double WRAP_AGE_TOLERANCE = 2.0e-5;
    private static final double WRAP_SKIP_AGE_SECONDS = 2.0e-4;
    /**
     * The life rates at which the clock keeps a point's life across its wrap.
     */
    private static final float[] WRAP_LIFE_RATES = {1.0F, 0.4F, 0.05F};
    private static final double LIT_VISIBLE_STEP = 1.0e-4;
    private static final double LIT_QUARTILE_TOLERANCE = 0.01;
    private static final double[] LIT_QUARTILE_SHARES = {0.25, 0.5, 0.75};
    private static final double[] LIT_QUARTILE_SECONDS = {0.16, 0.20, 0.25};
    private static final double LIT_SHORTEST_SECONDS = 0.12;
    private static final double LIT_LONGEST_SECONDS = 0.70;
    private static final int LIT_LIFE_COUNT = 4;
    /**
     * The time scale and the life rate at which the reference lit times hold, which the checks of the reference use.
     */
    private static final float REFERENCE_TIME_SCALE = 1.0F;
    private static final float REFERENCE_LIFE_RATE = 1.0F;
    /**
     * The lit times of the dust's setting that the stretch is checked at, in seconds.
     */
    private static final double[] LIT_SETTINGS_SECONDS = {0.2, 0.5, 1.0, 4.0};
    private static final double LIT_SCALE_TOLERANCE = 1.0e-6;
    private static final double LIT_SCALE_WIDE_TOLERANCE = 1.0e-5;
    private static final double LIT_RATE_TOLERANCE = 1.0e-7;
    private static final int FEWEST_LIFE_CYCLES = 1;
    private static final int MOST_LIFE_CYCLES = DustGlitterField.LEAST_LIFE_CYCLES + DustGlitterField.LIFE_CYCLE_RANGE
            - 1;
    /**
     * A life lasts at least the time scale less this share of it and at most this factor times the time scale.
     */
    private static final double LIT_LIFE_FLOOR_SHARE = 1.0e-5;
    private static final double LIT_LIFE_CEILING_FACTOR = 2.05;
    private static final double LIT_START_TOLERANCE_SHARE = 1.0e-5;
    private static final double ENVELOPE_TOLERANCE = 1.0e-6;
    private static final double ENVELOPE_HALF_TOLERANCE = 1.0e-5;
    private static final double ENVELOPE_FLAT_LOWEST_AGE = 0.351;
    private static final double ENVELOPE_FLAT_HIGHEST_AGE = 0.649;
    private static final double ENVELOPE_FLAT_STEP = 0.001;
    /**
     * The stretched envelope's time scale, lit start, and lit time in seconds, and the ages on its flat top.
     */
    private static final float ENVELOPE_STRETCHED_SCALE = 2.0F;
    private static final float ENVELOPE_STRETCHED_START = 0.3F;
    private static final float ENVELOPE_STRETCHED_LENGTH = 0.8F;
    private static final float[] ENVELOPE_STRETCHED_ZERO_AGES = {0.2F, 1.2F};
    private static final float[] ENVELOPE_STRETCHED_HALF_AGES = {0.35F, 1.05F};
    private static final double ENVELOPE_STRETCHED_FLAT_LOWEST_AGE = 0.401;
    private static final double ENVELOPE_STRETCHED_FLAT_HIGHEST_AGE = 0.999;
    private static final double SPEED_TOLERANCE = 1.0e-5;
    private static final double TILT_TOLERANCE_DEGREES = 1.0e-3;
    private static final double WIDEST_TILT_SEEN_DEGREES = 29.0;
    private static final double HALF_TILT_DEGREES = 15.0;
    private static final double HALF_TILT_SHARE_TOLERANCE = 0.02;
    private static final float SMALLEST_SPEED_LIMIT = 0.36F;
    private static final float LARGEST_SPEED_LIMIT = 0.64F;
    private static final double QUADRANT_LEAST_SHARE = 0.20;
    private static final double QUADRANT_TILT_DEGREES = 1.0;
    private static final double MIDDLE_OFFSET_TOLERANCE = 1.0e-6;
    private static final double[] MIDDLE_EYE_OFFSET = {5.3, 17.9, 30.2};
    private static final int MIDDLE_AGE_STEPS = 20;
    private static final double MIDDLE_TOLERANCE = 1.0e-4;
    private static final int MIDDLE_LEAST_COMPARED = 3000;
    private static final int SHOWN_SAMPLE_COUNT = 100000;
    private static final double SHOWN_MEAN_TOLERANCE = 1.0e-3;
    private static final float[] PLAIN_SHARES = {0.1F, 0.4F, 0.5F, 0.9F};
    private static final float[] HALO_PLAIN_SHARES = {0.0F, 0.5F, 1.0F};
    private static final int HALO_HASH_COUNT = 4096;
    private static final float[] NEAREST_DISTANCES = {0.0F, 0.5F, 1.0F};
    private static final float[] NEAR_SHARE_DISTANCES = {0.0F, DustGlitterField.NEAR_SHARE_DISTANCE};
    private static final float[] NEAR_ALL_SHOWN_DISTANCES = {DustGlitterField.NEAR_DISTANCE, 10.0F};
    private static final float[] NEAR_FALLOFF_DISTANCES = {4.0F, 6.0F, 10.0F, 15.0F};
    private static final double NEAR_RELATIVE_TOLERANCE = 1.0e-6;
    private static final double NEAR_BLUR_TOLERANCE = 1.0e-6;
    private static final double NEAR_WIDEST_BLUR_TOLERANCE = 1.0e-5;
    private static final float NEAR_REFERENCE_DISTANCE = 6.0F;
    private static final float[] NEAR_RATIO_DISTANCES = {3.0F, 2.5F, 2.0F, 1.5F};
    private static final double[] NEAR_RATIO_EXPECTED = {1.17, 0.91292, 0.65, 0.20034};
    private static final double[] NEAR_RATIO_TOLERANCES = {0.002, 0.002, 0.001, 0.0005};
    private static final double NEAR_DISTANCE_STEP = 0.01;
    private static final double NEAR_SHOWN_STEP_LIMIT = 0.09;
    private static final double NEAR_LIGHT_STEP_LIMIT = 0.01;
    private static final double NEAR_MEAN_TOLERANCE = 1.0e-3;
    private static final float SIZE_SPREAD_NONE = 0.0F;
    private static final float SIZE_SPREAD_ONE = 1.0F;
    private static final float SIZE_SPREAD_TWO = 2.0F;
    private static final float[] SIZE_SPREADS = {SIZE_SPREAD_NONE, SIZE_SPREAD_ONE, SIZE_SPREAD_TWO};
    private static final float[] SIZE_FLAT_DISTANCES = {1.0F, 3.0F, 6.0F, 16.0F};
    private static final double SIZE_FACTOR_TOLERANCE = 1.0e-6;
    private static final float SIZE_BASE_RADIUS = 5.1F;
    private static final double SIZE_RADIUS_TOLERANCE = 1.0e-5;
    private static final double SIZE_LIGHT_RELATIVE_TOLERANCE = 1.0e-5;
    private static final double SIZE_PEAK_TOLERANCE = 1.0e-7;
    private static final double SIZE_DISTANCE_STEP = 0.01;
    private static final double SIZE_STEP_LIMIT = 0.04;
    private static final double SIZE_SCAN_LOWEST_DISTANCE = 1.0;
    private static final double SIZE_SCAN_HIGHEST_DISTANCE = 16.0;
    private static final double SIZE_LIGHT_LOWEST_DISTANCE = 1.5;
    private static final double SIZE_LIGHT_HIGHEST_DISTANCE = 15.5;
    private static final double SIZE_LIGHT_STEP = 0.5;
    private static final double[] GLINT_DEPTHS = {0.1, 1.0};
    private static final double[] GLINT_SUN_DEGREES = {2.0, 6.0};
    private static final double[] GLINT_SUN_WEIGHTS = {0.0, 0.3, 1.0};
    private static final double[] GLINT_DEPTH_WEIGHTS = {0.0, 0.6, 1.0};
    private static final double[] FULL_LIGHT_SCALE = {1.0, 1.0, 1.0};
    private static final double MOON_SCALE = 0.0125;
    private static final double[] MOON_COLOR = {0.8, 0.9, 1.1};
    private static final double TRANSMITTANCE_TOLERANCE = 1.0e-9;
    private static final double GLINT_LIGHT_SUN_DEGREES = 10.0;
    private static final double GLINT_DARK_SUN_DEGREES = -2.0;
    private static final double[] MIRROR_SUN_DEGREES = {-1.0, 3.0, 6.0};
    private static final double MIRROR_WEIGHT = 0.1;
    private static final double MIRROR_SCALE_TOLERANCE = 1.0e-9;
    /**
     * The rows of column 0 compared, from -2 to +2 degrees, and the share of the largest of them below which a bin is
     * only printed.
     */
    private static final int MIRROR_ROWS = 8;
    private static final double MIRROR_SMALLEST_SHARE = 1.0e-3;
    /**
     * Bins below this share of the largest are held to the tolerance as if they were this large, since the map takes
     * each row's strength at its middle and the tail of the lobe falls steeply across a bin.
     */
    private static final double MIRROR_TAIL_SHARE = 0.1;
    private static final int MIRROR_STEPS = 200;
    private static final double MIRROR_TOLERANCE = 0.03;
    private static final double BELOW_MAP_LOWEST_DEGREES = -3.0;
    private static final double BELOW_MAP_HIGHEST_DEGREES = -2.0;
    private static final int BELOW_MAP_STEPS = 100;
    private static final double BELOW_MAP_LOBE = 0.1;
    private static final double BELOW_MAP_EDGE = 2.0;
    private static final double BELOW_MAP_ROW = 0.5;

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterPointsStayInTheirBoxAndInTheWorld(GameTestHelper helper) {
        float[] velocity = new float[AXIS_COUNT];
        for (double eye : EYE_COORDINATES) {
            float eyeOffset = (float) DustGlitterField.cellOffset(eye);
            for (int axis = 0; axis < AXIS_COUNT; axis++) {
                for (int index = 0; index < BOX_POINT_COUNT; index++) {
                    for (int life : BOX_LIVES) {
                        DustGlitterField.velocity(index, life, FALL_SPEED, velocity);
                        for (float age : BOX_AGES) {
                            float position = DustGlitterField.lifeStart(index, life, axis) + velocity[axis] * age;
                            float offset = DustGlitterField.nearestOffset(position, eyeOffset);
                            helper.assertTrue(Math.abs(offset) <= DustGlitterField.CELL_SIZE / 2.0 + BOX_TOLERANCE,
                                    "Point " + index + " on axis " + axis + " in life " + life + " at the age " + age
                                            + " with the eye at " + eye + " lies " + offset
                                            + " from the eye, outside the box of half size "
                                            + DustGlitterField.CELL_SIZE / 2.0);
                            double world = eye + offset;
                            double multiple = Math.round((world - position) / DustGlitterField.CELL_SIZE);
                            helper.assertTrue(Math.abs(world - position - multiple * DustGlitterField.CELL_SIZE)
                                            <= BOX_TOLERANCE, "Point " + index + " on axis " + axis + " in life "
                                    + life + " at the age " + age + " with the eye at " + eye + " stands at " + world
                                    + " instead of the " + (position + multiple * DustGlitterField.CELL_SIZE)
                                    + " of the copy " + multiple + " cells away");
                        }
                    }
                }
            }
        }

        int compared = 0;
        for (int index = 0; index < BOX_POINT_COUNT; index++) {
            float position = DustGlitterField.lifeStart(index, 0, 0);
            float startEyeOffset = (float) DustGlitterField.cellOffset(MOVING_EYE_START);
            float startOffset = DustGlitterField.nearestOffset(position, startEyeOffset);
            float startShift = DustGlitterField.copyShift(position, startEyeOffset);
            if (Math.abs(startOffset) > MOVING_EYE_INNER_HALF) {
                continue;
            }
            for (double eye = MOVING_EYE_START; eye <= MOVING_EYE_HIGHEST; eye += MOVING_EYE_STEP) {
                float eyeOffset = (float) DustGlitterField.cellOffset(eye);
                float offset = DustGlitterField.nearestOffset(position, eyeOffset);
                // The offset of the same copy at the moved eye, which stays the nearest one inside the box:
                double movedOffset = position - eyeOffset - (double) DustGlitterField.CELL_SIZE * startShift;
                if (Math.abs(movedOffset) > MOVING_EYE_INNER_HALF) {
                    continue;
                }
                compared++;
                helper.assertTrue(Math.abs(eye + offset - startOffset) <= MOVING_EYE_TOLERANCE, "Point " + index
                        + " with the eye moved from " + MOVING_EYE_START + " to " + eye + " stands at "
                        + (eye + offset) + " instead of at the " + startOffset + " it stood at, since its offset "
                        + startOffset + " and " + movedOffset + " both stay inside " + MOVING_EYE_INNER_HALF);
            }
        }
        helper.assertTrue(compared >= MOVING_EYE_LEAST_COMPARISONS, "Only " + compared
                + " points of the moving eye stay inside the box, not the " + MOVING_EYE_LEAST_COMPARISONS
                + " the test needs");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterDrawsANestedShareOfPoints(GameTestHelper helper) {
        helper.assertTrue(DustGlitterField.extinctionShare(0.5) == 0.5, "The extinction share of 0.5 per kilometer "
                + "is " + DustGlitterField.extinctionShare(0.5) + " instead of 0.5");
        helper.assertTrue(DustGlitterField.extinctionShare(3.0) == 1.0, "The extinction share of 3 per kilometer is "
                + DustGlitterField.extinctionShare(3.0) + " instead of the full 1");
        helper.assertTrue(DustGlitterField.extinctionShare(0.0) == 0.0, "The extinction share of 0 per kilometer is "
                + DustGlitterField.extinctionShare(0.0) + " instead of 0");
        helper.assertTrue(DustGlitterField.extinctionShare(-1.0) == 0.0, "The extinction share of -1 per kilometer is "
                + DustGlitterField.extinctionShare(-1.0) + " instead of 0");
        helper.assertTrue(DustGlitterField.heightWeight(-10.0F) == 1.0F, "The height weight 10 blocks below the "
                + "dust's bottom is " + DustGlitterField.heightWeight(-10.0F) + " instead of 1");
        helper.assertTrue(DustGlitterField.heightWeight(0.0F) == 1.0F, "The height weight at the dust's bottom is "
                + DustGlitterField.heightWeight(0.0F) + " instead of 1");
        float scaleHeight = (float) LightPillarSelection.DUST_SCALE_HEIGHT;
        double atScaleHeight = DustGlitterField.heightWeight(scaleHeight);
        helper.assertTrue(Math.abs(atScaleHeight - Math.exp(-1.0)) <= DRAWN_HEIGHT_TOLERANCE, "The height weight one "
                + "scale height of " + scaleHeight + " above the dust's bottom is " + atScaleHeight + " instead of "
                + Math.exp(-1.0));

        int[] drawnCounts = new int[DRAWN_SHARES.length];
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            boolean drawnBefore = false;
            for (int shareIndex = 0; shareIndex < DRAWN_SHARES.length; shareIndex++) {
                boolean drawn = DustGlitterField.isDrawn(index, DRAWN_SHARES[shareIndex]);
                if (drawnBefore && !drawn) {
                    helper.fail("Point " + index + " is drawn at the share " + DRAWN_SHARES[shareIndex - 1]
                            + " but not at the larger share " + DRAWN_SHARES[shareIndex]);
                }
                drawnBefore = drawn;
                if (drawn) {
                    drawnCounts[shareIndex]++;
                }
            }
        }
        for (int shareIndex = 0; shareIndex < DRAWN_SHARES.length; shareIndex++) {
            double drawnShare = (double) drawnCounts[shareIndex] / SHARE_POINT_COUNT;
            helper.assertTrue(Math.abs(drawnShare - DRAWN_SHARES[shareIndex]) <= SHARE_TOLERANCE, "The share "
                    + DRAWN_SHARES[shareIndex] + " draws " + drawnCounts[shareIndex] + " of the " + SHARE_POINT_COUNT
                    + " points, the share " + drawnShare + ", not within " + SHARE_TOLERANCE + " of "
                    + DRAWN_SHARES[shareIndex]);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterFadesOutBeforeTheCopiesSwitch(GameTestHelper helper) {
        helper.assertTrue(DustGlitterField.fade(0.0F) == 1.0F, "The fade at 0 blocks is "
                + DustGlitterField.fade(0.0F) + " instead of 1");
        helper.assertTrue(DustGlitterField.fade(DustGlitterField.FADE_START) == 1.0F, "The fade at "
                + DustGlitterField.FADE_START + " blocks is " + DustGlitterField.fade(DustGlitterField.FADE_START)
                + " instead of 1");
        helper.assertTrue(DustGlitterField.fade(DustGlitterField.FADE_END) == 0.0F, "The fade at "
                + DustGlitterField.FADE_END + " blocks is " + DustGlitterField.fade(DustGlitterField.FADE_END)
                + " instead of 0");
        float diagonal = (float) (DustGlitterField.FADE_END * Math.sqrt(3.0));
        helper.assertTrue(DustGlitterField.fade(diagonal) == 0.0F, "The fade at the diagonal of the cube, "
                + diagonal + " blocks, is " + DustGlitterField.fade(diagonal) + " instead of 0");
        float previous = DustGlitterField.fade(0.0F);
        for (int step = 1; step <= (int) Math.round(DustGlitterField.FADE_END / FADE_SCAN_STEP); step++) {
            float distance = (float) (step * FADE_SCAN_STEP);
            float fade = DustGlitterField.fade(distance);
            helper.assertTrue(fade <= previous, "The fade rises from " + previous + " to " + fade + " at "
                    + distance + " blocks");
            previous = fade;
        }
        helper.assertTrue(DustGlitterField.falloff(0.0F) == 1.0F, "The falloff at 0 blocks is "
                + DustGlitterField.falloff(0.0F) + " instead of 1");
        float halfFalloff = DustGlitterField.falloff(DustGlitterField.FALLOFF_DISTANCE);
        helper.assertTrue(Math.abs(halfFalloff - 0.5F) <= 1.0e-6, "The falloff at " + DustGlitterField.FALLOFF_DISTANCE
                + " blocks is " + halfFalloff + " instead of 0.5");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterLivesAWholeNumberOfLivesPerWrap(GameTestHelper helper) {
        int smallestCycles = Integer.MAX_VALUE;
        int largestCycles = Integer.MIN_VALUE;
        int mostCycles = DustGlitterField.LEAST_LIFE_CYCLES + DustGlitterField.LIFE_CYCLE_RANGE - 1;
        float shortestLife = DustGlitterField.lifeSeconds(DustGlitterField.LEAST_LIFE_CYCLES);
        float longestLife = DustGlitterField.lifeSeconds(mostCycles);
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            int cycles = DustGlitterField.lifeCycles(index, REFERENCE_LIFE_RATE);
            smallestCycles = Math.min(smallestCycles, cycles);
            largestCycles = Math.max(largestCycles, cycles);
            helper.assertTrue(cycles >= DustGlitterField.LEAST_LIFE_CYCLES && cycles <= mostCycles, "Point " + index
                    + " lives " + cycles + " lives per wrap, outside " + DustGlitterField.LEAST_LIFE_CYCLES + " to "
                    + mostCycles);
            float lifeSeconds = DustGlitterField.lifeSeconds(cycles);
            helper.assertTrue(lifeSeconds >= 1.0F - (float) LIFE_SECONDS_TOLERANCE
                            && lifeSeconds <= 2.0F + (float) LIFE_SECONDS_TOLERANCE, "Point " + index + " with "
                    + cycles + " lives per wrap lives " + lifeSeconds + " seconds, outside 1 to 2 seconds");
            float phase = DustGlitterField.lifePhase(index);
            helper.assertTrue(phase >= 0.0F && phase < 1.0F, "Point " + index + " stands at the phase " + phase
                    + " when the clock is 0, outside 0 up to 1");
        }
        helper.assertTrue(smallestCycles <= SMALLEST_CYCLES_LIMIT, "The fewest lives per wrap over the "
                + SHARE_POINT_COUNT + " points is " + smallestCycles + ", above the " + SMALLEST_CYCLES_LIMIT
                + " the test needs");
        helper.assertTrue(largestCycles >= LARGEST_CYCLES_LIMIT, "The most lives per wrap over the "
                + SHARE_POINT_COUNT + " points is " + largestCycles + ", below the " + LARGEST_CYCLES_LIMIT
                + " the test needs");
        helper.assertTrue(shortestLife == 2.0F, "The life of " + DustGlitterField.LEAST_LIFE_CYCLES
                + " lives per wrap is " + shortestLife + " seconds instead of exactly 2");
        helper.assertTrue(longestLife == 1.0F, "The life of " + mostCycles
                + " lives per wrap is " + longestLife + " seconds instead of exactly 1");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterLifeClockMatchesDoubleAndKeepsItsLifeAcrossTheWrap(GameTestHelper helper) {
        double highSteps = DustGlitterField.CLOCK_HIGH_STEPS;
        int comparisons = 0;
        int skips = 0;
        for (double clockSeconds : CLOCK_SECONDS) {
            double share = DustGlitterField.clockShare(clockSeconds);
            float high = DustGlitterField.clockHigh(share);
            float low = DustGlitterField.clockLow(share);
            helper.assertTrue(high * highSteps == Math.floor(high * highSteps), "The high part " + high + " of the "
                    + "clock at " + clockSeconds + " seconds is " + (high * highSteps) + " steps of "
                    + DustGlitterField.CLOCK_HIGH_STEPS + ", not a whole number");
            helper.assertTrue(low >= 0.0F && low < 1.0 / DustGlitterField.CLOCK_HIGH_STEPS + 1.0e-9, "The low part "
                    + low + " of the clock at " + clockSeconds + " seconds is outside 0 up to "
                    + (1.0 / DustGlitterField.CLOCK_HIGH_STEPS));
            for (int index = 0; index < CLOCK_POINT_COUNT; index++) {
                int cycles = DustGlitterField.lifeCycles(index, REFERENCE_LIFE_RATE);
                float phase = DustGlitterField.lifePhase(index);
                double scaled = share * cycles + phase;
                double fraction = scaled - Math.floor(scaled);
                comparisons++;
                if (fraction < CLOCK_EDGE_SHARE || fraction > 1.0 - CLOCK_EDGE_SHARE) {
                    skips++;
                    continue;
                }
                int lifeReference = Math.floorMod((long) Math.floor(scaled), cycles);
                double ageReference = fraction * DustGlitterField.CLOCK_WRAP_SECONDS / cycles;
                int life = DustGlitterField.lifeNumber(high, low, cycles, phase);
                helper.assertTrue(life == lifeReference, "Point " + index + " at the clock " + clockSeconds
                        + " seconds is in life " + life + " instead of in life " + lifeReference);
                float age = DustGlitterField.lifeAge(high, low, cycles, phase);
                helper.assertTrue(Math.abs(age - ageReference) <= CLOCK_AGE_TOLERANCE, "Point " + index + " at the "
                        + "clock " + clockSeconds + " seconds is " + age + " seconds into its life instead of "
                        + ageReference + ", off by " + Math.abs(age - ageReference));
            }
        }
        helper.assertTrue(skips <= CLOCK_SKIP_SHARE * comparisons, "The clock test skipped " + skips + " of the "
                + comparisons + " comparisons, more than the " + CLOCK_SKIP_SHARE + " that may stand too near a "
                + "whole life");

        double beforeShare = DustGlitterField.clockShare(DustGlitterField.CLOCK_WRAP_SECONDS - WRAP_BEFORE_SECONDS);
        float beforeHigh = DustGlitterField.clockHigh(beforeShare);
        float beforeLow = DustGlitterField.clockLow(beforeShare);
        double afterShare = DustGlitterField.clockShare(0.0);
        float afterHigh = DustGlitterField.clockHigh(afterShare);
        float afterLow = DustGlitterField.clockLow(afterShare);
        for (float lifeRate : WRAP_LIFE_RATES) {
            int wrapComparisons = 0;
            int wrapSkips = 0;
            for (int index = 0; index < SHARE_POINT_COUNT; index++) {
                int cycles = DustGlitterField.lifeCycles(index, lifeRate);
                float phase = DustGlitterField.lifePhase(index);
                float ageAfter = DustGlitterField.lifeAge(afterHigh, afterLow, cycles, phase);
                wrapComparisons++;
                if (ageAfter < WRAP_SKIP_AGE_SECONDS) {
                    wrapSkips++;
                    continue;
                }
                int lifeBefore = DustGlitterField.lifeNumber(beforeHigh, beforeLow, cycles, phase);
                int lifeAfter = DustGlitterField.lifeNumber(afterHigh, afterLow, cycles, phase);
                helper.assertTrue(lifeBefore == lifeAfter, "Point " + index + " with " + cycles + " lives per wrap at "
                        + "the life rate " + lifeRate + " is in life " + lifeBefore + " just before the clock's wrap "
                        + "and in life " + lifeAfter + " just after it");
                float ageBefore = DustGlitterField.lifeAge(beforeHigh, beforeLow, cycles, phase);
                helper.assertTrue(Math.abs(ageBefore + (float) WRAP_BEFORE_SECONDS - ageAfter) <= WRAP_AGE_TOLERANCE,
                        "Point " + index + " with " + cycles + " lives per wrap at the life rate " + lifeRate + " is "
                                + ageBefore + " seconds into its life just before the clock's wrap and " + ageAfter
                                + " just after it, which is off by "
                                + Math.abs(ageBefore + (float) WRAP_BEFORE_SECONDS - ageAfter) + " from the "
                                + WRAP_BEFORE_SECONDS + " seconds that passed");
            }
            helper.assertTrue(wrapSkips <= CLOCK_SKIP_SHARE * wrapComparisons, "The wrap test at the life rate "
                    + lifeRate + " skipped " + wrapSkips + " of the " + wrapComparisons + " points, more than the "
                    + CLOCK_SKIP_SHARE + " that may start their life at the wrap");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterLitWindowFitsTheReferenceLitTimes(GameTestHelper helper) {
        float shortestLit = DustGlitterField.LIT_TIME_BASE;
        float longestLit = DustGlitterField.LIT_TIME_BASE + DustGlitterField.LIT_TIME_SLOPE
                + DustGlitterField.LIT_TIME_TAIL;
        helper.assertTrue(Math.abs(DustGlitterField.litSeconds(0.0F, REFERENCE_TIME_SCALE) - shortestLit)
                        <= LIFE_SECONDS_TOLERANCE, "The lit time at the share 0 is "
                + DustGlitterField.litSeconds(0.0F, REFERENCE_TIME_SCALE) + " seconds instead of " + shortestLit);
        helper.assertTrue(Math.abs(DustGlitterField.litSeconds(1.0F, REFERENCE_TIME_SCALE) - longestLit)
                        <= LIFE_SECONDS_TOLERANCE, "The lit time at the share 1 is "
                + DustGlitterField.litSeconds(1.0F, REFERENCE_TIME_SCALE) + " seconds instead of " + longestLit);

        float previousVisible = DustGlitterField.litSeconds(0.0F, REFERENCE_TIME_SCALE)
                - DustGlitterField.LIT_RAMP_SECONDS;
        for (int step = 1; step <= (int) Math.round(1.0 / LIT_VISIBLE_STEP); step++) {
            float visible = DustGlitterField.litSeconds((float) (step * LIT_VISIBLE_STEP), REFERENCE_TIME_SCALE)
                    - DustGlitterField.LIT_RAMP_SECONDS;
            helper.assertTrue(visible >= previousVisible, "The visible lit time falls from " + previousVisible
                    + " to " + visible + " at the share " + (step * LIT_VISIBLE_STEP));
            previousVisible = visible;
        }

        double[] visibleTimes = new double[SHARE_POINT_COUNT];
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            visibleTimes[index] = DustGlitterField.litSeconds(
                    DustGlitterField.lifeShare(index, 0, DustGlitterField.LIT_TIME_HASH), REFERENCE_TIME_SCALE)
                    - DustGlitterField.LIT_RAMP_SECONDS;
        }
        Arrays.sort(visibleTimes);
        for (int quartile = 0; quartile < LIT_QUARTILE_SHARES.length; quartile++) {
            int position = (int) Math.round(LIT_QUARTILE_SHARES[quartile] * (SHARE_POINT_COUNT - 1));
            helper.assertTrue(Math.abs(visibleTimes[position] - LIT_QUARTILE_SECONDS[quartile])
                            <= LIT_QUARTILE_TOLERANCE, "The " + LIT_QUARTILE_SHARES[quartile]
                    + " quartile of the visible lit times over the " + SHARE_POINT_COUNT + " points is "
                    + visibleTimes[position] + " seconds instead of " + LIT_QUARTILE_SECONDS[quartile]);
        }
        helper.assertTrue(visibleTimes[0] >= LIT_SHORTEST_SECONDS - LIFE_SECONDS_TOLERANCE, "The shortest visible "
                + "lit time over the " + SHARE_POINT_COUNT + " points is " + visibleTimes[0] + " seconds, below the "
                + LIT_SHORTEST_SECONDS + " the reference shows");
        helper.assertTrue(visibleTimes[SHARE_POINT_COUNT - 1] <= LIT_LONGEST_SECONDS + LIFE_SECONDS_TOLERANCE,
                "The longest visible lit time over the " + SHARE_POINT_COUNT + " points is "
                        + visibleTimes[SHARE_POINT_COUNT - 1] + " seconds, above the " + LIT_LONGEST_SECONDS
                        + " the reference shows");

        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            float lifeSeconds = DustGlitterField.lifeSeconds(
                    DustGlitterField.lifeCycles(index, REFERENCE_LIFE_RATE));
            for (int life = 0; life < LIT_LIFE_COUNT; life++) {
                float litSeconds = DustGlitterField.litSeconds(
                        DustGlitterField.lifeShare(index, life, DustGlitterField.LIT_TIME_HASH), REFERENCE_TIME_SCALE);
                float litStart = DustGlitterField.litStart(
                        DustGlitterField.lifeShare(index, life, DustGlitterField.LIT_START_HASH), lifeSeconds,
                        litSeconds);
                helper.assertTrue(litStart >= 0.0F, "Point " + index + " in life " + life + " lights up at "
                        + litStart + " seconds, before its life begins");
                helper.assertTrue(litStart + litSeconds <= lifeSeconds + (float) LIFE_SECONDS_TOLERANCE, "Point "
                        + index + " in life " + life + " lights up at " + litStart + " for " + litSeconds
                        + " seconds, past the " + lifeSeconds + " seconds of its life");
            }
        }

        float litStart = 0.3F;
        float litLength = 0.4F;
        helper.assertTrue(DustGlitterField.envelope(0.2F, litStart, litLength, REFERENCE_TIME_SCALE) == 0.0F,
                "The envelope at the age 0.2 before the lit start " + litStart + " is "
                        + DustGlitterField.envelope(0.2F, litStart, litLength, REFERENCE_TIME_SCALE)
                        + " instead of 0");
        helper.assertTrue(DustGlitterField.envelope(0.8F, litStart, litLength, REFERENCE_TIME_SCALE) == 0.0F,
                "The envelope at the age 0.8 after the lit end " + (litStart + litLength) + " is "
                        + DustGlitterField.envelope(0.8F, litStart, litLength, REFERENCE_TIME_SCALE)
                        + " instead of 0");
        helper.assertTrue(DustGlitterField.envelope(0.3F, litStart, litLength, REFERENCE_TIME_SCALE)
                        <= ENVELOPE_TOLERANCE, "The envelope at the lit start " + litStart + " is "
                + DustGlitterField.envelope(0.3F, litStart, litLength, REFERENCE_TIME_SCALE) + " instead of 0 within "
                + ENVELOPE_TOLERANCE);
        helper.assertTrue(DustGlitterField.envelope(0.7F, litStart, litLength, REFERENCE_TIME_SCALE)
                        <= ENVELOPE_TOLERANCE, "The envelope at the lit end 0.7 is "
                + DustGlitterField.envelope(0.7F, litStart, litLength, REFERENCE_TIME_SCALE) + " instead of 0 within "
                + ENVELOPE_TOLERANCE);
        float halfRamp = DustGlitterField.envelope(0.325F, litStart, litLength, REFERENCE_TIME_SCALE);
        helper.assertTrue(Math.abs(halfRamp - 0.5F) <= ENVELOPE_HALF_TOLERANCE, "The envelope half a ramp of "
                + DustGlitterField.LIT_RAMP_SECONDS + " seconds into the lit window is " + halfRamp
                + " instead of 0.5");
        float halfRampDown = DustGlitterField.envelope(0.675F, litStart, litLength, REFERENCE_TIME_SCALE);
        helper.assertTrue(Math.abs(halfRampDown - 0.5F) <= ENVELOPE_HALF_TOLERANCE, "The envelope half a ramp of "
                + DustGlitterField.LIT_RAMP_SECONDS + " seconds before the lit window's end is " + halfRampDown
                + " instead of 0.5");
        float quarterRamp = DustGlitterField.envelope(0.3125F, litStart, litLength, REFERENCE_TIME_SCALE);
        helper.assertTrue(Math.abs(quarterRamp - 0.15625F) <= ENVELOPE_HALF_TOLERANCE, "The envelope a quarter of a "
                + "ramp into the lit window is " + quarterRamp + " instead of 0.15625");
        int flatSteps = (int) Math.round((ENVELOPE_FLAT_HIGHEST_AGE - ENVELOPE_FLAT_LOWEST_AGE) / ENVELOPE_FLAT_STEP);
        for (int step = 0; step <= flatSteps; step++) {
            float age = (float) (ENVELOPE_FLAT_LOWEST_AGE + ENVELOPE_FLAT_STEP * step);
            helper.assertTrue(DustGlitterField.envelope(age, litStart, litLength, REFERENCE_TIME_SCALE) == 1.0F,
                    "The envelope at the age " + age + " on the flat top of the lit window is "
                            + DustGlitterField.envelope(age, litStart, litLength, REFERENCE_TIME_SCALE)
                            + " instead of 1");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterLitTimeStretchesWithItsSetting(GameTestHelper helper) {
        helper.assertTrue(DustGlitterField.timeScale(DustGlitterField.REFERENCE_LIT_SECONDS) == 1.0F, "The time "
                + "scale of the reference " + DustGlitterField.REFERENCE_LIT_SECONDS + " seconds is "
                + DustGlitterField.timeScale(DustGlitterField.REFERENCE_LIT_SECONDS) + " instead of 1");
        helper.assertTrue(DustGlitterField.timeScale(0.1) == 1.0F, "The time scale of the 0.1 seconds below the "
                + "reference is " + DustGlitterField.timeScale(0.1) + " instead of 1");
        float halfSecondScale = DustGlitterField.timeScale(0.5);
        helper.assertTrue(Math.abs(halfSecondScale - 2.5F) <= LIT_SCALE_TOLERANCE, "The time scale of 0.5 seconds "
                + "is " + halfSecondScale + " instead of 2.5");
        float fourSecondScale = DustGlitterField.timeScale(4.0);
        helper.assertTrue(Math.abs(fourSecondScale - 20.0F) <= LIT_SCALE_WIDE_TOLERANCE, "The time scale of 4 "
                + "seconds is " + fourSecondScale + " instead of 20");

        helper.assertTrue(DustGlitterField.lifeRate(DustGlitterField.REFERENCE_LIT_SECONDS) == 1.0F, "The life rate "
                + "of the reference " + DustGlitterField.REFERENCE_LIT_SECONDS + " seconds is "
                + DustGlitterField.lifeRate(DustGlitterField.REFERENCE_LIT_SECONDS) + " instead of 1");
        helper.assertTrue(DustGlitterField.lifeRate(0.1) == 1.0F, "The life rate of the 0.1 seconds below the "
                + "reference is " + DustGlitterField.lifeRate(0.1) + " instead of 1");
        float halfSecondRate = DustGlitterField.lifeRate(0.5);
        helper.assertTrue(Math.abs(halfSecondRate - 0.4F) <= LIT_RATE_TOLERANCE, "The life rate of 0.5 seconds is "
                + halfSecondRate + " instead of 0.4");
        float fourSecondRate = DustGlitterField.lifeRate(4.0);
        helper.assertTrue(Math.abs(fourSecondRate - 0.05F) <= LIT_RATE_TOLERANCE, "The life rate of 4 seconds is "
                + fourSecondRate + " instead of 0.05");

        for (double litSettingSeconds : LIT_SETTINGS_SECONDS) {
            float timeScale = DustGlitterField.timeScale(litSettingSeconds);
            float lifeRate = DustGlitterField.lifeRate(litSettingSeconds);
            double[] visibleTimes = new double[SHARE_POINT_COUNT];
            for (int index = 0; index < SHARE_POINT_COUNT; index++) {
                int cycles = DustGlitterField.lifeCycles(index, lifeRate);
                helper.assertTrue(cycles >= FEWEST_LIFE_CYCLES && cycles <= MOST_LIFE_CYCLES, "Point " + index
                        + " at the lit time " + litSettingSeconds + " lives " + cycles + " lives per wrap, outside "
                        + FEWEST_LIFE_CYCLES + " to " + MOST_LIFE_CYCLES);
                float lifeSeconds = DustGlitterField.lifeSeconds(cycles);
                double shortestLife = timeScale * (1.0 - LIT_LIFE_FLOOR_SHARE);
                double longestLife = LIT_LIFE_CEILING_FACTOR * timeScale;
                helper.assertTrue(lifeSeconds >= shortestLife && lifeSeconds <= longestLife, "Point " + index
                        + " at the lit time " + litSettingSeconds + " with " + cycles + " lives per wrap lives "
                        + lifeSeconds + " seconds, outside " + shortestLife + " to " + longestLife);
                for (int life = 0; life < LIT_LIFE_COUNT; life++) {
                    float litSeconds = DustGlitterField.litSeconds(
                            DustGlitterField.lifeShare(index, life, DustGlitterField.LIT_TIME_HASH), timeScale);
                    float litStart = DustGlitterField.litStart(
                            DustGlitterField.lifeShare(index, life, DustGlitterField.LIT_START_HASH), lifeSeconds,
                            litSeconds);
                    helper.assertTrue(litStart >= 0.0F, "Point " + index + " in life " + life + " at the lit time "
                            + litSettingSeconds + " lights up at " + litStart + " seconds, before its life begins");
                    helper.assertTrue(litStart + litSeconds <= lifeSeconds + LIT_START_TOLERANCE_SHARE * timeScale,
                            "Point " + index + " in life " + life + " at the lit time " + litSettingSeconds
                                    + " lights up at " + litStart + " for " + litSeconds + " seconds, past the "
                                    + lifeSeconds + " seconds of its life by "
                                    + (litStart + litSeconds - lifeSeconds));
                }
                visibleTimes[index] = DustGlitterField.litSeconds(
                        DustGlitterField.lifeShare(index, 0, DustGlitterField.LIT_TIME_HASH), timeScale)
                        - DustGlitterField.LIT_RAMP_SECONDS * timeScale;
            }
            Arrays.sort(visibleTimes);
            for (int quartile = 0; quartile < LIT_QUARTILE_SHARES.length; quartile++) {
                int position = (int) Math.round(LIT_QUARTILE_SHARES[quartile] * (SHARE_POINT_COUNT - 1));
                double expected = LIT_QUARTILE_SECONDS[quartile] * timeScale;
                helper.assertTrue(Math.abs(visibleTimes[position] - expected)
                                <= LIT_QUARTILE_TOLERANCE * timeScale, "The " + LIT_QUARTILE_SHARES[quartile]
                        + " quartile of the visible lit times over the " + SHARE_POINT_COUNT + " points at the lit "
                        + "time " + litSettingSeconds + " is " + visibleTimes[position] + " seconds instead of "
                        + expected);
            }
        }

        for (float age : ENVELOPE_STRETCHED_ZERO_AGES) {
            float envelope = DustGlitterField.envelope(age, ENVELOPE_STRETCHED_START, ENVELOPE_STRETCHED_LENGTH,
                    ENVELOPE_STRETCHED_SCALE);
            helper.assertTrue(envelope == 0.0F, "The envelope at the age " + age + " outside the stretched lit "
                    + "window from " + ENVELOPE_STRETCHED_START + " for " + ENVELOPE_STRETCHED_LENGTH + " seconds at "
                    + "the time scale " + ENVELOPE_STRETCHED_SCALE + " is " + envelope + " instead of 0");
        }
        for (float age : ENVELOPE_STRETCHED_HALF_AGES) {
            float envelope = DustGlitterField.envelope(age, ENVELOPE_STRETCHED_START, ENVELOPE_STRETCHED_LENGTH,
                    ENVELOPE_STRETCHED_SCALE);
            helper.assertTrue(Math.abs(envelope - 0.5F) <= ENVELOPE_HALF_TOLERANCE, "The envelope at the age " + age
                    + " half a stretched ramp of " + DustGlitterField.LIT_RAMP_SECONDS * ENVELOPE_STRETCHED_SCALE
                    + " seconds from an end of the stretched lit window is " + envelope + " instead of 0.5");
        }
        int stretchedFlatSteps = (int) Math.round((ENVELOPE_STRETCHED_FLAT_HIGHEST_AGE
                - ENVELOPE_STRETCHED_FLAT_LOWEST_AGE) / ENVELOPE_FLAT_STEP);
        for (int step = 0; step <= stretchedFlatSteps; step++) {
            float age = (float) (ENVELOPE_STRETCHED_FLAT_LOWEST_AGE + ENVELOPE_FLAT_STEP * step);
            float envelope = DustGlitterField.envelope(age, ENVELOPE_STRETCHED_START, ENVELOPE_STRETCHED_LENGTH,
                    ENVELOPE_STRETCHED_SCALE);
            helper.assertTrue(envelope == 1.0F, "The envelope at the age " + age + " on the flat top of the "
                    + "stretched lit window at the time scale " + ENVELOPE_STRETCHED_SCALE + " is " + envelope
                    + " instead of 1");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterFallsDownwardWithinItsFan(GameTestHelper helper) {
        float slowestSpeed = FALL_SPEED * DustGlitterField.SLOWEST_FALL_SHARE;
        float fastestSpeed = FALL_SPEED * (DustGlitterField.SLOWEST_FALL_SHARE + DustGlitterField.FALL_SHARE_RANGE);
        double widestTiltDegrees = Math.toDegrees(DustGlitterField.WIDEST_TILT_RADIANS);
        float[] velocity = new float[AXIS_COUNT];
        double largestTiltDegrees = 0.0;
        float smallestSpeed = Float.MAX_VALUE;
        float largestSpeed = 0.0F;
        int withinHalfTilt = 0;
        int tilted = 0;
        int[] quadrantCounts = new int[4];
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            DustGlitterField.velocity(index, 0, FALL_SPEED, velocity);
            double speed = Math.sqrt((double) velocity[0] * velocity[0] + (double) velocity[1] * velocity[1]
                    + (double) velocity[2] * velocity[2]);
            helper.assertTrue(speed >= slowestSpeed - SPEED_TOLERANCE && speed <= fastestSpeed + SPEED_TOLERANCE,
                    "Point " + index + " falls at " + speed + " blocks per second instead of " + slowestSpeed
                            + " to " + fastestSpeed);
            helper.assertTrue(velocity[1] < 0.0F, "Point " + index + " falls at " + velocity[1]
                    + " blocks per second upward instead of downward");
            double tiltDegrees = Math.toDegrees(Math.acos(-velocity[1] / speed));
            helper.assertTrue(tiltDegrees <= widestTiltDegrees + TILT_TOLERANCE_DEGREES, "Point " + index
                    + " falls tilted " + tiltDegrees + " degrees from straight down, more than the "
                    + widestTiltDegrees + " the fan allows");
            largestTiltDegrees = Math.max(largestTiltDegrees, tiltDegrees);
            smallestSpeed = Math.min(smallestSpeed, (float) speed);
            largestSpeed = Math.max(largestSpeed, (float) speed);
            if (tiltDegrees <= HALF_TILT_DEGREES) {
                withinHalfTilt++;
            }
            if (tiltDegrees > QUADRANT_TILT_DEGREES) {
                tilted++;
                int quadrant = (int) Math.floor((Math.atan2(velocity[2], velocity[0]) + Math.PI) / (0.5 * Math.PI));
                quadrantCounts[Math.min(3, quadrant)]++;
            }
        }
        helper.assertTrue(largestTiltDegrees > WIDEST_TILT_SEEN_DEGREES, "The widest tilt over the "
                + SHARE_POINT_COUNT + " points is " + largestTiltDegrees + " degrees, not past the "
                + WIDEST_TILT_SEEN_DEGREES + " the test needs");
        double halfTiltShare = (double) withinHalfTilt / SHARE_POINT_COUNT;
        helper.assertTrue(Math.abs(halfTiltShare - 0.5) <= HALF_TILT_SHARE_TOLERANCE, "The share of tilts up to "
                + HALF_TILT_DEGREES + " degrees is " + halfTiltShare + " instead of 0.5 within "
                + HALF_TILT_SHARE_TOLERANCE);
        helper.assertTrue(smallestSpeed < SMALLEST_SPEED_LIMIT, "The slowest fall over the " + SHARE_POINT_COUNT
                + " points is " + smallestSpeed + " blocks per second, not below " + SMALLEST_SPEED_LIMIT);
        helper.assertTrue(largestSpeed > LARGEST_SPEED_LIMIT, "The fastest fall over the " + SHARE_POINT_COUNT
                + " points is " + largestSpeed + " blocks per second, not above " + LARGEST_SPEED_LIMIT);
        for (int quadrant = 0; quadrant < quadrantCounts.length; quadrant++) {
            double quadrantShare = (double) quadrantCounts[quadrant] / tilted;
            helper.assertTrue(quadrantShare >= QUADRANT_LEAST_SHARE, "Quadrant " + quadrant + " holds "
                    + quadrantCounts[quadrant] + " of the " + tilted + " tilts above " + QUADRANT_TILT_DEGREES
                    + " degrees, the share " + quadrantShare + ", below the " + QUADRANT_LEAST_SHARE + " the test "
                    + "needs");
        }

        for (int index = 0; index < 100; index++) {
            DustGlitterField.velocity(index, 0, 0.0F, velocity);
            helper.assertTrue(velocity[0] == 0.0F && velocity[1] == 0.0F && velocity[2] == 0.0F, "Point " + index
                    + " falls at " + velocity[0] + ", " + velocity[1] + " and " + velocity[2]
                    + " blocks per second at a fall speed of 0 instead of at 0 on all three axes");
        }

        float[] offsets = {3.7F, -2.25F, 0.0F, 1.9F};
        float[] speeds = {-0.42F, 0.61F, -0.05F, 0.33F};
        float[] ages = {0.31F, 1.75F, 0.0F, 0.9F};
        float[] starts = {0.2F, 0.0F, 1.4F, 0.55F};
        float[] lengths = {0.45F, 0.75F, 0.17F, 0.3F};
        for (int step = 0; step < offsets.length; step++) {
            float middle = DustGlitterField.middleOffset(offsets[step], speeds[step], ages[step], starts[step],
                    lengths[step]);
            double expected = offsets[step] + (double) speeds[step]
                    * ((double) starts[step] + 0.5 * lengths[step] - ages[step]);
            helper.assertTrue(Math.abs(middle - expected) <= MIDDLE_OFFSET_TOLERANCE, "The middle offset of the "
                    + "offset " + offsets[step] + ", the velocity " + speeds[step] + ", the age " + ages[step]
                    + ", the lit start " + starts[step] + " and the lit time " + lengths[step] + " is " + middle
                    + " instead of " + expected);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterDecidesAtTheMiddleOfItsLitWindow(GameTestHelper helper) {
        float[] velocity = new float[AXIS_COUNT];
        float[][] middles = new float[MIDDLE_AGE_STEPS + 1][AXIS_COUNT];
        int compared = 0;
        for (int index = 0; index < BOX_POINT_COUNT; index++) {
            float lifeSeconds = DustGlitterField.lifeSeconds(
                    DustGlitterField.lifeCycles(index, REFERENCE_LIFE_RATE));
            float litSeconds = DustGlitterField.litSeconds(
                    DustGlitterField.lifeShare(index, 0, DustGlitterField.LIT_TIME_HASH), REFERENCE_TIME_SCALE);
            float litStart = DustGlitterField.litStart(
                    DustGlitterField.lifeShare(index, 0, DustGlitterField.LIT_START_HASH), lifeSeconds, litSeconds);
            DustGlitterField.velocity(index, 0, FALL_SPEED, velocity);
            boolean inside = true;
            for (int step = 0; step <= MIDDLE_AGE_STEPS; step++) {
                float age = litStart + litSeconds * step / MIDDLE_AGE_STEPS;
                for (int axis = 0; axis < AXIS_COUNT; axis++) {
                    float position = DustGlitterField.lifeStart(index, 0, axis) + velocity[axis] * age;
                    float offset = DustGlitterField.nearestOffset(position, (float) MIDDLE_EYE_OFFSET[axis]);
                    middles[step][axis] = DustGlitterField.middleOffset(offset, velocity[axis], age, litStart,
                            litSeconds);
                    if (Math.abs(middles[step][axis]) > DustGlitterField.CELL_SIZE / 2.0F - 1.0F) {
                        inside = false;
                    }
                }
            }
            if (!inside) {
                continue;
            }
            compared++;
            float worstDeviation = 0.0F;
            int worstAxis = 0;
            int worstStep = 0;
            for (int axis = 0; axis < AXIS_COUNT; axis++) {
                for (int step = 1; step <= MIDDLE_AGE_STEPS; step++) {
                    float deviation = Math.abs(middles[step][axis] - middles[0][axis]);
                    if (deviation > worstDeviation) {
                        worstDeviation = deviation;
                        worstAxis = axis;
                        worstStep = step;
                    }
                }
            }
            helper.assertTrue(worstDeviation <= MIDDLE_TOLERANCE, "Point " + index + " moves its middle offset on "
                    + "axis " + worstAxis + " by " + worstDeviation + " from " + middles[0][worstAxis] + " to "
                    + middles[worstStep][worstAxis] + " at the age step " + worstStep + " of " + MIDDLE_AGE_STEPS
                    + " instead of holding it within " + MIDDLE_TOLERANCE);
        }
        helper.assertTrue(compared >= MIDDLE_LEAST_COMPARED, "Only " + compared + " points of the " + BOX_POINT_COUNT
                + " keep their middle offset inside the box, not the " + MIDDLE_LEAST_COMPARED + " the test needs");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterShowsMoreLivesOnTheHalos(GameTestHelper helper) {
        float halfShare = DustGlitterField.shownWeight(0.5F, 0.5F);
        helper.assertTrue(Math.abs(halfShare - 0.5F) <= 1.0e-6, "The shown weight at the share 0.5 and the hash 0.5 "
                + "is " + halfShare + " instead of 0.5");
        helper.assertTrue(DustGlitterField.shownWeight(0.5F, 0.3F) == 1.0F, "The shown weight at the share 0.5 and "
                + "the hash 0.3 is " + DustGlitterField.shownWeight(0.5F, 0.3F) + " instead of 1");
        helper.assertTrue(DustGlitterField.shownWeight(0.5F, 0.7F) == 0.0F, "The shown weight at the share 0.5 and "
                + "the hash 0.7 is " + DustGlitterField.shownWeight(0.5F, 0.7F) + " instead of 0");
        float insideMargin = DustGlitterField.shownWeight(0.5F, 0.55F);
        double expectedInsideMargin = 0.5 - 0.05 / DustGlitterField.SHOWN_MARGIN;
        helper.assertTrue(Math.abs(insideMargin - expectedInsideMargin) <= 1.0e-5, "The shown weight at the share "
                + "0.5 and the hash 0.55 is " + insideMargin + " instead of " + expectedInsideMargin);
        for (float plainShare : PLAIN_SHARES) {
            double sum = 0.0;
            for (int sample = 0; sample < SHOWN_SAMPLE_COUNT; sample++) {
                sum += DustGlitterField.shownWeight(plainShare, (sample + 0.5F) / SHOWN_SAMPLE_COUNT);
            }
            double mean = sum / SHOWN_SAMPLE_COUNT;
            helper.assertTrue(Math.abs(mean - plainShare) <= SHOWN_MEAN_TOLERANCE, "The mean shown weight at the "
                    + "share " + plainShare + " over the " + SHOWN_SAMPLE_COUNT + " hashes is " + mean
                    + " instead of " + plainShare);
        }
        float allShownShare = 1.0F + 0.5F * DustGlitterField.SHOWN_MARGIN;
        double allSum = 0.0;
        for (int sample = 0; sample < SHOWN_SAMPLE_COUNT; sample++) {
            allSum += DustGlitterField.shownWeight(allShownShare, (sample + 0.5F) / SHOWN_SAMPLE_COUNT);
        }
        double allMean = allSum / SHOWN_SAMPLE_COUNT;
        helper.assertTrue(Math.abs(allMean - 1.0) <= 1.0e-6, "The mean shown weight at the share " + allShownShare
                + " over the " + SHOWN_SAMPLE_COUNT + " hashes is " + allMean + " instead of 1");

        helper.assertTrue(DustGlitterField.ringShare(0.0F, 1.25F) == 0.0F, "The ring share of the phase 0 over the "
                + "ring phase 1.25 is " + DustGlitterField.ringShare(0.0F, 1.25F) + " instead of 0");
        helper.assertTrue(DustGlitterField.ringShare(1.25F, 1.25F) == 1.0F, "The ring share of the phase 1.25 over "
                + "the ring phase 1.25 is " + DustGlitterField.ringShare(1.25F, 1.25F) + " instead of 1");
        helper.assertTrue(DustGlitterField.ringShare(2.5F, 1.25F) == 1.0F, "The ring share of the phase 2.5 over "
                + "the ring phase 1.25 is " + DustGlitterField.ringShare(2.5F, 1.25F) + " instead of 1");
        float halfRing = DustGlitterField.ringShare(0.625F, 1.25F);
        helper.assertTrue(Math.abs(halfRing - 0.5F) <= 1.0e-6, "The ring share of the phase 0.625 over the ring "
                + "phase 1.25 is " + halfRing + " instead of 0.5");
        helper.assertTrue(DustGlitterField.ringShare(-1.0F, 1.25F) == 0.0F, "The ring share of the phase -1 over "
                + "the ring phase 1.25 is " + DustGlitterField.ringShare(-1.0F, 1.25F) + " instead of 0");
        helper.assertTrue(DustGlitterField.ringShare(0.3F, 0.0F) == 1.0F, "The ring share of the phase 0.3 over the "
                + "ring phase 0 is " + DustGlitterField.ringShare(0.3F, 0.0F) + " instead of 1");

        helper.assertTrue(DustGlitterField.shownShare(0.5F, 0.0F) == 0.5F, "The shown share of the plain share 0.5 "
                + "away from the halos is " + DustGlitterField.shownShare(0.5F, 0.0F) + " instead of 0.5");
        float onTheRing = DustGlitterField.shownShare(0.5F, 1.0F);
        helper.assertTrue(Math.abs(onTheRing - allShownShare) <= 1.0e-6, "The shown share of the plain share 0.5 on "
                + "the ring is " + onTheRing + " instead of " + allShownShare);
        float halfwayToTheRing = DustGlitterField.shownShare(0.4F, 0.5F);
        double expectedHalfway = 0.4 + 0.5 * (allShownShare - 0.4);
        helper.assertTrue(Math.abs(halfwayToTheRing - expectedHalfway) <= 1.0e-6, "The shown share of the plain share "
                + "0.4 half way to the ring is " + halfwayToTheRing + " instead of " + expectedHalfway);

        for (float plainShare : HALO_PLAIN_SHARES) {
            float shownShare = DustGlitterField.shownShare(plainShare, 1.0F);
            for (int step = 0; step < HALO_HASH_COUNT; step++) {
                float hashShare = (step + 0.5F) / HALO_HASH_COUNT;
                helper.assertTrue(DustGlitterField.shownWeight(shownShare, hashShare) == 1.0F, "The shown weight at "
                        + "the share " + shownShare + " of the plain share " + plainShare + " on the ring and the "
                        + "hash " + hashShare + " is " + DustGlitterField.shownWeight(shownShare, hashShare)
                        + " instead of 1");
            }
        }

        helper.assertTrue(DustGlitterField.tint(0.3F, 0.0F, 1.0F) == 1.0F, "The tint of a plain direction is "
                + DustGlitterField.tint(0.3F, 0.0F, 1.0F) + " instead of 1");
        helper.assertTrue(DustGlitterField.tint(0.5F, 1.0F, 0.0F) == 1.0F, "The tint away from the halos is "
                + DustGlitterField.tint(0.5F, 1.0F, 0.0F) + " instead of 1");
        float halfTint = DustGlitterField.tint(0.5F, 1.0F, 1.0F);
        double expectedHalfTint = 1.0 - 0.5 * DustGlitterField.TINT_SHARE;
        helper.assertTrue(Math.abs(halfTint - expectedHalfTint) <= 1.0e-6, "The tint of the half channel on the "
                + "ring is " + halfTint + " instead of " + expectedHalfTint);
        helper.assertTrue(DustGlitterField.tint(1.0F, 1.0F, 1.0F) == 1.0F, "The tint of the largest channel on the "
                + "ring is " + DustGlitterField.tint(1.0F, 1.0F, 1.0F) + " instead of 1");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void nearGlintsAreRareFaintAndNeverAtTheEye(GameTestHelper helper) {
        for (float distance : NEAREST_DISTANCES) {
            helper.assertTrue(DustGlitterField.lightFactor(distance) == 0.0F, "The light factor at " + distance
                    + " blocks is " + DustGlitterField.lightFactor(distance) + " instead of 0");
        }
        helper.assertTrue(DustGlitterField.nearBlur(DustGlitterField.NEAR_DISTANCE) == 1.0F, "The near blur at "
                + DustGlitterField.NEAR_DISTANCE + " blocks is "
                + DustGlitterField.nearBlur(DustGlitterField.NEAR_DISTANCE) + " instead of 1");
        helper.assertTrue(DustGlitterField.nearBlur(8.0F) == 1.0F, "The near blur at 8 blocks is "
                + DustGlitterField.nearBlur(8.0F) + " instead of 1");
        float halfBlur = DustGlitterField.nearBlur(2.0F);
        helper.assertTrue(Math.abs(halfBlur - 2.0F) <= NEAR_BLUR_TOLERANCE, "The near blur at 2 blocks is " + halfBlur
                + " instead of 2");
        helper.assertTrue(DustGlitterField.nearBlur(1.0F) == DustGlitterField.WIDEST_NEAR, "The near blur at 1 "
                + "block is " + DustGlitterField.nearBlur(1.0F) + " instead of " + DustGlitterField.WIDEST_NEAR);
        float widestBlur = DustGlitterField.nearBlur(4.0F / 3.0F);
        helper.assertTrue(Math.abs(widestBlur - DustGlitterField.WIDEST_NEAR) <= NEAR_WIDEST_BLUR_TOLERANCE, "The "
                + "near blur at four thirds of a block is " + widestBlur + " instead of "
                + DustGlitterField.WIDEST_NEAR);

        for (float distance : NEAR_FALLOFF_DISTANCES) {
            float factor = DustGlitterField.lightFactor(distance);
            float falloff = DustGlitterField.falloff(distance);
            helper.assertTrue(Math.abs(factor - falloff) <= NEAR_RELATIVE_TOLERANCE * falloff, "The light factor at "
                    + distance + " blocks is " + factor + " instead of the falloff " + falloff);
        }
        float reference = DustGlitterField.lightFactor(NEAR_REFERENCE_DISTANCE);
        for (int step = 0; step < NEAR_RATIO_DISTANCES.length; step++) {
            double ratio = DustGlitterField.lightFactor(NEAR_RATIO_DISTANCES[step]) / reference;
            helper.assertTrue(Math.abs(ratio - NEAR_RATIO_EXPECTED[step]) <= NEAR_RATIO_TOLERANCES[step], "The "
                    + "light factor at " + NEAR_RATIO_DISTANCES[step] + " blocks over the one at "
                    + NEAR_REFERENCE_DISTANCE + " blocks is " + ratio + " instead of " + NEAR_RATIO_EXPECTED[step]);
        }

        for (float distance : NEAR_SHARE_DISTANCES) {
            float nearShare = DustGlitterField.nearShare(distance);
            helper.assertTrue(Math.abs(nearShare - DustGlitterField.NEAR_SHARE) <= 1.0e-6, "The near share at "
                    + distance + " blocks is " + nearShare + " instead of " + DustGlitterField.NEAR_SHARE);
        }
        for (float distance : NEAR_ALL_SHOWN_DISTANCES) {
            float nearShare = DustGlitterField.nearShare(distance);
            double allShownShare = 1.0 + 0.5 * DustGlitterField.SHOWN_MARGIN;
            helper.assertTrue(Math.abs(nearShare - allShownShare) <= 1.0e-6, "The near share at " + distance
                    + " blocks is " + nearShare + " instead of " + allShownShare);
            for (int step = 0; step < HALO_HASH_COUNT; step++) {
                float hashShare = (step + 0.5F) / HALO_HASH_COUNT;
                helper.assertTrue(DustGlitterField.shownWeight(nearShare, hashShare) == 1.0F, "The shown weight at "
                        + "the near share " + nearShare + " of " + distance + " blocks and the hash " + hashShare
                        + " is " + DustGlitterField.shownWeight(nearShare, hashShare) + " instead of 1");
            }
        }

        int nearSteps = (int) Math.round((4.5 - 2.5) / NEAR_DISTANCE_STEP);
        for (int step = 0; step <= nearSteps; step++) {
            float distance = (float) (2.5 + NEAR_DISTANCE_STEP * step);
            float nearShare = DustGlitterField.nearShare(distance);
            float nextShare = DustGlitterField.nearShare(distance + (float) NEAR_DISTANCE_STEP);
            double worstChange = 0.0;
            float worstHash = 0.0F;
            for (int hashStep = 0; hashStep < HALO_HASH_COUNT; hashStep++) {
                float hashShare = (hashStep + 0.5F) / HALO_HASH_COUNT;
                double change = Math.abs(DustGlitterField.shownWeight(nextShare, hashShare)
                        - DustGlitterField.shownWeight(nearShare, hashShare));
                if (change > worstChange) {
                    worstChange = change;
                    worstHash = hashShare;
                }
            }
            helper.assertTrue(worstChange <= NEAR_SHOWN_STEP_LIMIT, "The shown weight at the hash " + worstHash
                    + " changes by " + worstChange + " from the near share " + nearShare + " at " + distance
                    + " blocks to the near share " + nextShare + " a step further, more than "
                    + NEAR_SHOWN_STEP_LIMIT);
        }
        int factorSteps = (int) Math.round((16.0 - 1.0) / NEAR_DISTANCE_STEP);
        for (int step = 0; step <= factorSteps; step++) {
            float distance = (float) (1.0 + NEAR_DISTANCE_STEP * step);
            double change = Math.abs(DustGlitterField.lightFactor(distance + (float) NEAR_DISTANCE_STEP)
                    - DustGlitterField.lightFactor(distance));
            helper.assertTrue(change <= NEAR_LIGHT_STEP_LIMIT, "The light factor changes by " + change + " from "
                    + distance + " blocks to " + (distance + (float) NEAR_DISTANCE_STEP) + " blocks, more than "
                    + NEAR_LIGHT_STEP_LIMIT);
        }
        double nearSum = 0.0;
        for (int step = 0; step < HALO_HASH_COUNT; step++) {
            nearSum += DustGlitterField.shownWeight(DustGlitterField.NEAR_SHARE, (step + 0.5F) / HALO_HASH_COUNT);
        }
        double nearMean = nearSum / HALO_HASH_COUNT;
        helper.assertTrue(Math.abs(nearMean - DustGlitterField.NEAR_SHARE) <= NEAR_MEAN_TOLERANCE, "The mean shown "
                + "weight at the near share " + DustGlitterField.NEAR_SHARE + " over the " + HALO_HASH_COUNT
                + " hashes is " + nearMean + " instead of " + DustGlitterField.NEAR_SHARE);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glitterSizeFollowsDistanceWithItsLightSpread(GameTestHelper helper) {
        helper.assertTrue(DustGlitterField.sizeFactor(DustGlitterField.SIZE_DISTANCE, SIZE_SPREAD_ONE) == 1.0F,
                "The size factor at " + DustGlitterField.SIZE_DISTANCE + " blocks is "
                        + DustGlitterField.sizeFactor(DustGlitterField.SIZE_DISTANCE, SIZE_SPREAD_ONE)
                        + " instead of 1");
        float twiceSize = DustGlitterField.sizeFactor(3.0F, SIZE_SPREAD_ONE);
        helper.assertTrue(Math.abs(twiceSize - 2.0F) <= SIZE_FACTOR_TOLERANCE, "The size factor at 3 blocks is "
                + twiceSize + " instead of 2");
        float halfSize = DustGlitterField.sizeFactor(12.0F, SIZE_SPREAD_ONE);
        helper.assertTrue(Math.abs(halfSize - 0.5F) <= SIZE_FACTOR_TOLERANCE, "The size factor at 12 blocks is "
                + halfSize + " instead of 0.5");
        helper.assertTrue(DustGlitterField.sizeFactor(1.0F, SIZE_SPREAD_ONE) == DustGlitterField.LARGEST_SIZE,
                "The size factor at 1 block is " + DustGlitterField.sizeFactor(1.0F, SIZE_SPREAD_ONE)
                        + " instead of the largest " + DustGlitterField.LARGEST_SIZE);
        helper.assertTrue(DustGlitterField.sizeFactor(30.0F, SIZE_SPREAD_ONE) == DustGlitterField.SMALLEST_SIZE,
                "The size factor at 30 blocks is " + DustGlitterField.sizeFactor(30.0F, SIZE_SPREAD_ONE)
                        + " instead of the smallest " + DustGlitterField.SMALLEST_SIZE);
        helper.assertTrue(DustGlitterField.sizeFactor(3.0F, SIZE_SPREAD_TWO) == DustGlitterField.LARGEST_SIZE,
                "The size factor at 3 blocks with the spread 2 is "
                        + DustGlitterField.sizeFactor(3.0F, SIZE_SPREAD_TWO) + " instead of the largest "
                        + DustGlitterField.LARGEST_SIZE);
        float quarterSize = DustGlitterField.sizeFactor(12.0F, SIZE_SPREAD_TWO);
        helper.assertTrue(Math.abs(quarterSize - 0.25F) <= SIZE_FACTOR_TOLERANCE, "The size factor at 12 blocks with "
                + "the spread 2 is " + quarterSize + " instead of 0.25");
        for (float distance : SIZE_FLAT_DISTANCES) {
            helper.assertTrue(DustGlitterField.sizeFactor(distance, SIZE_SPREAD_NONE) == 1.0F, "The size factor at "
                    + distance + " blocks with the spread 0 is "
                    + DustGlitterField.sizeFactor(distance, SIZE_SPREAD_NONE) + " instead of 1");
        }

        float previousSize = DustGlitterField.sizeFactor((float) SIZE_SCAN_LOWEST_DISTANCE, SIZE_SPREAD_ONE);
        int sizeSteps = (int) Math.round((SIZE_SCAN_HIGHEST_DISTANCE - SIZE_SCAN_LOWEST_DISTANCE)
                / SIZE_DISTANCE_STEP);
        for (int step = 1; step <= sizeSteps; step++) {
            float distance = (float) (SIZE_SCAN_LOWEST_DISTANCE + SIZE_DISTANCE_STEP * step);
            float size = DustGlitterField.sizeFactor(distance, SIZE_SPREAD_ONE);
            helper.assertTrue(size <= previousSize, "The size factor rises from " + previousSize + " at "
                    + (distance - (float) SIZE_DISTANCE_STEP) + " blocks to " + size + " at " + distance + " blocks");
            float nextSize = DustGlitterField.sizeFactor(distance + (float) SIZE_DISTANCE_STEP, SIZE_SPREAD_ONE);
            helper.assertTrue(Math.abs(nextSize - size) <= SIZE_STEP_LIMIT, "The size factor changes by "
                    + Math.abs(nextSize - size) + " from " + distance + " blocks to "
                    + (distance + (float) SIZE_DISTANCE_STEP) + " blocks, more than " + SIZE_STEP_LIMIT);
            previousSize = size;
        }

        helper.assertTrue(DustGlitterField.spriteRadius(SIZE_BASE_RADIUS, 1.0F) == SIZE_BASE_RADIUS,
                "The sprite radius of the base radius " + SIZE_BASE_RADIUS + " at the size factor 1 is "
                        + DustGlitterField.spriteRadius(SIZE_BASE_RADIUS, 1.0F) + " instead of " + SIZE_BASE_RADIUS);
        helper.assertTrue(DustGlitterField.spriteRadius(SIZE_BASE_RADIUS, DustGlitterField.SMALLEST_SIZE)
                        == DustGlitterField.SMALLEST_SPRITE_RADIUS, "The sprite radius of the base radius "
                + SIZE_BASE_RADIUS + " at the smallest size factor " + DustGlitterField.SMALLEST_SIZE + " is "
                + DustGlitterField.spriteRadius(SIZE_BASE_RADIUS, DustGlitterField.SMALLEST_SIZE) + " instead of the "
                + "smallest " + DustGlitterField.SMALLEST_SPRITE_RADIUS);
        helper.assertTrue(DustGlitterField.spriteRadius(1.0F, 1.0F) == DustGlitterField.SMALLEST_SPRITE_RADIUS,
                "The sprite radius of the base radius 1 at the size factor 1 is "
                        + DustGlitterField.spriteRadius(1.0F, 1.0F) + " instead of the smallest "
                        + DustGlitterField.SMALLEST_SPRITE_RADIUS);
        float doubledRadius = DustGlitterField.spriteRadius(SIZE_BASE_RADIUS, 2.0F);
        helper.assertTrue(Math.abs(doubledRadius - 2.0 * SIZE_BASE_RADIUS) <= SIZE_RADIUS_TOLERANCE, "The sprite "
                + "radius of the base radius " + SIZE_BASE_RADIUS + " at the size factor 2 is " + doubledRadius
                + " instead of " + 2.0 * SIZE_BASE_RADIUS);

        int lightSteps = (int) Math.round((SIZE_LIGHT_HIGHEST_DISTANCE - SIZE_LIGHT_LOWEST_DISTANCE)
                / SIZE_LIGHT_STEP);
        for (float spread : SIZE_SPREADS) {
            for (int step = 0; step <= lightSteps; step++) {
                float distance = (float) (SIZE_LIGHT_LOWEST_DISTANCE + SIZE_LIGHT_STEP * step);
                float radius = DustGlitterField.spriteRadius(SIZE_BASE_RADIUS,
                        DustGlitterField.sizeFactor(distance, spread));
                double widening = radius / (double) SIZE_BASE_RADIUS;
                double peak = DustGlitterField.peakFactor(distance, radius, SIZE_BASE_RADIUS) * widening * widening;
                double light = DustGlitterField.lightFactor(distance);
                helper.assertTrue(Math.abs(peak - light) <= SIZE_LIGHT_RELATIVE_TOLERANCE * light, "The peak of a "
                        + "glint at " + distance + " blocks with the size spread " + spread + " and the sprite "
                        + "radius " + radius + " times its widening squared is " + peak + " instead of the light "
                        + "factor " + light + ", off by " + Math.abs(peak - light));
            }
        }
        float referencePeak = DustGlitterField.peakFactor(DustGlitterField.SIZE_DISTANCE, SIZE_BASE_RADIUS,
                SIZE_BASE_RADIUS);
        float referenceLight = DustGlitterField.lightFactor(DustGlitterField.SIZE_DISTANCE);
        helper.assertTrue(Math.abs(referencePeak - referenceLight) <= SIZE_PEAK_TOLERANCE, "The peak at "
                + DustGlitterField.SIZE_DISTANCE + " blocks with the base radius " + SIZE_BASE_RADIUS + " is "
                + referencePeak + " instead of the light factor " + referenceLight);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void glintLightIsTheBlendedBeamTimesTheLightScale(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double blurSigma = IceHaloMap.blurSigma(SkySettings.DEFAULT.actualTexelDegrees());
        IceHaloProfile profile = new IceHaloProfile(data, blurSigma);
        SkyTables[] depthTables = new SkyTables[GLINT_DEPTHS.length];
        for (int depthEnd = 0; depthEnd < GLINT_DEPTHS.length; depthEnd++) {
            depthTables[depthEnd] = new SkyTables(SkySettings.DEFAULT.withIceOpticalDepth(GLINT_DEPTHS[depthEnd]));
        }
        SkyView[][] viewGrid = new SkyView[GLINT_DEPTHS.length][GLINT_SUN_DEGREES.length];
        SkyViewSet views = new SkyViewSet();
        for (int sunEnd = 0; sunEnd < GLINT_SUN_DEGREES.length; sunEnd++) {
            double sunElevation = Math.toRadians(GLINT_SUN_DEGREES[sunEnd]);
            IceHaloMap map = new IceHaloMap(data, sunElevation, blurSigma);
            for (int depthEnd = 0; depthEnd < GLINT_DEPTHS.length; depthEnd++) {
                SkyView view = depthTables[depthEnd].computeView(sunElevation, profile, map);
                viewGrid[depthEnd][sunEnd] = view;
                views.setView(SkyViewSet.slot(depthEnd, sunEnd), view);
                logTransmittance(GLINT_DEPTHS[depthEnd], GLINT_SUN_DEGREES[sunEnd], view);
            }
        }
        SkyLight moon = new SkyLight();
        moon.set(SkyLight.Kind.MOON, 0.0, MOON_SCALE, MOON_COLOR[0], MOON_COLOR[1], MOON_COLOR[2]);
        double[][] lightScales = new double[2][SkyAtmosphere.CHANNEL_COUNT];
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            lightScales[0][channel] = FULL_LIGHT_SCALE[channel];
            lightScales[1][channel] = moon.radianceScale(channel, SkyLight.Kind.MOON);
        }
        double[] result = new double[SkyAtmosphere.CHANNEL_COUNT];
        for (double sunWeight : GLINT_SUN_WEIGHTS) {
            for (double depthWeight : GLINT_DEPTH_WEIGHTS) {
                for (int scaleIndex = 0; scaleIndex < lightScales.length; scaleIndex++) {
                    double[] lightScale = lightScales[scaleIndex];
                    views.lightTransmittance(sunWeight, depthWeight, lightScale, result);
                    for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                        double expected = blendedTransmittance(viewGrid, channel, sunWeight, depthWeight)
                                * lightScale[channel];
                        helper.assertTrue(Math.abs(result[channel] - expected)
                                        <= TRANSMITTANCE_TOLERANCE * Math.abs(expected), "The glint light at the sun "
                                + "weight " + sunWeight + ", the depth weight " + depthWeight + " and the light scale "
                                + lightScale[channel] + " is " + result[channel] + " in channel " + channel
                                + " instead of the blended " + expected + ", off by "
                                + Math.abs(result[channel] - expected));
                    }
                }
            }
        }

        SkyView[] brightViews = new SkyView[GLINT_DEPTHS.length];
        for (int depthEnd = 0; depthEnd < GLINT_DEPTHS.length; depthEnd++) {
            double sunElevation = Math.toRadians(GLINT_LIGHT_SUN_DEGREES);
            brightViews[depthEnd] = depthTables[depthEnd].computeView(sunElevation, profile,
                    new IceHaloMap(data, sunElevation, blurSigma));
            logTransmittance(GLINT_DEPTHS[depthEnd], GLINT_LIGHT_SUN_DEGREES, brightViews[depthEnd]);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                double transmittance = brightViews[depthEnd].lightTransmittance()[channel];
                helper.assertTrue(transmittance > 0.0 && transmittance < 1.0, "The transmittance at the sun "
                        + GLINT_LIGHT_SUN_DEGREES + " degrees and the depth " + GLINT_DEPTHS[depthEnd] + " is "
                        + transmittance + " in channel " + channel + ", outside 0 to 1");
            }
        }
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double thick = brightViews[1].lightTransmittance()[channel];
            double thin = brightViews[0].lightTransmittance()[channel];
            helper.assertTrue(thick < thin, "The transmittance at the sun " + GLINT_LIGHT_SUN_DEGREES
                    + " degrees and the depth " + GLINT_DEPTHS[1] + " is " + thick + " in channel " + channel
                    + ", not below the " + thin + " of the depth " + GLINT_DEPTHS[0]);
        }
        for (int depthEnd = 0; depthEnd < GLINT_DEPTHS.length; depthEnd++) {
            double sunElevation = Math.toRadians(GLINT_DARK_SUN_DEGREES);
            SkyView darkView = depthTables[depthEnd].computeView(sunElevation, profile,
                    new IceHaloMap(data, sunElevation, blurSigma));
            logTransmittance(GLINT_DEPTHS[depthEnd], GLINT_DARK_SUN_DEGREES, darkView);
            for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                double transmittance = darkView.lightTransmittance()[channel];
                helper.assertTrue(transmittance == 0.0, "The transmittance at the sun " + GLINT_DARK_SUN_DEGREES
                        + " degrees and the depth " + GLINT_DEPTHS[depthEnd] + " is " + transmittance
                        + " in channel " + channel + " instead of 0");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void mirrorLobeContinuesTheHaloMapsPillar(GameTestHelper helper) {
        IceCrystalData data = IceCrystalData.get();
        double[] tiltSigmas = new double[IceHaloMap.MIRROR_KINDS];
        double[] halfHeights = new double[IceHaloMap.MIRROR_KINDS];
        int kinds = DustGlitterField.plateKinds(data, tiltSigmas, halfHeights);
        int plateCrystals = 0;
        for (IceCrystalData.Crystal crystal : data.crystals()) {
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE) {
                plateCrystals++;
            }
        }
        helper.assertTrue(kinds == IceHaloMap.MIRROR_KINDS && plateCrystals == kinds, "The data set has "
                + plateCrystals + " plate kinds, of which " + kinds + " are kept, not the " + IceHaloMap.MIRROR_KINDS
                + " that the halo map keeps");

        // With all of a plate kind's light mirrored and no blur, the map holds only its pillar and the sun's bin:
        IceHaloFormulas formulas = new IceHaloFormulas(data);
        double[][] maps = new double[SkyAtmosphere.CHANNEL_COUNT][IceHaloMap.AZIMUTH_BINS * IceHaloMap.ELEVATION_BINS];
        double[][] pathShares = new double[SkyAtmosphere.CHANNEL_COUNT][IceCrystalData.PATH_COUNT];
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            pathShares[channel][IceCrystalData.END_MIRROR_PATH] = 1.0;
        }
        double[] lobeScales = new double[kinds];
        double[] binValues = new double[MIRROR_ROWS];
        int kind = 0;
        for (IceCrystalData.Crystal crystal : data.crystals()) {
            if (crystal.orientation() != IceCrystalData.Orientation.PLATE || kind >= kinds) {
                continue;
            }
            for (double sunDegrees : MIRROR_SUN_DEGREES) {
                double sunElevation = Math.toRadians(sunDegrees);
                double[] sun = {Math.cos(sunElevation), Math.sin(sunElevation), 0.0};
                for (double[] map : maps) {
                    Arrays.fill(map, 0.0);
                }
                formulas.addCrystal(maps, crystal, sun, MIRROR_WEIGHT, pathShares);
                int sunBin = IceHaloMap.binOf(sun[0], sun[1], sun[2]);
                double crossSection = IceMirror.crossSection(sunElevation, tiltSigmas[kind], halfHeights[kind]);
                for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
                    double scale = formulas.mirrorLobeScale(channel);
                    double expectedScale = MIRROR_WEIGHT / IceMirror.pillarShare(sunElevation, tiltSigmas[kind],
                            crossSection, IceHaloFormulas.refractiveIndex(channel));
                    String where = "The " + crystal.name() + " at sun " + sunDegrees + " degrees in channel " + channel;
                    helper.assertTrue(Math.abs(scale - expectedScale) <= MIRROR_SCALE_TOLERANCE * expectedScale,
                            where + " has the mirror lobe scale " + scale + " instead of the weight over the pillar "
                                    + "share, " + expectedScale);
                    Arrays.fill(lobeScales, 0.0);
                    lobeScales[kind] = scale;
                    double largest = 0.0;
                    for (int row = 0; row < MIRROR_ROWS; row++) {
                        binValues[row] = maps[channel][row * IceHaloMap.AZIMUTH_BINS];
                        if (row * IceHaloMap.AZIMUTH_BINS != sunBin) {
                            largest = Math.max(largest, binValues[row]);
                        }
                    }
                    // The map takes the strength at each row's middle, which is not the bin's mean next to the sun's row:
                    int sunRow = sunBin >= 0 ? sunBin / IceHaloMap.AZIMUTH_BINS : Integer.MIN_VALUE / 2;
                    int worstRow = -1;
                    double worstDeviation = 0.0;
                    double worstLobe = 0.0;
                    for (int row = 0; row < MIRROR_ROWS; row++) {
                        if (Math.abs(row - sunRow) <= 1) {
                            continue;
                        }
                        double lobe = integrateMirrorLobeInBin(channel, row, sunElevation, kinds, tiltSigmas,
                                halfHeights, lobeScales);
                        LOGGER.info("MirrorLobe " + crystal.name() + " sun " + sunDegrees + " channel " + channel
                                + " row " + row + " lobe " + lobe + " map " + binValues[row]);
                        double deviation = Math.abs(lobe - binValues[row])
                                / Math.max(binValues[row], MIRROR_TAIL_SHARE * largest);
                        if (binValues[row] >= MIRROR_SMALLEST_SHARE * largest && deviation > worstDeviation) {
                            worstDeviation = deviation;
                            worstRow = row;
                            worstLobe = lobe;
                        }
                    }
                    helper.assertTrue(worstDeviation <= MIRROR_TOLERANCE, where + " gives the mirror lobe "
                            + worstLobe + " per steradian over the bin of row " + worstRow + " and column 0, where "
                            + "the map holds " + (worstRow >= 0 ? binValues[worstRow] : 0.0) + ", off by more than "
                            + MIRROR_TOLERANCE);
                }
            }
            kind++;
        }

        double atTheEdge = DustGlitterField.belowMapPhase(2.0, 2.0, 3.0, IceHaloMap.LOWEST_ELEVATION);
        helper.assertTrue(atTheEdge == 3.0, "The phase at the map's lowest elevation with the lobe 2, the lobe at "
                + "the edge 2 and the lowest row 3 is " + atTheEdge + " instead of the row 3");
        double belowTheEdge = DustGlitterField.belowMapPhase(2.0, 2.5, 3.0,
                IceHaloMap.LOWEST_ELEVATION - Math.toRadians(1.0));
        helper.assertTrue(belowTheEdge == 2.0, "The phase a degree below the map's lowest elevation with the lobe 2, "
                + "the lobe at the edge 2.5 and the lowest row 3 is " + belowTheEdge + " instead of the lobe 2");
        for (int step = 0; step <= BELOW_MAP_STEPS; step++) {
            double elevation = Math.toRadians(BELOW_MAP_LOWEST_DEGREES
                    + (BELOW_MAP_HIGHEST_DEGREES - BELOW_MAP_LOWEST_DEGREES) * step / BELOW_MAP_STEPS);
            double phase = DustGlitterField.belowMapPhase(BELOW_MAP_LOBE, BELOW_MAP_EDGE, BELOW_MAP_ROW, elevation);
            helper.assertTrue(phase >= 0.0, "The phase at " + Math.toDegrees(elevation) + " degrees with the lobe "
                    + BELOW_MAP_LOBE + ", the lobe at the edge " + BELOW_MAP_EDGE + " and the lowest row "
                    + BELOW_MAP_ROW + " is " + phase + ", below 0");
        }
        helper.succeed();
    }

    /**
     * The transmittance that SkyViewSet.lightTransmittance blends from the four views, without the light scale.
     */
    private static double blendedTransmittance(SkyView[][] viewGrid, int channel, double sunWeight,
            double depthWeight) {
        double lowDepthLowSun = viewGrid[0][0].lightTransmittance()[channel];
        double lowDepthHighSun = viewGrid[0][1].lightTransmittance()[channel];
        double highDepthLowSun = viewGrid[1][0].lightTransmittance()[channel];
        double highDepthHighSun = viewGrid[1][1].lightTransmittance()[channel];
        double lowDepth = lowDepthLowSun + (lowDepthHighSun - lowDepthLowSun) * sunWeight;
        double highDepth = highDepthLowSun + (highDepthHighSun - highDepthLowSun) * sunWeight;
        return lowDepth + (highDepth - lowDepth) * depthWeight;
    }

    private static void logTransmittance(double depth, double sunDegrees, SkyView view) {
        double[] transmittance = view.lightTransmittance();
        LOGGER.info("GlintLight depth " + depth + " sun " + sunDegrees + " transmittance " + transmittance[0] + ","
                + transmittance[1] + "," + transmittance[2]);
    }

    /**
     * The mirror lobe without blur integrated over the bin of a row in column 0, both halves of the sky, with
     * dv * dside as the solid angle, per steradian of the bin.
     */
    private static double integrateMirrorLobeInBin(int channel, int row, double lightElevation, int kinds,
            double[] tiltSigmas, double[] halfHeights, double[] lobeScales) {
        double lowestElevation = IceHaloMap.LOWEST_ELEVATION + row * IceHaloMap.BIN_STEP;
        double elevationStep = IceHaloMap.BIN_STEP / MIRROR_STEPS;
        double sum = 0.0;
        for (int elevationIndex = 0; elevationIndex < MIRROR_STEPS; elevationIndex++) {
            double viewElevation = lowestElevation + (elevationIndex + 0.5) * elevationStep;
            double widestSide = IceHaloMap.BIN_STEP * Math.cos(viewElevation);
            double sideStep = 2.0 * widestSide / MIRROR_STEPS;
            for (int sideIndex = 0; sideIndex < MIRROR_STEPS; sideIndex++) {
                double sideAngle = -widestSide + (sideIndex + 0.5) * sideStep;
                sum += DustGlitterField.mirrorLobe(channel, viewElevation, sideAngle, lightElevation, kinds,
                        tiltSigmas, halfHeights, lobeScales, 0.0) * elevationStep * sideStep;
            }
        }
        return sum / IceHaloMap.binSolidAngle(row * IceHaloMap.AZIMUTH_BINS);
    }
}
