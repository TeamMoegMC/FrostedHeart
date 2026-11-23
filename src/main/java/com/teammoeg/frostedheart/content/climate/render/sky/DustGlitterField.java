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
 * in plain Java for the GameTests. Each point lives one life after another, and each life is a new crystal: it starts
 * at a hashed place in a cube of CELL_SIZE blocks that repeats over the world, falls in a straight line, and is lit
 * once, steadily, for a short window. A glint shows at its copy nearest the eye and fades out before the copies switch.
 * A point is drawn while its own number lies below the share that the dust's extinction calls for, so the drawn points
 * grow and shrink as a nested set as the extinction glides, and its light thins with its height above the dust's
 * bottom. The sky's ice phase toward a glint sets how many lives show there and tints them, which below
 * IceHaloMap.LOWEST_ELEVATION is the plates' mirror lobe. Glints grow nearer the eye and shrink farther away with their
 * light spread over their size, and within NEAR_DISTANCE of the eye they are rarer and fainter. The halo lookup and the
 * tone map are only in the shader.
 * The eye reaches the shader as its offset in its cube, and the clock as two parts, so it only sees small numbers.
 */
public final class DustGlitterField {
    public static final float CELL_SIZE = 32.0F;
    /**
     * Glints fade out between these distances in blocks from the eye, which must reach half the cell.
     */
    public static final float FADE_START = 12.0F;
    public static final float FADE_END = 16.0F;
    /**
     * The distance in blocks at which a glint shows half the light of one at the eye.
     */
    public static final float FALLOFF_DISTANCE = 4.0F;
    /**
     * The clock wraps here. Each point lives a whole number of lives per wrap, from the least count up by less than the
     * range, divided by the time scale, so nothing jumps where the clock wraps. Lives last 1 to 2 seconds times the
     * time scale.
     */
    public static final double CLOCK_WRAP_SECONDS = 4096.0;
    public static final int LEAST_LIFE_CYCLES = 2048;
    public static final int LIFE_CYCLE_RANGE = 2049;
    /**
     * The clock's high part is a whole number of these steps of the wrap, so its product with a count of lives is exact
     * in float.
     */
    public static final int CLOCK_HIGH_STEPS = 4096;
    /**
     * A life falls at dustGlitterFallSpeed times a factor from the slowest share up by less than the range.
     */
    public static final float SLOWEST_FALL_SHARE = 0.7F;
    public static final float FALL_SHARE_RANGE = 0.6F;
    /**
     * The largest tilt of a fall from straight down. Tilts are uniform in angle up to it.
     */
    public static final float WIDEST_TILT_RADIANS = (float) Math.toRadians(30.0);
    /**
     * The median time in seconds a glint is above half its peak at a time scale of 1. dustGlitterLitSeconds over it is
     * the time scale, which stretches the lit windows, their ramps, and the lives, so the share of lit lives stays.
     */
    public static final double REFERENCE_LIT_SECONDS = 0.2;
    /**
     * A life's lit time in seconds is LIT_TIME_BASE + LIT_TIME_SLOPE h + LIT_TIME_TAIL h^LIT_TIME_TAIL_POWER for its
     * hashed h, times the time scale, so at a time scale of 1 it is above half its peak for 0.12 to 0.7 seconds, with
     * quartiles 0.16, 0.2, and 0.25.
     */
    public static final float LIT_TIME_BASE = 0.17F;
    public static final float LIT_TIME_SLOPE = 0.16F;
    public static final float LIT_TIME_TAIL = 0.42F;
    public static final float LIT_TIME_TAIL_POWER = 12.0F;
    /**
     * The seconds over which a glint lights up and goes dark at the ends of its lit window, times the time scale.
     */
    public static final float LIT_RAMP_SECONDS = 0.05F;
    /**
     * The width of the margin of a shown share over which a life's weight goes from 1 to 0.
     */
    public static final float SHOWN_MARGIN = 0.15F;
    /**
     * How far toward the phase's own color a glint on the 22 degree halo is tinted.
     */
    public static final float TINT_SHARE = 0.5F;
    /**
     * Within NEAR_DISTANCE blocks a glint is out of focus by NEAR_DISTANCE over its distance, up to WIDEST_NEAR, and
     * its light falls as the square of that. Its light fades out from NEAR_FADE_END down to NEAREST_DISTANCE.
     */
    public static final float NEAR_DISTANCE = 4.0F;
    public static final float WIDEST_NEAR = 3.0F;
    public static final float NEAREST_DISTANCE = 1.0F;
    public static final float NEAR_FADE_END = 2.0F;
    /**
     * A glint's width is dustGlitterSize at SIZE_DISTANCE blocks and goes as SIZE_DISTANCE over its distance to the
     * power dustGlitterSizeSpread, from SMALLEST_SIZE to LARGEST_SIZE times dustGlitterSize. Its light spreads over
     * its width, so its peak falls as the square of the widening.
     */
    public static final float SIZE_DISTANCE = 6.0F;
    public static final float SMALLEST_SIZE = 0.25F;
    public static final float LARGEST_SIZE = 4.0F;
    /**
     * The least radius in pixels of a glint's sprite, SPRITE_RADIUS_SIGMAS times a core's standard deviation of 0.6
     * pixels, so a small glint moving across pixels keeps its light.
     */
    public static final float SMALLEST_SPRITE_RADIUS = 2.4F;
    /**
     * The share of lives that show within NEAR_SHARE_DISTANCE blocks, rising to all of them by NEAR_DISTANCE.
     */
    public static final float NEAR_SHARE = 0.25F;
    public static final float NEAR_SHARE_DISTANCE = 3.0F;
    /**
     * The angle from the light at which the tumbling crystals' phase in green sets the 22 degree halo's phase, which
     * shows every life and tints them fully.
     */
    public static final double RING_RADIANS = Math.toRadians(22.0);
    /**
     * The angle below IceHaloMap.LOWEST_ELEVATION over which the map's lowest row fades out.
     */
    public static final double ROW_FADE_RADIANS = Math.toRadians(1.0);
    /**
     * The gain on the light of a glint at a dustGlitterBrightness of 1. A glint 6 blocks away at the peak of its lit
     * window, with a 10 degree sun through ice of optical depth 0.1, then rises 50 of 255 in green over a background of
     * 100 of 255 with the screen blend.
     */
    public static final double CALIBRATED_BRIGHTNESS = 0.5;
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
    public static final int DENSITY_HASH = 1;
    public static final int LIFE_CLOCK_HASH = 2;
    /**
     * The hash steps from a life's seed to each of its numbers, as in diamond_dust.glsl.
     */
    public static final int LIT_TIME_HASH = 0;
    public static final int LIT_START_HASH = 1;
    public static final int START_X_HASH = 2;
    public static final int SPEED_HASH = 5;
    public static final int TILT_HASH = 6;
    public static final int AZIMUTH_HASH = 7;
    public static final int DIRECTION_HASH = 8;
    public static final int NEAR_HASH = 9;
    private static final int POINT_SEED = 0x2545f491;
    private static final int LIFE_SALT = 0x6c8e9cf5;
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
     * The hash of a point's index after the given number of further steps, 0 for its seed.
     */
    public static int pointHash(int pointIndex, int step) {
        int hashed = hashOf(pointIndex ^ POINT_SEED);
        for (int index = 0; index < step; index++) {
            hashed = hashOf(hashed);
        }
        return hashed;
    }

    /**
     * The hash of a point's life after the given number of further steps, 0 for the life's seed.
     */
    public static int lifeHash(int pointIndex, int lifeNumber, int step) {
        int hashed = hashOf(pointHash(pointIndex, 0) ^ hashOf(lifeNumber ^ LIFE_SALT));
        for (int index = 0; index < step; index++) {
            hashed = hashOf(hashed);
        }
        return hashed;
    }

    public static float lifeShare(int pointIndex, int lifeNumber, int step) {
        return uniformOf(lifeHash(pointIndex, lifeNumber, step));
    }

    /**
     * How much longer than at the reference a glint is lit, from dustGlitterLitSeconds, at least 1.
     */
    public static float timeScale(double litSeconds) {
        return (float) Math.max(1.0, litSeconds / REFERENCE_LIT_SECONDS);
    }

    /**
     * The share of the reference count of lives a point lives per wrap, from dustGlitterLitSeconds, at most 1.
     */
    public static float lifeRate(double litSeconds) {
        return (float) Math.min(1.0, REFERENCE_LIT_SECONDS / litSeconds);
    }

    /**
     * The lives a point lives per wrap of the clock, from LEAST_LIFE_CYCLES up by less than LIFE_CYCLE_RANGE, times the
     * life rate and rounded down, at least 1. A multiplication, unlike a division, rounds the same in the shader.
     */
    public static int lifeCycles(int pointIndex, float lifeRate) {
        int cycles = LEAST_LIFE_CYCLES + Integer.remainderUnsigned(hashOf(pointHash(pointIndex, LIFE_CLOCK_HASH)),
                LIFE_CYCLE_RANGE);
        return Math.max(1, (int) Math.floor(cycles * lifeRate));
    }

    /**
     * Where in its life a point is when the clock is 0, from 0 up to 1.
     */
    public static float lifePhase(int pointIndex) {
        return uniformOf(pointHash(pointIndex, LIFE_CLOCK_HASH));
    }

    public static float lifeSeconds(int cycles) {
        return (float) CLOCK_WRAP_SECONDS / cycles;
    }

    /**
     * The clock's share of its wrap, from 0 up to 1.
     */
    public static double clockShare(double clockSeconds) {
        double wrapped = clockSeconds % CLOCK_WRAP_SECONDS;
        return (wrapped < 0.0 ? wrapped + CLOCK_WRAP_SECONDS : wrapped) / CLOCK_WRAP_SECONDS;
    }

    /**
     * The clock's share rounded down to a whole number of CLOCK_HIGH_STEPS.
     */
    public static float clockHigh(double clockShare) {
        return (float) (Math.floor(clockShare * CLOCK_HIGH_STEPS) / CLOCK_HIGH_STEPS);
    }

    /**
     * The clock's share less its high part, from 0 up to 1 / CLOCK_HIGH_STEPS.
     */
    public static float clockLow(double clockShare) {
        return (float) (clockShare - Math.floor(clockShare * CLOCK_HIGH_STEPS) / CLOCK_HIGH_STEPS);
    }

    /**
     * The lives counted from the wrap, whole and not, less the whole lives of the high part. Its whole part, added to
     * that of the high part's lives, gives the life's number, and its fraction is how far the life has gone.
     */
    public static float lifeRest(float clockHigh, float clockLow, int cycles, float phase) {
        float scaled = clockHigh * cycles;
        return (scaled - (float) Math.floor(scaled)) + clockLow * cycles + phase;
    }

    /**
     * The number of a point's current life, from 0 up to its cycles. The life that spans the clock's wrap keeps its
     * number.
     */
    public static int lifeNumber(float clockHigh, float clockLow, int cycles, float phase) {
        int whole = (int) Math.floor(clockHigh * cycles);
        int restWhole = (int) Math.floor(lifeRest(clockHigh, clockLow, cycles, phase));
        return Math.floorMod(whole + restWhole, cycles);
    }

    /**
     * The seconds since a point's current life began.
     */
    public static float lifeAge(float clockHigh, float clockLow, int cycles, float phase) {
        float rest = lifeRest(clockHigh, clockLow, cycles, phase);
        return (rest - (float) Math.floor(rest)) * lifeSeconds(cycles);
    }

    /**
     * A life's position along an axis, 0 to 2, at its start, from 0 up to CELL_SIZE.
     */
    public static float lifeStart(int pointIndex, int lifeNumber, int axis) {
        return lifeShare(pointIndex, lifeNumber, START_X_HASH + axis) * CELL_SIZE;
    }

    /**
     * Writes a life's velocity in blocks per second, falling at the given speed times its own factor, tilted from
     * straight down by up to WIDEST_TILT_RADIANS toward its azimuth.
     */
    public static void velocity(int pointIndex, int lifeNumber, float fallSpeed, float[] result) {
        float speed = fallSpeed * (SLOWEST_FALL_SHARE + FALL_SHARE_RANGE * lifeShare(pointIndex, lifeNumber,
                SPEED_HASH));
        float tilt = WIDEST_TILT_RADIANS * lifeShare(pointIndex, lifeNumber, TILT_HASH);
        float azimuth = 2.0F * (float) Math.PI * lifeShare(pointIndex, lifeNumber, AZIMUTH_HASH);
        float across = speed * (float) Math.sin(tilt);
        result[0] = across * (float) Math.cos(azimuth);
        result[1] = -speed * (float) Math.cos(tilt);
        result[2] = across * (float) Math.sin(azimuth);
    }

    /**
     * The length in seconds of a life's lit window for its hashed share, from 0.17 to 0.75 times the time scale. The
     * glint is above half its peak for its ramp less.
     */
    public static float litSeconds(float litTimeShare, float timeScale) {
        return timeScale * (LIT_TIME_BASE + LIT_TIME_SLOPE * litTimeShare
                + LIT_TIME_TAIL * (float) Math.pow(litTimeShare, LIT_TIME_TAIL_POWER));
    }

    /**
     * The second within its life at which a life lights up, so its lit window ends within the life.
     */
    public static float litStart(float litStartShare, float lifeSeconds, float litSeconds) {
        return litStartShare * (lifeSeconds - litSeconds);
    }

    /**
     * A life's light at an age, 1 on its lit window's flat top, rising and falling over LIT_RAMP_SECONDS times the time
     * scale at its ends, and 0 outside it.
     */
    public static float envelope(float age, float litStart, float litSeconds, float timeScale) {
        float ramp = LIT_RAMP_SECONDS * timeScale;
        return smoothstep(0.0F, ramp, age - litStart) * smoothstep(0.0F, ramp, litStart + litSeconds - age);
    }

    /**
     * The offset of the eye along an axis in its cube, from 0 to CELL_SIZE, computed in double precision.
     */
    public static double cellOffset(double coordinate) {
        return coordinate - CELL_SIZE * Math.floor(coordinate / CELL_SIZE);
    }

    /**
     * How many cubes the copy nearest the eye lies below a glint's position less the eye, along one axis, from the
     * glint's position in its cube's frame and the eye's offset in its cube.
     */
    public static float copyShift(float position, float eyeOffset) {
        return (float) Math.floor((position - eyeOffset) / CELL_SIZE + 0.5F);
    }

    /**
     * The offset from the eye of a glint's copy nearest the eye along one axis, from -CELL_SIZE / 2 to CELL_SIZE / 2.
     */
    public static float nearestOffset(float position, float eyeOffset) {
        return position - eyeOffset - CELL_SIZE * copyShift(position, eyeOffset);
    }

    /**
     * The offset from the eye along one axis of where a glint is at the middle of its lit window, from its offset now.
     * The halo, the tint, and the near share are taken there, so they hold through the lit window while the eye
     * stands still.
     */
    public static float middleOffset(float offset, float velocity, float age, float litStart, float litSeconds) {
        return offset + velocity * (litStart + 0.5F * litSeconds - age);
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
     * The weight of a life whose hashed share is given, where the given share of lives show. It is 1 below the share
     * less half of SHOWN_MARGIN, 0 above the share plus half of it, and linear between, so its mean over lives is the
     * share while the share lies at least half the margin inside 0 to 1.
     */
    public static float shownWeight(float shownShare, float hashShare) {
        return Math.max(0.0F, Math.min(1.0F, (shownShare - hashShare) / SHOWN_MARGIN + 0.5F));
    }

    /**
     * How far toward the 22 degree halo a direction is, from the largest channel of its phase over the phase on the
     * halo, up to 1.
     */
    public static float ringShare(float largestPhase, float ringPhase) {
        if (!(ringPhase > 0.0F)) {
            return 1.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, largestPhase) / ringPhase);
    }

    /**
     * The share of lives that show in a direction, the plain share away from the halos, rising to more than all of
     * them on the halos, so shownWeight is 1 for every life there.
     */
    public static float shownShare(float plainShare, float ringShare) {
        return plainShare + (1.0F + 0.5F * SHOWN_MARGIN - plainShare) * ringShare;
    }

    /**
     * A channel of a glint's tint, white away from the halos and up to TINT_SHARE of the way to the phase's own color
     * on them.
     */
    public static float tint(float channelPhase, float largestPhase, float ringShare) {
        if (!(largestPhase > 0.0F)) {
            return 1.0F;
        }
        return 1.0F + (channelPhase / largestPhase - 1.0F) * TINT_SHARE * ringShare;
    }

    /**
     * The share of lives that may show at a distance from the eye, NEAR_SHARE within NEAR_SHARE_DISTANCE and rising to
     * more than all of them by NEAR_DISTANCE, so shownWeight is 1 for every life from there on.
     */
    public static float nearShare(float distance) {
        return NEAR_SHARE + (1.0F + 0.5F * SHOWN_MARGIN - NEAR_SHARE)
                * smoothstep(NEAR_SHARE_DISTANCE, NEAR_DISTANCE, distance);
    }

    /**
     * How far out of focus a glint is at a distance, 1 from NEAR_DISTANCE on and up to WIDEST_NEAR.
     */
    public static float nearBlur(float distance) {
        return Math.max(1.0F, Math.min(WIDEST_NEAR, NEAR_DISTANCE / distance));
    }

    public static float falloff(float distance) {
        float ratio = distance / FALLOFF_DISTANCE;
        return 1.0F / (1.0F + ratio * ratio);
    }

    /**
     * The factor on a glint's whole light at a distance from the eye: the falloff, divided by the square of its blur,
     * and faded out toward NEAREST_DISTANCE.
     */
    public static float lightFactor(float distance) {
        float blur = nearBlur(distance);
        return falloff(distance) / (blur * blur) * smoothstep(NEAREST_DISTANCE, NEAR_FADE_END, distance);
    }

    /**
     * How many times dustGlitterSize wide a glint is at a distance for a size spread, before the least core width.
     */
    public static float sizeFactor(float distance, float sizeSpread) {
        float size = (float) Math.pow(SIZE_DISTANCE / distance, sizeSpread);
        return Math.max(SMALLEST_SIZE, Math.min(LARGEST_SIZE, size));
    }

    /**
     * The radius in pixels of a glint's sprite from the radius at dustGlitterSize and the size factor, at least
     * SMALLEST_SPRITE_RADIUS.
     */
    public static float spriteRadius(float baseRadius, float sizeFactor) {
        return Math.max(SMALLEST_SPRITE_RADIUS, baseRadius * sizeFactor);
    }

    /**
     * The factor on a glint's peak at a distance: its whole light spread over its sprite, whose radius is the given
     * one over the radius at dustGlitterSize.
     */
    public static float peakFactor(float distance, float spriteRadius, float baseRadius) {
        float widening = spriteRadius / baseRadius;
        return lightFactor(distance) / (widening * widening);
    }

    /**
     * The tumbling crystals' phase in green at RING_RADIANS, which shows every life.
     */
    public static double ringPhase(IceHaloProfile profile) {
        return profile.phase(SkyAtmosphere.GREEN_CHANNEL, Math.cos(RING_RADIANS), 0.0);
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
