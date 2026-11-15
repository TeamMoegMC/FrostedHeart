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

import java.util.Arrays;
import java.util.List;

/**
 * Phase function per steradian of the oriented ice crystals for one sun elevation, as an RGB table of
 * AZIMUTH_BINS by ELEVATION_BINS bins. Columns are the absolute azimuth from the sun over 0 to pi, since the display
 * is mirror symmetric, and rows are elevation from LOWEST_ELEVATION to the zenith in equal steps.
 * The table is blurred by the sun disc and by part of a sky texel, so one lookup per texel is enough.
 * Light of the oriented crystals that leaves below LOWEST_ELEVATION is dropped. The plates' mirror pillar is the
 * exception while the sun is in the table, since its part below LOWEST_ELEVATION stays at the sun.
 * An instance keeps its table until it is built again, and a map that anything still reads must not be rebuilt.
 * The lookup is repeated in calculated_sky.fsh and must stay equal.
 */
public final class IceHaloMap {
    public static final int AZIMUTH_BINS = 360;
    public static final double BIN_STEP = Math.PI / AZIMUTH_BINS;
    public static final double LOWEST_ELEVATION = Math.toRadians(-2.0);
    public static final int ELEVATION_BINS = (int) Math.ceil((Math.PI / 2.0 - LOWEST_ELEVATION) / BIN_STEP);
    public static final double SUN_RADIUS_RADIANS = Math.toRadians(0.2665);
    /**
     * The plate kinds of the data set, in its order, whose mirror lobe scale the map keeps.
     */
    public static final int MIRROR_KINDS = 2;
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int BIN_COUNT = AZIMUTH_BINS * ELEVATION_BINS;
    private static final double TEXEL_BLUR_FRACTION = 0.45;
    private static final double BLUR_SPAN_SIGMAS = 3.0;
    private static final int MAXIMUM_BLUR_SPAN = Math.max(AZIMUTH_BINS, ELEVATION_BINS - 1);
    private static final int LOOKUP_CELLS = 65536;
    private static final double[] ROW_SOLID_ANGLES = new double[ELEVATION_BINS];
    private static final double[] ROW_INVERSE_SOLID_ANGLES = new double[ELEVATION_BINS];
    private static final double LOWEST_SINE = Math.sin(LOWEST_ELEVATION);
    private static final double[] ROW_BOUNDARY_SINES = new double[ELEVATION_BINS + 1];
    private static final int[] ROW_LOOKUP = new int[LOOKUP_CELLS];
    private static final double[] COLUMN_BOUNDARY_KEYS = new double[AZIMUTH_BINS + 1];
    private static final int[] COLUMN_LOOKUP = new int[LOOKUP_CELLS];

    static {
        for (int row = 0; row < ELEVATION_BINS; row++) {
            double lower = LOWEST_ELEVATION + row * BIN_STEP;
            double upper = Math.min(Math.PI / 2.0, lower + BIN_STEP);
            ROW_SOLID_ANGLES[row] = 2.0 * BIN_STEP * (Math.sin(upper) - Math.sin(lower));
            ROW_INVERSE_SOLID_ANGLES[row] = 1.0 / ROW_SOLID_ANGLES[row];
        }
        for (int row = 0; row <= ELEVATION_BINS; row++) {
            ROW_BOUNDARY_SINES[row] = Math.sin(Math.min(Math.PI / 2.0, LOWEST_ELEVATION + row * BIN_STEP));
        }
        for (int column = 0; column <= AZIMUTH_BINS; column++) {
            double azimuth = column * BIN_STEP;
            COLUMN_BOUNDARY_KEYS[column] = azimuthKey(Math.cos(azimuth), Math.abs(Math.sin(azimuth)));
        }
        int row = 0;
        int column = 0;
        for (int cell = 0; cell < LOOKUP_CELLS; cell++) {
            double height = LOWEST_SINE + (double) cell / LOOKUP_CELLS * (1.0 - LOWEST_SINE);
            while (row + 1 < ELEVATION_BINS && height >= ROW_BOUNDARY_SINES[row + 1]) {
                row++;
            }
            ROW_LOOKUP[cell] = row;
            double key = 2.0 * cell / LOOKUP_CELLS;
            while (column + 1 < AZIMUTH_BINS && key >= COLUMN_BOUNDARY_KEYS[column + 1]) {
                column++;
            }
            COLUMN_LOOKUP[cell] = column;
        }
    }

    /**
     * The buffers that building maps needs, kept from build to build so that a build allocates nothing.
     * One thread at a time may build with a workspace.
     */
    static final class Workspace {
        private final IceCrystalData data;
        private final IceHaloFormulas formulas;
        private final double[] sun = new double[3];
        private final double[][] pathShares = new double[CHANNELS][IceCrystalData.PATH_COUNT];
        private final double[][] accumulated = new double[CHANNELS][BIN_COUNT];
        private final double[][] elevationBlurred = new double[CHANNELS][BIN_COUNT];
        private final double[] extended = new double[3 * AZIMUTH_BINS];
        private final double[] kernel = new double[2 * MAXIMUM_BLUR_SPAN + 1];

        Workspace(IceCrystalData data) {
            this.data = data;
            this.formulas = new IceHaloFormulas(data);
        }
    }

    private double sunElevationRadians;
    private double blurSigma;
    private final float[] values = new float[BIN_COUNT * CHANNELS];
    private final double[][] mirrorLobeScales = new double[MIRROR_KINDS][CHANNELS];

    /**
     * An empty map for build to fill.
     */
    IceHaloMap() {
    }

    /**
     * Builds the table for a sun elevation with a workspace of its own.
     */
    public IceHaloMap(IceCrystalData data, double sunElevationRadians, double blurSigma) {
        build(new Workspace(data), sunElevationRadians, blurSigma);
    }

    /**
     * Fills the table for a sun elevation, with the oriented crystals of the data set sharing the light that is
     * not diffracted.
     */
    void build(Workspace workspace, double sunElevationRadians, double blurSigma) {
        this.sunElevationRadians = sunElevationRadians;
        this.blurSigma = blurSigma;
        for (double[] scales : mirrorLobeScales) {
            Arrays.fill(scales, 0.0);
        }
        IceCrystalData data = workspace.data;
        double[] sun = workspace.sun;
        sun[0] = Math.cos(sunElevationRadians);
        sun[1] = Math.sin(sunElevationRadians);
        sun[2] = 0.0;
        double sunElevationDegrees = Math.toDegrees(sunElevationRadians);
        double[][] accumulated = workspace.accumulated;
        for (int channel = 0; channel < CHANNELS; channel++) {
            Arrays.fill(accumulated[channel], 0.0);
        }
        double refracted = 1.0 - SkyAtmosphere.ICE_DIFFRACTION_FRACTION;
        List<IceCrystalData.Crystal> crystals = data.crystals();
        int plateKind = 0;
        for (int index = 0; index < crystals.size(); index++) {
            IceCrystalData.Crystal crystal = crystals.get(index);
            if (crystal.orientation() == IceCrystalData.Orientation.RANDOM) {
                continue;
            }
            data.pathShares(crystal, sunElevationDegrees, workspace.pathShares);
            workspace.formulas.addCrystal(accumulated, crystal, sun, refracted * data.shareOf(crystal),
                    workspace.pathShares);
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE && plateKind < MIRROR_KINDS) {
                for (int channel = 0; channel < CHANNELS; channel++) {
                    mirrorLobeScales[plateKind][channel] = workspace.formulas.mirrorLobeScale(channel);
                }
                plateKind++;
            }
        }
        blurElevation(accumulated, workspace.elevationBlurred, workspace.kernel, blurSigma);
        blurAzimuth(workspace.elevationBlurred, values, workspace.extended, workspace.kernel, blurSigma);
    }

    /**
     * The blur that keeps arcs about equally bright as they move across sky texels of the given size in degrees,
     * combined with the sun disc. Sizes below the bin size, including 0 for a smooth sky, blur as the bin size, since
     * the map holds no finer detail.
     */
    public static double blurSigma(double texelDegrees) {
        double texelSigma = TEXEL_BLUR_FRACTION * Math.max(Math.toRadians(texelDegrees), BIN_STEP);
        double sunDiscSigma = SUN_RADIUS_RADIANS / 2.0;
        return Math.sqrt(texelSigma * texelSigma + sunDiscSigma * sunDiscSigma);
    }

    public double sunElevationRadians() {
        return sunElevationRadians;
    }

    /**
     * The blur the map was built with, as a standard deviation in radians.
     */
    public double blurSigma() {
        return blurSigma;
    }

    /**
     * The factor on IceMirror.pillarPhase that gives the mirror pillar of a plate kind per steradian over the whole
     * sky, of which the map holds the part above LOWEST_ELEVATION. The kind is its place among the plate kinds of the
     * data set.
     */
    public double mirrorLobeScale(int kind, int channel) {
        return mirrorLobeScales[kind][channel];
    }

    /**
     * The table as interleaved RGB, by column and then by row, as it is uploaded.
     */
    public float[] values() {
        return values;
    }

    public double value(int channel, int column, int row) {
        return values[(column + row * AZIMUTH_BINS) * CHANNELS + channel];
    }

    static int binOf(double x, double y, double z) {
        if (y < LOWEST_SINE) {
            return -1;
        }
        int row = ROW_LOOKUP[Math.min(LOOKUP_CELLS - 1, (int) ((y - LOWEST_SINE) / (1.0 - LOWEST_SINE) * LOOKUP_CELLS))];
        while (row + 1 < ELEVATION_BINS && y >= ROW_BOUNDARY_SINES[row + 1]) {
            row++;
        }
        double key = azimuthKey(x, Math.abs(z));
        int column = COLUMN_LOOKUP[Math.min(LOOKUP_CELLS - 1, (int) (key * 0.5 * LOOKUP_CELLS))];
        while (column + 1 < AZIMUTH_BINS && key >= COLUMN_BOUNDARY_KEYS[column + 1]) {
            column++;
        }
        return column + row * AZIMUTH_BINS;
    }

    /**
     * A number that grows with the azimuth from 0 to pi, from 0 to 2, without trigonometry.
     */
    private static double azimuthKey(double x, double absoluteZ) {
        if (x >= 0.0) {
            double sum = x + absoluteZ;
            return sum > 0.0 ? absoluteZ / sum : 0.0;
        }
        return 1.0 - x / (absoluteZ - x);
    }

    /**
     * The solid angle of a bin, counting both mirrored halves of the sky.
     */
    static double binSolidAngle(int bin) {
        return ROW_SOLID_ANGLES[bin / AZIMUTH_BINS];
    }

    static double inverseBinSolidAngle(int bin) {
        return ROW_INVERSE_SOLID_ANGLES[bin / AZIMUTH_BINS];
    }

    public static int rowOfElevation(double elevation) {
        return (int) Math.floor((elevation - LOWEST_ELEVATION) / BIN_STEP);
    }

    /**
     * The total of the table over the sphere, which is the share of ice light the oriented crystals keep above
     * LOWEST_ELEVATION.
     */
    public double total(int channel) {
        double sum = 0.0;
        for (int bin = 0; bin < BIN_COUNT; bin++) {
            sum += values[bin * CHANNELS + channel] * binSolidAngle(bin);
        }
        return sum;
    }

    /**
     * Phase per steradian toward a direction given in the frame of the sky view tables, where x points to the
     * sun's azimuth. Interpolated bilinearly, mirrored at azimuth 0 and pi, and clamped at the lowest and highest row.
     */
    public double phase(int channel, double[] direction) {
        double elevation = Math.asin(Math.max(-1.0, Math.min(1.0, direction[1])));
        double rowPosition = (elevation - LOWEST_ELEVATION) / BIN_STEP - 0.5;
        double columnPosition = Math.abs(Math.atan2(direction[2], direction[0])) / BIN_STEP - 0.5;
        int row = (int) Math.floor(rowPosition);
        int column = (int) Math.floor(columnPosition);
        double rowFraction = Math.max(0.0, Math.min(1.0, rowPosition - row));
        double columnFraction = columnPosition - column;
        int rowLow = Math.max(0, Math.min(ELEVATION_BINS - 1, row));
        int rowHigh = Math.max(0, Math.min(ELEVATION_BINS - 1, row + 1));
        int columnLow = mirroredColumn(column);
        int columnHigh = mirroredColumn(column + 1);
        double low = value(channel, columnLow, rowLow) * (1.0 - columnFraction)
                + value(channel, columnHigh, rowLow) * columnFraction;
        double high = value(channel, columnLow, rowHigh) * (1.0 - columnFraction)
                + value(channel, columnHigh, rowHigh) * columnFraction;
        return low * (1.0 - rowFraction) + high * rowFraction;
    }

    /**
     * Writes the irradiance on level ground from the oriented crystals per unit of the ice table for every channel
     * into the result, summed bin by bin above the horizon. Samples is a scratch array of one entry per channel.
     */
    void irradiance(float[] iceTable, double[] result, double[] samples) {
        Arrays.fill(result, 0.0);
        for (int row = 0; row < ELEVATION_BINS; row++) {
            double elevation = LOWEST_ELEVATION + (row + 0.5) * BIN_STEP;
            if (elevation <= 0.0) {
                continue;
            }
            double height = Math.sin(elevation);
            for (int column = 0; column < AZIMUTH_BINS; column++) {
                int texel = (column + row * AZIMUTH_BINS) * CHANNELS;
                boolean lit = false;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    lit |= values[texel + channel] != 0.0F;
                }
                if (!lit) {
                    continue;
                }
                SkyView.sampleChannels(iceTable, (column + 0.5) * BIN_STEP, elevation, samples);
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double phase = values[texel + channel];
                    if (phase != 0.0) {
                        result[channel] += phase * ROW_SOLID_ANGLES[row] * height * samples[channel];
                    }
                }
            }
        }
    }

    private static int mirroredColumn(int column) {
        int period = Math.floorMod(column, 2 * AZIMUTH_BINS);
        return period < AZIMUTH_BINS ? period : 2 * AZIMUTH_BINS - period - 1;
    }

    /**
     * Blurs each column over elevation from the source into the target, weighting rows by their solid angle.
     * Rows farther than the whole table never meet, so the span stops at ELEVATION_BINS - 1.
     */
    private static void blurElevation(double[][] maps, double[][] result, double[] kernel, double blurSigma) {
        for (int channel = 0; channel < CHANNELS; channel++) {
            Arrays.fill(result[channel], 0.0);
        }
        double sigmaBins = blurSigma / BIN_STEP;
        int span = (int) Math.min(ELEVATION_BINS - 1, Math.ceil(BLUR_SPAN_SIGMAS * sigmaBins));
        for (int offset = -span; offset <= span; offset++) {
            kernel[offset + span] = Math.exp(-0.5 * offset * offset / (sigmaBins * sigmaBins));
        }
        for (int row = 0; row < ELEVATION_BINS; row++) {
            int first = Math.max(0, row - span);
            int last = Math.min(ELEVATION_BINS - 1, row + span);
            double weightSum = 0.0;
            for (int other = first; other <= last; other++) {
                weightSum += kernel[other - row + span] * ROW_SOLID_ANGLES[other];
            }
            int targetOffset = row * AZIMUTH_BINS;
            for (int other = first; other <= last; other++) {
                double weight = kernel[other - row + span] * ROW_SOLID_ANGLES[other] / weightSum;
                int sourceOffset = other * AZIMUTH_BINS;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double[] source = maps[channel];
                    double[] target = result[channel];
                    for (int column = 0; column < AZIMUTH_BINS; column++) {
                        target[targetOffset + column] += weight * source[sourceOffset + column];
                    }
                }
            }
        }
    }

    /**
     * Blurs each row over azimuth from the source into the interleaved float values, mirrored at azimuth 0 and pi.
     */
    private static void blurAzimuth(double[][] maps, float[] values, double[] extended, double[] kernel,
            double blurSigma) {
        for (int row = 0; row < ELEVATION_BINS; row++) {
            double elevation = LOWEST_ELEVATION + (row + 0.5) * BIN_STEP;
            double sigmaBins = blurSigma / BIN_STEP / Math.max(1.0e-6, Math.cos(elevation));
            int rowOffset = row * AZIMUTH_BINS;
            if (sigmaBins > 0.5 * AZIMUTH_BINS) {
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double sum = 0.0;
                    for (int column = 0; column < AZIMUTH_BINS; column++) {
                        sum += maps[channel][rowOffset + column];
                    }
                    for (int column = 0; column < AZIMUTH_BINS; column++) {
                        values[(rowOffset + column) * CHANNELS + channel] = (float) (sum / AZIMUTH_BINS);
                    }
                }
                continue;
            }
            int span = Math.min(AZIMUTH_BINS, (int) Math.ceil(BLUR_SPAN_SIGMAS * sigmaBins));
            double kernelSum = 0.0;
            for (int offset = -span; offset <= span; offset++) {
                kernel[offset + span] = Math.exp(-0.5 * offset * offset / (sigmaBins * sigmaBins));
                kernelSum += kernel[offset + span];
            }
            for (int channel = 0; channel < CHANNELS; channel++) {
                double[] source = maps[channel];
                for (int column = 0; column < AZIMUTH_BINS; column++) {
                    extended[AZIMUTH_BINS + column] = source[rowOffset + column];
                }
                for (int offset = 1; offset <= span; offset++) {
                    extended[AZIMUTH_BINS - offset] = source[rowOffset + offset - 1];
                    extended[2 * AZIMUTH_BINS + offset - 1] = source[rowOffset + AZIMUTH_BINS - offset];
                }
                for (int column = 0; column < AZIMUTH_BINS; column++) {
                    double sum = 0.0;
                    int center = column + AZIMUTH_BINS;
                    for (int offset = -span; offset <= span; offset++) {
                        sum += kernel[offset + span] * extended[center + offset];
                    }
                    values[(rowOffset + column) * CHANNELS + channel] = (float) (sum / kernelSum);
                }
            }
        }
    }
}
