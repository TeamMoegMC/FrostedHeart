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

import com.teammoeg.frostedheart.FHMain;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The ice crystal tables in assets/frostedheart/datasets/ice_halos, loaded once and immutable afterwards.
 * The tables are written by the offline sky preview, as described in the dataset's src.md.
 */
public final class IceCrystalData {
    public static final int SIDE_PATH = 0;
    public static final int SIDE_MIRRORED_PATH = 1;
    public static final int END_TO_SIDE_PATH = 2;
    public static final int END_TO_END_PATH = 3;
    public static final int END_MIRROR_PATH = 4;
    public static final int OTHER_PATH = 5;
    public static final int PATH_COUNT = 6;
    public static final int SCATTERING_ANGLE_BINS = 3600;
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final String FOLDER = "ice_halos";

    public enum Orientation {
        RANDOM,
        PLATE,
        COLUMN,
        PARRY
    }

    /**
     * One kind of ice crystal. Tilt is the spread of the crystal axis in degrees, and rotation is the spread of a
     * Parry column's turn about its axis in degrees.
     */
    public record Crystal(String name, Orientation orientation, double share, double halfHeightOverRadius,
            double tiltDegrees, double rotationDegrees) {
        String fileName() {
            return name.toLowerCase(Locale.ROOT).replace(' ', '_');
        }
    }

    private static final class Holder {
        private static final IceCrystalData INSTANCE = load();
    }

    private final List<Crystal> crystals;
    private final double shareSum;
    private final double[] sideFaceIndices;
    private final float[][] sideFaceDensities;
    private final double sideFaceDeviationStep;
    private final Map<String, PathShares> pathShares;
    private final Map<String, double[][]> scattering;
    private final double[] tumblingAsymmetry = new double[CHANNELS];

    private record PathShares(double lowestElevationDegrees, double[][] rows) {
    }

    private IceCrystalData(List<Crystal> crystals, double[] sideFaceIndices, float[][] sideFaceDensities,
            Map<String, PathShares> pathShares, Map<String, double[][]> scattering) {
        this.crystals = Collections.unmodifiableList(crystals);
        double sum = 0.0;
        for (Crystal crystal : crystals) {
            sum += crystal.share();
        }
        this.shareSum = sum;
        this.sideFaceIndices = sideFaceIndices;
        this.sideFaceDensities = sideFaceDensities;
        this.sideFaceDeviationStep = Math.PI / sideFaceDensities[0].length;
        this.pathShares = pathShares;
        this.scattering = scattering;
        double angleStep = Math.PI / SCATTERING_ANGLE_BINS;
        for (int channel = 0; channel < CHANNELS; channel++) {
            double energy = 0.0;
            double cosineEnergy = 0.0;
            for (Crystal crystal : crystals) {
                if (crystal.orientation() != Orientation.RANDOM) {
                    continue;
                }
                double[] angles = scattering.get(crystal.name())[channel];
                for (int bin = 0; bin < SCATTERING_ANGLE_BINS; bin++) {
                    double solidAngle = 2.0 * Math.PI * (Math.cos(bin * angleStep) - Math.cos((bin + 1) * angleStep));
                    double weighted = crystal.share() * angles[bin] * solidAngle;
                    energy += weighted;
                    cosineEnergy += weighted * Math.cos((bin + 0.5) * angleStep);
                }
            }
            tumblingAsymmetry[channel] = energy > 0.0 ? cosineEnergy / energy : 0.0;
        }
    }

    public static IceCrystalData get() {
        return Holder.INSTANCE;
    }

    public List<Crystal> crystals() {
        return crystals;
    }

    /**
     * The share of a crystal kind among all crystals, from 0 to 1.
     */
    public double shareOf(Crystal crystal) {
        return crystal.share() / shareSum;
    }

    public int sideFaceDeviationColumns() {
        return sideFaceDensities[0].length;
    }

    public double sideFaceDeviationStep() {
        return sideFaceDeviationStep;
    }

    public int sideFaceIndexRows() {
        return sideFaceIndices.length;
    }

    public double sideFaceIndex(int row) {
        return sideFaceIndices[row];
    }

    public float sideFaceDensity(int row, int column) {
        return sideFaceDensities[row][column];
    }

    /**
     * Writes the density of in-plane deviation through side faces, per radian of absolute deviation and averaged over
     * each column, for an effective refractive index. Rows are interpolated linearly and clamped at both ends.
     */
    public void sideFaceDensities(double effectiveIndex, double[] result) {
        int last = sideFaceIndices.length - 1;
        int lower;
        double fraction;
        if (effectiveIndex <= sideFaceIndices[0]) {
            lower = 0;
            fraction = 0.0;
        } else if (effectiveIndex >= sideFaceIndices[last]) {
            lower = last - 1;
            fraction = 1.0;
        } else {
            int low = 0;
            int high = last;
            while (high - low > 1) {
                int middle = (low + high) >>> 1;
                if (sideFaceIndices[middle] <= effectiveIndex) {
                    low = middle;
                } else {
                    high = middle;
                }
            }
            lower = low;
            fraction = (effectiveIndex - sideFaceIndices[low]) / (sideFaceIndices[low + 1] - sideFaceIndices[low]);
        }
        float[] lowerRow = sideFaceDensities[lower];
        float[] upperRow = sideFaceDensities[lower + 1];
        for (int column = 0; column < lowerRow.length; column++) {
            result[column] = lowerRow[column] * (1.0 - fraction) + upperRow[column] * fraction;
        }
    }

    /**
     * Writes the share of light on each path class by channel for an oriented crystal kind, interpolated between
     * whole degrees of sun elevation and clamped to the table.
     */
    public void pathShares(Crystal crystal, double sunElevationDegrees, double[][] result) {
        PathShares shares = pathShares.get(crystal.name());
        double position = sunElevationDegrees - shares.lowestElevationDegrees();
        int last = shares.rows().length - 1;
        int lower = (int) Math.floor(Math.max(0.0, Math.min(last - 1.0, position)));
        double fraction = Math.max(0.0, Math.min(1.0, position - lower));
        double[] lowerRow = shares.rows()[lower];
        double[] upperRow = shares.rows()[lower + 1];
        for (int channel = 0; channel < CHANNELS; channel++) {
            for (int path = 0; path < PATH_COUNT; path++) {
                int column = channel * PATH_COUNT + path;
                result[channel][path] = lowerRow[column] * (1.0 - fraction) + upperRow[column] * fraction;
            }
        }
    }

    public int pathShareRows(Crystal crystal) {
        return pathShares.get(crystal.name()).rows().length;
    }

    public double pathShare(Crystal crystal, int row, int channel, int path) {
        return pathShares.get(crystal.name()).rows()[row][channel * PATH_COUNT + path];
    }

    /**
     * The scattering phase function of a tumbling crystal kind per steradian, in bins of the angle from the sun of
     * 180 / SCATTERING_ANGLE_BINS degrees.
     */
    public double[] scattering(Crystal crystal, int channel) {
        return scattering.get(crystal.name())[channel];
    }

    /**
     * The mean cosine of the scattering angle of the tumbling crystal kinds, weighted by their shares, without
     * diffraction.
     */
    public double tumblingAsymmetry(int channel) {
        return tumblingAsymmetry[channel];
    }

    private static IceCrystalData load() {
        List<Crystal> crystals = new ArrayList<>();
        List<String[]> crystalRows = readCsv("crystals.csv");
        for (int row = 1; row < crystalRows.size(); row++) {
            String[] cells = crystalRows.get(row);
            crystals.add(new Crystal(cells[0], Orientation.valueOf(cells[1].toUpperCase(Locale.ROOT)),
                    number(cells[2]), number(cells[3]), number(cells[4]), number(cells[5])));
        }

        List<String[]> sideFaceRows = readCsv("side_faces.csv");
        int columns = sideFaceRows.get(0).length - 1;
        double[] indices = new double[sideFaceRows.size() - 1];
        float[][] densities = new float[indices.length][columns];
        for (int row = 0; row < indices.length; row++) {
            String[] cells = sideFaceRows.get(row + 1);
            indices[row] = number(cells[0]);
            for (int column = 0; column < columns; column++) {
                densities[row][column] = (float) number(cells[column + 1]);
            }
        }

        Map<String, PathShares> pathShares = new HashMap<>();
        Map<String, double[][]> scattering = new HashMap<>();
        for (Crystal crystal : crystals) {
            if (crystal.orientation() == Orientation.RANDOM) {
                List<String[]> rows = readCsv("scattering_" + crystal.fileName() + ".csv");
                double[][] table = new double[CHANNELS][SCATTERING_ANGLE_BINS];
                for (int bin = 0; bin < SCATTERING_ANGLE_BINS; bin++) {
                    String[] cells = rows.get(bin + 1);
                    for (int channel = 0; channel < CHANNELS; channel++) {
                        table[channel][bin] = number(cells[channel + 1]);
                    }
                }
                scattering.put(crystal.name(), table);
                continue;
            }
            List<String[]> rows = readCsv("paths_" + crystal.fileName() + ".csv");
            double[][] table = new double[rows.size() - 1][CHANNELS * PATH_COUNT];
            for (int row = 0; row < table.length; row++) {
                String[] cells = rows.get(row + 1);
                for (int column = 0; column < CHANNELS * PATH_COUNT; column++) {
                    table[row][column] = number(cells[column + 1]);
                }
            }
            pathShares.put(crystal.name(), new PathShares(number(rows.get(1)[0]), table));
        }
        return new IceCrystalData(crystals, indices, densities, pathShares, scattering);
    }

    private static double number(String cell) {
        return Double.parseDouble(cell.trim());
    }

    private static List<String[]> readCsv(String fileName) {
        Path path = ModList.get().getModFileById(FHMain.MODID).getFile()
                .findResource("assets", FHMain.MODID, "datasets", FOLDER, fileName);
        String text;
        try {
            text = Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the ice crystal dataset " + fileName, exception);
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                rows.add(line.split(","));
            }
        }
        return rows;
    }
}
