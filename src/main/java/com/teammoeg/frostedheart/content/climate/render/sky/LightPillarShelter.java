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

import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Locale;

/**
 * The shelter of one light, read from the blocks right at it, which LightPillarSources reads for every light it finds
 * and LightPillarClusters keeps. Its cap is a height below which light_pillars.glsl shows the pillar, its rim in each
 * of BINS directions an angle above which it shows, and its ceiling in each direction an angle below which it shows.
 * A light is read from the center of its block.
 * A column's cap is the bottom of the lowest block in it that stops light from above the light up. The cap is the
 * light's own column's, except for a full block whose top is covered, whose light leaves through its sides: its cap is
 * the highest of its 4 side columns' caps, or OPEN_CAP where one of them has none, so a lamp set into a wall shows its
 * pillar through the column in front of it.
 * The rim follows the walls around the light. The columns within RIM_REACH blocks of the light's column each have a
 * wall where the block at the flame's level stops light, up to the top of the blocks that stop light from there up.
 * Light leaves the light toward the eye for the whole pillar, so in each direction a ray from the light meets the
 * walls' squares, and the direction's rim is the steepest of the wall tops seen from the top of the light where the ray
 * enters them, or OPEN_RIM where it meets none.
 * The ceilings follow the overhangs around the light. Each of those columns has an overhang at the bottom of the
 * lowest block that stops light above the air over its wall, or over the flame's level where it has no wall, read up
 * to WALL_HEIGHT above the top of the light. Light rising in a direction meets an overhang once it reaches the
 * overhang's bottom where the ray leaves its square, so the direction's ceiling is the lowest of those bottoms seen
 * from the top of the light there, or OPEN_CEILING where it crosses none. Light never passes above an overhang.
 * Heights are world heights in blocks and angles are in radians, the directions counted from +x toward +z, with
 * direction i centered at (i + 0.5) times BIN_ANGLE. Not thread safe.
 */
public final class LightPillarShelter {
    /**
     * The rim of a direction without a wall.
     */
    public static final double OPEN_RIM = -0.5 * Math.PI;
    /**
     * The ceiling of a direction without an overhang.
     */
    public static final double OPEN_CEILING = 0.5 * Math.PI;
    public static final double OPEN_CAP = Double.POSITIVE_INFINITY;
    /**
     * Columns up to this many blocks from the light's column along each axis can hold a wall or an overhang.
     */
    public static final int RIM_REACH = 2;
    /**
     * Walls and overhangs are read up to this many blocks above the top of the light.
     */
    public static final int WALL_HEIGHT = 16;
    /**
     * The directions around the light whose walls and overhangs are read. No direction from the center of a block aims
     * at a corner of the blocks around it.
     */
    public static final int BINS = 32;
    public static final double BIN_ANGLE = 2.0 * Math.PI / BINS;
    /**
     * Lights compare their rims and ceilings rounded to this step, so lights whose walls cut them alike merge.
     */
    public static final double COMPARED_ANGLE_STEP = Math.toRadians(2.0);
    /**
     * Walls and overhangs nearer to the light than this count as this far, which keeps the angle finite where a ray
     * enters a wall at the light.
     */
    private static final double NEAREST_WALL = 0.05;
    /**
     * A column's square is widened by this on each side, so a ray through the corner where two columns meet enters
     * both.
     */
    private static final double SQUARE_MARGIN = 1.0e-4;
    private static final double NO_WALL = Double.NEGATIVE_INFINITY;
    private static final double NO_OVERHANG = Double.POSITIVE_INFINITY;
    private static final double NO_HIT = -1.0;
    private static final int[] SIDE_OFFSETS_X = {1, -1, 0, 0};
    private static final int[] SIDE_OFFSETS_Z = {0, 0, 1, -1};
    private static final int RIM_COLUMNS = (2 * RIM_REACH + 1) * (2 * RIM_REACH + 1) - 1;
    private static final int[] RIM_OFFSETS_X = new int[RIM_COLUMNS];
    private static final int[] RIM_OFFSETS_Z = new int[RIM_COLUMNS];
    private static final double[] BIN_COSINES = new double[BINS];
    private static final double[] BIN_SINES = new double[BINS];
    /**
     * The columns of a chunk section and RIM_REACH around it along each axis, which the lights of the section read.
     */
    private static final int CACHE_SIDE = 16 + 2 * RIM_REACH;

    static {
        int column = 0;
        for (int offsetX = -RIM_REACH; offsetX <= RIM_REACH; offsetX++) {
            for (int offsetZ = -RIM_REACH; offsetZ <= RIM_REACH; offsetZ++) {
                if (offsetX != 0 || offsetZ != 0) {
                    RIM_OFFSETS_X[column] = offsetX;
                    RIM_OFFSETS_Z[column] = offsetZ;
                    column++;
                }
            }
        }
        for (int bin = 0; bin < BINS; bin++) {
            BIN_COSINES[bin] = Math.cos((bin + 0.5) * BIN_ANGLE);
            BIN_SINES[bin] = Math.sin((bin + 0.5) * BIN_ANGLE);
        }
    }

    private final LightPillarOccluders occluders = new LightPillarOccluders();
    private final double[] wallTops = new double[RIM_COLUMNS];
    private final double[] overhangBottoms = new double[RIM_COLUMNS];
    private final double[] rims = new double[BINS];
    private final double[] ceilings = new double[BINS];
    /**
     * Where the ray that enterSquare followed last leaves the square.
     */
    private double squareExit;
    /**
     * The wall tops and overhang bottoms read in the current section, by column from the cache's lowest corner, with
     * the flame's level and the highest block they were read for and the section they were read in.
     */
    private final double[] cachedWallTops = new double[CACHE_SIDE * CACHE_SIDE];
    private final double[] cachedOverhangBottoms = new double[CACHE_SIDE * CACHE_SIDE];
    private final int[] cachedFlameLevels = new int[CACHE_SIDE * CACHE_SIDE];
    private final int[] cachedHighestBlocks = new int[CACHE_SIDE * CACHE_SIDE];
    private final int[] cachedSections = new int[CACHE_SIDE * CACHE_SIDE];
    private boolean caching;
    private int cacheSection;
    private int cacheLowestX;
    private int cacheLowestZ;

    public LightPillarOccluders occluders() {
        return occluders;
    }

    /**
     * Keeps the columns read from now until endSection, for the lights of the chunk section whose lowest corner is at
     * the block coordinates, while nothing in the world changes. Lights at the same flame's level and height then read
     * each column once.
     */
    public void beginSection(int lowestX, int lowestZ) {
        caching = true;
        cacheLowestX = lowestX - RIM_REACH;
        cacheLowestZ = lowestZ - RIM_REACH;
        cacheSection++;
        if (cacheSection == 0) {
            Arrays.fill(cachedSections, 0);
            cacheSection = 1;
        }
    }

    /**
     * Reads every column again from now on.
     */
    public void endSection() {
        caching = false;
    }

    /**
     * Reads the rims and the ceilings of a light with its top at the height in the column at the block coordinates,
     * which rim and ceiling then give.
     */
    public void read(LightPillarHorizons.Columns columns, int lightX, int lightZ, double top) {
        int flameY = Mth.floor(top);
        int highestY = Mth.ceil(top + WALL_HEIGHT) - 1;
        boolean blocked = false;
        for (int column = 0; column < RIM_COLUMNS; column++) {
            readCachedColumn(columns, lightX + RIM_OFFSETS_X[column], lightZ + RIM_OFFSETS_Z[column], flameY,
                    highestY, column);
            blocked |= wallTops[column] != NO_WALL || overhangBottoms[column] != NO_OVERHANG;
        }
        double x = lightX + 0.5;
        double z = lightZ + 0.5;
        for (int bin = 0; bin < BINS; bin++) {
            double steepest = OPEN_RIM;
            double lowest = OPEN_CEILING;
            for (int column = 0; blocked && column < RIM_COLUMNS; column++) {
                double wallTop = wallTops[column];
                double overhangBottom = overhangBottoms[column];
                if (wallTop == NO_WALL && overhangBottom == NO_OVERHANG) {
                    continue;
                }
                double entry = enterSquare(x, z, BIN_COSINES[bin], BIN_SINES[bin], lightX + RIM_OFFSETS_X[column],
                        lightZ + RIM_OFFSETS_Z[column]);
                if (entry == NO_HIT) {
                    continue;
                }
                if (wallTop != NO_WALL) {
                    steepest = Math.max(steepest, Math.atan2(wallTop - top, Math.max(entry, NEAREST_WALL)));
                }
                if (overhangBottom != NO_OVERHANG) {
                    lowest = Math.min(lowest, Math.atan2(overhangBottom - top, Math.max(squareExit, NEAREST_WALL)));
                }
            }
            rims[bin] = steepest;
            ceilings[bin] = lowest;
        }
    }

    /**
     * The rim of the direction that read gave last, or OPEN_RIM.
     */
    public double rim(int bin) {
        return rims[bin];
    }

    /**
     * The ceiling of the direction that read gave last, or OPEN_CEILING.
     */
    public double ceiling(int bin) {
        return ceilings[bin];
    }

    /**
     * Sets the wall top and the overhang bottom of the column as readColumn does, from the cache where the column was
     * read for the same flame's level and highest block since beginSection.
     */
    private void readCachedColumn(LightPillarHorizons.Columns columns, int columnX, int columnZ, int flameY,
            int highestY, int column) {
        int cacheX = columnX - cacheLowestX;
        int cacheZ = columnZ - cacheLowestZ;
        if (!caching || cacheX < 0 || cacheX >= CACHE_SIDE || cacheZ < 0 || cacheZ >= CACHE_SIDE) {
            readColumn(columns, columnX, columnZ, flameY, highestY, column);
            return;
        }
        int cached = cacheZ * CACHE_SIDE + cacheX;
        if (cachedSections[cached] == cacheSection && cachedFlameLevels[cached] == flameY
                && cachedHighestBlocks[cached] == highestY) {
            wallTops[column] = cachedWallTops[cached];
            overhangBottoms[column] = cachedOverhangBottoms[cached];
            return;
        }
        readColumn(columns, columnX, columnZ, flameY, highestY, column);
        cachedSections[cached] = cacheSection;
        cachedFlameLevels[cached] = flameY;
        cachedHighestBlocks[cached] = highestY;
        cachedWallTops[cached] = wallTops[column];
        cachedOverhangBottoms[cached] = overhangBottoms[column];
    }

    /**
     * Sets the wall top and the overhang bottom of the column. The wall is the blocks that stop light from the flame's
     * level up, NO_WALL where the block at the flame's level lets light through, and the overhang the bottom of the
     * lowest block that stops light above it, NO_OVERHANG where none does, both read up to the highest block, which is
     * WALL_HEIGHT above the top of the light.
     */
    private void readColumn(LightPillarHorizons.Columns columns, int columnX, int columnZ, int flameY, int highestRead,
            int column) {
        wallTops[column] = NO_WALL;
        overhangBottoms[column] = NO_OVERHANG;
        int surface = columns.beginColumn(columnX, columnZ);
        if (surface == LightPillarHorizons.NO_BLOCKS || flameY > surface) {
            return;
        }
        int highestY = Math.min(surface, highestRead);
        int blockY = flameY;
        for (; blockY <= highestY; blockY++) {
            double height = columns.stoppedHeight(blockY);
            if (height == LightPillarOccluders.PASSES) {
                break;
            }
            wallTops[column] = blockY + height;
        }
        for (blockY++; blockY <= highestY; blockY++) {
            if (columns.stoppedHeight(blockY) != LightPillarOccluders.PASSES) {
                overhangBottoms[column] = blockY;
                return;
            }
        }
    }

    /**
     * The distance along a level ray from the origin in the direction to where it enters the square of the column,
     * widened by SQUARE_MARGIN, 0 where it starts inside, or NO_HIT where it misses. Where it hits, squareExit is
     * where it leaves the square.
     */
    private double enterSquare(double originX, double originZ, double directionX, double directionZ, int columnX,
            int columnZ) {
        double nearest = 0.0;
        double farthest = Double.POSITIVE_INFINITY;
        double lowestX = columnX - SQUARE_MARGIN;
        double highestX = columnX + 1.0 + SQUARE_MARGIN;
        if (directionX == 0.0) {
            if (originX < lowestX || originX > highestX) {
                return NO_HIT;
            }
        } else {
            double first = (lowestX - originX) / directionX;
            double second = (highestX - originX) / directionX;
            nearest = Math.max(nearest, Math.min(first, second));
            farthest = Math.min(farthest, Math.max(first, second));
        }
        double lowestZ = columnZ - SQUARE_MARGIN;
        double highestZ = columnZ + 1.0 + SQUARE_MARGIN;
        if (directionZ == 0.0) {
            if (originZ < lowestZ || originZ > highestZ) {
                return NO_HIT;
            }
        } else {
            double first = (lowestZ - originZ) / directionZ;
            double second = (highestZ - originZ) / directionZ;
            nearest = Math.max(nearest, Math.min(first, second));
            farthest = Math.min(farthest, Math.max(first, second));
        }
        if (nearest > farthest) {
            return NO_HIT;
        }
        squareExit = farthest;
        return nearest;
    }

    /**
     * The cap of a light with its top at the height in the column at the block coordinates, as a world height or
     * OPEN_CAP: its own column's, or for a full block whose top is covered the highest of its 4 side columns' caps.
     */
    static double readLightCap(LightPillarHorizons.Columns columns, int lightX, int lightZ, double top,
            boolean fullBlock) {
        double cap = readCap(columns, lightX, lightZ, top);
        if (fullBlock && cap == Mth.ceil(top)) {
            cap = Double.NEGATIVE_INFINITY;
            for (int side = 0; side < SIDE_OFFSETS_X.length; side++) {
                cap = Math.max(cap, readCap(columns, lightX + SIDE_OFFSETS_X[side], lightZ + SIDE_OFFSETS_Z[side],
                        top));
            }
        }
        return cap;
    }

    private static double readCap(LightPillarHorizons.Columns columns, int columnX, int columnZ, double top) {
        int surface = columns.beginColumn(columnX, columnZ);
        if (surface == LightPillarHorizons.NO_BLOCKS) {
            return OPEN_CAP;
        }
        for (int blockY = Mth.ceil(top); blockY <= surface; blockY++) {
            if (columns.stoppedHeight(blockY) != LightPillarOccluders.PASSES) {
                return blockY;
            }
        }
        return OPEN_CAP;
    }

    /**
     * The rim toward the eye of the angles stored per direction from the offset, for a light at the origin, an angle
     * over the horizontal seen from the top of the light, blended linearly between the two directions whose centers
     * lie nearest to the eye's direction from the light. Directions whose walls stay below the horizon count as 0, so
     * the rim is never below 0, where it lifts no pillar.
     */
    public static double rimToward(float[] rims, int offset, double originX, double originZ, double eyeX,
            double eyeZ) {
        double position = binPosition(originX, originZ, eyeX, eyeZ);
        double lowerPosition = Math.floor(position);
        double share = position - lowerPosition;
        int lowerBin = Math.floorMod((int) lowerPosition, BINS);
        int upperBin = (lowerBin + 1) % BINS;
        return (1.0 - share) * Math.max(0.0, rims[offset + lowerBin])
                + share * Math.max(0.0, rims[offset + upperBin]);
    }

    /**
     * The ceiling toward the eye of the angles stored per direction from the offset, for a light at the origin,
     * blended linearly between the two directions whose centers lie nearest to the eye's direction from the light, so
     * OPEN_CEILING where both are open.
     */
    public static double ceilingToward(float[] ceilings, int offset, double originX, double originZ, double eyeX,
            double eyeZ) {
        double position = binPosition(originX, originZ, eyeX, eyeZ);
        double lowerPosition = Math.floor(position);
        double share = position - lowerPosition;
        int lowerBin = Math.floorMod((int) lowerPosition, BINS);
        int upperBin = (lowerBin + 1) % BINS;
        return (1.0 - share) * Math.min(OPEN_CEILING, ceilings[offset + lowerBin])
                + share * Math.min(OPEN_CEILING, ceilings[offset + upperBin]);
    }

    /**
     * The eye's direction from the light in bins, with direction i at i.
     */
    private static double binPosition(double originX, double originZ, double eyeX, double eyeZ) {
        return Math.atan2(eyeZ - originZ, eyeX - originX) / BIN_ANGLE - 0.5;
    }

    /**
     * The step of COMPARED_ANGLE_STEP that a rim or a ceiling rounds to when lights are compared, between 0 and the
     * vertical, so every rim below the horizon compares as 0 and every ceiling near the vertical as open.
     */
    public static int comparedStep(double angle) {
        return (int) Math.round(Math.max(0.0, Math.min(OPEN_CEILING, angle)) / COMPARED_ANGLE_STEP);
    }

    /**
     * A line about the cap, the rim and the ceiling toward the eye, and how many directions have neither a wall nor an
     * overhang, for the client command.
     */
    static String describe(double cap, double rimToward, double ceilingToward, int openBins) {
        String capText = cap == OPEN_CAP ? "cap open" : String.format(Locale.ROOT, "cap y %.2f", cap);
        String ceilingText = ceilingToward < OPEN_CEILING
                ? String.format(Locale.ROOT, "ceiling %.1f degrees", Math.toDegrees(ceilingToward)) : "ceiling open";
        return String.format(Locale.ROOT, "%s, rim %.1f degrees and %s toward the eye, %d of %d directions open",
                capText, Math.toDegrees(rimToward), ceilingText, openBins, BINS);
    }
}
