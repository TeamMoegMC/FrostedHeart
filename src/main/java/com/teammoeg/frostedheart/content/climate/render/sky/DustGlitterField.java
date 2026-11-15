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

import java.util.List;

/**
 * The field of points that diamond_dust.glsl draws as the glints of diamond dust near the eye, with the shader's math
 * in plain Java for the GameTests. Each point has a fixed position in a cube of CELL_SIZE blocks that repeats over the
 * world, and it shows at its copy nearest the eye, so it stays in place as the eye moves and fades out before the
 * copies switch. The drift moves every point together, and each point also sways on its own. A point is drawn while
 * its own number lies below the share that the dust's extinction calls for, so the drawn points grow and shrink as a
 * nested set as the extinction glides, and its light thins with its height above the dust's bottom. Each copy flashes
 * for TWINKLE_DUTY of its own period. A glint's light is the light's beam times the sky's ice phase toward it, which
 * below IceHaloMap.LOWEST_ELEVATION is the plates' mirror lobe, plus a floor in every direction. The halo lookup and
 * the tone map are only in the shader.
 * The positions and the clock reach the shader as offsets within their cubes, so it only sees small numbers.
 */
public final class DustGlitterField {
    public static final float CELL_SIZE = 32.0F;
    /**
     * Points fade out between these distances in blocks from the eye, which must reach half the cell.
     */
    public static final float FADE_START = 12.0F;
    public static final float FADE_END = 16.0F;
    /**
     * The distance in blocks at which a glint shows half the light of one at the eye.
     */
    public static final float FALLOFF_DISTANCE = 4.0F;
    /**
     * Blocks per second the dust falls and drifts along x and z.
     */
    public static final double FALL_SPEED = 0.12;
    public static final double DRIFT_SPEED_X = 0.03;
    public static final double DRIFT_SPEED_Z = 0.02;
    /**
     * The radius in blocks of the circle each point sways on.
     */
    public static final float SWAY_RADIUS = 0.1F;
    /**
     * The clock wraps here. Each point's sway and each copy's twinkle repeat a whole number of cycles per wrap, from
     * the least count up by less than the range, so nothing jumps where the clock wraps. Twinkles last 1 to 3 seconds
     * and sways 3.2 to 5.1 seconds.
     */
    public static final double CLOCK_WRAP_SECONDS = 4096.0;
    public static final int LEAST_TWINKLE_CYCLES = 1366;
    public static final int TWINKLE_CYCLE_RANGE = 2731;
    public static final int LEAST_SWAY_CYCLES = 800;
    public static final int SWAY_CYCLE_RANGE = 500;
    /**
     * The share of its period over which a copy flashes. Its flash is a squared sine scaled so its mean is 1.
     */
    public static final float TWINKLE_DUTY = 0.15F;
    /**
     * The angle from the light at which the floor takes the phase of the tumbling crystals, the 22 degree halo.
     */
    public static final double RING_RADIANS = Math.toRadians(22.0);
    /**
     * The angle below IceHaloMap.LOWEST_ELEVATION over which the map's lowest row fades out.
     */
    public static final double ROW_FADE_RADIANS = Math.toRadians(1.0);
    /**
     * The gain on the light of a glint at a dustGlitterBrightness of 1. A glint 6 blocks away in a direction with only
     * the floor and the tumbling crystals, with a 10 degree sun through ice of optical depth 0.1, then shows 55% of white
     * in linear light at the peak of its flash.
     */
    public static final double CALIBRATED_BRIGHTNESS = 20.0;
    /**
     * The width at half maximum of a Gaussian over its standard deviation, which turns dustGlitterSize into the
     * standard deviation of a glint's core.
     */
    public static final double HALF_MAXIMUM_WIDTH_PER_SIGMA = 2.0 * Math.sqrt(2.0 * Math.log(2.0));
    /**
     * The radius of a glint's sprite in standard deviations of its core.
     */
    public static final double SPRITE_RADIUS_SIGMAS = 4.0;
    /**
     * The hash steps from a point's seed to each of its numbers, as in diamond_dust.glsl.
     */
    public static final int X_HASH = 0;
    public static final int Y_HASH = 1;
    public static final int Z_HASH = 2;
    public static final int DENSITY_HASH = 3;
    public static final int SWAY_HASH = 4;
    public static final int TWINKLE_HASH = 5;
    private static final int POINT_SEED = 0x2545f491;
    private static final double SMALLEST_SIGMA = 1.0e-9;

    private DustGlitterField() {
    }

    /**
     * The lowbias32 integer hash by Chris Wellons, as hashOf in diamond_dust.glsl on unsigned integers.
     */
    public static int hashOf(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        mixed ^= mixed >>> 16;
        return mixed;
    }

    /**
     * A number from 0 up to 1 from the top 24 bits of a hash.
     */
    public static float uniformOf(int hashed) {
        return (hashed >>> 8) / 16777216.0F;
    }

    /**
     * The hash of a point's index after the given number of further steps, X_HASH to TWINKLE_HASH.
     */
    public static int pointHash(int pointIndex, int step) {
        int hashed = hashOf(pointIndex ^ POINT_SEED);
        for (int index = 0; index < step; index++) {
            hashed = hashOf(hashed);
        }
        return hashed;
    }

    /**
     * The point's position along an axis, 0 to 2, in its cube, from 0 up to CELL_SIZE.
     */
    public static float positionInCell(int pointIndex, int axis) {
        return uniformOf(pointHash(pointIndex, X_HASH + axis)) * CELL_SIZE;
    }

    /**
     * The offset of a coordinate in its cube, from 0 to CELL_SIZE, computed in double precision.
     */
    public static double cellOffset(double coordinate) {
        return coordinate - CELL_SIZE * Math.floor(coordinate / CELL_SIZE);
    }

    public static int cellIndex(double coordinate) {
        return (int) Math.floor(coordinate / CELL_SIZE);
    }

    /**
     * How many cubes the copy nearest the eye lies below the point's position plus the drift less the eye, along one
     * axis, from the offsets of the position, the eye, and the drift in their cubes.
     */
    public static float copyShift(float positionInCell, float eyeOffset, float driftOffset) {
        float relative = positionInCell + driftOffset - eyeOffset;
        return (float) Math.floor(relative / CELL_SIZE + 0.5F);
    }

    /**
     * The offset from the eye of the point's copy nearest the eye along one axis, from -CELL_SIZE / 2 to CELL_SIZE / 2.
     */
    public static float nearestOffset(float positionInCell, float eyeOffset, float driftOffset) {
        float relative = positionInCell + driftOffset - eyeOffset;
        return relative - CELL_SIZE * copyShift(positionInCell, eyeOffset, driftOffset);
    }

    /**
     * The cube of the copy that a copy shift picks along one axis, from the eye's cube less the drift's cube. A copy
     * keeps its cube as the eye and the drift move.
     */
    public static int copyCell(int eyeCellLessDriftCell, float copyShift) {
        return eyeCellLessDriftCell - (int) copyShift;
    }

    public static float fade(float distance) {
        return 1.0F - smoothstep(FADE_START, FADE_END, distance);
    }

    /**
     * The share of points the dust's extinction per kilometer draws at and below its bottom.
     */
    public static double extinctionShare(double extinctionPerKilometer) {
        return Math.max(0.0, Math.min(1.0, extinctionPerKilometer / DiamondDustLaw.FULL_EXTINCTION));
    }

    /**
     * The factor on a glint's light at a height in blocks above the dust's bottom, as the light pillars thin the dust.
     */
    public static float heightWeight(float heightAboveBottom) {
        return (float) Math.exp(-Math.max(0.0F, heightAboveBottom) / LightPillarSelection.DUST_SCALE_HEIGHT);
    }

    public static boolean isDrawn(int pointIndex, float extinctionShare) {
        return uniformOf(pointHash(pointIndex, DENSITY_HASH)) < extinctionShare;
    }

    /**
     * The hash of a point's copy, from its twinkle hash and the copy's cube, so each copy twinkles on its own.
     */
    public static int copyHash(int twinkleHash, int cellX, int cellY, int cellZ) {
        return hashOf(twinkleHash ^ hashOf(cellX ^ hashOf(cellY ^ hashOf(cellZ))));
    }

    /**
     * Where in its cycle a sway or a twinkle is, from 0 up to 1, from the clock over CLOCK_WRAP_SECONDS and the hash
     * that sets its count of cycles per wrap and its start.
     */
    public static float cyclePhase(float clockShare, int hashed, int leastCycles, int cycleRange) {
        int cycles = leastCycles + Integer.remainderUnsigned(hashOf(hashed), cycleRange);
        float phase = clockShare * cycles + uniformOf(hashed);
        return phase - (float) Math.floor(phase);
    }

    /**
     * The flash of a copy at a phase of its twinkle, whose mean over the phase is 1.
     */
    public static float twinkle(float phase) {
        if (!(phase < TWINKLE_DUTY)) {
            return 0.0F;
        }
        float sine = (float) Math.sin(Math.PI * phase / TWINKLE_DUTY);
        return 2.0F / TWINKLE_DUTY * sine * sine;
    }

    public static float falloff(float distance) {
        float ratio = distance / FALLOFF_DISTANCE;
        return 1.0F / (1.0F + ratio * ratio);
    }

    /**
     * The floor of the phase in every channel, the setting times the tumbling crystals' phase in green at
     * RING_RADIANS, so the floor's glints are white.
     */
    public static double floorPhase(IceHaloProfile profile, double floorShare) {
        return floorShare * profile.phase(SkyAtmosphere.GREEN_CHANNEL, Math.cos(RING_RADIANS), 0.0);
    }

    /**
     * Writes the tilt spread in radians and the half height over the radius of the plate kinds of the data set, in
     * its order, as IceHaloMap counts them, and returns how many there are, up to IceHaloMap.MIRROR_KINDS.
     */
    public static int plateKinds(IceCrystalData data, double[] tiltSigmas, double[] halfHeightsOverRadius) {
        int kinds = 0;
        List<IceCrystalData.Crystal> crystals = data.crystals();
        for (int index = 0; index < crystals.size() && kinds < IceHaloMap.MIRROR_KINDS; index++) {
            IceCrystalData.Crystal crystal = crystals.get(index);
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE) {
                tiltSigmas[kinds] = Math.toRadians(crystal.tiltDegrees());
                halfHeightsOverRadius[kinds] = crystal.halfHeightOverRadius();
                kinds++;
            }
        }
        return kinds;
    }

    /**
     * The mirror lobe scale of a plate kind in a channel, blended between the two halo maps as the sky blends them.
     */
    public static double blendedLobeScale(IceHaloMap lowHalo, IceHaloMap highHalo, double haloBlend, int kind,
            int channel) {
        double low = lowHalo.mirrorLobeScale(kind, channel);
        return low + (highHalo.mirrorLobeScale(kind, channel) - low) * haloBlend;
    }

    /**
     * The plates' mirror lobe per steradian in a channel toward a view elevation and an angle across the light's
     * vertical plane, for a light at the given elevation, summed over the plate kinds with each kind's blended lobe
     * scale, and blurred by the halo map's variance as the map is. diamond_dust.glsl folds each kind's cross section
     * into its scale.
     */
    public static double mirrorLobe(int channel, double viewElevation, double sideAngle, double lightElevation,
            int kinds, double[] tiltSigmas, double[] halfHeightsOverRadius, double[] lobeScales, double blurVariance) {
        double sum = 0.0;
        for (int kind = 0; kind < kinds; kind++) {
            sum += lobeScales[kind] * IceMirror.pillarPhase(viewElevation, sideAngle, lightElevation,
                    Math.max(SMALLEST_SIGMA, tiltSigmas[kind]), halfHeightsOverRadius[kind],
                    IceHaloFormulas.refractiveIndex(channel), blurVariance);
        }
        return sum;
    }

    /**
     * The oriented crystals' phase below IceHaloMap.LOWEST_ELEVATION, from the mirror lobe toward the direction, the
     * lobe at the map's lowest elevation at the same side angle, and the map's lowest row. The row less the lobe at
     * the edge fades out over ROW_FADE_RADIANS, so the phase meets the map without a step and arcs end softly.
     */
    public static double belowMapPhase(double lobe, double lobeAtEdge, double lowestRow, double viewElevation) {
        double rowFade = smoothstep(IceHaloMap.LOWEST_ELEVATION - ROW_FADE_RADIANS, IceHaloMap.LOWEST_ELEVATION,
                viewElevation);
        return Math.max(0.0, lobe + (lowestRow - lobeAtEdge) * rowFade);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float position = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return position * position * (3.0F - 2.0F * position);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double position = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return position * position * (3.0 - 2.0 * position);
    }
}
