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
 * Scattering and extinction coefficients by altitude, in 1/m, stored as altitude index times channel count plus channel.
 * Air, ozone, and ground aerosol use the Earth values from Hillaire 2020.
 * The totals hold ice delta scaled, without the share ICE_DIFFRACTION_FRACTION that diffraction sends on within a few
 * degrees of its direction, so that light counts as transmitted. The ice scattering holds all of it.
 * The diffuse totals keep only the share 1 - g of the delta scaled ice, where g is the asymmetry of the tumbling
 * crystals. This similarity scaling serves light that SkyTables scatters as if isotropically, because the share g goes
 * on roughly in its direction. Oriented crystals are taken to scatter like the tumbling ones.
 */
public final class SkyAtmosphere {
    public static final int CHANNEL_COUNT = 3;
    public static final int GREEN_CHANNEL = 1;
    public static final double GROUND_RADIUS = 6360000.0;
    public static final double TOP_RADIUS = 6460000.0;
    public static final double ATMOSPHERE_HEIGHT = TOP_RADIUS - GROUND_RADIUS;
    public static final double CAMERA_RADIUS = GROUND_RADIUS + 100.0;
    public static final double HORIZON_DIP_RADIANS = Math.acos(GROUND_RADIUS / CAMERA_RADIUS);
    public static final double ICE_DIFFRACTION_FRACTION = 0.5;
    public static final double ICE_DIFFRACTION_ASYMMETRY = 0.99;
    static final double GROUND_ALBEDO = 0.8;
    static final double AEROSOL_ASYMMETRY = 0.8;
    private static final double ALTITUDE_STEP = 50.0;
    private static final int ALTITUDE_SAMPLE_COUNT = (int) (ATMOSPHERE_HEIGHT / ALTITUDE_STEP) + 1;
    private static final double[] RAYLEIGH_COEFFICIENT = {5.802e-6, 13.558e-6, 33.1e-6};
    private static final double RAYLEIGH_SCALE_HEIGHT = 8000.0;
    private static final double AEROSOL_SCATTERING = 3.996e-6;
    private static final double AEROSOL_ABSORPTION = 4.40e-6;
    private static final double AEROSOL_SCALE_HEIGHT = 1200.0;
    private static final double[] OZONE_ABSORPTION = {0.650e-6, 1.881e-6, 0.085e-6};
    private static final double NANOBOT_CENTER_METERS = 25000.0;
    private static final double NANOBOT_WIDTH_METERS = 6000.0;
    private static final double ICE_CENTER_METERS = 11000.0;
    private static final double ICE_WIDTH_METERS = 1200.0;
    private static final double ICE_BAND_HALF_WIDTHS = 4.0;
    /**
     * Radii between which the ice layer holds all but a negligible share of its optical depth.
     */
    static final double ICE_BAND_LOW_RADIUS = GROUND_RADIUS + ICE_CENTER_METERS - ICE_BAND_HALF_WIDTHS * ICE_WIDTH_METERS;
    static final double ICE_BAND_HIGH_RADIUS = GROUND_RADIUS + ICE_CENTER_METERS + ICE_BAND_HALF_WIDTHS * ICE_WIDTH_METERS;

    final SkyParticleOptics.Bands nanobotBands;
    final double[] rayleighScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] aerosolScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] nanobotScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] iceScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] totalScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] totalExtinction = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] diffuseScattering = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];
    final double[] diffuseExtinction = new double[ALTITUDE_SAMPLE_COUNT * CHANNEL_COUNT];

    SkyAtmosphere(SkySettings settings) {
        this.nanobotBands = SkyParticleOptics.forDistribution(settings.nanobotMedianNanometers(),
                settings.nanobotSpread());
        double greenCrossSection = nanobotBands.crossSectionSquareMeters()[GREEN_CHANNEL];
        IceCrystalData crystals = IceCrystalData.get();
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            double nanobotRatio = nanobotBands.crossSectionSquareMeters()[channel] / greenCrossSection;
            double iceAsymmetry = crystals.tumblingAsymmetry(channel);
            for (int index = 0; index < ALTITUDE_SAMPLE_COUNT; index++) {
                double altitude = index * ALTITUDE_STEP;
                double rayleigh = RAYLEIGH_COEFFICIENT[channel] * Math.exp(-altitude / RAYLEIGH_SCALE_HEIGHT);
                double aerosolDensity = Math.exp(-altitude / AEROSOL_SCALE_HEIGHT);
                double ozone = OZONE_ABSORPTION[channel] * Math.max(0.0, 1.0 - Math.abs(altitude - 25000.0) / 15000.0);
                double nanobot = settings.nanobotOpticalDepth() * nanobotRatio
                        * gaussian(altitude, NANOBOT_CENTER_METERS, NANOBOT_WIDTH_METERS);
                double ice = settings.iceOpticalDepth() * gaussian(altitude, ICE_CENTER_METERS, ICE_WIDTH_METERS);
                double deltaScaledIce = (1.0 - ICE_DIFFRACTION_FRACTION) * ice;

                int slot = index * CHANNEL_COUNT + channel;
                rayleighScattering[slot] = rayleigh;
                aerosolScattering[slot] = AEROSOL_SCATTERING * aerosolDensity;
                nanobotScattering[slot] = nanobot;
                iceScattering[slot] = ice;
                totalScattering[slot] = rayleigh + AEROSOL_SCATTERING * aerosolDensity + nanobot + deltaScaledIce;
                totalExtinction[slot] = rayleigh + (AEROSOL_SCATTERING + AEROSOL_ABSORPTION) * aerosolDensity
                        + ozone + nanobot + deltaScaledIce;
                diffuseScattering[slot] = totalScattering[slot] - iceAsymmetry * deltaScaledIce;
                diffuseExtinction[slot] = totalExtinction[slot] - iceAsymmetry * deltaScaledIce;
            }
        }
    }

    private static double gaussian(double value, double center, double width) {
        double deviation = (value - center) / width;
        return Math.exp(-0.5 * deviation * deviation) / (width * Math.sqrt(2.0 * Math.PI));
    }

    int altitudeSlot(double radius) {
        int index = (int) ((radius - GROUND_RADIUS) / ALTITUDE_STEP + 0.5);
        if (index < 0) {
            return 0;
        }
        return Math.min(index, ALTITUDE_SAMPLE_COUNT - 1) * CHANNEL_COUNT;
    }

    static double distanceToTop(double radius, double cosine) {
        double discriminant = radius * radius * (cosine * cosine - 1.0) + TOP_RADIUS * TOP_RADIUS;
        return Math.max(0.0, -radius * cosine + Math.sqrt(Math.max(0.0, discriminant)));
    }

    static double distanceToGroundOrInfinity(double radius, double cosine) {
        if (cosine >= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double discriminant = radius * radius * (cosine * cosine - 1.0) + GROUND_RADIUS * GROUND_RADIUS;
        if (discriminant < 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0.0, -radius * cosine - Math.sqrt(discriminant));
    }

    static double radiusAlongRay(double radius, double cosine, double distance) {
        return Math.sqrt(distance * distance + 2.0 * radius * cosine * distance + radius * radius);
    }
}
