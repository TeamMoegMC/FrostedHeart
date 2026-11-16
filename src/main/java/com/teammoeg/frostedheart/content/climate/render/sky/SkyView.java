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

/**
 * Sky radiance for one sun elevation, as two RGB tables of SkyTables.VIEW_WIDTH by SkyTables.VIEW_HEIGHT texels.
 * Columns are azimuth from the sun, and rows follow the latitude mapping of Hillaire 2020.
 * The sky table already contains light reflected between snow and atmosphere, and no single scattering by ice.
 * The ice table holds single scattering by ice per unit phase function, which the ice phase of IceHaloProfile and
 * IceHaloMap scales per sky texel, with the diffraction peak scaled by SkyTables.diffractionWeights.
 * The mean horizon radiance is the sky table at elevation zero, averaged over all azimuths.
 * SkyTables.computeView fills an instance, and a view that anything still reads must not be filled again.
 * The texel lookup is repeated in calculated_sky.fsh and must stay equal.
 */
public final class SkyView {
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int TABLE_LENGTH = SkyTables.VIEW_WIDTH * SkyTables.VIEW_HEIGHT * CHANNELS;

    private double sunElevationRadians;
    private final float[] sky = new float[TABLE_LENGTH];
    private final float[] ice = new float[TABLE_LENGTH];
    private final double[] groundRadiance = new double[CHANNELS];
    private final double[] meanHorizonRadiance = new double[CHANNELS];
    private final double[] lightTransmittance = new double[CHANNELS];
    private final double[] adaptationRadiance = new double[CHANNELS];
    private final double[] brightestHorizonRadiance = new double[CHANNELS];

    public double sunElevationRadians() {
        return sunElevationRadians;
    }

    void setSunElevationRadians(double sunElevationRadians) {
        this.sunElevationRadians = sunElevationRadians;
    }

    public float[] sky() {
        return sky;
    }

    public float[] ice() {
        return ice;
    }

    public double[] groundRadiance() {
        return groundRadiance;
    }

    public double[] meanHorizonRadiance() {
        return meanHorizonRadiance;
    }

    /**
     * The share of the light's beam that reaches the eye, with the light diffracted by ice counted as transmitted.
     * It is 0 while the light is below the eye's horizon.
     */
    public double[] lightTransmittance() {
        return lightTransmittance;
    }

    /**
     * The sky's mean radiance weighted by the cosine to the zenith, which is its irradiance on level ground over pi,
     * with the light of the ice and the glow of the ground's light in the air, but without the light itself and the
     * ice's diffraction peak around it. The exposure adapts to it.
     */
    public double[] adaptationRadiance() {
        return adaptationRadiance;
    }

    /**
     * The sky table at elevation zero toward the azimuth where its luminance is highest, among the azimuths of the
     * mean horizon radiance. After sunset it is the glow toward the sun.
     */
    public double[] brightestHorizonRadiance() {
        return brightestHorizonRadiance;
    }

    /**
     * Vanilla's lightmap color at full sky light for a sky darkness from ClientLevel.getSkyDarken, outside lightning
     * flashes.
     */
    public static float vanillaSkyLight(int channel, float skyDarken) {
        float tint = channel == 2 ? 1.0F : skyDarken + (1.0F - skyDarken) * 0.35F;
        return tint * (skyDarken * 0.95F + 0.05F);
    }

    /**
     * The sky table at elevation zero toward the azimuth, blended toward the mean horizon radiance by one minus the
     * horizontal fraction.
     */
    double horizonRadiance(int channel, double azimuth, double horizontalFraction) {
        return horizontalFraction * sample(sky, channel, azimuth, 0.0)
                + (1.0 - horizontalFraction) * meanHorizonRadiance[channel];
    }

    /**
     * Radiance toward a direction given by azimuth from the sun and elevation, with the ice phase of the given
     * profile and map, and the diffraction weights of the tables the view was computed with. The sun's square is
     * left out.
     */
    public double radiance(int channel, double azimuth, double elevation, IceHaloProfile haloProfile,
            IceHaloMap haloMap, float[] diffractionWeights) {
        double directionY = Math.sin(elevation);
        if (SkyAtmosphere.distanceToGroundOrInfinity(SkyAtmosphere.CAMERA_RADIUS, directionY)
                < Double.POSITIVE_INFINITY) {
            return groundRadiance[channel];
        }
        double[] direction = {
                Math.cos(elevation) * Math.cos(azimuth), directionY, Math.cos(elevation) * Math.sin(azimuth)
        };
        double sunCosine = direction[0] * Math.cos(sunElevationRadians) + directionY * Math.sin(sunElevationRadians);
        double diffractionWeight = sampleRow(diffractionWeights, channel, elevation);
        double icePhase = haloProfile.phase(channel, sunCosine, diffractionWeight) + haloMap.phase(channel, direction);
        return sample(sky, channel, azimuth, elevation) + icePhase * sample(ice, channel, azimuth, elevation);
    }

    /**
     * Samples a table of one RGB entry per row of the sky view, with the same rows as sample.
     */
    static double sampleRow(float[] rowTable, int channel, double elevation) {
        double rowPosition = rowPositionAt(elevation);
        int row = Math.min((int) rowPosition, SkyTables.VIEW_HEIGHT - 2);
        double rowFraction = rowPosition - row;
        return rowTable[row * CHANNELS + channel] * (1.0 - rowFraction)
                + rowTable[(row + 1) * CHANNELS + channel] * rowFraction;
    }

    static double latitudeToElevation(double latitude) {
        double scale = 1.0 - Math.abs(latitude) * 2.0 / Math.PI;
        return latitude - SkyAtmosphere.HORIZON_DIP_RADIANS * scale;
    }

    static double elevationToLatitude(double elevation) {
        double offset = elevation + SkyAtmosphere.HORIZON_DIP_RADIANS;
        if (offset >= 0.0) {
            return offset / (1.0 + SkyAtmosphere.HORIZON_DIP_RADIANS * 2.0 / Math.PI);
        }
        return offset / (1.0 - SkyAtmosphere.HORIZON_DIP_RADIANS * 2.0 / Math.PI);
    }

    /**
     * Where an elevation falls between the rows of the view, from 0 at the lowest row to VIEW_HEIGHT - 1 at the
     * highest, by the latitude mapping of Hillaire 2020.
     */
    private static double rowPositionAt(double elevation) {
        int height = SkyTables.VIEW_HEIGHT;
        double latitude = elevationToLatitude(elevation);
        double position = Math.signum(latitude) * Math.sqrt(Math.abs(latitude) * 2.0 / Math.PI);
        double rowPosition = (0.5 + 0.5 * position) * height - 0.5;
        return Math.max(0.0, Math.min(height - 1.0, rowPosition));
    }

    static double sample(float[] table, int channel, double azimuth, double elevation) {
        int width = SkyTables.VIEW_WIDTH;
        int height = SkyTables.VIEW_HEIGHT;
        double rowPosition = rowPositionAt(elevation);
        double columnPosition = (azimuth + Math.PI) / (2.0 * Math.PI) * width - 0.5;
        int columnFloor = (int) Math.floor(columnPosition);
        double columnFraction = columnPosition - columnFloor;
        int column = Math.floorMod(columnFloor, width);
        int columnNext = Math.floorMod(columnFloor + 1, width);
        int row = Math.min((int) rowPosition, height - 2);
        double rowFraction = rowPosition - row;
        double top = table[(column + row * width) * CHANNELS + channel] * (1.0 - columnFraction)
                + table[(columnNext + row * width) * CHANNELS + channel] * columnFraction;
        double bottom = table[(column + (row + 1) * width) * CHANNELS + channel] * (1.0 - columnFraction)
                + table[(columnNext + (row + 1) * width) * CHANNELS + channel] * columnFraction;
        return top * (1.0 - rowFraction) + bottom * rowFraction;
    }

    /**
     * Writes every channel of the table toward a direction into the result, with the same values as sample.
     */
    static void sampleChannels(float[] table, double azimuth, double elevation, double[] result) {
        int width = SkyTables.VIEW_WIDTH;
        int height = SkyTables.VIEW_HEIGHT;
        double rowPosition = rowPositionAt(elevation);
        double columnPosition = (azimuth + Math.PI) / (2.0 * Math.PI) * width - 0.5;
        int columnFloor = (int) Math.floor(columnPosition);
        double columnFraction = columnPosition - columnFloor;
        int column = Math.floorMod(columnFloor, width);
        int columnNext = Math.floorMod(columnFloor + 1, width);
        int row = Math.min((int) rowPosition, height - 2);
        double rowFraction = rowPosition - row;
        int topLeft = (column + row * width) * CHANNELS;
        int topRight = (columnNext + row * width) * CHANNELS;
        int bottomLeft = (column + (row + 1) * width) * CHANNELS;
        int bottomRight = (columnNext + (row + 1) * width) * CHANNELS;
        for (int channel = 0; channel < CHANNELS; channel++) {
            double top = table[topLeft + channel] * (1.0 - columnFraction) + table[topRight + channel] * columnFraction;
            double bottom = table[bottomLeft + channel] * (1.0 - columnFraction)
                    + table[bottomRight + channel] * columnFraction;
            result[channel] = top * (1.0 - rowFraction) + bottom * rowFraction;
        }
    }
}
