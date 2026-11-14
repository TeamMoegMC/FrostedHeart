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
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks the reflection off plate crystals and the light pillar they show against an integral along the view ray, the
 * pillar in the diamond dust that the shader draws with its ribbons and brightness, how world changes queue the
 * sections whose lights are found again, and how the lights that show pillars are merged, colored, chosen, faded,
 * and shadowed by the blocks around them.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class LightPillarGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double TABLE_INDEX = 1.31;
    private static final double[] TABLE_GRAZING_DEGREES = {1.0, 2.0, 5.0, 10.0, 15.0, 20.0, 30.0, 45.0, 90.0};
    private static final double[] TABLE_REFLECTANCES = {0.944, 0.889, 0.728, 0.499, 0.330, 0.217, 0.101, 0.048,
            0.035};
    private static final double TABLE_TOLERANCE = 0.0006;
    private static final double[] PILLAR_ELEVATIONS_DEGREES = {3.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0, 45.0, 60.0};
    private static final double[] PILLAR_LATERAL_SPREADS = {0.0, 1.0, 2.0};
    /**
     * Horizontal distances of the light, each with the heights of the light relative to the eye.
     */
    private static final double[][] PILLAR_LIGHTS = {
            {300.0, -20.0, 0.0, 15.0},
            {32.0, -3.0, 0.0, 3.0},
            {10.0, -1.0, 0.0, 1.0}
    };
    /**
     * Covers the small tilt approximation of the across tilt, which reaches 4% two spreads off the center of the
     * pillar of wobbling plates at 60 degrees. The plates stay within 0.2%.
     */
    private static final double PILLAR_TOLERANCE = 0.05;
    /**
     * Covers the crystal count of wobbling plates at the bottom of far pillars, which reaches 23% at 3 degrees. Plates
     * stay within 1.2%.
     */
    private static final double WOBBLING_COUNT_TOLERANCE = 0.25;
    private static final int RAY_STEPS = 40000;
    private static final double RAY_WINDOWS = 60.0;
    /**
     * Horizontal distance and height of the light, then the height of the dust bottom, all relative to the eye.
     */
    private static final double[][] RIBBON_LIGHTS = {
            {4.0, 20.0, -1.62}, {8.0, -1.12, -1.62}, {32.0, -1.12, -1.62}, {32.0, 0.0, 30.0}, {128.0, -40.0, -1.62},
            {128.0, 20.0, -40.0}, {256.0, -1.12, -1.62}
    };
    /**
     * A torch, and a cluster of 16 lights of full strength such as a lava lake's.
     */
    private static final double[] RIBBON_STRENGTHS = {196.0 / 225.0, 16.0};
    private static final double RIBBON_EXTINCTION = 0.001;
    /**
     * The angle of a pixel at the center of a screen 1080 pixels tall with a vertical field of view of 70 degrees, as
     * LightPillarOptics.BRIGHTNESS is calibrated with.
     */
    private static final double REFERENCE_PIXEL_ANGLE = pixelAngle(1080);
    /**
     * Screen heights in pixels, with a vertical field of view of 70 degrees, that set the margin and blur of ribbons.
     */
    private static final int[] RIBBON_SCREEN_HEIGHTS = {720, 1080, 2160};
    /**
     * As SEGMENTS and WIDEST_RIBBON_ANGLE in light_pillars.glsl.
     */
    private static final int RIBBON_SEGMENTS = 16;
    private static final double WIDEST_RIBBON_ANGLE = 1.4;
    private static final int EDGE_STEPS = 40;
    /**
     * The near fade and the horizon, ceiling, and top of the cases of the ribbon edge test with overhangs.
     */
    private static final double[][] RIBBON_CASES = {
            {0.0, LightPillarHorizons.OPEN, 0.3, 0.6},
            {0.0, 0.1, 0.4, LightPillarHorizons.CLOSED},
            {8.0, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN},
            {8.0, LightPillarHorizons.OPEN, 0.5, LightPillarHorizons.CLOSED}
    };
    /**
     * The scene distance of directions where no scene stands.
     */
    private static final double NO_SCENE = Double.POSITIVE_INFINITY;
    /**
     * Scene distances as shares of the distance to the crystals with level faces.
     */
    private static final double[] SCENE_DISTANCE_SHARES = {0.25, 0.75, 0.95, 1.0, 1.05, 1.5, 4.0};
    private static final double[] SCENE_LATERAL_SPREADS = {0.0, 1.0};
    /**
     * A chunk radius that reaches the row of lights of the selection tests, which is about 58 chunks along x.
     */
    private static final int WIDE_CHUNK_RADIUS = 1000;
    /**
     * The floor height of the horizon scenes, above the terrain that covers the templates of the GameTest world.
     */
    private static final int HIGH_FLOOR_Y = 300;
    /**
     * Cell angles in degrees whose atlas layout the cell tests check.
     */
    private static final double[] CELL_ANGLES_DEGREES = {0.1, 0.25, 0.5, 2.0};
    /**
     * The cell angle in degrees that the cell grid tests use.
     */
    private static final double CELL_ANGLE_DEGREES = 0.25;
    /**
     * The most texels the cell atlas may have along one side, which keeps it within the largest texture of the game.
     */
    private static final int MOST_ATLAS_HEIGHT = 3072;
    /**
     * Covers the reflectance and the across Gaussian that sceneShare leaves out of the share of the pillar of plates,
     * and its near weight falling linearly in the tilt. It reaches 0.063 within the near fade of a light 10 blocks away.
     */
    private static final double PLATE_SCENE_SHARE_TOLERANCE = 0.1;
    /**
     * Covers the same for the pillar of wobbling plates, whose tilts span 40 degrees. It reaches 0.235 within the near
     * fade of a light 10 blocks away.
     */
    private static final double WOBBLING_SCENE_SHARE_TOLERANCE = 0.25;
    /**
     * Covers the fit of the logistic curve to the normal distribution that normalShare uses.
     */
    private static final double NORMAL_SHARE_TOLERANCE = 2.0e-4;
    /**
     * The scene distances of the bounds test, spaced evenly from NEAREST_SCENE_SHARE to FARTHEST_SCENE_SHARE times the
     * distance to the light.
     */
    private static final int SCENE_DISTANCE_STEPS = 200;
    private static final double NEAREST_SCENE_SHARE = 0.01;
    private static final double FARTHEST_SCENE_SHARE = 10.0;
    /**
     * A scene distance just in front of the eye, where the share of the pillar in front of the scene is nearly 0.
     */
    private static final double NEAREST_SCENE_DISTANCE = 1.0e-6;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void iceMirrorReflectsLikeASlab(GameTestHelper helper) {
        for (int row = 0; row < TABLE_GRAZING_DEGREES.length; row++) {
            double reflectance = IceMirror.slabReflectance(Math.sin(Math.toRadians(TABLE_GRAZING_DEGREES[row])),
                    TABLE_INDEX);
            helper.assertTrue(Math.abs(reflectance - TABLE_REFLECTANCES[row]) <= TABLE_TOLERANCE,
                    "Slab reflectance at " + TABLE_GRAZING_DEGREES[row] + " degrees is " + reflectance + " instead of "
                            + TABLE_REFLECTANCES[row]);
        }
        double face = Math.pow((TABLE_INDEX - 1.0) / (TABLE_INDEX + 1.0), 2.0);
        double normal = IceMirror.slabReflectance(1.0, TABLE_INDEX);
        helper.assertTrue(Math.abs(normal - 2.0 * face / (1.0 + face)) <= 1.0e-12,
                "Slab reflectance at normal incidence is " + normal + " instead of " + 2.0 * face / (1.0 + face));
        helper.assertTrue(Math.abs(IceMirror.slabReflectance(0.0, TABLE_INDEX) - 1.0) <= 1.0e-12,
                "Slab reflectance at grazing incidence is " + IceMirror.slabReflectance(0.0, TABLE_INDEX)
                        + " instead of 1");
        double previous = 1.0;
        for (double degrees = 0.25; degrees <= 90.0; degrees += 0.25) {
            double reflectance = IceMirror.slabReflectance(Math.sin(Math.toRadians(degrees)), TABLE_INDEX);
            helper.assertTrue(reflectance <= previous,
                    "Slab reflectance rises to " + reflectance + " at " + degrees + " degrees");
            previous = reflectance;
        }
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            int steps = 20000;
            double step = 0.5 * Math.PI / steps;
            double total = 0.0;
            for (int index = 0; index < steps; index++) {
                double tilt = (index + 0.5) * step;
                total += IceMirror.tiltDensity(tilt, tiltSigma) * 2.0 * Math.PI * Math.sin(tilt) * step;
            }
            helper.assertTrue(Math.abs(total - 1.0) <= 0.005,
                    "Tilt density of " + crystal.name() + " integrates to " + total + " over the half sphere");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarGeometryMirrorsTheLight(GameTestHelper helper) {
        for (double[] light : PILLAR_LIGHTS) {
            double horizontalDistance = light[0];
            for (int height = 1; height < light.length; height++) {
                double lightHeight = light[height];
                for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                    double elevation = Math.toRadians(elevationDegrees);
                    double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance, lightHeight);
                    if (crystalHeight <= Math.max(0.0, lightHeight)) {
                        continue;
                    }
                    String where = "light " + horizontalDistance + " away and " + lightHeight + " high, elevation "
                            + elevationDegrees + " degrees";
                    double back = LightPillarOptics.mirrorElevation(crystalHeight, horizontalDistance, lightHeight);
                    helper.assertTrue(Math.abs(back - elevation) <= 1.0e-12,
                            "Elevation from the mirror height is " + Math.toDegrees(back) + " degrees for " + where);
                    double crystalDistance = LightPillarOptics.crystalDistance(crystalHeight, horizontalDistance,
                            lightHeight);
                    double viewElevation = Math.atan2(crystalHeight, crystalDistance);
                    double lightDepression = Math.atan2(crystalHeight - lightHeight,
                            horizontalDistance - crystalDistance);
                    helper.assertTrue(Math.abs(viewElevation - elevation) <= 1.0e-12,
                            "Crystals are at elevation " + Math.toDegrees(viewElevation) + " degrees for " + where);
                    helper.assertTrue(Math.abs(lightDepression - elevation) <= 1.0e-12,
                            "Crystals see the light " + Math.toDegrees(lightDepression) + " degrees down for " + where);
                }
            }
        }
        helper.assertTrue(LightPillarOptics.radiance(Math.toRadians(2.0), 0.0, 100.0, 10.0, 0.01, TABLE_INDEX) == 0.0,
                "A pillar shows below the light");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void lightPillarMatchesTheRayIntegral(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double worst = -1.0;
        String worstWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            double crystalWorst = -1.0;
            String crystalWorstWhere = "nowhere";
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : PILLAR_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            double expected = rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight,
                                    tiltSigma, refractiveIndex, NO_SCENE, LightPillarHorizons.OPEN);
                            double actual = LightPillarOptics.radiance(elevation, lateralAngle, horizontalDistance,
                                    lightHeight, tiltSigma, refractiveIndex);
                            double difference = Math.abs(actual / expected - 1.0);
                            if (!(difference <= crystalWorst)) {
                                crystalWorst = difference;
                                crystalWorstWhere = String.format(Locale.ROOT,
                                        "light %.0f away and %.0f high, elevation %.0f degrees, %.0f spreads off "
                                                + "the center, %.4g against %.4g",
                                        horizontalDistance, lightHeight, elevationDegrees, spreads, actual, expected);
                            }
                        }
                    }
                }
            }
            LOGGER.info("Light pillar of " + crystal.name() + " differs from the ray integral by at most "
                    + crystalWorst + " at " + crystalWorstWhere);
            if (!(crystalWorst <= worst)) {
                worst = crystalWorst;
                worstWhere = crystal.name() + ", " + crystalWorstWhere;
            }
        }
        helper.assertTrue(worst <= PILLAR_TOLERANCE,
                "Light pillar differs from the ray integral by " + worst + " at " + worstWhere + ", more than "
                        + PILLAR_TOLERANCE);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void lightPillarCountsOnlyCrystalsInFrontOfTheScene(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double worst = -1.0;
        String worstWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double mirrorDistance = crystalHeight / Math.sin(elevation);
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : SCENE_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            double whole = rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight,
                                    tiltSigma, refractiveIndex, NO_SCENE, LightPillarHorizons.OPEN);
                            for (double share : SCENE_DISTANCE_SHARES) {
                                double sceneDistance = share * mirrorDistance;
                                double expected = rayIntegral(elevation, lateralAngle, horizontalDistance,
                                        lightHeight, tiltSigma, refractiveIndex, sceneDistance,
                                        LightPillarHorizons.OPEN);
                                double actual = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9, 0.0,
                                        sceneDistance, LightPillarHorizons.OPEN);
                                double difference = Math.abs(actual - expected) / whole;
                                if (!(difference <= worst)) {
                                    worst = difference;
                                    worstWhere = String.format(Locale.ROOT,
                                            "%s, light %.0f away and %.0f high, elevation %.0f degrees, %.0f spreads "
                                                    + "off the center, scene at %.2f of the crystals, %.4g against "
                                                    + "%.4g of %.4g",
                                            crystal.name(), horizontalDistance, lightHeight, elevationDegrees, spreads,
                                            share, actual, expected, whole);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Light pillar in front of a scene differs from the ray integral by at most " + worst
                + " of the whole pillar, at " + worstWhere);
        helper.assertTrue(worst <= PILLAR_TOLERANCE, "Light pillar in front of a scene differs from the ray integral by "
                + worst + " of the whole pillar, at " + worstWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarLightPassesBelowTheViewRayBeforeTheLight(GameTestHelper helper) {
        int checked = 0;
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    double lightElevation = Math.atan2(lightHeight, horizontalDistance);
                    double lightDistance = Math.hypot(horizontalDistance, lightHeight);
                    for (double elevationDegrees = -60.0; elevationDegrees <= 85.0; elevationDegrees += 0.5) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double lowestTilt = Math.max(-4.0 * tiltSigma, elevation - 0.5 * Math.PI);
                        double highestTilt = Math.min(4.0 * tiltSigma, 0.5 * (elevation + lightElevation));
                        if (!(Math.sin(elevation - lightElevation) > 0.0) || !(highestTilt > lowestTilt)) {
                            continue;
                        }
                        for (int step = 1; step < 20; step++) {
                            double alongTilt = lowestTilt + (highestTilt - lowestTilt) * step / 20.0;
                            double lightDepression = elevation - 2.0 * alongTilt;
                            double eyeLeg = lightDistance * Math.sin(lightDepression + lightElevation)
                                    / Math.sin(elevation + lightDepression);
                            double crystalX = eyeLeg * Math.cos(elevation);
                            double crystalY = eyeLeg * Math.sin(elevation);
                            if (!(crystalX < horizontalDistance)) {
                                continue;
                            }
                            double towardLight = Math.atan2(lightHeight - crystalY, horizontalDistance - crystalX);
                            boolean mirrored = Math.abs(towardLight + lightDepression) <= 1.0e-9;
                            boolean below = true;
                            for (double fraction : new double[]{0.25, 0.5, 0.75}) {
                                double between = crystalX + fraction * (horizontalDistance - crystalX);
                                double rayHeight = between * Math.tan(elevation);
                                double legHeight = crystalY + fraction * (lightHeight - crystalY);
                                below &= legHeight < rayHeight;
                            }
                            if (!mirrored || !below) {
                                helper.fail(String.format(Locale.ROOT, "The light %s for %s, light %.0f away and %.0f "
                                                + "high, elevation %.1f degrees, tilt %.2f degrees",
                                        mirrored ? "passes above the view ray on its way to the crystals"
                                                : "is not where the crystals see it", crystal.name(), horizontalDistance,
                                        lightHeight, elevationDegrees, Math.toDegrees(alongTilt)));
                            }
                            checked++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(checked > 1000, "Only " + checked + " crystals were checked");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarInDustReducesToTheUniformPillar(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        for (double spreads : PILLAR_LATERAL_SPREADS) {
                            double lateralAngle = spreads * tiltSigma * Math.sin(elevation);
                            double uniform = LightPillarOptics.radiance(elevation, lateralAngle, horizontalDistance,
                                    lightHeight, tiltSigma, refractiveIndex);
                            double inDust = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                    horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9, 0.0,
                                    NO_SCENE, LightPillarHorizons.OPEN);
                            helper.assertTrue(Math.abs(inDust - uniform) <= 1.0e-9 * uniform,
                                    "The pillar in dust of the same density everywhere is " + inDust + " instead of "
                                            + uniform + " for " + crystal.name() + ", light " + horizontalDistance
                                            + " away and " + lightHeight + " high, elevation " + elevationDegrees);
                            double dimmed = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                    horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, -1.62, 0.001,
                                    NO_SCENE, LightPillarHorizons.OPEN);
                            helper.assertTrue(dimmed <= uniform, "Thinning dust and extinction brighten the pillar");
                        }
                    }
                }
            }
        }
        double scaleHeight = LightPillarSelection.DUST_SCALE_HEIGHT;
        double upward = LightPillarOptics.dustOpticalDepth(-2.0, -2.0 + scaleHeight, scaleHeight, 0.001, -2.0);
        helper.assertTrue(Math.abs(upward - 0.001 * scaleHeight * (1.0 - Math.exp(-1.0))) <= 1.0e-12,
                "The optical depth up through one scale height is " + upward);
        double level = LightPillarOptics.dustOpticalDepth(-5.0, -5.0, 100.0, 0.001, -2.0);
        helper.assertTrue(Math.abs(level - 0.1) <= 1.0e-12,
                "The optical depth of a level path below the dust bottom is " + level);
        double downward = LightPillarOptics.dustOpticalDepth(-2.0 + scaleHeight, -2.0, scaleHeight, 0.001, -2.0);
        helper.assertTrue(Math.abs(downward - upward) <= 1.0e-12, "The optical depth depends on the direction");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lightPillarNearWeightFadesFromAQuarter(GameTestHelper helper) {
        for (double distance : new double[]{0.0, 1.0, 2.0}) {
            double weight = LightPillarOptics.nearWeight(distance, 8.0);
            helper.assertTrue(weight == 0.0, "The near weight at " + distance + " blocks is " + weight
                    + " instead of 0");
        }
        for (double distance : new double[]{8.0, 9.0, 100.0}) {
            double weight = LightPillarOptics.nearWeight(distance, 8.0);
            helper.assertTrue(weight == 1.0, "The near weight at " + distance + " blocks is " + weight
                    + " instead of 1");
        }
        assertNearWeight(helper, 5.0, 8.0, 0.5);
        assertNearWeight(helper, 3.5, 8.0, 0.15625);
        double previous = 0.0;
        for (int step = 0; step <= 200; step++) {
            double distance = 0.05 * step;
            double weight = LightPillarOptics.nearWeight(distance, 8.0);
            helper.assertTrue(weight >= previous, "The near weight falls from " + previous + " to " + weight + " at "
                    + distance + " blocks");
            previous = weight;
        }
        for (double distance : new double[]{0.0, 0.5, 100.0}) {
            double weight = LightPillarOptics.nearWeight(distance, 0.0);
            helper.assertTrue(weight == 1.0, "Without a near fade the near weight at " + distance + " blocks is "
                    + weight + " instead of 1");
        }

        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        IceCrystalData.Crystal plates = plateWithTilt(helper, 0.7);
        IceCrystalData.Crystal wobblingPlates = plateWithTilt(helper, 5.0);
        for (double elevationDegrees : new double[]{16.0, 20.0, 24.0, 28.0}) {
            double elevation = Math.toRadians(elevationDegrees);
            double fadedPlates = nearRadiance(plates, elevation, 3.0, -1.12, refractiveIndex, 8.0);
            helper.assertTrue(fadedPlates == 0.0, "The plates of a light 3 blocks away and 1.12 below the eye show "
                    + fadedPlates + " at " + elevationDegrees + " degrees with a near fade of 8 blocks instead of 0");
            double fadedWobbling = nearRadiance(wobblingPlates, elevation, 3.0, -1.12, refractiveIndex, 8.0);
            double wholeWobbling = nearRadiance(wobblingPlates, elevation, 3.0, -1.12, refractiveIndex, 0.0);
            helper.assertTrue(fadedWobbling <= 0.01 * wholeWobbling, "The wobbling plates of a light 3 blocks away "
                    + "and 1.12 below the eye show " + fadedWobbling + " at " + elevationDegrees + " degrees with a "
                    + "near fade of 8 blocks instead of at most " + 0.01 * wholeWobbling);
        }
        double unfadedPlates = nearRadiance(plates, Math.toRadians(24.0), 3.0, -1.12, refractiveIndex, 0.0);
        helper.assertTrue(unfadedPlates > 0.0, "The plates of a light 3 blocks away and 1.12 below the eye show "
                + unfadedPlates + " at 24 degrees without a near fade instead of more than 0");
        // Wobbling plates reach crystals near the eye with their most tilted faces, so only plates stay the same:
        for (double elevationDegrees : new double[]{15.0, 20.0, 30.0, 45.0, 60.0}) {
            double elevation = Math.toRadians(elevationDegrees);
            double faded = nearRadiance(plates, elevation, 300.0, -20.0, refractiveIndex, 8.0);
            double whole = nearRadiance(plates, elevation, 300.0, -20.0, refractiveIndex, 0.0);
            helper.assertTrue(faded == whole, "The plates of a light 300 blocks away and 20 below the eye show "
                    + faded + " at " + elevationDegrees + " degrees with a near fade of 8 blocks instead of the "
                    + whole + " they show without one");
        }
        // The Gauss rule over the shorter tilt range of the near fade can come out up to about 1e-5 higher:
        for (IceCrystalData.Crystal crystal : plates()) {
            for (double[] light : PILLAR_LIGHTS) {
                for (int height = 1; height < light.length; height++) {
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double faded = nearRadiance(crystal, elevation, light[0], light[height], refractiveIndex, 8.0);
                        double whole = nearRadiance(crystal, elevation, light[0], light[height], refractiveIndex, 0.0);
                        helper.assertTrue(faded <= whole * (1.0 + 1.0e-4), "The " + crystal.name() + " of light " + light[0]
                                + " away and " + light[height] + " high show " + faded + " at " + elevationDegrees
                                + " degrees with a near fade of 8 blocks instead of at most the " + whole
                                + " they show without one");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void lightPillarRibbonsHoldTheVisibleLight(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double lateralBlur = LightPillarOptics.lateralBlur(REFERENCE_PIXEL_ANGLE);
        double visible = LightPillarOptics.VISIBLE_VALUE;
        List<IceCrystalData.Crystal> plates = plates();
        double worstBlurChange = 0.0;
        double worstOutside = 0.0;
        String worstOutsideWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            double share = IceCrystalData.get().shareOf(crystal);
            for (double[] light : RIBBON_LIGHTS) {
                double horizontalDistance = light[0];
                double lightHeight = light[1];
                double dustBottomHeight = light[2];
                for (double elevationDegrees : new double[]{5.0, 10.0, 20.0, 40.0, 70.0}) {
                    double elevation = Math.toRadians(elevationDegrees);
                    double width = LightPillarOptics.widestLateralSpread(elevation, horizontalDistance, lightHeight,
                            tiltSigma, lateralBlur, LightPillarHorizons.OPEN);
                    if (width > 0.0) {
                        double sharp = lateralIntegral(elevation, horizontalDistance, lightHeight, tiltSigma,
                                refractiveIndex, 0.0, dustBottomHeight, 10.0 * width);
                        double blurred = lateralIntegral(elevation, horizontalDistance, lightHeight, tiltSigma,
                                refractiveIndex, lateralBlur, dustBottomHeight, 10.0 * width);
                        helper.assertTrue(sharp > 0.0, "A pillar with a width has no light across it");
                        worstBlurChange = Math.max(worstBlurChange, Math.abs(blurred / sharp - 1.0));
                    }
                }
                for (double strength : RIBBON_STRENGTHS) {
                    double scale = LightPillarOptics.BRIGHTNESS * strength * share * RIBBON_EXTINCTION;
                    double lowest = LightPillarOptics.lowestRibbonElevation(horizontalDistance, lightHeight,
                            tiltSigma, LightPillarHorizons.OPEN);
                    for (double elevationDegrees = -45.0; elevationDegrees < 89.5; elevationDegrees += 0.5) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double center = scale * LightPillarOptics.dustRadiance(elevation, 0.0, horizontalDistance,
                                lightHeight, tiltSigma, refractiveIndex, lateralBlur, dustBottomHeight,
                                RIBBON_EXTINCTION, NO_SCENE, LightPillarHorizons.OPEN);
                        double halfWidth = elevation < lowest || elevation > LightPillarOptics.HIGHEST_RIBBON_ELEVATION
                                ? 0.0 : LightPillarOptics.ribbonHalfWidth(center, visible,
                                LightPillarOptics.widestLateralSpread(elevation, horizontalDistance, lightHeight,
                                        tiltSigma, lateralBlur, LightPillarHorizons.OPEN));
                        for (double lateralDegrees = Math.toDegrees(halfWidth) + 0.01; lateralDegrees < 60.0;
                                lateralDegrees += 0.05) {
                            double value = scale * LightPillarOptics.dustRadiance(elevation,
                                    Math.toRadians(lateralDegrees), horizontalDistance, lightHeight, tiltSigma,
                                    refractiveIndex, lateralBlur, dustBottomHeight, RIBBON_EXTINCTION, NO_SCENE,
                                    LightPillarHorizons.OPEN);
                            if (value > worstOutside) {
                                worstOutside = value;
                                worstOutsideWhere = String.format(Locale.ROOT,
                                        "%s of strength %.2f, light %.0f away and %.2f high, dust bottom %.2f, "
                                                + "elevation %.1f, %.2f degrees off the center",
                                        crystal.name(), strength, horizontalDistance, lightHeight, dustBottomHeight,
                                        elevationDegrees, lateralDegrees);
                            }
                            if (value < 1.0e-3 * visible) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Light pillar blur changes the light across a pillar by at most " + worstBlurChange
                + ", and outside the ribbons a pillar shows at most " + worstOutside / visible
                + " times the visible value, at " + worstOutsideWhere);
        helper.assertTrue(worstBlurChange <= 0.005,
                "The lateral blur changes the light across a pillar by " + worstBlurChange);
        helper.assertTrue(worstOutside <= 1.01 * visible, "A pillar shows " + worstOutside / visible
                + " times the visible value outside its ribbon, at " + worstOutsideWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lightPillarRibbonEdgesHoldTheVisibleLight(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double visible = LightPillarOptics.VISIBLE_VALUE;
        double worst = 0.0;
        String worstWhere = "nowhere";
        for (int screenHeight : RIBBON_SCREEN_HEIGHTS) {
            double pixelAngle = pixelAngle(screenHeight);
            double lateralBlur = LightPillarOptics.lateralBlur(pixelAngle);
            for (IceCrystalData.Crystal crystal : plates()) {
                double tiltSigma = Math.toRadians(crystal.tiltDegrees());
                double share = IceCrystalData.get().shareOf(crystal);
                for (double[] light : RIBBON_LIGHTS) {
                    for (double strength : RIBBON_STRENGTHS) {
                        double scale = LightPillarOptics.BRIGHTNESS * strength * share * RIBBON_EXTINCTION;
                        double[][] corners = ribbonEdge(light[0], light[1], light[2], tiltSigma, refractiveIndex,
                                lateralBlur, pixelAngle, scale, LightPillarHorizons.OPEN);
                        if (corners == null) {
                            continue;
                        }
                        for (int segment = 0; segment < RIBBON_SEGMENTS; segment++) {
                            double[] below = corners[segment];
                            double[] above = corners[segment + 1];
                            for (int step = 0; step <= EDGE_STEPS; step++) {
                                double fraction = (double) step / EDGE_STEPS;
                                double x = below[0] + (above[0] - below[0]) * fraction;
                                double y = below[1] + (above[1] - below[1]) * fraction;
                                double z = below[2] + (above[2] - below[2]) * fraction;
                                double length = Math.sqrt(x * x + y * y + z * z);
                                double elevation = Math.asin(y / length);
                                double lateralAngle = Math.asin(z / length);
                                double value = scale * LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        light[0], light[1], tiltSigma, refractiveIndex, lateralBlur, light[2],
                                        RIBBON_EXTINCTION, NO_SCENE, LightPillarHorizons.OPEN);
                                if (value > worst) {
                                    worst = value;
                                    worstWhere = String.format(Locale.ROOT,
                                            "%s of strength %.2f, light %.0f away and %.2f high, dust bottom %.2f, "
                                                    + "screen %d pixels tall, elevation %.2f, %.3f degrees off the "
                                                    + "center", crystal.name(), strength, light[0], light[1],
                                            light[2], screenHeight, Math.toDegrees(elevation),
                                            Math.toDegrees(lateralAngle));
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("On the edges of light pillar ribbons a pillar shows at most " + worst / visible
                + " times the visible value, at " + worstWhere);
        helper.assertTrue(worst <= 1.01 * visible, "A pillar shows " + worst / visible
                + " times the visible value on the edge of its ribbon, at " + worstWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarBrightnessIsCalibratedOnATorch(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double lateralBlur = LightPillarOptics.lateralBlur(REFERENCE_PIXEL_ANGLE);
        double radiance = 0.0;
        for (IceCrystalData.Crystal crystal : plates()) {
            radiance += IceCrystalData.get().shareOf(crystal) * 0.001 * LightPillarOptics.dustRadiance(
                    Math.toRadians(10.0), 0.0, 32.0, -1.12, Math.toRadians(crystal.tiltDegrees()), refractiveIndex,
                    lateralBlur, -1.62, 0.001, NO_SCENE, LightPillarHorizons.OPEN);
        }
        double value = LightPillarOptics.BRIGHTNESS * LightPillarEmitters.strength(14) * radiance;
        double shown = 1.0 - Math.exp(-value);
        helper.assertTrue(Math.abs(shown - 0.1) <= 0.001,
                "The calibrated torch's pillar shows " + shown + " of the display at 10 degrees instead of 0.1");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightClustersMergeLightsPerCell(GameTestHelper helper) {
        LightPillarClusters clusters = new LightPillarClusters();
        clusters.beginSection(2, 4, -1);
        clusters.addLight(33, 65, -15, 65.625, 1.0F, 0.5F, 0.25F);
        clusters.addLight(35, 66, -13, 67.0, 0.5F, 0.25F, 0.125F);
        clusters.addLight(40, 65, -15, 65.5, 0.0F, 0.0F, 1.0F);
        clusters.endSection();
        helper.assertTrue(clusters.count() == 2, "The finished section has " + clusters.count() + " clusters, not 2");
        int merged = clusterWithKey(clusters, BlockPos.asLong(8, 16, -4));
        int single = clusterWithKey(clusters, BlockPos.asLong(10, 16, -4));
        helper.assertTrue(merged >= 0 && single >= 0, "The clusters do not have the keys of their cells");
        double firstWeight = 1.75;
        double secondWeight = 0.875;
        double expectedX = (firstWeight * 33.5 + secondWeight * 35.5) / (firstWeight + secondWeight);
        double expectedY = (firstWeight * 65.5 + secondWeight * 66.5) / (firstWeight + secondWeight);
        double expectedZ = (firstWeight * -14.5 + secondWeight * -12.5) / (firstWeight + secondWeight);
        helper.assertTrue(Math.abs(clusters.x(merged) - expectedX) < 1.0e-9
                        && Math.abs(clusters.y(merged) - expectedY) < 1.0e-9
                        && Math.abs(clusters.z(merged) - expectedZ) < 1.0e-9,
                "The merged cluster is centered at " + clusters.x(merged) + " " + clusters.y(merged) + " "
                        + clusters.z(merged) + " instead of " + expectedX + " " + expectedY + " " + expectedZ);
        helper.assertTrue(clusters.red(merged) == 1.5F && clusters.green(merged) == 0.75F
                        && clusters.blue(merged) == 0.375F,
                "The merged cluster has the strengths " + clusters.red(merged) + " " + clusters.green(merged) + " "
                        + clusters.blue(merged) + " instead of 1.5 0.75 0.375");
        helper.assertTrue(clusters.x(single) == 40.5 && clusters.y(single) == 65.5 && clusters.z(single) == -14.5,
                "A single light's cluster is not centered on its block");
        helper.assertTrue(clusters.top(merged) == 67.0,
                "The merged cluster's top is " + clusters.top(merged) + " instead of 67.0");
        helper.assertTrue(clusters.top(single) == 65.5,
                "The single cluster's top is " + clusters.top(single) + " instead of 65.5");

        clusters.beginSection(3, 4, -1);
        clusters.addLight(56, 70, -10, 71.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        long otherKey = BlockPos.asLong(14, 17, -3);
        helper.assertTrue(clusters.count() == 3 && clusters.sectionCount() == 2,
                "A second section gives " + clusters.count() + " clusters in " + clusters.sectionCount()
                        + " sections instead of 3 in 2");
        helper.assertTrue(clusterWithKey(clusters, otherKey) >= 0,
                "The second section has no cluster with the key " + BlockPos.of(otherKey).toShortString());
        clusters.beginSection(2, 4, -1);
        clusters.addLight(33, 65, -15, 66.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        helper.assertTrue(clusterWithKey(clusters, BlockPos.asLong(10, 16, -4)) < 0,
                "The old cluster of the cell " + BlockPos.of(BlockPos.asLong(10, 16, -4)).toShortString()
                        + " is still there after its section was begun again");
        int replaced = clusterWithKey(clusters, BlockPos.asLong(8, 16, -4));
        helper.assertTrue(replaced >= 0, "The cell " + BlockPos.of(BlockPos.asLong(8, 16, -4)).toShortString()
                + " has no cluster after its section was begun again");
        helper.assertTrue(clusters.x(replaced) == 33.5 && clusters.y(replaced) == 65.5 && clusters.z(replaced) == -14.5,
                "The replaced cluster is centered at " + clusters.x(replaced) + " " + clusters.y(replaced) + " "
                        + clusters.z(replaced) + " instead of 33.5 65.5 -14.5");
        helper.assertTrue(clusters.red(replaced) == 1.0F && clusters.green(replaced) == 1.0F
                        && clusters.blue(replaced) == 1.0F,
                "The replaced cluster has the strengths " + clusters.red(replaced) + " " + clusters.green(replaced) + " "
                        + clusters.blue(replaced) + " instead of 1 1 1");
        helper.assertTrue(clusterWithKey(clusters, otherKey) >= 0,
                "The cluster of the other section went away with the section begun again");
        helper.assertTrue(clusters.count() == 2 && clusters.sectionCount() == 2,
                "After the section was begun again there are " + clusters.count() + " clusters in "
                        + clusters.sectionCount() + " sections instead of 2 in 2");

        clusters.beginSection(2, 4, -1);
        clusters.endSection();
        helper.assertTrue(clusterWithKey(clusters, BlockPos.asLong(8, 16, -4)) < 0,
                "A section begun again without lights keeps its cluster");
        helper.assertTrue(clusterWithKey(clusters, otherKey) >= 0,
                "A section begun again without lights removed the cluster of another section");
        helper.assertTrue(clusters.count() == 1 && clusters.sectionCount() == 1,
                "A section begun again without lights leaves " + clusters.count() + " clusters in "
                        + clusters.sectionCount() + " sections instead of 1 in 1");

        LightPillarClusters column = new LightPillarClusters();
        for (int sectionY = -4; sectionY <= 3; sectionY++) {
            column.beginSection(5, sectionY, 7);
            column.addLight(5 * 16 + 8, sectionY * 16 + 8, 7 * 16 + 8, sectionY * 16 + 9.0, 1.0F, 1.0F, 1.0F);
            column.endSection();
        }
        column.beginSection(6, 0, 7);
        column.addLight(6 * 16 + 8, 8, 7 * 16 + 8, 9.0, 1.0F, 1.0F, 1.0F);
        column.endSection();
        helper.assertTrue(column.count() == 9 && column.sectionCount() == 9,
                "The column and its neighbor give " + column.count() + " clusters in " + column.sectionCount()
                        + " sections instead of 9 in 9");
        long neighborKey = BlockPos.asLong(26, 2, 30);
        for (int sectionY = -4; sectionY <= 3; sectionY++) {
            column.removeSection(5, sectionY, 7);
            assertDistinctKeys(helper, column, " after removing the section at y " + sectionY + " of the column");
            int remaining = 4 - sectionY;
            for (int keptY = sectionY + 1; keptY <= 3; keptY++) {
                long keptKey = BlockPos.asLong(22, 4 * keptY + 2, 30);
                helper.assertTrue(clusterWithKey(column, keptKey) >= 0,
                        "The cluster " + BlockPos.of(keptKey).toShortString() + " went away with the section at y "
                                + sectionY + " of the column");
            }
            helper.assertTrue(clusterWithKey(column, neighborKey) >= 0,
                    "The cluster of the neighboring column went away with the section at y " + sectionY);
            helper.assertTrue(column.count() == remaining && column.sectionCount() == remaining,
                    "After removing the section at y " + sectionY + " of the column there are " + column.count()
                            + " clusters in " + column.sectionCount() + " sections instead of " + remaining + " in "
                            + remaining);
        }
        for (int sectionY = -4; sectionY <= 3; sectionY++) {
            column.beginSection(5, sectionY, 7);
            column.addLight(5 * 16 + 8, sectionY * 16 + 8, 7 * 16 + 8, sectionY * 16 + 9.0, 1.0F, 1.0F, 1.0F);
            column.endSection();
        }
        helper.assertTrue(column.count() == 9, "The filled column has " + column.count() + " clusters, not 9");
        column.removeColumn(5, 7, -4, 24);
        assertDistinctKeys(helper, column, " after removing the column");
        helper.assertTrue(column.count() == 1 && column.sectionCount() == 1,
                "Removing the column leaves " + column.count() + " clusters in " + column.sectionCount()
                        + " sections instead of 1 in 1");
        helper.assertTrue(clusterWithKey(column, neighborKey) >= 0,
                "Removing the column removed the cluster of the neighboring column");
        for (int sectionY = -4; sectionY <= 3; sectionY++) {
            long removedKey = BlockPos.asLong(22, 4 * sectionY + 2, 30);
            helper.assertTrue(clusterWithKey(column, removedKey) < 0, "Removing the column kept the cluster "
                    + BlockPos.of(removedKey).toShortString() + " of its section at y " + sectionY);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarSelectionKeepsTheBrightestAndFades(GameTestHelper helper) {
        double near = LightPillarSelection.score(1.0, 1.0, 1.0, 10.0, 0.0, 0.0, 1000.0);
        double far = LightPillarSelection.score(1.0, 1.0, 1.0, 20.0, 0.0, 0.0, 1000.0);
        helper.assertTrue(Math.abs(near / far - 2.0) < 1.0e-12, "Scores do not fall with the distance: " + near
                + " at 10 blocks and " + far + " at 20");
        helper.assertTrue(LightPillarSelection.score(1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1000.0)
                        == LightPillarSelection.score(1.0, 1.0, 1.0, LightPillarSelection.NEAREST_SCORED_DISTANCE, 0.0,
                        0.0, 1000.0),
                "A light next to the eye scores differently from one at the nearest scored distance");
        double inDust = LightPillarSelection.score(1.0, 1.0, 1.0, 100.0, 0.0, 0.0, 0.0);
        double crystalHeight = 0.5 * 100.0 * Math.tan(Math.toRadians(10.0));
        double expectedDensity = Math.exp(-crystalHeight / LightPillarSelection.DUST_SCALE_HEIGHT);
        helper.assertTrue(Math.abs(inDust / (expectedDensity / 100.0) - 1.0) < 1.0e-12,
                "A light 100 blocks away scores " + inDust + " instead of " + expectedDensity / 100.0);
        helper.assertTrue(LightPillarSelection.score(1.0, 1.0, 1.0, 100.0, 40.0, 0.0, 0.0) < inDust,
                "A light 40 blocks above the eye scores as high as one at the eye");

        int lightCount = LightPillarSelection.SLOT_COUNT + 10;
        LightPillarClusters clusters = new LightPillarClusters();
        addLightsInARow(clusters, lightCount, -1);
        LightPillarSelection selection = new LightPillarSelection();
        selection.choose(clusters, 0.0, 0.5, 0.5, 1000.0, WIDE_CHUNK_RADIUS);
        helper.assertTrue(selection.chosenCount() == LightPillarSelection.SLOT_COUNT
                        && selection.usedCount() == LightPillarSelection.SLOT_COUNT,
                "The selection chose " + selection.chosenCount() + " and uses " + selection.usedCount() + " slots");
        for (int light = 0; light < lightCount; light++) {
            boolean shouldBeChosen = light < LightPillarSelection.SLOT_COUNT;
            helper.assertTrue((slotWithKey(selection, rowKey(light)) >= 0) == shouldBeChosen,
                    "Light " + light + " of the row is " + (shouldBeChosen ? "not chosen" : "chosen"));
        }
        int nearestSlot = slotWithKey(selection, rowKey(0));
        float fadeStep = 1.0F / LightPillarSelection.FADE_TICKS;
        helper.assertTrue(!selection.isReady(nearestSlot) && selection.fade(nearestSlot, 1.0F) == 0.0F,
                "A new slot is ready or fades before its horizons are built");
        for (int slot = 0; slot < LightPillarSelection.SLOT_COUNT; slot++) {
            if (selection.isUsed(slot)) {
                selection.setReady(slot, true);
            }
        }
        stepSelection(selection, clusters, 1);
        helper.assertTrue(selection.fade(nearestSlot, 1.0F) == fadeStep && selection.fade(nearestSlot, 0.0F) == 0.0F
                        && Math.abs(selection.fade(nearestSlot, 0.5F) - 0.5F * fadeStep) < 1.0e-6F,
                "A ready slot does not start its fade from 0");
        stepSelection(selection, clusters, LightPillarSelection.FADE_TICKS);
        helper.assertTrue(selection.fade(nearestSlot, 1.0F) == 1.0F, "A chosen slot does not fade in fully");

        LightPillarClusters withoutNearest = new LightPillarClusters();
        addLightsInARow(withoutNearest, lightCount, 0);
        int stepsToFreeASlot = LightPillarSelection.FADE_TICKS;
        stepSelection(selection, withoutNearest, 1);
        long nextKey = rowKey(LightPillarSelection.SLOT_COUNT);
        helper.assertTrue(selection.isUsed(nearestSlot) && selection.key(nearestSlot) == rowKey(0)
                        && slotWithKey(selection, nextKey) < 0,
                "A newly chosen light did not wait while every slot is used");
        stepSelection(selection, withoutNearest, stepsToFreeASlot - 1);
        helper.assertTrue(!selection.isUsed(nearestSlot) && slotWithKey(selection, nextKey) < 0,
                "The slot of a light that went away is not free once it faded out");
        stepSelection(selection, withoutNearest, 1);
        helper.assertTrue(slotWithKey(selection, nextKey) == nearestSlot && !selection.isReady(nearestSlot)
                        && selection.fade(nearestSlot, 1.0F) == 0.0F,
                "The waiting light did not take the freed slot unready and at fade 0");
        selection.setReady(nearestSlot, true);
        stepSelection(selection, withoutNearest, 1);
        helper.assertTrue(selection.fade(nearestSlot, 1.0F) == fadeStep,
                "The freed slot does not fade in once it is ready");

        for (int step = 0; step < stepsToFreeASlot; step++) {
            selection.chooseNone();
        }
        helper.assertTrue(selection.usedCount() == 0, "Slots stay used after choosing nothing");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarEmittersTakeTheirColors(GameTestHelper helper) {
        float[] color = new float[3];
        LightPillarEmitters.averageTextureColor(new int[]{0xFF0000FF}, 1, color);
        helper.assertTrue(color[0] == 1.0F && color[1] == 0.0F && color[2] == 0.0F,
                "A red pixel reads as " + color[0] + " " + color[1] + " " + color[2]);
        LightPillarEmitters.averageTextureColor(new int[]{0x00FFFFFF, 0xFFFF0000}, 2, color);
        helper.assertTrue(color[0] == 0.0F && color[1] == 0.0F && color[2] == 1.0F,
                "A transparent white pixel and a blue pixel read as " + color[0] + " " + color[1] + " " + color[2]);
        LightPillarEmitters.averageTextureColor(new int[]{0x00FFFFFF, 0xFF000000}, 2, color);
        helper.assertTrue(color[0] == 1.0F && color[1] == 1.0F && color[2] == 1.0F,
                "A texture without a bright opaque pixel is not white");
        int flame = 0xFF64C8FF;
        int stick = 0xFF102040;
        LightPillarEmitters.averageTextureColor(new int[]{stick, flame, stick, stick, stick}, 5, color);
        double expectedGreen = decodeSrgb(200.0 / 255.0);
        double expectedBlue = decodeSrgb(100.0 / 255.0);
        helper.assertTrue(color[0] == 1.0F && Math.abs(color[1] - expectedGreen) < 0.01
                        && Math.abs(color[2] - expectedBlue) < 0.01,
                "A torch-like texture reads as " + color[0] + " " + color[1] + " " + color[2] + " instead of its flame, 1 "
                        + expectedGreen + " " + expectedBlue);

        LightPillarEmitters overrides = LightPillarEmitters.overrides();
        float[] campfire = overrides.overrideColor(Blocks.CAMPFIRE.defaultBlockState());
        helper.assertTrue(campfire != null && campfire[0] == 1.0F && Math.abs(campfire[1] - 0.680F) < 1.0e-6F
                        && Math.abs(campfire[2] - 0.224F) < 1.0e-6F,
                "The campfire has no override or the wrong one");
        float[] soulCampfire = overrides.overrideColor(Blocks.SOUL_CAMPFIRE.defaultBlockState());
        helper.assertTrue(soulCampfire != null && soulCampfire[2] == 1.0F && soulCampfire[0] < soulCampfire[1],
                "The soul campfire's override is not normalized to its blue");
        float[] candle = overrides.overrideColor(Blocks.RED_CANDLE.defaultBlockState());
        helper.assertTrue(candle != null && Math.abs(candle[2] - 0.077F) < 1.0e-6F,
                "A red candle does not take the candle tag's override");
        helper.assertTrue(overrides.overrideColor(Blocks.TORCH.defaultBlockState()) == null,
                "The torch has an override although its texture gives its color");

        helper.assertTrue(LightPillarEmitters.showsPillar(Blocks.TORCH.defaultBlockState())
                        && LightPillarEmitters.showsPillar(Blocks.REDSTONE_TORCH.defaultBlockState())
                        && !LightPillarEmitters.showsPillar(Blocks.MAGMA_BLOCK.defaultBlockState())
                        && !LightPillarEmitters.showsPillar(Blocks.STONE.defaultBlockState()),
                "Lights below " + LightPillarEmitters.MINIMUM_EMISSION + " or above it are sorted wrongly");
        helper.assertTrue(LightPillarEmitters.strength(15) == 1.0F
                        && Math.abs(LightPillarEmitters.strength(14) - 196.0F / 225.0F) < 1.0e-6F,
                "Light strength is not the square of the emission over 15");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarSkyCheckReadsTheSectionAndTheLayerAbove(GameTestHelper helper) {
        assertSkyCheck(helper, "no data at all", null, null, true);
        assertSkyCheck(helper, "no data in the section", new DataLayer(), null, true);
        assertSkyCheck(helper, "no data above", null, new DataLayer(), true);
        assertSkyCheck(helper, "dark everywhere", new DataLayer(), new DataLayer(), false);
        assertSkyCheck(helper, "8 everywhere", new DataLayer(8), new DataLayer(8), false);
        assertSkyCheck(helper, "9 everywhere", new DataLayer(9), new DataLayer(), true);
        assertSkyCheck(helper, "15 everywhere", new DataLayer(15), new DataLayer(), true);

        assertSkyCheck(helper, "one block at 9 at 0 0 0", layerWith(0, 0, 0, 9), new DataLayer(), true);
        assertSkyCheck(helper, "one block at 9 at 15 15 15", layerWith(15, 15, 15, 9), new DataLayer(), true);
        assertSkyCheck(helper, "one block at 9 at 1 7 4", layerWith(1, 7, 4, 9), new DataLayer(), true);
        assertSkyCheck(helper, "one block at 8 at 0 0 0", layerWith(0, 0, 0, 8), new DataLayer(), false);
        assertSkyCheck(helper, "one block at 8 at 15 15 15", layerWith(15, 15, 15, 8), new DataLayer(), false);
        assertSkyCheck(helper, "one block at 8 at 1 7 4", layerWith(1, 7, 4, 8), new DataLayer(), false);

        assertSkyCheck(helper, "one block at 12 in the lowest layer above at 3 0 7", new DataLayer(),
                layerWith(3, 0, 7, 12), true);
        assertSkyCheck(helper, "one block at 15 in the second layer above at 3 1 7", new DataLayer(),
                layerWith(3, 1, 7, 15), false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarWorkQueueFollowsLoadsLightAndBlockChanges(GameTestHelper helper) {
        long column = ChunkPos.asLong(1, 2);
        long otherColumn = ChunkPos.asLong(-3, 4);
        long blockInColumn = BlockPos.asLong(1 * 16 + 3, 5 * 16 + 4, 2 * 16 + 5);
        long otherBlockInSection = BlockPos.asLong(1 * 16 + 9, 5 * 16 + 10, 2 * 16 + 11);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState lavaSource = Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 0);
        BlockState flowingLava = Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
        BlockState youngFire = Blocks.FIRE.defaultBlockState().setValue(FireBlock.AGE, 0);
        BlockState oldFire = Blocks.FIRE.defaultBlockState().setValue(FireBlock.AGE, 5);
        BlockState litCampfire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        BlockState unlitCampfire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);

        LightPillarWorkQueue queue = new LightPillarWorkQueue();
        queue.onColumnLoaded(column);
        helper.assertTrue(!queue.onBlockChanged(blockInColumn, air, torch),
                "A block change in a column that waits for its light queues its section");
        helper.assertTrue(queue.queuedSectionCount() == 0 && !queue.nextSection(),
                "A column that waits for its light has " + queue.queuedSectionCount() + " queued sections");
        queue.startTracking(-4, 24);
        helper.assertTrue(queue.onColumnLit(column), "A loaded column does not report that it waited for its light");
        helper.assertTrue(queue.queuedSectionCount() == 24,
                "A lit column queues " + queue.queuedSectionCount() + " sections instead of 24");
        for (int sectionY = -4; sectionY <= 19; sectionY++) {
            helper.assertTrue(queue.nextSection(), "The lit column has no section left at y " + sectionY);
            helper.assertTrue(queue.takenSection() == SectionPos.asLong(1, sectionY, 2),
                    "The lit column took the section " + describeSection(queue.takenSection()) + " instead of y "
                            + sectionY);
        }
        helper.assertTrue(!queue.nextSection(), "The lit column has more than 24 sections");
        helper.assertTrue(!queue.onColumnLit(column), "A column reports a second time that it waited for its light");
        helper.assertTrue(queue.queuedSectionCount() == 0,
                "A column lit twice queues " + queue.queuedSectionCount() + " sections");

        helper.assertTrue(queue.onBlockChanged(blockInColumn, air, torch),
                "Air turning into a torch does not queue the block's section");
        drainSections(helper, queue, 1, "after air turned into a torch");
        helper.assertTrue(queue.onBlockChanged(blockInColumn, torch, air),
                "A torch turning into air does not queue the block's section");
        drainSections(helper, queue, 1, "after a torch turned into air");
        helper.assertTrue(!queue.onBlockChanged(blockInColumn, lavaSource, flowingLava),
                "Lava changing its level queues the block's section");
        helper.assertTrue(!queue.onBlockChanged(blockInColumn, youngFire, oldFire),
                "Fire changing its age queues the block's section");
        helper.assertTrue(!queue.onBlockChanged(blockInColumn, stone, dirt),
                "Stone turning into dirt queues the block's section");
        helper.assertTrue(queue.onBlockChanged(blockInColumn, litCampfire, unlitCampfire),
                "A lit campfire turning into an unlit one does not queue the block's section");
        drainSections(helper, queue, 1, "after a campfire went out");
        helper.assertTrue(!LightPillarWorkQueue.changesPillars(lavaSource, flowingLava),
                "Lava changing its level from 0 to 3 changes the pillars");
        helper.assertTrue(!LightPillarWorkQueue.changesPillars(youngFire, oldFire),
                "Fire changing its age from 0 to 5 changes the pillars");
        helper.assertTrue(!LightPillarWorkQueue.changesPillars(stone, dirt),
                "Stone turning into dirt changes the pillars");
        helper.assertTrue(LightPillarWorkQueue.changesPillars(air, torch),
                "Air turning into a torch does not change the pillars");
        helper.assertTrue(LightPillarWorkQueue.changesPillars(torch, air),
                "A torch turning into air does not change the pillars");
        helper.assertTrue(LightPillarWorkQueue.changesPillars(litCampfire, unlitCampfire),
                "A lit campfire turning into an unlit one does not change the pillars");

        helper.assertTrue(queue.onBlockChanged(blockInColumn, air, torch),
                "The first of two changes in a section does not queue it");
        helper.assertTrue(!queue.onBlockChanged(otherBlockInSection, air, torch),
                "A second change in a queued section queues it again");
        drainSections(helper, queue, 1, "after two changes in one section");

        helper.assertTrue(queue.onSkyLightChanged(SectionPos.asLong(1, 5, 2)) == 2,
                "A sky light change at y 5 queues " + queue.queuedSectionCount() + " sections instead of 2");
        drainSections(helper, queue, 2, "after a sky light change at y 5");
        helper.assertTrue(queue.onSkyLightChanged(SectionPos.asLong(1, 20, 2)) == 1,
                "A sky light change at y 20 does not queue only y 19");
        helper.assertTrue(queue.nextSection() && queue.takenSection() == SectionPos.asLong(1, 19, 2),
                "A sky light change at y 20 queues " + describeSection(queue.takenSection()) + " instead of y 19");
        drainSections(helper, queue, 0, "after a sky light change at y 20");
        helper.assertTrue(queue.onSkyLightChanged(SectionPos.asLong(1, -4, 2)) == 1,
                "A sky light change at y -4 does not queue only y -4");
        helper.assertTrue(queue.nextSection() && queue.takenSection() == SectionPos.asLong(1, -4, 2),
                "A sky light change at y -4 queues " + describeSection(queue.takenSection()) + " instead of y -4");
        drainSections(helper, queue, 0, "after a sky light change at y -4");

        helper.assertTrue(queue.onBlockChanged(otherBlockInSection, air, torch),
                "A change in a loaded and lit column does not queue its section");
        queue.onColumnUnloaded(column);
        helper.assertTrue(!queue.nextSection(), "A queued section of an unloaded column is taken");
        helper.assertTrue(queue.nextDroppedColumn() && queue.takenColumn() == column,
                "The unloaded column is not reported as dropped");
        helper.assertTrue(!queue.nextDroppedColumn(), "The unloaded column is reported twice as dropped");

        queue.stopTracking();
        queue.onColumnLoaded(column);
        queue.onColumnLoaded(otherColumn);
        helper.assertTrue(queue.onColumnLit(column), "A column loaded again does not wait for its light again");
        helper.assertTrue(queue.queuedSectionCount() == 0 && !queue.nextSection(),
                "A column lit while not tracking queues " + queue.queuedSectionCount() + " sections");
        helper.assertTrue(queue.isLoaded(otherColumn) && queue.isAwaitingLight(otherColumn),
                "A column loaded while not tracking is not kept");
        int litColumns = queue.startTracking(-4, 24);
        helper.assertTrue(litColumns == 1,
                "Starting to track queues " + litColumns + " columns instead of the 1 with its light");
        for (int sectionY = -4; sectionY <= 19; sectionY++) {
            helper.assertTrue(queue.nextSection(), "The column lit before tracking has no section left at y "
                    + sectionY);
            helper.assertTrue(queue.takenSection() == SectionPos.asLong(1, sectionY, 2),
                    "The column lit before tracking took the section " + describeSection(queue.takenSection())
                            + " instead of y " + sectionY);
        }
        helper.assertTrue(!queue.nextSection(), "Starting to track queues more than the 24 sections of the lit column");
        helper.assertTrue(queue.onColumnLit(otherColumn),
                "The column that waited for its light does not report that it waited");
        helper.assertTrue(queue.queuedSectionCount() == 24,
                "Lighting the second column queues " + queue.queuedSectionCount() + " sections instead of 24");
        drainSections(helper, queue, 24, "after lighting the second column");

        queue.reset();
        helper.assertTrue(queue.loadedColumnCount() == 0,
                "A reset queue still has " + queue.loadedColumnCount() + " loaded columns");
        helper.assertTrue(!queue.isTracking(), "A reset queue still tracks");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarSelectionSkipsClustersOutsideTheChunkRadius(GameTestHelper helper) {
        assertWithinRadius(helper, "chunk 2 0", BlockPos.asLong(8, 0, 0), 2, true);
        assertWithinRadius(helper, "chunk 2 1", BlockPos.asLong(8, 0, 4), 2, true);
        assertWithinRadius(helper, "chunk 2 2", BlockPos.asLong(8, 0, 8), 2, false);
        assertWithinRadius(helper, "chunk -1 -2", BlockPos.asLong(-1, 0, -5), 2, true);
        assertWithinRadius(helper, "chunk -3 0", BlockPos.asLong(-12, 0, 0), 2, false);

        long nearKey = BlockPos.asLong(0, 0, 0);
        long farKey = BlockPos.asLong(12, 0, 0);
        LightPillarClusters clusters = new LightPillarClusters();
        clusters.beginSection(0, 0, 0);
        clusters.addLight(1, 1, 1, 2.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        clusters.beginSection(3, 0, 0);
        clusters.addLight(3 * 16 + 1, 1, 1, 2.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        helper.assertTrue(clusterWithKey(clusters, nearKey) >= 0 && clusterWithKey(clusters, farKey) >= 0,
                "The clusters do not have the keys of their cells");

        LightPillarSelection selection = new LightPillarSelection();
        selection.choose(clusters, 0.5, 0.5, 0.5, 1000.0, 2);
        helper.assertTrue(selection.chosenCount() == 1 && selection.usedCount() == 1,
                "Within 2 chunks the selection chose " + selection.chosenCount() + " clusters and uses "
                        + selection.usedCount() + " slots instead of 1 and 1");
        helper.assertTrue(slotWithKey(selection, nearKey) >= 0,
                "Within 2 chunks the near cluster is not chosen");
        helper.assertTrue(slotWithKey(selection, farKey) < 0,
                "Within 2 chunks the cluster 3 chunks away is chosen");

        selection.clear();
        selection.choose(clusters, 0.5, 0.5, 0.5, 1000.0, 3);
        helper.assertTrue(selection.chosenCount() == 2 && selection.usedCount() == 2,
                "Within 3 chunks the selection chose " + selection.chosenCount() + " clusters and uses "
                        + selection.usedCount() + " slots instead of 2 and 2");
        helper.assertTrue(slotWithKey(selection, nearKey) >= 0 && slotWithKey(selection, farKey) >= 0,
                "Within 3 chunks not both clusters are chosen");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void lightPillarHorizonCutMatchesTheRayIntegral(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double worst = -1.0;
        String worstWhere = "nowhere";
        double worstLoss = -1.0;
        String worstLossWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : SCENE_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            double whole = rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight,
                                    tiltSigma, refractiveIndex, NO_SCENE, LightPillarHorizons.OPEN);
                            double previous = Double.NaN;
                            for (int horizonStep = -2; horizonStep <= 2; horizonStep++) {
                                double horizon = elevation - 2.0 * horizonStep * tiltSigma;
                                double expected = rayIntegral(elevation, lateralAngle, horizontalDistance,
                                        lightHeight, tiltSigma, refractiveIndex, NO_SCENE, horizon);
                                double actual = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9,
                                        0.0, NO_SCENE, horizon);
                                double difference = Math.abs(actual - expected) / whole;
                                if (!(difference <= worst)) {
                                    worst = difference;
                                    worstWhere = String.format(Locale.ROOT,
                                            "%s, light %.0f away and %.0f high, elevation %.0f degrees, %.0f spreads "
                                                    + "off the center, horizon %.4f, %.4g against %.4g of %.4g",
                                            crystal.name(), horizontalDistance, lightHeight, elevationDegrees, spreads,
                                            horizon, actual, expected, whole);
                                }
                                // A higher horizon cuts more tilts, so the light may only grow as the horizon falls
                                // from one horizon step to the next higher one:
                                if (!Double.isNaN(previous) && actual < previous - 1.0e-12 * whole) {
                                    double loss = (previous - actual) / whole;
                                    if (!(loss <= worstLoss)) {
                                        worstLoss = loss;
                                        worstLossWhere = String.format(Locale.ROOT,
                                                "%s, light %.0f away and %.0f high, elevation %.0f degrees, %.0f "
                                                        + "spreads off the center, horizon %.4f, %.4g against %.4g",
                                                crystal.name(), horizontalDistance, lightHeight, elevationDegrees,
                                                spreads, horizon, actual, previous);
                                    }
                                }
                                previous = actual;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Light pillar behind a horizon differs from the ray integral by at most " + worst
                + " of the whole pillar, at " + worstWhere);
        helper.assertTrue(worst <= PILLAR_TOLERANCE,
                "Light pillar behind a horizon differs from the ray integral by " + worst + " of the whole pillar, at "
                        + worstWhere);
        helper.assertTrue(worstLoss <= 0.0, "The pillar loses light as the horizon falls from one horizon step to "
                + "the next higher one, by " + worstLoss + " of the whole pillar, at " + worstLossWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void lightPillarNearFadeAndOverhangsMatchTheRayIntegral(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double worst = -1.0;
        String worstWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : SCENE_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            double whole = rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight,
                                    tiltSigma, refractiveIndex, NO_SCENE, 0.0, LightPillarHorizons.OPEN,
                                    LightPillarHorizons.OPEN, LightPillarHorizons.OPEN);
                            for (int caseNumber = 1; caseNumber <= 7; caseNumber++) {
                                double[] fadeCase = rayIntegralCase(caseNumber, elevation, tiltSigma);
                                double nearFade = fadeCase[0];
                                double horizon = fadeCase[1];
                                double ceiling = fadeCase[2];
                                double top = fadeCase[3];
                                double expected = rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight,
                                        tiltSigma, refractiveIndex, NO_SCENE, nearFade, horizon, ceiling, top);
                                double actual = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9,
                                        0.0, NO_SCENE, nearFade, horizon, ceiling, top);
                                double difference = Math.abs(actual - expected) / whole;
                                if (!(difference <= worst)) {
                                    worst = difference;
                                    worstWhere = String.format(Locale.ROOT,
                                            "case %d, %s, light %.0f away and %.0f high, elevation %.0f degrees, "
                                                    + "%.0f spreads off the center, near fade %.0f, horizon %.4f, "
                                                    + "ceiling %.4f, top %.4f, %.4g against %.4g of %.4g",
                                            caseNumber, crystal.name(), horizontalDistance, lightHeight,
                                            elevationDegrees, spreads, nearFade, horizon, ceiling, top, actual,
                                            expected, whole);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Light pillar behind a near fade and overhangs differs from the ray integral by at most " + worst
                + " of the whole pillar, at " + worstWhere);
        helper.assertTrue(worst <= PILLAR_TOLERANCE, "Light pillar behind a near fade and overhangs differs from the "
                + "ray integral by " + worst + " of the whole pillar, at " + worstWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void lightPillarCrystalCountMatchesTheRayIntegral(GameTestHelper helper) {
        List<IceCrystalData.Crystal> plates = plates();
        double[] worstByKind = new double[plates.size()];
        String[] worstWhereByKind = new String[plates.size()];
        for (int kind = 0; kind < plates.size(); kind++) {
            IceCrystalData.Crystal crystal = plates.get(kind);
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            double worst = -1.0;
            String worstWhere = "nowhere";
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : SCENE_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            double whole = rayCrystalCount(elevation, lateralAngle, horizontalDistance, lightHeight,
                                    tiltSigma, NO_SCENE, 0.0, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN,
                                    LightPillarHorizons.OPEN);
                            for (int caseNumber = 0; caseNumber <= 7; caseNumber++) {
                                double[] fadeCase = caseNumber == 0
                                        ? new double[]{0.0, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN,
                                        LightPillarHorizons.OPEN}
                                        : rayIntegralCase(caseNumber, elevation, tiltSigma);
                                double expected = rayCrystalCount(elevation, lateralAngle, horizontalDistance,
                                        lightHeight, tiltSigma, NO_SCENE, fadeCase[0], fadeCase[1], fadeCase[2],
                                        fadeCase[3]);
                                double actual = LightPillarOptics.dustCrystalCount(elevation, lateralAngle,
                                        horizontalDistance, lightHeight, tiltSigma, 0.0, 1.0e9, NO_SCENE, fadeCase[0],
                                        fadeCase[1], fadeCase[2], fadeCase[3]);
                                double difference = Math.abs(actual - expected) / whole;
                                if (!(difference <= worst)) {
                                    worst = difference;
                                    worstWhere = String.format(Locale.ROOT,
                                            "case %d, %s, light %.0f away and %.0f high, elevation %.0f degrees, "
                                                    + "%.0f spreads off the center, %.4g against %.4g of %.4g",
                                            caseNumber, crystal.name(), horizontalDistance, lightHeight,
                                            elevationDegrees, spreads, actual, expected, whole);
                                }
                            }
                        }
                    }
                }
            }
            worstByKind[kind] = worst;
            worstWhereByKind[kind] = worstWhere;
            LOGGER.info("Light pillar crystal counts of " + crystal.name() + " differ from the ray integral by at most "
                    + worst + " of the whole count, at " + worstWhere);
        }
        for (int kind = 0; kind < plates.size(); kind++) {
            // The tilts of wobbling plates span 40 degrees, over which the spread of the reflection changes too much
            // for the 8 point rule at low elevations. The count only sets how strongly a pillar glitters:
            double tolerance = plates.get(kind).tiltDegrees() > 1.0 ? WOBBLING_COUNT_TOLERANCE : PILLAR_TOLERANCE;
            helper.assertTrue(worstByKind[kind] <= tolerance, "Light pillar crystal counts differ from the ray "
                    + "integral by " + worstByKind[kind] + " of the whole count, at " + worstWhereByKind[kind]);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void lightPillarSparkleKeepsTheMeanValue(GameTestHelper helper) {
        helper.assertTrue(LightPillarOptics.sparkle(0.0, 3, 4, 5, 6.5) == 1.0,
                "A cell without a crystal count does not show its mean value");
        for (double meanCount : new double[]{0.001, 1.0 / 64.0, 0.05, 0.2, 1.0, 4.0, 15.0, 17.0, 100.0, 1.0e4}) {
            double cap = LightPillarOptics.sparkleCap(meanCount);
            double sum = 0.0;
            int samples = 0;
            for (int lateralCell = -100; lateralCell < 100; lateralCell++) {
                for (int elevationCell = 0; elevationCell < 100; elevationCell++) {
                    // Two periods apart, the counts a sample blends are drawn anew:
                    for (int period = 0; period < 10; period++) {
                        double factor = LightPillarOptics.sparkle(meanCount, lateralCell, elevationCell, 7,
                                2.0 * period + 0.5);
                        helper.assertTrue(factor >= 0.0 && factor <= cap * (1.0 + 1.0e-12), "A cell with a mean "
                                + "count of " + meanCount
                                + " shows " + factor + " of its mean value, outside 0 to " + cap);
                        sum += factor;
                        samples++;
                    }
                }
            }
            double mean = sum / samples;
            double expected = Math.min(1.0, LightPillarOptics.MOST_SPARKLE * (1.0 - Math.exp(-meanCount)));
            // With few crystals, a sample is 0 or the value of one glint, whose variance sets the standard error:
            double glintValue = Math.min(1.0 / meanCount, LightPillarOptics.MOST_SPARKLE);
            double tolerance = meanCount >= 1.0 ? 0.01 : 4.0 * Math.sqrt(glintValue * expected / samples) + 0.012;
            helper.assertTrue(Math.abs(mean - expected) <= tolerance, "Cells with a mean count of " + meanCount
                    + " show " + mean + " of their mean value on average instead of " + expected + " within "
                    + tolerance);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void lightPillarCrystalDrawsFollowThePoissonDistribution(GameTestHelper helper) {
        int draws = 200000;
        for (double meanCount : new double[]{0.3, 2.9, 3.1, 6.0, 15.0, 17.0, 100.0, 1.0e4}) {
            int largest = (int) Math.ceil(meanCount + 10.0 * Math.sqrt(meanCount) + 10.0);
            int[] drawnCounts = new int[largest + 1];
            for (int draw = 0; draw < draws; draw++) {
                double count = LightPillarOptics.crystalsDrawn(meanCount, draw * 0x9e3779b9);
                helper.assertTrue(count >= 0.0 && count == Math.floor(count), "A cell with a mean count of "
                        + meanCount + " draws " + count + " crystals, not a whole count");
                drawnCounts[(int) Math.min(count, largest)]++;
            }
            // The Kolmogorov Smirnov distance of the drawn counts from the Poisson distribution, whose terms are summed
            // as logarithms since exp(-meanCount) underflows for large means:
            double logTerm = -meanCount;
            double poisson = 0.0;
            double drawn = 0.0;
            double distance = 0.0;
            for (int count = 0; count <= largest; count++) {
                poisson += Math.exp(logTerm);
                drawn += drawnCounts[count] / (double) draws;
                distance = Math.max(distance, Math.abs(drawn - poisson));
                logTerm += Math.log(meanCount / (count + 1));
            }
            helper.assertTrue(distance <= 0.01, "Crystal counts drawn for a mean count of " + meanCount
                    + " differ from the Poisson distribution by " + distance);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1200)
    public static void lightPillarRibbonsHoldTheGlints(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double visible = LightPillarOptics.VISIBLE_VALUE;
        double glint = LightPillarOptics.GLINT_VALUE;
        double pixelAngle = pixelAngle(1080);
        double lateralBlur = LightPillarOptics.lateralBlur(pixelAngle);
        double open = LightPillarHorizons.OPEN;
        double worstMiddle = 0.0;
        double worst = 0.0;
        String worstWhere = "nowhere";
        double worstMean = 0.0;
        String worstMeanWhere = "nowhere";
        for (double micrometers : new double[]{50.0, 65.0, 100.0, 200.0}) {
            double countScale = LightPillarOptics.crystalCountScale(pixelAngle, micrometers);
            for (IceCrystalData.Crystal crystal : plates()) {
                double tiltSigma = Math.toRadians(crystal.tiltDegrees());
                double share = IceCrystalData.get().shareOf(crystal);
                for (double[] light : RIBBON_LIGHTS) {
                    for (double strength : RIBBON_STRENGTHS) {
                        double scale = LightPillarOptics.BRIGHTNESS * strength * share * RIBBON_EXTINCTION;
                        double counts = countScale * share * RIBBON_EXTINCTION;
                        double[][] corners = ribbonEdge(light[0], light[1], light[2], tiltSigma, refractiveIndex,
                                lateralBlur, pixelAngle, scale, counts, 0.0, open, open, open);
                        if (corners == null) {
                            continue;
                        }
                        for (int segment = 0; segment < RIBBON_SEGMENTS; segment++) {
                            double[] below = corners[segment];
                            double[] above = corners[segment + 1];
                            for (int step = 0; step <= EDGE_STEPS; step++) {
                                double fraction = (double) step / EDGE_STEPS;
                                double x = below[0] + (above[0] - below[0]) * fraction;
                                double y = below[1] + (above[1] - below[1]) * fraction;
                                double z = below[2] + (above[2] - below[2]) * fraction;
                                double length = Math.sqrt(x * x + y * y + z * z);
                                double elevation = Math.asin(y / length);
                                double lateralAngle = Math.asin(z / length);
                                double mean = scale * LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        light[0], light[1], tiltSigma, refractiveIndex, lateralBlur, light[2],
                                        RIBBON_EXTINCTION, NO_SCENE, 0.0, open, open, open);
                                double meanCount = counts * LightPillarOptics.dustCrystalCount(elevation,
                                        lateralAngle, light[0], light[1], tiltSigma, lateralBlur, light[2], NO_SCENE,
                                        0.0, open, open, open);
                                double chance = visibleGlintChance(mean, meanCount, glint);
                                if (segment >= 1 && segment < RIBBON_SEGMENTS - 2) {
                                    worstMiddle = Math.max(worstMiddle, chance);
                                }
                                String where = String.format(Locale.ROOT,
                                        "%.0f micrometers, %s of strength %.2f, light %.0f away and %.2f high, "
                                                + "dust bottom %.2f, elevation %.2f, %.3f degrees off the center, "
                                                + "%.3g crystals, mean %.3g times the visible value",
                                        micrometers, crystal.name(), strength, light[0], light[1], light[2],
                                        Math.toDegrees(elevation), Math.toDegrees(lateralAngle), meanCount,
                                        mean / visible);
                                if (chance > worst) {
                                    worst = chance;
                                    worstWhere = where;
                                }
                                if (mean > worstMean) {
                                    worstMean = mean;
                                    worstMeanWhere = where;
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("On the edges of glittering light pillar ribbons a pixel shows the glint value with a chance of "
                + "at most " + worstMiddle + " away from the ends of the ribbon and " + worst + " anywhere, at "
                + worstWhere + ", and a mean of at most " + worstMean / visible + " times the visible value, at "
                + worstMeanWhere);
        // The top rows come close to the zenith and the bottom row to the light, where the half rows sample coarsely:
        helper.assertTrue(worstMiddle <= 1.5 * LightPillarOptics.RIBBON_GLINT_CHANCE, "Away from the ends of "
                + "glittering light pillar ribbons a pixel on their edges shows the glint value with a chance of "
                + worstMiddle);
        helper.assertTrue(worst <= 10.0 * LightPillarOptics.RIBBON_GLINT_CHANCE, "On the edges of glittering light "
                + "pillar ribbons a pixel shows the glint value with a chance of " + worst + ", at " + worstWhere);
        helper.assertTrue(worstMean <= 1.01 * visible, "On the edges of glittering light pillar ribbons a pillar "
                + "shows " + worstMean / visible + " times the visible value on average, at " + worstMeanWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarOccludersSortBlocks(GameTestHelper helper) {
        LightPillarOccluders occluders = new LightPillarOccluders();
        BlockGetter level = EmptyBlockGetter.INSTANCE;
        BlockPos position = BlockPos.ZERO;
        BlockState[] passing = {Blocks.AIR.defaultBlockState(), Blocks.GLASS.defaultBlockState(),
                Blocks.WHITE_STAINED_GLASS.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(),
                Blocks.ICE.defaultBlockState(), Blocks.FROSTED_ICE.defaultBlockState(),
                FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState(), Blocks.TORCH.defaultBlockState(),
                Blocks.DANDELION.defaultBlockState(), Blocks.WATER.defaultBlockState(),
                Blocks.LAVA.defaultBlockState(), Blocks.COBWEB.defaultBlockState(),
                Blocks.SCAFFOLDING.defaultBlockState()};
        for (BlockState state : passing) {
            helper.assertTrue(LightPillarOccluders.passesLight(state),
                    "The occluders stop the light of " + state + " which should pass it");
        }
        BlockState[] stopping = {Blocks.STONE.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(),
                Blocks.PACKED_ICE.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(),
                Blocks.POWDER_SNOW.defaultBlockState(), Blocks.OAK_FENCE.defaultBlockState(),
                Blocks.OAK_SLAB.defaultBlockState()};
        for (BlockState state : stopping) {
            helper.assertTrue(!LightPillarOccluders.passesLight(state),
                    "The occluders pass the light of " + state + " which should stop it");
        }
        for (int layers = 1; layers <= SnowLayerBlock.MAX_HEIGHT; layers++) {
            helper.assertTrue(!LightPillarOccluders.passesLight(
                            Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers)),
                    "The occluders pass the light of snow with " + layers + " layers which should stop it");
        }
        assertStoppedHeight(helper, occluders, level, position, Blocks.GLASS.defaultBlockState(),
                LightPillarOccluders.PASSES);
        assertStoppedHeight(helper, occluders, level, position, Blocks.WATER.defaultBlockState(),
                LightPillarOccluders.PASSES);
        assertStoppedHeight(helper, occluders, level, position, Blocks.STONE.defaultBlockState(), 1.0);
        assertStoppedHeight(helper, occluders, level, position,
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), 0.5);
        assertStoppedHeight(helper, occluders, level, position,
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4), 0.5);
        assertStoppedHeight(helper, occluders, level, position, Blocks.OAK_FENCE.defaultBlockState(), 1.0);
        assertStoppedHeight(helper, occluders, level, position, Blocks.POWDER_SNOW.defaultBlockState(), 1.0);
        assertStopsTheSame(helper, occluders, Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), true);
        assertStopsTheSame(helper, occluders, Blocks.AIR.defaultBlockState(), Blocks.GLASS.defaultBlockState(), true);
        assertStopsTheSame(helper, occluders, Blocks.OAK_FENCE.defaultBlockState(),
                Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), true);
        assertStopsTheSame(helper, occluders, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1),
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2), false);
        assertStopsTheSame(helper, occluders, Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
        assertStopsTheSame(helper, occluders, Blocks.POWDER_SNOW.defaultBlockState(),
                Blocks.POWDER_SNOW.defaultBlockState(), false);
        assertLightTop(helper, level, position, Blocks.TORCH.defaultBlockState(), 0.625);
        assertLightTop(helper, level, position, Blocks.GLOWSTONE.defaultBlockState(), 1.0);
        assertLightTop(helper, level, position,
                Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 0.5625);
        assertLightTop(helper, level, position, Blocks.LAVA.defaultBlockState(), 8.0 / 9.0);
        assertLightTop(helper, level, position, Blocks.LIGHT.defaultBlockState(), 0.5);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsRiseOverWallsAroundALight(GameTestHelper helper) {
        double overTheWall = Math.atan(1.375 / 0.5);
        double overTheWallDiagonally = Math.atan(1.375 / (0.5 * Math.sqrt(2.0)));
        double overTheSnow = Math.atan(0.875 / 0.5);
        double overTheSnowDiagonally = Math.atan(0.875 / (0.5 * Math.sqrt(2.0)));
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState[] walls = {Blocks.STONE.defaultBlockState(), leaves};
        for (int wallIndex = 0; wallIndex < walls.length; wallIndex++) {
            placeRingAroundTheLight(helper, walls[wallIndex], walls[wallIndex], 1, 2);
            HorizonSetup setup = buildSlotAndHorizons(helper, lightBlock, 0.625);
            for (int bin : new int[]{0, 16, 32, 48}) {
                assertBinRisesOver(helper, setup, bin, 5, overTheWall);
            }
            assertBinRisesOver(helper, setup, 8, 6, overTheWallDiagonally);
            if (wallIndex == 0) {
                assertHorizonToward(helper, setup, 4.0, 0.0, overTheWall, 1.0e-6);
                assertHorizonToward(helper, setup, 4.0, -0.01, overTheWall, 0.005);
            }
        }
        placeRingAroundTheLight(helper, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), 1, 1);
        placeRingAroundTheLight(helper, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4),
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4), 2, 2);
        HorizonSetup snowSetup = buildSlotAndHorizons(helper, lightBlock, 0.625);
        assertBinRisesOver(helper, snowSetup, 0, 5, overTheSnow);
        assertBinRisesOver(helper, snowSetup, 8, 6, overTheSnowDiagonally);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsLetLightThroughGlassPlantsAndWater(GameTestHelper helper) {
        double onOpenGround = Math.atan(-0.625 / 5.5);
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        placeRingAroundTheLight(helper, Blocks.GLASS.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(), 1, 2);
        HorizonSetup glass = buildSlotAndHorizons(helper, lightBlock, 0.625);
        assertSample(helper, glass, 0, 15, onOpenGround);
        assertSamplesBetweenOpenAndZero(helper, glass);
        placeRingAroundTheLight(helper, Blocks.DANDELION.defaultBlockState(), Blocks.WATER.defaultBlockState(), 1, 1);
        HorizonSetup plants = buildSlotAndHorizons(helper, lightBlock, 0.625);
        assertSample(helper, plants, 0, 15, onOpenGround);
        assertSamplesBetweenOpenAndZero(helper, plants);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsLeaveOutTheLightsOwnBlock(GameTestHelper helper) {
        double belowOpenGround = Math.atan(-1.0 / 5.5);
        fillGrassFloor(helper);
        BlockPos standingBlock = highAbove(helper, 10, 1, 10);
        helper.getLevel().setBlockAndUpdate(standingBlock, Blocks.GLOWSTONE.defaultBlockState());
        HorizonSetup standing = buildSlotAndHorizons(helper, standingBlock, 1.0);
        assertSample(helper, standing, 0, 15, belowOpenGround);
        helper.getLevel().setBlockAndUpdate(standingBlock, Blocks.AIR.defaultBlockState());
        BlockPos buriedBlock = highAbove(helper, 10, 0, 10);
        helper.getLevel().setBlockAndUpdate(buriedBlock, Blocks.GLOWSTONE.defaultBlockState());
        HorizonSetup buried = buildSlotAndHorizons(helper, buriedBlock, 1.0);
        for (int bin : new int[]{0, 8, 16, 32, 48}) {
            assertSamplesNearZero(helper, buried, bin, 6);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsReachAFartherWall(GameTestHelper helper) {
        double belowTheWallBase = Math.atan(-0.625 / 3.5);
        double overTheWall = Math.atan(2.375 / 4.5);
        double onOpenGround = Math.atan(-0.625 / 5.5);
        double nearOnOpenGround = Math.atan(-0.625 / 1.5);
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        for (int y = 1; y <= 3; y++) {
            for (int z = 5; z <= 15; z++) {
                helper.getLevel().setBlockAndUpdate(highAbove(helper, 15, y, z), Blocks.STONE.defaultBlockState());
            }
        }
        HorizonSetup setup = buildSlotAndHorizons(helper, lightBlock, 0.625);
        assertSample(helper, setup, 0, 12, belowTheWallBase);
        assertSample(helper, setup, 0, 13, overTheWall);
        assertSample(helper, setup, 0, 14, overTheWall);
        assertSample(helper, setup, 0, 15, overTheWall);
        assertSample(helper, setup, 32, 15, onOpenGround);
        assertHorizonToward(helper, setup, 5.5, 0.0, overTheWall, 1.0e-6);
        assertHorizonToward(helper, setup, 2.0, 0.0, nearOnOpenGround, 1.0e-6);
        int farSurface = setup.columns().beginColumn(1_000_000, 1_000_000);
        helper.assertTrue(farSurface == LightPillarHorizons.NO_BLOCKS, "A column outside the loaded chunks has its "
                + "surface at " + farSurface + " instead of NO_BLOCKS");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsLetLightPassBelowACanopy(GameTestHelper helper) {
        double horizonNear = Math.atan(-0.625 / 0.5);
        double horizonMid = Math.atan(-0.625 / 1.5);
        double horizonFar = Math.atan(-0.625 / 2.5);
        double horizonFarther = Math.atan(-0.625 / 3.5);
        double horizonOnOpenGround = Math.atan(-0.625 / 5.5);
        double ceilingNear = Math.atan(2.375 / 1.5);
        double ceilingMid = Math.atan(2.375 / 2.5);
        double ceilingFar = Math.atan(2.375 / 3.5);
        double topOverTheCanopy = Math.atan(3.375 / 0.5);
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 11; x <= 13; x++) {
            for (int z = 9; z <= 11; z++) {
                helper.getLevel().setBlockAndUpdate(highAbove(helper, x, 4, z), leaves);
            }
        }
        HorizonSetup setup = buildSlotAndHorizons(helper, lightBlock, 0.625);
        for (int sample = 1; sample <= 4; sample++) {
            assertSample(helper, setup, 0, sample, LightPillarHorizons.OPEN);
            assertCeilingSample(helper, setup, 0, sample, LightPillarHorizons.OPEN);
            assertTopSample(helper, setup, 0, sample, LightPillarHorizons.OPEN);
        }
        for (int sample = 5; sample <= 7; sample++) {
            assertSample(helper, setup, 0, sample, horizonNear);
            assertCeilingSample(helper, setup, 0, sample, ceilingNear);
            assertTopSample(helper, setup, 0, sample, topOverTheCanopy);
        }
        for (int sample = 8; sample <= 9; sample++) {
            assertSample(helper, setup, 0, sample, horizonMid);
            assertCeilingSample(helper, setup, 0, sample, ceilingMid);
            assertTopSample(helper, setup, 0, sample, topOverTheCanopy);
        }
        for (int sample = 10; sample <= 11; sample++) {
            assertSample(helper, setup, 0, sample, horizonFar);
            assertCeilingSample(helper, setup, 0, sample, ceilingFar);
            assertTopSample(helper, setup, 0, sample, topOverTheCanopy);
        }
        assertSample(helper, setup, 0, 12, horizonFarther);
        assertCeilingSample(helper, setup, 0, 12, ceilingFar);
        assertTopSample(helper, setup, 0, 12, topOverTheCanopy);
        assertSample(helper, setup, 0, 15, horizonOnOpenGround);
        assertCeilingSample(helper, setup, 0, 15, ceilingFar);
        assertTopSample(helper, setup, 0, 15, topOverTheCanopy);
        for (int sample = 5; sample <= 15; sample++) {
            assertCeilingAndTopMatchTheHorizon(helper, setup, 32, sample);
        }
        double pointX = lightBlock.getX() + 0.5 + 5.5;
        double pointZ = lightBlock.getZ() + 0.5;
        float ceiling = setup.horizons().ceiling(setup.slot(), pointX, pointZ);
        helper.assertTrue(Math.abs(ceiling - ceilingFar) <= 1.0e-6, "The ceiling 5.5 blocks along +x from the light "
                + "is " + ceiling + " instead of " + ceilingFar);
        float top = setup.horizons().top(setup.slot(), pointX, pointZ);
        helper.assertTrue(Math.abs(top - topOverTheCanopy) <= 1.0e-6, "The top 5.5 blocks along +x from the light is "
                + top + " instead of " + topOverTheCanopy);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsCountBlocksAboveTheLight(GameTestHelper helper) {
        double overTheBlock = Math.atan(1.375 / 0.5);
        double overTheBlockDiagonally = Math.atan(1.375 / (0.5 * Math.sqrt(2.0)));
        double justOverTheTorch = Math.atan(0.375 / 0.5);
        double underTheSlit = Math.atan(0.375 / 1.5);
        double overTheSlit = Math.atan(1.375 / 0.5);
        double underTheFloor = Math.atan(-0.625 / 0.5);
        double onOpenGround = Math.atan(-0.625 / 5.5);
        double closed = LightPillarHorizons.CLOSED;
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos aboveTheTorch = highAbove(helper, 10, 3, 10);

        helper.getLevel().setBlockAndUpdate(aboveTheTorch, Blocks.STONE.defaultBlockState());
        HorizonSetup overTheLight = buildSlotAndHorizons(helper, lightBlock, 0.625);
        for (int sample = 0; sample < LightPillarHorizons.SAMPLES; sample++) {
            assertCeilingSample(helper, overTheLight, 0, sample, overTheBlock);
            assertTopSample(helper, overTheLight, 0, sample, closed);
            assertCeilingSample(helper, overTheLight, 8, sample, overTheBlockDiagonally);
            assertTopSample(helper, overTheLight, 8, sample, closed);
        }
        assertSample(helper, overTheLight, 0, 0, LightPillarHorizons.OPEN);
        assertSample(helper, overTheLight, 0, 15, onOpenGround);

        helper.getLevel().setBlockAndUpdate(aboveTheTorch, Blocks.AIR.defaultBlockState());
        BlockPos onTheTorch = highAbove(helper, 10, 2, 10);
        helper.getLevel().setBlockAndUpdate(onTheTorch, Blocks.STONE.defaultBlockState());
        HorizonSetup onTopOfTheLight = buildSlotAndHorizons(helper, lightBlock, 0.625);
        assertCeilingSample(helper, onTopOfTheLight, 0, 15, justOverTheTorch);

        helper.getLevel().setBlockAndUpdate(onTheTorch, Blocks.AIR.defaultBlockState());
        for (int z = 9; z <= 11; z++) {
            helper.getLevel().setBlockAndUpdate(highAbove(helper, 11, 2, z), leaves);
        }
        HorizonSetup slit = buildSlotAndHorizons(helper, lightBlock, 0.625);
        for (int sample = 5; sample <= 7; sample++) {
            assertSample(helper, slit, 0, sample, underTheFloor);
            assertCeilingSample(helper, slit, 0, sample, underTheSlit);
            assertTopSample(helper, slit, 0, sample, overTheSlit);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsJoinLowBlocksToTheGround(GameTestHelper helper) {
        double overTheWall = Math.atan(1.375 / 1.5);
        double underTheHighLeaves = Math.atan(4.375 / 4.5);
        double overTheHighLeaves = Math.atan(5.375 / 3.5);
        fillGrassFloor(helper);
        BlockPos lightBlock = highAbove(helper, 10, 1, 10);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int y = 1; y <= 2; y++) {
            for (int z = 8; z <= 12; z++) {
                helper.getLevel().setBlockAndUpdate(highAbove(helper, 12, y, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int z = 8; z <= 12; z++) {
            helper.getLevel().setBlockAndUpdate(highAbove(helper, 14, 3, z), leaves);
        }
        HorizonSetup lowLeaves = buildSlotAndHorizons(helper, lightBlock, 0.625);
        for (int sample = 8; sample <= 15; sample++) {
            assertSample(helper, lowLeaves, 0, sample, overTheWall);
            assertCeilingSample(helper, lowLeaves, 0, sample, overTheWall);
            assertTopSample(helper, lowLeaves, 0, sample, overTheWall);
        }

        for (int z = 8; z <= 12; z++) {
            helper.getLevel().setBlockAndUpdate(highAbove(helper, 14, 3, z), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(highAbove(helper, 14, 6, z), leaves);
        }
        HorizonSetup highLeaves = buildSlotAndHorizons(helper, lightBlock, 0.625);
        for (int sample = 8; sample <= 11; sample++) {
            assertSample(helper, highLeaves, 0, sample, overTheWall);
            assertCeilingSample(helper, highLeaves, 0, sample, overTheWall);
            assertTopSample(helper, highLeaves, 0, sample, overTheWall);
        }
        for (int sample = 12; sample <= 15; sample++) {
            assertSample(helper, highLeaves, 0, sample, overTheWall);
            assertCeilingSample(helper, highLeaves, 0, sample, underTheHighLeaves);
            assertTopSample(helper, highLeaves, 0, sample, overTheHighLeaves);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsMarkOnlyTheBinsAChangeReaches(GameTestHelper helper) {
        int[] columnsRead = {0};
        LightPillarHorizons.Columns columns = new SolidColumns((blockX, blockZ) -> {
            columnsRead[0]++;
            return LightPillarHorizons.NO_BLOCKS;
        });
        long acrossEast = LightPillarHorizons.binsAcross(0.5, 0.5, 2, 0, 1);
        helper.assertTrue(acrossEast == 0xE00000000000000FL, "The bins across a column 2 blocks east are "
                + describeBins(acrossEast) + " instead of 0xE00000000000000F (bins 61 to 3)");
        long acrossWest = LightPillarHorizons.binsAcross(0.5, 0.5, -3, 0, 1);
        helper.assertTrue(acrossWest == 0x7C0000000L, "The bins across a column 3 blocks west are "
                + describeBins(acrossWest) + " instead of 0x7C0000000 (bins 30 to 34)");
        long acrossAFarSquare = LightPillarHorizons.binsAcross(0.5, 0.5, 16, 0, 16);
        helper.assertTrue(acrossAFarSquare == 0x1FFL, "The bins across a square 16 blocks east are "
                + describeBins(acrossAFarSquare) + " instead of 0x1FF (bins 0 to 8)");
        long acrossThePoint = LightPillarHorizons.binsAcross(0.5, 0.5, 0, 0, 1);
        helper.assertTrue(acrossThePoint == -1L, "The bins across a square holding the light are "
                + describeBins(acrossThePoint) + " instead of every bin");
        long acrossTheEdge = LightPillarHorizons.binsAcross(1.0, 0.5, 1, 0, 1);
        helper.assertTrue(acrossTheEdge == -1L, "The bins across a square holding the light on its edge are "
                + describeBins(acrossTheEdge) + " instead of every bin");
        double expectedReach = 16.0 * (Math.sqrt(72.0) + Math.sqrt(2.0));
        helper.assertTrue(Math.abs(LightPillarHorizons.reachForChunkRadius(8) - expectedReach) <= 1.0e-9,
                "The reach for 8 chunks is " + LightPillarHorizons.reachForChunkRadius(8) + " instead of "
                        + expectedReach);

        LightPillarClusters clusters = new LightPillarClusters();
        clusters.beginSection(0, 3, 0);
        clusters.addLight(0, 63, 0, 64.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        LightPillarSelection selection = new LightPillarSelection();
        selection.choose(clusters, 3.5, 64.6, 0.5, 1000.0, WIDE_CHUNK_RADIUS);
        int slot = usedSlot(helper, selection);
        LightPillarHorizons horizons = new LightPillarHorizons();

        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == -1L, "A new slot waits to build "
                + describeBins(horizons.dirtyBins(slot)) + " instead of every bin");
        helper.assertTrue(!horizons.isBuilt(slot) && !selection.isReady(slot),
                "A new slot is built or ready before any bin was built");
        helper.assertTrue(columnsRead[0] == 0, "The columns were read " + columnsRead[0]
                + " times before any bin was built");

        horizons.update(selection, columns, 10.0, 1_000_000);
        helper.assertTrue(horizons.dirtyBins(slot) == 0L && horizons.isBuilt(slot) && selection.isReady(slot),
                "A full build leaves the slot dirty " + describeBins(horizons.dirtyBins(slot)) + ", unbuilt, or not "
                        + "ready");
        helper.assertTrue(columnsRead[0] > 0, "A full build reads no columns");
        assertAllSamplesOpen(helper, horizons, slot);

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        horizons.onBlockChanged(2, 0, stone, dirt);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == 0L, "A change that stops light up to the same height marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of nothing");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.onBlockChanged(2, 0, stone, air);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == 0xE00000000000000FL, "A change 2 blocks east marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of 0xE00000000000000F");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.onBlockChanged(0, 0, stone, air);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == -1L, "A change in the light's own column marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of every bin");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.onBlockChanged(20, 0, stone, air);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == 0L, "A change beyond the reach marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of nothing");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.onChunkChanged(0, 0);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == -1L, "A change of the light's chunk marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of every bin");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.onChunkChanged(5, 0);
        horizons.update(selection, columns, 10.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == 0L, "A change of a chunk beyond the reach marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of nothing");
        horizons.update(selection, columns, 10.0, 1_000_000);

        horizons.update(selection, columns, 40.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == -1L, "A larger reach marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of every bin");
        helper.assertTrue(horizons.reach(slot) == 40.0, "The reach of the slot is " + horizons.reach(slot)
                + " instead of 40.0");
        helper.assertTrue(selection.isReady(slot), "A larger reach unsets ready on a built slot");
        horizons.update(selection, columns, 40.0, 1_000_000);

        horizons.onChunkChanged(1, 0);
        horizons.update(selection, columns, 40.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == 0x1FFL, "A change of the neighboring chunk marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of 0x1FF");
        horizons.update(selection, columns, 40.0, 1_000_000);

        clusters.beginSection(0, 3, 0);
        clusters.addLight(0, 63, 0, 64.0, 1.0F, 1.0F, 1.0F);
        clusters.addLight(1, 63, 0, 64.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        selection.choose(clusters, 3.5, 64.6, 0.5, 1000.0, WIDE_CHUNK_RADIUS);
        horizons.update(selection, columns, 40.0, 0);
        helper.assertTrue(horizons.dirtyBins(slot) == -1L, "A light that moves marks "
                + describeBins(horizons.dirtyBins(slot)) + " instead of every bin");
        helper.assertTrue(selection.isReady(slot), "A light that moves unsets ready on a built slot of the same key");

        int steps = 0;
        while (selection.isUsed(slot) && steps <= LightPillarSelection.FADE_TICKS) {
            selection.chooseNone();
            steps++;
        }
        helper.assertTrue(!selection.isUsed(slot), "The slot does not fade out while choosing nothing");
        horizons.update(selection, columns, 40.0, 1_000_000);
        helper.assertTrue(!horizons.isTracked(slot), "The horizons still track a slot that faded out");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarHorizonsWrapBetweenTheLastAndTheFirstBin(GameTestHelper helper) {
        double reach = 40.0;
        LightPillarHorizons.Columns columns = new SolidColumns((blockX, blockZ) -> {
            if (blockZ <= -2) {
                return 70;
            }
            if (blockZ >= 2) {
                return 66;
            }
            return LightPillarHorizons.NO_BLOCKS;
        });
        LightPillarClusters clusters = new LightPillarClusters();
        clusters.beginSection(0, 3, 0);
        clusters.addLight(0, 63, 0, 64.0, 1.0F, 1.0F, 1.0F);
        clusters.endSection();
        LightPillarSelection selection = new LightPillarSelection();
        selection.choose(clusters, 3.5, 64.6, 0.5, 1000.0, WIDE_CHUNK_RADIUS);
        int slot = usedSlot(helper, selection);
        double originX = selection.x(slot);
        double originZ = selection.z(slot);
        LightPillarHorizons horizons = new LightPillarHorizons();
        horizons.update(selection, columns, reach, 1_000_000);
        helper.assertTrue(horizons.isBuilt(slot), "The slot is not built after a full update");
        int lastSample = LightPillarHorizons.SAMPLES - 1;
        double lastBin = horizons.sample(slot, LightPillarHorizons.BINS - 1, lastSample);
        double firstBin = horizons.sample(slot, 0, lastSample);
        double secondBin = horizons.sample(slot, 1, lastSample);
        helper.assertTrue(lastBin != firstBin && secondBin != firstBin && lastBin != secondBin,
                "The last samples of bins 63, 0, and 1 are " + lastBin + ", " + firstBin + ", and " + secondBin
                        + " instead of three different horizons");
        double angle = LightPillarHorizons.BIN_ANGLE / 4.0;
        double belowTheFirstBin = horizonAtReach(horizons, slot, originX, originZ, reach, -angle);
        double expectedBelow = 0.25 * lastBin + 0.75 * firstBin;
        helper.assertTrue(Math.abs(belowTheFirstBin - expectedBelow) <= 1.0e-5, "A quarter bin below bin 0 the "
                + "horizon is " + belowTheFirstBin + " instead of " + expectedBelow + " from bins 63 and 0");
        double aboveTheFirstBin = horizonAtReach(horizons, slot, originX, originZ, reach, angle);
        double expectedAbove = 0.75 * firstBin + 0.25 * secondBin;
        helper.assertTrue(Math.abs(aboveTheFirstBin - expectedAbove) <= 1.0e-5, "A quarter bin above bin 0 the "
                + "horizon is " + aboveTheFirstBin + " instead of " + expectedAbove + " from bins 0 and 1");

        int steps = 0;
        while (selection.isUsed(slot) && steps <= LightPillarSelection.FADE_TICKS) {
            selection.chooseNone();
            steps++;
        }
        horizons.update(selection, columns, reach, 1_000_000);
        helper.assertTrue(!horizons.isTracked(slot), "The horizons still track a slot that was freed");
        double afterFreeing = horizonAtReach(horizons, slot, originX, originZ, reach, -angle);
        helper.assertTrue(afterFreeing == belowTheFirstBin, "The horizon of a freed slot is " + afterFreeing
                + " instead of its last " + belowTheFirstBin + ", which the slot still shows while it fades out");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lightPillarRibbonEdgesHoldTheVisibleLightAboveAHorizon(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double visible = LightPillarOptics.VISIBLE_VALUE;
        double pixelAngle = pixelAngle(1080);
        double lateralBlur = LightPillarOptics.lateralBlur(pixelAngle);
        double worst = 0.0;
        String worstWhere = "nowhere";
        double bestRise = 0.0;
        String bestRiseWhere = "nowhere";
        for (double horizon : new double[]{0.1, 0.5, 1.0}) {
            for (IceCrystalData.Crystal crystal : plates()) {
                double tiltSigma = Math.toRadians(crystal.tiltDegrees());
                double share = IceCrystalData.get().shareOf(crystal);
                for (double[] light : RIBBON_LIGHTS) {
                    for (double strength : RIBBON_STRENGTHS) {
                        double scale = LightPillarOptics.BRIGHTNESS * strength * share * RIBBON_EXTINCTION;
                        double[][] corners = ribbonEdge(light[0], light[1], light[2], tiltSigma, refractiveIndex,
                                lateralBlur, pixelAngle, scale, horizon);
                        if (corners == null) {
                            continue;
                        }
                        double lowestOpen = LightPillarOptics.lowestRibbonElevation(light[0], light[1], tiltSigma,
                                LightPillarHorizons.OPEN);
                        double rise = corners[0][1] - Math.sin(lowestOpen - pixelAngle);
                        if (rise > bestRise) {
                            bestRise = rise;
                            bestRiseWhere = String.format(Locale.ROOT,
                                    "%s, light %.0f away and %.2f high, dust bottom %.2f, horizon %.2f, the lowest "
                                            + "row %.4f against %.4f",
                                    crystal.name(), light[0], light[1], light[2], horizon, corners[0][1],
                                    Math.sin(lowestOpen - pixelAngle));
                        }
                        for (int segment = 0; segment < RIBBON_SEGMENTS; segment++) {
                            double[] below = corners[segment];
                            double[] above = corners[segment + 1];
                            for (int step = 0; step <= EDGE_STEPS; step++) {
                                double fraction = (double) step / EDGE_STEPS;
                                double x = below[0] + (above[0] - below[0]) * fraction;
                                double y = below[1] + (above[1] - below[1]) * fraction;
                                double z = below[2] + (above[2] - below[2]) * fraction;
                                double length = Math.sqrt(x * x + y * y + z * z);
                                double elevation = Math.asin(y / length);
                                double lateralAngle = Math.asin(z / length);
                                double value = scale * LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        light[0], light[1], tiltSigma, refractiveIndex, lateralBlur, light[2],
                                        RIBBON_EXTINCTION, NO_SCENE, horizon);
                                if (value > worst) {
                                    worst = value;
                                    worstWhere = String.format(Locale.ROOT,
                                            "%s of strength %.2f, light %.0f away and %.2f high, dust bottom %.2f, "
                                                    + "horizon %.2f, elevation %.2f, %.3f degrees off the center",
                                            crystal.name(), strength, light[0], light[1], light[2], horizon,
                                            Math.toDegrees(elevation), Math.toDegrees(lateralAngle));
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("Above a horizon the edges of light pillar ribbons show at most " + worst / visible
                + " times the visible value, at " + worstWhere);
        helper.assertTrue(worst <= 1.01 * visible, "Above a horizon a pillar shows " + worst / visible
                + " times the visible value on the edge of its ribbon, at " + worstWhere);
        LOGGER.info("A horizon raises the lowest row of a ribbon by at most " + bestRise + ", at " + bestRiseWhere);
        helper.assertTrue(bestRise > 0.0, "No horizon raises the lowest row of a ribbon; the largest change is "
                + bestRise + ", at " + bestRiseWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void lightPillarRibbonEdgesHoldTheVisibleLightWithOverhangsAndNearFade(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double visible = LightPillarOptics.VISIBLE_VALUE;
        double pixelAngle = pixelAngle(1080);
        double lateralBlur = LightPillarOptics.lateralBlur(pixelAngle);
        double worst = 0.0;
        String worstWhere = "nowhere";
        for (int caseNumber = 1; caseNumber <= RIBBON_CASES.length; caseNumber++) {
            double[] ribbonCase = RIBBON_CASES[caseNumber - 1];
            double nearFade = ribbonCase[0];
            double horizon = ribbonCase[1];
            double ceiling = ribbonCase[2];
            double top = ribbonCase[3];
            for (IceCrystalData.Crystal crystal : plates()) {
                double tiltSigma = Math.toRadians(crystal.tiltDegrees());
                double share = IceCrystalData.get().shareOf(crystal);
                for (double[] light : RIBBON_LIGHTS) {
                    for (double strength : RIBBON_STRENGTHS) {
                        double scale = LightPillarOptics.BRIGHTNESS * strength * share * RIBBON_EXTINCTION;
                        double[][] corners = ribbonEdge(light[0], light[1], light[2], tiltSigma, refractiveIndex,
                                lateralBlur, pixelAngle, scale, nearFade, horizon, ceiling, top);
                        if (corners == null) {
                            continue;
                        }
                        for (int segment = 0; segment < RIBBON_SEGMENTS; segment++) {
                            double[] below = corners[segment];
                            double[] above = corners[segment + 1];
                            for (int step = 0; step <= EDGE_STEPS; step++) {
                                double fraction = (double) step / EDGE_STEPS;
                                double x = below[0] + (above[0] - below[0]) * fraction;
                                double y = below[1] + (above[1] - below[1]) * fraction;
                                double z = below[2] + (above[2] - below[2]) * fraction;
                                double length = Math.sqrt(x * x + y * y + z * z);
                                double elevation = Math.asin(y / length);
                                double lateralAngle = Math.asin(z / length);
                                double value = scale * LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        light[0], light[1], tiltSigma, refractiveIndex, lateralBlur, light[2],
                                        RIBBON_EXTINCTION, NO_SCENE, nearFade, horizon, ceiling, top);
                                if (value > worst) {
                                    worst = value;
                                    worstWhere = String.format(Locale.ROOT,
                                            "case %d, %s of strength %.2f, light %.0f away and %.2f high, dust bottom "
                                                    + "%.2f, elevation %.2f, %.3f degrees off the center",
                                            caseNumber, crystal.name(), strength, light[0], light[1], light[2],
                                            Math.toDegrees(elevation), Math.toDegrees(lateralAngle));
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("With overhangs and a near fade the edges of light pillar ribbons show at most " + worst / visible
                + " times the visible value, at " + worstWhere);
        helper.assertTrue(worst <= 1.01 * visible, "With overhangs and a near fade a pillar shows " + worst / visible
                + " times the visible value on the edge of its ribbon, at " + worstWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lightPillarCellAtlasKeepsEveryRibbonApart(GameTestHelper helper) {
        double widest = 0.0;
        for (IceCrystalData.Crystal crystal : plates()) {
            widest = Math.max(widest, Math.toRadians(crystal.tiltDegrees()));
        }
        double tightest = Double.POSITIVE_INFINITY;
        String tightestWhere = "nowhere";
        for (double cellDegrees : CELL_ANGLES_DEGREES) {
            double cellAngle = Math.toRadians(cellDegrees);
            int rows = LightPillarCells.atlasRows(cellAngle, widest);
            helper.assertTrue(rows <= LightPillarCells.MOST_ROWS, "A cell of " + cellDegrees + " degrees gives "
                    + rows + " rows of each ribbon instead of at most " + LightPillarCells.MOST_ROWS);
            int height = LightPillarCells.atlasHeight(rows);
            helper.assertTrue(height <= MOST_ATLAS_HEIGHT, "A cell of " + cellDegrees + " degrees gives an atlas "
                    + height + " rows tall instead of at most " + MOST_ATLAS_HEIGHT);
            boolean[] marked = new boolean[LightPillarCells.ATLAS_COLUMNS * height];
            for (int ribbon = 0; ribbon < 2 * LightPillarSelection.SLOT_COUNT; ribbon++) {
                int kind = ribbon - 2 * (ribbon / 2);
                int half = LightPillarCells.halfColumns(kind);
                int axis = LightPillarCells.axisColumn(ribbon);
                int bottom = LightPillarCells.bottomRow(ribbon, rows);
                helper.assertTrue(axis >= half && axis + half < LightPillarCells.ATLAS_COLUMNS, "The ribbon "
                        + ribbon + " of a cell of " + cellDegrees + " degrees reaches the columns " + (axis - half)
                        + " to " + (axis + half) + " instead of within 0 to "
                        + (LightPillarCells.ATLAS_COLUMNS - 1));
                helper.assertTrue(bottom >= 0 && bottom + rows <= height, "The ribbon " + ribbon + " of a cell of "
                        + cellDegrees + " degrees reaches the rows " + bottom + " to " + (bottom + rows - 1)
                        + " instead of within 0 to " + (height - 1));
                for (int column = axis - half; column <= axis + half; column++) {
                    for (int row = bottom; row < bottom + rows; row++) {
                        int texel = row * LightPillarCells.ATLAS_COLUMNS + column;
                        if (marked[texel]) {
                            helper.fail("The texel at column " + column + " and row " + row + " belongs to the ribbon "
                                    + ribbon + " and to another one, at a cell of " + cellDegrees + " degrees");
                        }
                        marked[texel] = true;
                    }
                }
            }
            double covered = rows * cellAngle;
            double needed = LightPillarOptics.HIGHEST_RIBBON_ELEVATION
                    + LightPillarCells.SHADER_TILT_RANGE_SIGMAS * widest + cellAngle;
            if (rows < LightPillarCells.MOST_ROWS) {
                helper.assertTrue(covered >= needed, "A cell of " + cellDegrees + " degrees covers " + covered
                        + " radians in " + rows + " rows instead of the " + needed
                        + " from the lowest cell origin past the top of every ribbon");
                if (covered - needed < tightest) {
                    tightest = covered - needed;
                    tightestWhere = String.format(Locale.ROOT, "%s degrees, %d rows, %.6f radians covered against "
                            + "%.6f", cellDegrees, rows, covered, needed);
                }
            }
            LOGGER.info("A cell of " + cellDegrees + " degrees gives " + rows + " rows of each ribbon in an atlas "
                    + height + " rows tall");
        }
        LOGGER.info("The rows of a ribbon reach the least above the top of the highest ribbon by " + tightest
                + " radians, at " + tightestWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarCellsStandOnTheLight(GameTestHelper helper) {
        double cellAngle = Math.toRadians(CELL_ANGLE_DEGREES);
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            double lowestLevel = -LightPillarCells.SHADER_TILT_RANGE_SIGMAS * tiltSigma;
            for (double elevationDegrees = -60.0; elevationDegrees <= 60.0; elevationDegrees += 0.5) {
                double lightElevation = Math.toRadians(elevationDegrees);
                double expected = lightElevation >= lowestLevel ? lightElevation
                        : -lightElevation - 2.0 * LightPillarCells.SHADER_TILT_RANGE_SIGMAS * tiltSigma;
                double origin = LightPillarCells.cellOrigin(lightElevation, tiltSigma);
                helper.assertTrue(Math.abs(origin - expected) <= 1.0e-12, "The cell origin of the " + crystal.name()
                        + " for a light at " + elevationDegrees + " degrees is " + origin + " instead of " + expected);
            }
            double atTheLowestLevel = LightPillarCells.cellOrigin(lowestLevel, tiltSigma);
            helper.assertTrue(Math.abs(atTheLowestLevel - lowestLevel) <= 1.0e-12, "The cell origin of the "
                    + crystal.name() + " at the lowest level elevation is " + atTheLowestLevel + " instead of "
                    + lowestLevel);
        }
        for (int index = -5; index <= 5; index++) {
            for (double fraction : new double[]{-0.49, 0.0, 0.49}) {
                double lateralAngle = (index + fraction) * cellAngle;
                int column = LightPillarCells.column(lateralAngle, cellAngle);
                helper.assertTrue(column == index, "The lateral angle " + index + " plus " + fraction
                        + " cells is in the column " + column + " instead of " + index);
            }
        }
        for (double origin : new double[]{-0.2, 0.0, 0.3}) {
            for (int row = 0; row <= 20; row++) {
                for (double fraction : new double[]{0.01, 0.5, 0.99}) {
                    double elevation = origin + (row + fraction) * cellAngle;
                    int actual = LightPillarCells.row(elevation, origin, cellAngle);
                    helper.assertTrue(actual == row, "The elevation " + row + " plus " + fraction
                            + " cells above the origin " + origin + " is in the row " + actual + " instead of " + row);
                }
            }
            int below = LightPillarCells.row(origin - 0.01 * cellAngle, origin, cellAngle);
            helper.assertTrue(below == -1, "The elevation a hundredth of a cell below the origin " + origin
                    + " is in the row " + below + " instead of -1");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void lightPillarSceneShareFollowsTheCutIntegral(GameTestHelper helper) {
        double refractiveIndex = IceHaloFormulas.refractiveIndex(1);
        double[][] cases = sceneShareCases();
        List<IceCrystalData.Crystal> plateKinds = plates();
        double[] worstByKind = new double[plateKinds.size()];
        String[] worstWhereByKind = new String[plateKinds.size()];
        for (int kind = 0; kind < plateKinds.size(); kind++) {
            IceCrystalData.Crystal crystal = plateKinds.get(kind);
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            double worst = -1.0;
            String worstWhere = "nowhere";
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance,
                                lightHeight);
                        if (crystalHeight <= Math.max(0.0, lightHeight) + 0.05) {
                            continue;
                        }
                        double mirrorDistance = crystalHeight / Math.sin(elevation);
                        double spread = LightPillarOptics.lateralSpread(elevation, horizontalDistance, lightHeight,
                                tiltSigma);
                        for (double spreads : SCENE_LATERAL_SPREADS) {
                            double lateralAngle = spreads * spread;
                            for (int caseNumber = 0; caseNumber < cases.length; caseNumber++) {
                                double nearFade = cases[caseNumber][0];
                                double horizon = cases[caseNumber][1];
                                double ceiling = cases[caseNumber][2];
                                double top = cases[caseNumber][3];
                                double whole = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                        horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9, 0.0,
                                        NO_SCENE, nearFade, horizon, ceiling, top);
                                if (!(whole > 0.0)) {
                                    continue;
                                }
                                for (double share : SCENE_DISTANCE_SHARES) {
                                    double sceneDistance = share * mirrorDistance;
                                    double cut = LightPillarOptics.dustRadiance(elevation, lateralAngle,
                                            horizontalDistance, lightHeight, tiltSigma, refractiveIndex, 0.0, 1.0e9,
                                            0.0, sceneDistance, nearFade, horizon, ceiling, top);
                                    double expected = cut / whole;
                                    double actual = LightPillarOptics.sceneShare(elevation, horizontalDistance,
                                            lightHeight, tiltSigma, sceneDistance, nearFade, horizon, ceiling, top);
                                    double difference = Math.abs(actual - expected);
                                    if (!(difference <= worst)) {
                                        worst = difference;
                                        worstWhere = String.format(Locale.ROOT, "case %d, light %.0f away and %.0f "
                                                        + "high, elevation %.0f degrees, %.0f spreads off the center, "
                                                        + "near fade %.0f, horizon %.4f, ceiling %.4f, top %.4f, "
                                                        + "scene at %.2f of the crystals, %.4g against %.4g of %.4g",
                                                caseNumber, horizontalDistance, lightHeight, elevationDegrees,
                                                spreads, nearFade, horizon, ceiling, top, share, actual, expected,
                                                whole);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            worstByKind[kind] = worst;
            worstWhereByKind[kind] = worstWhere;
            LOGGER.info("The scene share of " + crystal.name() + " differs from the cut integral by at most " + worst
                    + ", at " + worstWhere);
        }
        for (int kind = 0; kind < plateKinds.size(); kind++) {
            // The share only weighs the tilts by their Gaussian and the near weight, so the reflectance and the across
            // Gaussian are left out:
            double tolerance = plateKinds.get(kind).tiltDegrees() > 1.0 ? WOBBLING_SCENE_SHARE_TOLERANCE
                    : PLATE_SCENE_SHARE_TOLERANCE;
            helper.assertTrue(worstByKind[kind] <= tolerance, "The scene share of " + plateKinds.get(kind).name()
                    + " differs from the cut integral by " + worstByKind[kind] + ", at " + worstWhereByKind[kind]
                    + ", more than " + tolerance);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void lightPillarSceneShareStaysWithinItsBounds(GameTestHelper helper) {
        double[][] cases = sceneShareCases();
        double worstFall = 0.0;
        String worstFallWhere = "nowhere";
        double worstNear = 0.0;
        String worstNearWhere = "nowhere";
        for (IceCrystalData.Crystal crystal : plates()) {
            double tiltSigma = Math.toRadians(crystal.tiltDegrees());
            for (double[] light : PILLAR_LIGHTS) {
                double horizontalDistance = light[0];
                for (int height = 1; height < light.length; height++) {
                    double lightHeight = light[height];
                    double lightDistance = Math.hypot(horizontalDistance, lightHeight);
                    for (double elevationDegrees : PILLAR_ELEVATIONS_DEGREES) {
                        double elevation = Math.toRadians(elevationDegrees);
                        for (int caseNumber = 0; caseNumber < cases.length; caseNumber++) {
                            double nearFade = cases[caseNumber][0];
                            double horizon = cases[caseNumber][1];
                            double ceiling = cases[caseNumber][2];
                            double top = cases[caseNumber][3];
                            String where = String.format(Locale.ROOT, "%s, light %.0f away and %.0f high, elevation "
                                            + "%.0f degrees, near fade %.0f, horizon %.4f, ceiling %.4f, top %.4f",
                                    crystal.name(), horizontalDistance, lightHeight, elevationDegrees, nearFade,
                                    horizon, ceiling, top);
                            double whole = LightPillarOptics.sceneShare(elevation, horizontalDistance, lightHeight,
                                    tiltSigma, NO_SCENE, nearFade, horizon, ceiling, top);
                            helper.assertTrue(whole == 1.0, "Without a scene the share is " + whole
                                    + " instead of 1 for " + where);
                            double previous = -1.0;
                            for (int step = 0; step < SCENE_DISTANCE_STEPS; step++) {
                                double sceneDistance = lightDistance * (NEAREST_SCENE_SHARE
                                        + (FARTHEST_SCENE_SHARE - NEAREST_SCENE_SHARE) * step
                                        / (SCENE_DISTANCE_STEPS - 1.0));
                                double share = LightPillarOptics.sceneShare(elevation, horizontalDistance,
                                        lightHeight, tiltSigma, sceneDistance, nearFade, horizon, ceiling, top);
                                helper.assertTrue(share >= 0.0 && share <= 1.0, "The share is " + share
                                        + " at a scene " + sceneDistance + " blocks away for " + where);
                                if (share < previous - 1.0e-12) {
                                    double fall = previous - share;
                                    if (!(fall <= worstFall)) {
                                        worstFall = fall;
                                        worstFallWhere = String.format(Locale.ROOT, "from %.6f to %.6f at a scene "
                                                + "%.4f blocks away for %s", previous, share, sceneDistance, where);
                                    }
                                }
                                previous = share;
                            }
                            double near = LightPillarOptics.sceneShare(elevation, horizontalDistance, lightHeight,
                                    tiltSigma, NEAREST_SCENE_DISTANCE, nearFade, horizon, ceiling, top);
                            if (!(near <= worstNear)) {
                                worstNear = near;
                                worstNearWhere = where;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("The scene share falls by at most " + worstFall + ", " + worstFallWhere
                + ", and is at most " + worstNear + " with the scene " + NEAREST_SCENE_DISTANCE
                + " blocks away, at " + worstNearWhere);
        helper.assertTrue(worstFall <= 0.0, "The scene share falls as the scene moves away, by " + worstFall + ", "
                + worstFallWhere);
        helper.assertTrue(worstNear <= 1.0e-3, "The scene share is " + worstNear + " with the scene "
                + NEAREST_SCENE_DISTANCE + " blocks away, at " + worstNearWhere);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void lightPillarNormalShareFollowsTheNormalDistribution(GameTestHelper helper) {
        double worst = 0.0;
        String worstWhere = "nowhere";
        for (int step = -600; step <= 600; step++) {
            double deviation = step * 0.01;
            double expected = 0.5 * complementaryErrorFunction(-deviation / Math.sqrt(2.0));
            double actual = LightPillarOptics.normalShare(deviation);
            double difference = Math.abs(actual - expected);
            helper.assertTrue(difference <= NORMAL_SHARE_TOLERANCE, "The normal share at a deviation of " + deviation
                    + " is " + actual + " instead of " + expected + ", off by " + difference);
            if (!(difference <= worst)) {
                worst = difference;
                worstWhere = "a deviation of " + deviation;
            }
            double mirrored = actual + LightPillarOptics.normalShare(-deviation);
            helper.assertTrue(Math.abs(mirrored - 1.0) <= 1.0e-12, "The normal shares at a deviation of " + deviation
                    + " and its mirror add up to " + mirrored + " instead of 1");
        }
        LOGGER.info("The normal share differs from the normal distribution by at most " + worst + ", at " + worstWhere);
        helper.assertTrue(Math.abs(LightPillarOptics.normalShare(0.0) - 0.5) <= 1.0e-15,
                "The normal share at a deviation of 0 is " + LightPillarOptics.normalShare(0.0) + " instead of 0.5");
        helper.succeed();
    }

    private static void assertSkyCheck(GameTestHelper helper, String where, DataLayer sectionSkyLight,
            DataLayer aboveSkyLight, boolean expected) {
        boolean actual = LightPillarClusters.sectionCanShowPillars(sectionSkyLight, aboveSkyLight);
        helper.assertTrue(actual == expected, "A section can show pillars " + actual + " with " + where
                + " instead of " + expected);
    }

    /**
     * A layer of sky light that is 0 everywhere except at the one block.
     */
    private static DataLayer layerWith(int blockX, int blockY, int blockZ, int skyLight) {
        DataLayer layer = new DataLayer();
        layer.set(blockX, blockY, blockZ, skyLight);
        return layer;
    }

    /**
     * Checks that no two clusters share a key, and that each cluster is found by its key at its own index.
     */
    private static void assertDistinctKeys(GameTestHelper helper, LightPillarClusters clusters, String when) {
        for (int cluster = 0; cluster < clusters.count(); cluster++) {
            long key = clusters.key(cluster);
            for (int other = cluster + 1; other < clusters.count(); other++) {
                helper.assertTrue(clusters.key(other) != key, "The clusters " + cluster + " and " + other
                        + " have the same key " + BlockPos.of(key).toShortString() + when);
            }
            helper.assertTrue(clusterWithKey(clusters, key) == cluster, "The cluster " + cluster + " with the key "
                    + BlockPos.of(key).toShortString() + " is not found at its index" + when);
        }
    }

    /**
     * Takes the count sections of the queue and checks that no more are left.
     */
    private static void drainSections(GameTestHelper helper, LightPillarWorkQueue queue, int count, String when) {
        for (int taken = 0; taken < count; taken++) {
            helper.assertTrue(queue.nextSection(), "The queue has only " + taken + " sections " + when
                    + " instead of " + count);
        }
        helper.assertTrue(!queue.nextSection(), "The queue has more than " + count + " sections " + when);
    }

    private static String describeSection(long section) {
        return SectionPos.x(section) + " " + SectionPos.y(section) + " " + SectionPos.z(section);
    }

    private static void assertWithinRadius(GameTestHelper helper, String where, long clusterKey, int chunkRadius,
            boolean expected) {
        boolean actual = LightPillarSelection.isWithinChunkRadius(clusterKey, 0, 0, chunkRadius);
        helper.assertTrue(actual == expected, "The cluster in " + where + " is within " + chunkRadius
                + " chunks " + actual + " instead of " + expected);
    }

    private static int clusterWithKey(LightPillarClusters clusters, long key) {
        for (int cluster = 0; cluster < clusters.count(); cluster++) {
            if (clusters.key(cluster) == key) {
                return cluster;
            }
        }
        return -1;
    }

    private static int slotWithKey(LightPillarSelection selection, long key) {
        for (int slot = 0; slot < LightPillarSelection.SLOT_COUNT; slot++) {
            if (selection.isUsed(slot) && selection.key(slot) == key) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Adds equal lights along the x axis, one per section from x = 8 on, leaving out the light at the skipped index.
     */
    private static void addLightsInARow(LightPillarClusters clusters, int count, int skipped) {
        for (int light = 0; light < count; light++) {
            if (light == skipped) {
                continue;
            }
            clusters.beginSection(light, 0, 0);
            clusters.addLight(16 * light + 8, 0, 0, 1.0, 1.0F, 1.0F, 1.0F);
            clusters.endSection();
        }
    }

    private static long rowKey(int light) {
        return BlockPos.asLong(4 * light + 2, 0, 0);
    }

    private static void stepSelection(LightPillarSelection selection, LightPillarClusters clusters, int steps) {
        for (int step = 0; step < steps; step++) {
            selection.choose(clusters, 0.0, 0.5, 0.5, 1000.0, WIDE_CHUNK_RADIUS);
        }
    }

    /**
     * The radiance of the pillar in dust summed across it, from the given lateral angle on one side to the other.
     */
    private static double lateralIntegral(double elevation, double horizontalDistance, double lightHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double dustBottomHeight, double end) {
        int steps = 4000;
        double step = 2.0 * end / steps;
        double total = 0.0;
        for (int index = 0; index < steps; index++) {
            double lateralAngle = -end + (index + 0.5) * step;
            total += LightPillarOptics.dustRadiance(elevation, lateralAngle, horizontalDistance, lightHeight,
                    tiltSigma, refractiveIndex, lateralBlur, dustBottomHeight, RIBBON_EXTINCTION, NO_SCENE,
                    LightPillarHorizons.OPEN) * step;
        }
        return total;
    }

    /**
     * The corners on one side of a pillar's ribbon from the bottom row up, as the vertex stage of light_pillars.glsl
     * places them at a distance of 1, with x level toward the light, y up, and z across. It is null where the ribbon is
     * collapsed.
     */
    private static double[][] ribbonEdge(double horizontalDistance, double lightHeight, double dustBottomHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double marginAngle, double valueScale,
            double horizon) {
        return ribbonEdge(horizontalDistance, lightHeight, dustBottomHeight, tiltSigma, refractiveIndex, lateralBlur,
                marginAngle, valueScale, 0.0, horizon, horizon, horizon);
    }

    /**
     * The corners on one side of a pillar's ribbon from the bottom row up, as the vertex stage of light_pillars.glsl
     * places them at a distance of 1, with x level toward the light, y up, and z across, for the near fade and the
     * angles the horizons around the light let through. It is null where the ribbon is collapsed.
     */
    private static double[][] ribbonEdge(double horizontalDistance, double lightHeight, double dustBottomHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double marginAngle, double valueScale,
            double nearFade, double horizon, double ceiling, double top) {
        return ribbonEdge(horizontalDistance, lightHeight, dustBottomHeight, tiltSigma, refractiveIndex, lateralBlur,
                marginAngle, valueScale, 0.0, nearFade, horizon, ceiling, top);
    }

    /**
     * The corners on one side of a pillar's ribbon as above, for crystals that glitter with the scale from
     * dustCrystalCount to the mean count of crystals per sky cell, or 0 where they do not.
     */
    private static double[][] ribbonEdge(double horizontalDistance, double lightHeight, double dustBottomHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double marginAngle, double valueScale,
            double countScale, double nearFade, double horizon, double ceiling, double top) {
        double lowest = LightPillarOptics.lowestRibbonElevation(horizontalDistance, lightHeight, tiltSigma, horizon);
        double highest = LightPillarOptics.HIGHEST_RIBBON_ELEVATION;
        if (!(highest > lowest)) {
            return null;
        }
        double firstRow = lowest - marginAngle;
        double lowestEvaluated = Math.min(lowest + 1.0e-3, highest);
        double[] halfWidths = new double[2 * RIBBON_SEGMENTS + 1];
        for (int halfRow = 0; halfRow <= 2 * RIBBON_SEGMENTS; halfRow++) {
            double rowElevation = ribbonRowElevation(0.5 * halfRow, firstRow);
            double evaluated = Math.max(lowestEvaluated, Math.min(rowElevation, highest));
            halfWidths[halfRow] = ribbonHalfWidthAt(evaluated, horizontalDistance, lightHeight, dustBottomHeight,
                    tiltSigma, refractiveIndex, lateralBlur, marginAngle, valueScale, countScale, nearFade, horizon,
                    ceiling, top);
        }
        double reappearance = top + 2.0 * tiltSigma;
        double[][] corners = new double[RIBBON_SEGMENTS + 1][];
        for (int row = 0; row <= RIBBON_SEGMENTS; row++) {
            int firstHalfRow = Math.max(2 * row - 2, 0);
            int lastHalfRow = Math.min(2 * row + 2, 2 * RIBBON_SEGMENTS);
            double halfWidth = 0.0;
            for (int halfRow = firstHalfRow; halfRow <= lastHalfRow; halfRow++) {
                halfWidth = Math.max(halfWidth, halfWidths[halfRow]);
            }
            if (ceiling < top && reappearance > ribbonRowElevation(0.5 * firstHalfRow, firstRow)
                    && reappearance < ribbonRowElevation(0.5 * lastHalfRow, firstRow)) {
                double evaluated = Math.max(lowestEvaluated, Math.min(reappearance, highest));
                halfWidth = Math.max(halfWidth, ribbonHalfWidthAt(evaluated, horizontalDistance, lightHeight,
                        dustBottomHeight, tiltSigma, refractiveIndex, lateralBlur, marginAngle, valueScale, countScale,
                        nearFade, horizon, ceiling, top));
            }
            double elevation = ribbonRowElevation(row, firstRow);
            corners[row] = new double[]{Math.cos(elevation), Math.sin(elevation), Math.tan(halfWidth)};
        }
        return corners;
    }

    /**
     * The half width of a ribbon row with the margin, as halfWidthAt in light_pillars.glsl gives it.
     */
    private static double ribbonHalfWidthAt(double evaluated, double horizontalDistance, double lightHeight,
            double dustBottomHeight, double tiltSigma, double refractiveIndex, double lateralBlur, double marginAngle,
            double valueScale, double countScale, double nearFade, double horizon, double ceiling, double top) {
        double visible = LightPillarOptics.VISIBLE_VALUE;
        double glint = LightPillarOptics.GLINT_VALUE;
        double center = valueScale * LightPillarOptics.dustRadiance(evaluated, 0.0, horizontalDistance, lightHeight,
                tiltSigma, refractiveIndex, lateralBlur, dustBottomHeight, RIBBON_EXTINCTION, NO_SCENE, nearFade,
                horizon, ceiling, top);
        double meanCount = countScale * LightPillarOptics.dustCrystalCount(evaluated, 0.0, horizontalDistance,
                lightHeight, tiltSigma, lateralBlur, dustBottomHeight, NO_SCENE, nearFade, horizon, ceiling, top);
        boolean glintsShow = meanCount > 0.0
                && meanCount > LightPillarOptics.leastVisibleCount(center / (meanCount * glint));
        if (!(center > visible) && !glintsShow) {
            return 0.0;
        }
        double halfWidth = LightPillarOptics.ribbonHalfWidth(center, meanCount, visible, glint,
                LightPillarOptics.widestLateralSpread(evaluated, horizontalDistance, lightHeight, tiltSigma,
                        lateralBlur, nearFade, horizon, ceiling, top));
        return Math.min(halfWidth + marginAngle, WIDEST_RIBBON_ANGLE);
    }

    /**
     * The chance per twinkle period that a sky cell shows at least the shown value, for its mean value and its mean
     * count of crystals, as LightPillarOptics.sparkle draws the counts.
     */
    private static double visibleGlintChance(double mean, double meanCount, double shownValue) {
        if (!(meanCount > 0.0) || !(mean > 0.0)
                || LightPillarOptics.sparkleCap(meanCount) * mean < shownValue) {
            return 0.0;
        }
        double neededCount = shownValue * meanCount / mean;
        if (meanCount > 16.0) {
            return 0.5 * complementaryErrorFunction((neededCount - meanCount) / Math.sqrt(2.0 * meanCount));
        }
        int needed = Math.max(1, (int) Math.ceil(neededCount));
        double term = Math.exp(-meanCount);
        double below = 0.0;
        for (int count = 0; count < needed; count++) {
            below += term;
            term *= meanCount / (count + 1);
        }
        return Math.max(0.0, 1.0 - below);
    }

    /**
     * The complementary error function to about 1e-7, from Numerical Recipes.
     */
    private static double complementaryErrorFunction(double value) {
        double scaled = 1.0 / (1.0 + 0.5 * Math.abs(value));
        double result = scaled * Math.exp(-value * value - 1.26551223 + scaled * (1.00002368 + scaled * (0.37409196
                + scaled * (0.09678418 + scaled * (-0.18628806 + scaled * (0.27886807 + scaled * (-1.13520398
                + scaled * (1.48851587 + scaled * (-0.82215223 + scaled * 0.17087277)))))))));
        return value >= 0.0 ? result : 2.0 - result;
    }

    private static double ribbonRowElevation(double row, double firstRow) {
        double rowFraction = row / RIBBON_SEGMENTS;
        return firstRow + (LightPillarOptics.HIGHEST_RIBBON_ELEVATION - firstRow) * rowFraction * rowFraction;
    }

    private static double pixelAngle(int screenHeight) {
        return 2.0 * Math.tan(Math.toRadians(35.0)) / screenHeight;
    }

    private static double decodeSrgb(double encoded) {
        return encoded <= 0.04045 ? encoded / 12.92 : Math.pow((encoded + 0.055) / 1.055, 2.4);
    }

    private static List<IceCrystalData.Crystal> plates() {
        List<IceCrystalData.Crystal> plates = new ArrayList<>();
        for (IceCrystalData.Crystal crystal : IceCrystalData.get().crystals()) {
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE) {
                plates.add(crystal);
            }
        }
        return plates;
    }

    /**
     * The plate kind whose tilt has the standard deviation in degrees.
     */
    private static IceCrystalData.Crystal plateWithTilt(GameTestHelper helper, double tiltDegrees) {
        for (IceCrystalData.Crystal crystal : plates()) {
            if (crystal.tiltDegrees() == tiltDegrees) {
                return crystal;
            }
        }
        helper.fail("No plate kind has a tilt of " + tiltDegrees + " degrees");
        return null;
    }

    /**
     * The radiance of a plate kind toward the center of the pillar, for a light at the distance and height, in dust
     * that is dense everywhere, without extinction, blur, or blocks around the light, for the near fade.
     */
    private static double nearRadiance(IceCrystalData.Crystal crystal, double elevation, double horizontalDistance,
            double lightHeight, double refractiveIndex, double nearFade) {
        return LightPillarOptics.dustRadiance(elevation, 0.0, horizontalDistance, lightHeight,
                Math.toRadians(crystal.tiltDegrees()), refractiveIndex, 0.0, 1.0e9, 0.0, NO_SCENE, nearFade,
                LightPillarHorizons.OPEN, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN);
    }

    private static void assertNearWeight(GameTestHelper helper, double distance, double nearFade, double expected) {
        double actual = LightPillarOptics.nearWeight(distance, nearFade);
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-12, "The near weight at " + distance + " blocks with a "
                + "near fade of " + nearFade + " blocks is " + actual + " instead of " + expected);
    }

    /**
     * The radiance of the pillar summed along the view ray from the eye up to the scene distance, with the reflection
     * of every crystal worked out in three dimensions. The light is on the x axis and z points up.
     */
    private static double rayIntegral(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double tiltSigma, double refractiveIndex, double sceneDistance, double horizon) {
        return rayIntegral(elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, refractiveIndex,
                sceneDistance, 0.0, horizon, horizon, horizon);
    }

    /**
     * The radiance of the pillar summed along the view ray from the eye up to the scene distance, with the reflection
     * of every crystal worked out in three dimensions. The light is on the x axis and z points up. Steps whose light
     * leaves the light outside the angles the horizons around the light let through are left out, and steps nearer to
     * the eye than the near fade count less.
     */
    private static double rayIntegral(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double tiltSigma, double refractiveIndex, double sceneDistance, double nearFade,
            double horizon, double ceiling, double top) {
        return raySum(false, elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, refractiveIndex,
                sceneDistance, nearFade, horizon, ceiling, top);
    }

    /**
     * The crystals that mirror the light toward the direction, per unit face area per volume and unit
     * crystalCountScale, counted along the view ray as rayIntegral sums their light. Each counts while its face lies
     * within the spread of its reflection by diffraction, which grows with one over the squared cosine of incidence.
     */
    private static double rayCrystalCount(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double tiltSigma, double sceneDistance, double nearFade, double horizon, double ceiling,
            double top) {
        return raySum(true, elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, 1.0, sceneDistance,
                nearFade, horizon, ceiling, top);
    }

    private static double raySum(boolean countsCrystals, double elevation, double lateralAngle,
            double horizontalDistance, double lightHeight, double tiltSigma, double refractiveIndex,
            double sceneDistance, double nearFade, double horizon, double ceiling, double top) {
        double azimuth = Math.asin(Math.sin(lateralAngle) / Math.cos(elevation));
        double directionX = Math.cos(elevation) * Math.cos(azimuth);
        double directionY = Math.cos(elevation) * Math.sin(azimuth);
        double directionZ = Math.sin(elevation);
        double crystalHeight = LightPillarOptics.mirrorHeight(elevation, horizontalDistance, lightHeight);
        double mirrorDistance = crystalHeight / Math.sin(elevation);
        double lightLeg = Math.hypot(horizontalDistance - mirrorDistance * Math.cos(elevation),
                crystalHeight - lightHeight);
        double window = tiltSigma * lightLeg / (Math.sin(elevation) * Math.cos(elevation));
        double start = Math.max(1.0e-6, mirrorDistance - RAY_WINDOWS * window);
        double end = Math.min(mirrorDistance + RAY_WINDOWS * window, sceneDistance);
        if (!(end > start)) {
            return 0.0;
        }
        double step = (end - start) / RAY_STEPS;
        double total = 0.0;
        for (int index = 0; index < RAY_STEPS; index++) {
            double distance = start + (index + 0.5) * step;
            double toLightX = horizontalDistance - distance * directionX;
            double toLightY = -distance * directionY;
            double toLightZ = lightHeight - distance * directionZ;
            // The light leaves the light toward the crystal rising at this angle over the horizontal:
            double rising = Math.atan2(-toLightZ, Math.hypot(toLightX, toLightY));
            if (!((rising > horizon && rising < ceiling) || rising > top)) {
                continue;
            }
            double lightDistance = Math.sqrt(toLightX * toLightX + toLightY * toLightY + toLightZ * toLightZ);
            double halfX = toLightX / lightDistance - directionX;
            double halfY = toLightY / lightDistance - directionY;
            double halfZ = toLightZ / lightDistance - directionZ;
            double halfLength = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
            if (halfLength < 1.0e-12) {
                continue;
            }
            double tilt = Math.acos(Math.min(1.0, Math.abs(halfZ) / halfLength));
            double cosineOfIncidence = Math.abs(toLightX * halfX + toLightY * halfY + toLightZ * halfZ)
                    / (lightDistance * halfLength);
            if (countsCrystals) {
                double spreadCosine = Math.max(cosineOfIncidence, 0.02);
                total += IceMirror.tiltDensity(tilt, tiltSigma) * distance * distance
                        / (spreadCosine * spreadCosine) * step * rayNearWeight(distance, nearFade);
            } else {
                total += 0.25 * IceMirror.slabReflectance(cosineOfIncidence, refractiveIndex)
                        * IceMirror.tiltDensity(tilt, tiltSigma) / (lightDistance * lightDistance) * step
                        * rayNearWeight(distance, nearFade);
            }
        }
        return total;
    }

    /**
     * The share that the crystals at the distance along the view ray count with for the near fade, worked out here
     * rather than by LightPillarOptics.nearWeight.
     */
    private static double rayNearWeight(double distance, double nearFade) {
        if (nearFade == 0.0) {
            return 1.0;
        }
        double share = Math.max(0.0, Math.min(1.0, (distance - 0.25 * nearFade) / (0.75 * nearFade)));
        return share * share * (3.0 - 2.0 * share);
    }

    /**
     * The near fade and the horizon, ceiling, and top of one case of the ray integral test, for the case number
     * counted from 1 and the elevation and tilt spread in radians.
     */
    private static double[] rayIntegralCase(int caseNumber, double elevation, double tiltSigma) {
        double open = LightPillarHorizons.OPEN;
        switch (caseNumber) {
            case 1:
                return new double[]{8.0, open, open, open};
            case 2:
                return new double[]{32.0, open, open, open};
            case 3:
                return new double[]{0.0, open, elevation - 2.0 * tiltSigma, elevation + 2.0 * tiltSigma};
            case 4:
                return new double[]{0.0, elevation - 6.0 * tiltSigma, elevation, elevation + 4.0 * tiltSigma};
            case 5:
                return new double[]{0.0, elevation - 4.0 * tiltSigma, elevation + 4.0 * tiltSigma,
                        LightPillarHorizons.CLOSED};
            case 6:
                return new double[]{0.0, open, elevation + 2.0 * tiltSigma, LightPillarHorizons.CLOSED};
            default:
                return new double[]{8.0, open, elevation - 2.0 * tiltSigma, elevation + 2.0 * tiltSigma};
        }
    }

    /**
     * The block at the template's relative x and z, the given height above HIGH_FLOOR_Y.
     */
    private static BlockPos highAbove(GameTestHelper helper, int x, int y, int z) {
        BlockPos column = helper.absolutePos(new BlockPos(x, 0, z));
        return new BlockPos(column.getX(), HIGH_FLOOR_Y + y, column.getZ());
    }

    /**
     * Fills a grass floor at HIGH_FLOOR_Y over the template's footprint, so the floor's top lies 1 above HIGH_FLOOR_Y.
     */
    private static void fillGrassFloor(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                helper.getLevel().setBlockAndUpdate(highAbove(helper, x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    /**
     * Sets the states into the 8 columns around the light's column at relative (10, 10), the first state along the
     * axes and the second one along the diagonals, at each height above HIGH_FLOOR_Y from the lowest to the highest.
     */
    private static void placeRingAroundTheLight(GameTestHelper helper, BlockState axisState, BlockState diagonalState,
            int lowestY, int highestY) {
        for (int x = 9; x <= 11; x++) {
            for (int z = 9; z <= 11; z++) {
                if (x == 10 && z == 10) {
                    continue;
                }
                BlockState state = x == 10 || z == 10 ? axisState : diagonalState;
                for (int y = lowestY; y <= highestY; y++) {
                    helper.getLevel().setBlockAndUpdate(highAbove(helper, x, y, z), state);
                }
            }
        }
    }

    /**
     * Chooses the light at the block as the only cluster of a new selection and builds every bin of its slot from the
     * level out to the reach of the block tests. The top of the light lies the given offset above the block.
     */
    private static HorizonSetup buildSlotAndHorizons(GameTestHelper helper, BlockPos lightBlock, double topOffset) {
        LightPillarClusters clusters = new LightPillarClusters();
        clusters.beginSection(SectionPos.blockToSectionCoord(lightBlock.getX()),
                SectionPos.blockToSectionCoord(lightBlock.getY()),
                SectionPos.blockToSectionCoord(lightBlock.getZ()));
        clusters.addLight(lightBlock.getX(), lightBlock.getY(), lightBlock.getZ(), lightBlock.getY() + topOffset,
                1.0F, 1.0F, 1.0F);
        clusters.endSection();
        LightPillarSelection selection = new LightPillarSelection();
        selection.choose(clusters, lightBlock.getX() + 3.5, lightBlock.getY() + 1.6, lightBlock.getZ() + 0.5, 1000.0,
                WIDE_CHUNK_RADIUS);
        int slot = usedSlot(helper, selection);
        LightPillarHorizons horizons = new LightPillarHorizons();
        LightPillarHorizons.LevelColumns columns = new LightPillarHorizons.LevelColumns(horizons.occluders());
        columns.setLevel(helper.getLevel());
        horizons.update(selection, columns, 6.0, 1_000_000);
        helper.assertTrue(horizons.isBuilt(slot) && selection.isReady(slot), "The horizons of the light at "
                + lightBlock.toShortString() + " are not built and ready");
        return new HorizonSetup(selection, horizons, columns, slot, lightBlock);
    }

    /**
     * The slot, horizons, and columns of one light standing in the template, with every bin of its slot built.
     */
    private record HorizonSetup(LightPillarSelection selection, LightPillarHorizons horizons,
            LightPillarHorizons.LevelColumns columns, int slot, BlockPos lightBlock) {
    }

    /**
     * The one used slot of a selection that chose a single cluster.
     */
    private static int usedSlot(GameTestHelper helper, LightPillarSelection selection) {
        int used = -1;
        for (int slot = 0; slot < LightPillarSelection.SLOT_COUNT; slot++) {
            if (selection.isUsed(slot)) {
                helper.assertTrue(used < 0, "The selection uses more than one slot");
                used = slot;
            }
        }
        helper.assertTrue(used >= 0, "The selection uses no slot");
        return used;
    }

    private static void assertStoppedHeight(GameTestHelper helper, LightPillarOccluders occluders, BlockGetter level,
            BlockPos position, BlockState state, double expected) {
        double actual = occluders.stoppedHeight(state, level, position);
        if (expected == LightPillarOccluders.PASSES) {
            helper.assertTrue(actual == LightPillarOccluders.PASSES, "The stopped height of " + state + " is "
                    + actual + " instead of PASSES");
            return;
        }
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-6, "The stopped height of " + state + " is " + actual
                + " instead of " + expected);
    }

    private static void assertStopsTheSame(GameTestHelper helper, LightPillarOccluders occluders, BlockState oldState,
            BlockState newState, boolean expected) {
        boolean actual = occluders.stopsTheSame(oldState, newState);
        helper.assertTrue(actual == expected, "A change from " + oldState + " to " + newState + " stops the light "
                + (actual ? "the same" : "differently") + " instead of "
                + (expected ? "the same" : "differently"));
    }

    private static void assertLightTop(GameTestHelper helper, BlockGetter level, BlockPos position, BlockState state,
            double expected) {
        double actual = LightPillarOccluders.lightTop(state, level, position);
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-6, "The light top of " + state + " is " + actual
                + " instead of " + expected);
    }

    /**
     * Checks that the samples of the bin below the first sample are open and the samples from the first sample on
     * hold the angle.
     */
    private static void assertBinRisesOver(GameTestHelper helper, HorizonSetup setup, int bin, int firstSample,
            double angle) {
        for (int sample = 1; sample < firstSample; sample++) {
            float actual = setup.horizons().sample(setup.slot(), bin, sample);
            helper.assertTrue(actual == LightPillarHorizons.OPEN, "Sample " + sample + " of bin " + bin + " is "
                    + actual + " instead of OPEN (" + LightPillarHorizons.OPEN + ")");
        }
        for (int sample = firstSample; sample < LightPillarHorizons.SAMPLES; sample++) {
            assertSample(helper, setup, bin, sample, angle);
        }
    }

    private static void assertSample(GameTestHelper helper, HorizonSetup setup, int bin, int sample, double expected) {
        float actual = setup.horizons().sample(setup.slot(), bin, sample);
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-6, "Sample " + sample + " of bin " + bin + " is "
                + actual + " instead of " + expected);
    }

    private static void assertCeilingSample(GameTestHelper helper, HorizonSetup setup, int bin, int sample,
            double expected) {
        float actual = setup.horizons().ceilingSample(setup.slot(), bin, sample);
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-6, "The ceiling of sample " + sample + " of bin " + bin
                + " is " + actual + " instead of " + expected);
    }

    private static void assertTopSample(GameTestHelper helper, HorizonSetup setup, int bin, int sample,
            double expected) {
        float actual = setup.horizons().topSample(setup.slot(), bin, sample);
        helper.assertTrue(Math.abs(actual - expected) <= 1.0e-6, "The top of sample " + sample + " of bin " + bin
                + " is " + actual + " instead of " + expected);
    }

    private static void assertCeilingAndTopMatchTheHorizon(GameTestHelper helper, HorizonSetup setup, int bin,
            int sample) {
        float horizon = setup.horizons().sample(setup.slot(), bin, sample);
        float ceiling = setup.horizons().ceilingSample(setup.slot(), bin, sample);
        float top = setup.horizons().topSample(setup.slot(), bin, sample);
        helper.assertTrue(ceiling == horizon && top == horizon, "The ceiling " + ceiling + " and the top " + top
                + " of sample " + sample + " of bin " + bin + " are not the horizon " + horizon);
    }

    private static void assertSamplesNearZero(GameTestHelper helper, HorizonSetup setup, int bin, int firstSample) {
        for (int sample = firstSample; sample < LightPillarHorizons.SAMPLES; sample++) {
            float actual = setup.horizons().sample(setup.slot(), bin, sample);
            helper.assertTrue(Math.abs(actual) <= 1.0e-9, "Sample " + sample + " of bin " + bin + " is " + actual
                    + " instead of 0");
        }
    }

    private static void assertSamplesBetweenOpenAndZero(GameTestHelper helper, HorizonSetup setup) {
        for (int bin : new int[]{0, 8, 16, 32, 48}) {
            for (int sample = 6; sample < LightPillarHorizons.SAMPLES; sample++) {
                float actual = setup.horizons().sample(setup.slot(), bin, sample);
                helper.assertTrue(actual > LightPillarHorizons.OPEN && actual < 0.0F, "Sample " + sample
                        + " of bin " + bin + " is " + actual + " instead of strictly between OPEN ("
                        + LightPillarHorizons.OPEN + ") and 0");
            }
        }
    }

    private static void assertAllSamplesOpen(GameTestHelper helper, LightPillarHorizons horizons, int slot) {
        for (int bin = 0; bin < LightPillarHorizons.BINS; bin++) {
            for (int sample = 0; sample < LightPillarHorizons.SAMPLES; sample++) {
                float actual = horizons.sample(slot, bin, sample);
                helper.assertTrue(actual == LightPillarHorizons.OPEN, "Sample " + sample + " of bin " + bin + " is "
                        + actual + " instead of OPEN");
            }
        }
    }

    /**
     * Checks the horizon of the slot toward the point at the distance from the light's center along the angle from
     * the +x axis toward the z axis.
     */
    private static void assertHorizonToward(GameTestHelper helper, HorizonSetup setup, double distance, double angle,
            double expected, double tolerance) {
        double pointX = setup.lightBlock().getX() + 0.5 + distance * Math.cos(angle);
        double pointZ = setup.lightBlock().getZ() + 0.5 + distance * Math.sin(angle);
        double actual = setup.horizons().horizon(setup.slot(), pointX, pointZ);
        helper.assertTrue(Math.abs(actual - expected) <= tolerance, "The horizon " + distance + " blocks from the "
                + "light at " + angle + " radians is " + actual + " instead of " + expected);
    }

    /**
     * The horizon of the slot toward the point at the reach from the light's center along the angle from the +x axis
     * toward the z axis, which is the last samples of the two nearest bins.
     */
    private static double horizonAtReach(LightPillarHorizons horizons, int slot, double originX, double originZ,
            double reach, double angle) {
        return horizons.horizon(slot, originX + reach * Math.cos(angle), originZ + reach * Math.sin(angle));
    }

    private static String describeBins(long bins) {
        return "0x" + Long.toHexString(bins);
    }

    /**
     * The near fade and the horizon, ceiling, and top of the cases of the scene share tests, the open case without a
     * near fade first.
     */
    private static double[][] sceneShareCases() {
        double[][] cases = new double[RIBBON_CASES.length + 1][];
        cases[0] = new double[]{0.0, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN, LightPillarHorizons.OPEN};
        System.arraycopy(RIBBON_CASES, 0, cases, 1, RIBBON_CASES.length);
        return cases;
    }

    /**
     * The height up to which a column is solid, or LightPillarHorizons.NO_BLOCKS for a column that cannot be read.
     */
    private interface ColumnHeights {
        int solidUpTo(int blockX, int blockZ);
    }

    /**
     * Columns that stop light with whole blocks from SOLID_FROM up to a height per column.
     */
    private static final class SolidColumns implements LightPillarHorizons.Columns {
        private static final int SOLID_FROM = -64;
        private final ColumnHeights heights;
        private int solidUpTo;

        private SolidColumns(ColumnHeights heights) {
            this.heights = heights;
        }

        @Override
        public int beginColumn(int blockX, int blockZ) {
            solidUpTo = heights.solidUpTo(blockX, blockZ);
            return solidUpTo == LightPillarHorizons.NO_BLOCKS ? LightPillarHorizons.NO_BLOCKS : solidUpTo - 1;
        }

        @Override
        public double stoppedHeight(int blockY) {
            return blockY < solidUpTo ? 1.0 : LightPillarOccluders.PASSES;
        }

        @Override
        public int lowestBlockY() {
            return SOLID_FROM;
        }
    }
}
