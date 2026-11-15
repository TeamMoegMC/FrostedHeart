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
 * over the world, the nested share of points that the dust draws, the fade before the copies switch, the twinkle, the
 * light of a glint, and the plates' mirror lobe below the halo map.
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
    private static final double[] DRIFT_COORDINATES = {0.0, -7.3, 31.99, 1234.5};
    private static final int BOX_POINT_COUNT = 4096;
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
    private static final int TWINKLE_PHASE_STEPS = 100000;
    private static final double TWINKLE_MEAN_TOLERANCE = 1.0e-3;
    private static final int TWINKLE_SIGN_STEPS = 20000;
    private static final float[] TWINKLE_CLOCK_SHARES = {0.0F, 0.37F, 0.81F};
    private static final double TWINKLE_POINTS_TOLERANCE = 0.05;
    /**
     * Half a float ulp of the phase at the most cycles per wrap, 4096, where clockShare * cycles lies near 4096.
     */
    private static final double WRAP_TOLERANCE = 5.0e-4;
    private static final int COPY_POINT_COUNT = 4096;
    private static final int[] COPY_CELL = {3, -2, 7};
    private static final int[] OTHER_COPY_CELL = {4, -2, 7};
    private static final float COPY_CLOCK_SHARE = 0.37F;
    private static final double COPY_DIFFERENT_SHARE = 0.9;
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
        for (double eye : EYE_COORDINATES) {
            for (double drift : DRIFT_COORDINATES) {
                float eyeOffset = (float) DustGlitterField.cellOffset(eye);
                float driftOffset = (float) DustGlitterField.cellOffset(drift);
                int eyeCellLessDriftCell = DustGlitterField.cellIndex(eye) - DustGlitterField.cellIndex(drift);
                for (int axis = 0; axis < AXIS_COUNT; axis++) {
                    for (int index = 0; index < BOX_POINT_COUNT; index++) {
                        float position = DustGlitterField.positionInCell(index, axis);
                        float offset = DustGlitterField.nearestOffset(position, eyeOffset, driftOffset);
                        helper.assertTrue(Math.abs(offset) <= DustGlitterField.CELL_SIZE / 2.0 + BOX_TOLERANCE,
                                "Point " + index + " on axis " + axis + " with the eye at " + eye + " and the drift at "
                                        + drift + " lies " + offset + " from the eye, outside the box of half size "
                                        + DustGlitterField.CELL_SIZE / 2.0);
                        float shift = DustGlitterField.copyShift(position, eyeOffset, driftOffset);
                        int copyCell = DustGlitterField.copyCell(eyeCellLessDriftCell, shift);
                        double world = eye + offset;
                        double expected = (double) DustGlitterField.CELL_SIZE * copyCell + position + drift;
                        helper.assertTrue(Math.abs(world - expected) <= BOX_TOLERANCE, "Point " + index + " on axis "
                                + axis + " with the eye at " + eye + " and the drift at " + drift + " stands at "
                                + world + " instead of " + expected + " in the copy cell " + copyCell);
                    }
                }
            }
        }

        int compared = 0;
        for (int index = 0; index < BOX_POINT_COUNT; index++) {
            float position = DustGlitterField.positionInCell(index, 0);
            float startEyeOffset = (float) DustGlitterField.cellOffset(MOVING_EYE_START);
            float startOffset = DustGlitterField.nearestOffset(position, startEyeOffset, 0.0F);
            float startShift = DustGlitterField.copyShift(position, startEyeOffset, 0.0F);
            int startCell = DustGlitterField.copyCell(
                    DustGlitterField.cellIndex(MOVING_EYE_START) - DustGlitterField.cellIndex(0.0), startShift);
            if (Math.abs(startOffset) > MOVING_EYE_INNER_HALF) {
                continue;
            }
            for (double eye = MOVING_EYE_START; eye <= MOVING_EYE_HIGHEST; eye += MOVING_EYE_STEP) {
                float eyeOffset = (float) DustGlitterField.cellOffset(eye);
                float offset = DustGlitterField.nearestOffset(position, eyeOffset, 0.0F);
                float shift = DustGlitterField.copyShift(position, eyeOffset, 0.0F);
                int copyCell = DustGlitterField.copyCell(
                        DustGlitterField.cellIndex(eye) - DustGlitterField.cellIndex(0.0), shift);
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
                helper.assertTrue(copyCell == startCell, "Point " + index + " with the eye moved from "
                        + MOVING_EYE_START + " to " + eye + " lies in the copy cell " + copyCell + " instead of in "
                        + startCell + ", since its offset " + startOffset + " and " + movedOffset
                        + " both stay inside " + MOVING_EYE_INNER_HALF);
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
    public static void glitterTwinklesWithAMeanOfOne(GameTestHelper helper) {
        double phaseSum = 0.0;
        for (int step = 0; step < TWINKLE_PHASE_STEPS; step++) {
            phaseSum += DustGlitterField.twinkle((float) ((step + 0.5) / TWINKLE_PHASE_STEPS));
        }
        double phaseMean = phaseSum / TWINKLE_PHASE_STEPS;
        LOGGER.info("Glitter twinkle mean over the phase " + phaseMean);
        helper.assertTrue(Math.abs(phaseMean - 1.0) <= TWINKLE_MEAN_TOLERANCE, "The mean twinkle over the phase is "
                + phaseMean + " instead of 1 within " + TWINKLE_MEAN_TOLERANCE);
        helper.assertTrue(DustGlitterField.twinkle(DustGlitterField.TWINKLE_DUTY) == 0.0F, "The twinkle at the duty "
                + DustGlitterField.TWINKLE_DUTY + " is " + DustGlitterField.twinkle(DustGlitterField.TWINKLE_DUTY)
                + " instead of 0");
        helper.assertTrue(DustGlitterField.twinkle(DustGlitterField.TWINKLE_DUTY + 0.1F) == 0.0F, "The twinkle above "
                + "the duty is " + DustGlitterField.twinkle(DustGlitterField.TWINKLE_DUTY + 0.1F) + " instead of 0");
        helper.assertTrue(DustGlitterField.twinkle(1.0F) == 0.0F, "The twinkle at the end of the phase is "
                + DustGlitterField.twinkle(1.0F) + " instead of 0");
        for (int step = 0; step <= TWINKLE_SIGN_STEPS; step++) {
            float phase = (float) step / TWINKLE_SIGN_STEPS;
            helper.assertTrue(DustGlitterField.twinkle(phase) >= 0.0F, "The twinkle at the phase " + phase + " is "
                    + DustGlitterField.twinkle(phase) + ", below 0");
        }

        for (float clockShare : TWINKLE_CLOCK_SHARES) {
            double pointSum = 0.0;
            for (int index = 0; index < SHARE_POINT_COUNT; index++) {
                int twinkleHash = DustGlitterField.copyHash(
                        DustGlitterField.pointHash(index, DustGlitterField.TWINKLE_HASH), COPY_CELL[0], COPY_CELL[1],
                        COPY_CELL[2]);
                pointSum += DustGlitterField.twinkle(DustGlitterField.cyclePhase(clockShare, twinkleHash,
                        DustGlitterField.LEAST_TWINKLE_CYCLES, DustGlitterField.TWINKLE_CYCLE_RANGE));
            }
            double pointMean = pointSum / SHARE_POINT_COUNT;
            LOGGER.info("Glitter twinkle mean at the clock share " + clockShare + " over the points " + pointMean);
            helper.assertTrue(Math.abs(pointMean - 1.0) <= TWINKLE_POINTS_TOLERANCE, "The mean twinkle at the clock "
                    + "share " + clockShare + " over the " + SHARE_POINT_COUNT + " points is " + pointMean
                    + " instead of 1 within " + TWINKLE_POINTS_TOLERANCE);
        }

        float largestTwinkleJump = 0.0F;
        float largestSwayJump = 0.0F;
        int widestTwinkleHash = 0;
        int widestSwayHash = 0;
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            int twinkleHash = DustGlitterField.copyHash(
                    DustGlitterField.pointHash(index, DustGlitterField.TWINKLE_HASH), COPY_CELL[0], COPY_CELL[1],
                    COPY_CELL[2]);
            float twinkleJump = wrapDistance(
                    DustGlitterField.cyclePhase(0.0F, twinkleHash, DustGlitterField.LEAST_TWINKLE_CYCLES,
                            DustGlitterField.TWINKLE_CYCLE_RANGE),
                    DustGlitterField.cyclePhase(1.0F, twinkleHash, DustGlitterField.LEAST_TWINKLE_CYCLES,
                            DustGlitterField.TWINKLE_CYCLE_RANGE));
            if (twinkleJump > largestTwinkleJump) {
                largestTwinkleJump = twinkleJump;
                widestTwinkleHash = twinkleHash;
            }
            float swayJump = wrapDistance(
                    DustGlitterField.cyclePhase(0.0F, twinkleHash, DustGlitterField.LEAST_SWAY_CYCLES,
                            DustGlitterField.SWAY_CYCLE_RANGE),
                    DustGlitterField.cyclePhase(1.0F, twinkleHash, DustGlitterField.LEAST_SWAY_CYCLES,
                            DustGlitterField.SWAY_CYCLE_RANGE));
            if (swayJump > largestSwayJump) {
                largestSwayJump = swayJump;
                widestSwayHash = twinkleHash;
            }
        }
        LOGGER.info("Glitter wrap jump twinkle " + largestTwinkleJump + " at the hash " + widestTwinkleHash
                + ", sway " + largestSwayJump + " at the hash " + widestSwayHash);
        for (int index = 0; index < SHARE_POINT_COUNT; index++) {
            int twinkleHash = DustGlitterField.copyHash(
                    DustGlitterField.pointHash(index, DustGlitterField.TWINKLE_HASH), COPY_CELL[0], COPY_CELL[1],
                    COPY_CELL[2]);
            float twinkleAtStart = DustGlitterField.cyclePhase(0.0F, twinkleHash,
                    DustGlitterField.LEAST_TWINKLE_CYCLES, DustGlitterField.TWINKLE_CYCLE_RANGE);
            float twinkleAtWrap = DustGlitterField.cyclePhase(1.0F, twinkleHash,
                    DustGlitterField.LEAST_TWINKLE_CYCLES, DustGlitterField.TWINKLE_CYCLE_RANGE);
            helper.assertTrue(wrapDistance(twinkleAtStart, twinkleAtWrap) <= WRAP_TOLERANCE, "Point " + index
                    + " with the twinkle hash " + twinkleHash + " stands at the phase " + twinkleAtStart
                    + " at the start of the clock and at " + twinkleAtWrap + " at its wrap, a jump of "
                    + wrapDistance(twinkleAtStart, twinkleAtWrap) + " instead of at most " + WRAP_TOLERANCE);
            float swayAtStart = DustGlitterField.cyclePhase(0.0F, twinkleHash, DustGlitterField.LEAST_SWAY_CYCLES,
                    DustGlitterField.SWAY_CYCLE_RANGE);
            float swayAtWrap = DustGlitterField.cyclePhase(1.0F, twinkleHash, DustGlitterField.LEAST_SWAY_CYCLES,
                    DustGlitterField.SWAY_CYCLE_RANGE);
            helper.assertTrue(wrapDistance(swayAtStart, swayAtWrap) <= WRAP_TOLERANCE, "Point " + index
                    + " with the sway hash " + twinkleHash + " stands at the phase " + swayAtStart
                    + " at the start of the clock and at " + swayAtWrap + " at its wrap, a jump of "
                    + wrapDistance(swayAtStart, swayAtWrap) + " instead of at most " + WRAP_TOLERANCE);
        }

        int different = 0;
        for (int index = 0; index < COPY_POINT_COUNT; index++) {
            int twinkleHash = DustGlitterField.pointHash(index, DustGlitterField.TWINKLE_HASH);
            float firstPhase = DustGlitterField.cyclePhase(COPY_CLOCK_SHARE,
                    DustGlitterField.copyHash(twinkleHash, COPY_CELL[0], COPY_CELL[1], COPY_CELL[2]),
                    DustGlitterField.LEAST_TWINKLE_CYCLES, DustGlitterField.TWINKLE_CYCLE_RANGE);
            float secondPhase = DustGlitterField.cyclePhase(COPY_CLOCK_SHARE,
                    DustGlitterField.copyHash(twinkleHash, OTHER_COPY_CELL[0], OTHER_COPY_CELL[1],
                            OTHER_COPY_CELL[2]),
                    DustGlitterField.LEAST_TWINKLE_CYCLES, DustGlitterField.TWINKLE_CYCLE_RANGE);
            if (firstPhase != secondPhase) {
                different++;
            }
        }
        helper.assertTrue(different >= COPY_DIFFERENT_SHARE * COPY_POINT_COUNT, "The copies " + COPY_CELL[0] + " "
                + COPY_CELL[1] + " " + COPY_CELL[2] + " and " + OTHER_COPY_CELL[0] + " " + OTHER_COPY_CELL[1] + " "
                + OTHER_COPY_CELL[2] + " of the " + COPY_POINT_COUNT + " points twinkle apart at " + different
                + " of them, less than the " + COPY_DIFFERENT_SHARE + " the test needs");
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

    /**
     * The distance between two phases on the circle of the clock, from 0 to 0.5.
     */
    private static float wrapDistance(float first, float second) {
        float distance = Math.abs(first - second);
        return Math.min(distance, 1.0F - distance);
    }
}
