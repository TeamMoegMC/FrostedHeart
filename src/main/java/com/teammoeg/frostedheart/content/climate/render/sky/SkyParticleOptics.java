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
 * Mie scattering of titanium dioxide nanobots, averaged over a size distribution and three colour bands.
 */
public final class SkyParticleOptics {
    public static final double[] CHANNEL_CENTER_NANOMETERS = {680.0, 550.0, 440.0};
    private static final double CHANNEL_WIDTH_NANOMETERS = 25.0;
    private static final int CHANNEL_SAMPLE_COUNT = 9;
    private static final int DIAMETER_BIN_COUNT = 160;
    private static final double MINIMUM_DIAMETER_NANOMETERS = 50.0;
    private static final double MAXIMUM_DIAMETER_NANOMETERS = 1000.0;
    private static final double TITANIUM_DIOXIDE_DENSITY = 4200.0;
    private static final double EARTH_SURFACE_SQUARE_METERS = 5.1e14;

    public record Bands(double[] crossSectionSquareMeters, double[] asymmetry, double particleMassKilograms) {
        public double megatonsForUnitOpticalDepth() {
            double gramsPerSquareMeter = particleMassKilograms / crossSectionSquareMeters[SkyAtmosphere.GREEN_CHANNEL]
                    * 1000.0;
            return gramsPerSquareMeter * EARTH_SURFACE_SQUARE_METERS / 1.0e12;
        }
    }

    public record Efficiencies(double extinction, double scattering, double asymmetry) {
    }

    private SkyParticleOptics() {
    }

    public static Bands forDistribution(double medianNanometers, double spread) {
        double logarithmMinimum = Math.log(MINIMUM_DIAMETER_NANOMETERS);
        double logarithmMaximum = Math.log(MAXIMUM_DIAMETER_NANOMETERS);
        double logarithmStep = (logarithmMaximum - logarithmMinimum) / DIAMETER_BIN_COUNT;
        double[] diameters = new double[DIAMETER_BIN_COUNT];
        double[] weights = new double[DIAMETER_BIN_COUNT];
        double totalWeight = 0.0;
        for (int bin = 0; bin < DIAMETER_BIN_COUNT; bin++) {
            double logarithmDiameter = logarithmMinimum + (bin + 0.5) * logarithmStep;
            double deviation = (logarithmDiameter - Math.log(medianNanometers)) / Math.log(spread);
            diameters[bin] = Math.exp(logarithmDiameter);
            weights[bin] = Math.exp(-0.5 * deviation * deviation);
            totalWeight += weights[bin];
        }

        double meanVolume = 0.0;
        for (int bin = 0; bin < DIAMETER_BIN_COUNT; bin++) {
            weights[bin] /= totalWeight;
            double diameterMeters = diameters[bin] * 1.0e-9;
            meanVolume += weights[bin] * Math.PI / 6.0 * diameterMeters * diameterMeters * diameterMeters;
        }

        double[] crossSections = new double[SkyAtmosphere.CHANNEL_COUNT];
        double[] asymmetries = new double[SkyAtmosphere.CHANNEL_COUNT];
        for (int channel = 0; channel < SkyAtmosphere.CHANNEL_COUNT; channel++) {
            double bandCrossSection = 0.0;
            double bandAsymmetry = 0.0;
            double bandWeight = 0.0;
            for (int sample = 0; sample < CHANNEL_SAMPLE_COUNT; sample++) {
                double offset = -2.0 + 4.0 * sample / (CHANNEL_SAMPLE_COUNT - 1);
                double wavelength = CHANNEL_CENTER_NANOMETERS[channel] + offset * CHANNEL_WIDTH_NANOMETERS;
                double wavelengthWeight = Math.exp(-0.5 * offset * offset);
                double refractiveIndex = rutileRefractiveIndex(wavelength);
                for (int bin = 0; bin < DIAMETER_BIN_COUNT; bin++) {
                    double sizeParameter = Math.PI * diameters[bin] / wavelength;
                    Efficiencies efficiencies = mie(sizeParameter, refractiveIndex);
                    double diameterMeters = diameters[bin] * 1.0e-9;
                    double crossSection = efficiencies.scattering() * Math.PI / 4.0 * diameterMeters * diameterMeters;
                    double weighted = wavelengthWeight * weights[bin] * crossSection;
                    bandCrossSection += weighted;
                    bandAsymmetry += weighted * efficiencies.asymmetry();
                }
                bandWeight += wavelengthWeight;
            }
            crossSections[channel] = bandCrossSection / bandWeight;
            asymmetries[channel] = bandAsymmetry / bandCrossSection;
        }
        return new Bands(crossSections, asymmetries, meanVolume * TITANIUM_DIOXIDE_DENSITY);
    }

    public static double rutileRefractiveIndex(double wavelengthNanometers) {
        double wavelengthMicrometers = wavelengthNanometers / 1000.0;
        return Math.sqrt(5.913 + 0.2441 / (wavelengthMicrometers * wavelengthMicrometers - 0.0803));
    }

    public static Efficiencies mie(double sizeParameter, double refractiveIndex) {
        double scaledSize = refractiveIndex * sizeParameter;
        int termCount = (int) (sizeParameter + 4.0 * Math.cbrt(sizeParameter) + 2.0);
        int startOrder = (int) Math.max(termCount, Math.abs(scaledSize)) + 15;
        double[] logarithmicDerivative = new double[startOrder + 1];
        for (int order = startOrder; order > 0; order--) {
            logarithmicDerivative[order - 1] = order / scaledSize
                    - 1.0 / (logarithmicDerivative[order] + order / scaledSize);
        }

        double psiPrevious = Math.cos(sizeParameter);
        double psiCurrent = Math.sin(sizeParameter);
        double chiPrevious = -Math.sin(sizeParameter);
        double chiCurrent = Math.cos(sizeParameter);
        double scatteringSum = 0.0;
        double extinctionSum = 0.0;
        double asymmetrySum = 0.0;
        double previousAReal = 0.0;
        double previousAImaginary = 0.0;
        double previousBReal = 0.0;
        double previousBImaginary = 0.0;
        for (int order = 1; order <= termCount; order++) {
            double psiNext = (2 * order - 1) * psiCurrent / sizeParameter - psiPrevious;
            double chiNext = (2 * order - 1) * chiCurrent / sizeParameter - chiPrevious;
            double derivative = logarithmicDerivative[order];

            double factorA = derivative / refractiveIndex + order / sizeParameter;
            double numeratorA = factorA * psiNext - psiCurrent;
            double denominatorAReal = factorA * psiNext - psiCurrent;
            double denominatorAImaginary = -(factorA * chiNext - chiCurrent);
            double squaredA = denominatorAReal * denominatorAReal + denominatorAImaginary * denominatorAImaginary;
            double aReal = numeratorA * denominatorAReal / squaredA;
            double aImaginary = -numeratorA * denominatorAImaginary / squaredA;

            double factorB = derivative * refractiveIndex + order / sizeParameter;
            double numeratorB = factorB * psiNext - psiCurrent;
            double denominatorBReal = factorB * psiNext - psiCurrent;
            double denominatorBImaginary = -(factorB * chiNext - chiCurrent);
            double squaredB = denominatorBReal * denominatorBReal + denominatorBImaginary * denominatorBImaginary;
            double bReal = numeratorB * denominatorBReal / squaredB;
            double bImaginary = -numeratorB * denominatorBImaginary / squaredB;

            scatteringSum += (2 * order + 1) * (aReal * aReal + aImaginary * aImaginary + bReal * bReal
                    + bImaginary * bImaginary);
            extinctionSum += (2 * order + 1) * (aReal + bReal);
            asymmetrySum += (2 * order + 1) / (double) (order * (order + 1)) * (aReal * bReal + aImaginary * bImaginary);
            if (order > 1) {
                asymmetrySum += (order - 1) * (order + 1) / (double) order
                        * (previousAReal * aReal + previousAImaginary * aImaginary + previousBReal * bReal
                                + previousBImaginary * bImaginary);
            }

            previousAReal = aReal;
            previousAImaginary = aImaginary;
            previousBReal = bReal;
            previousBImaginary = bImaginary;
            psiPrevious = psiCurrent;
            psiCurrent = psiNext;
            chiPrevious = chiCurrent;
            chiCurrent = chiNext;
        }

        double scattering = 2.0 / (sizeParameter * sizeParameter) * scatteringSum;
        double extinction = 2.0 / (sizeParameter * sizeParameter) * extinctionSum;
        double asymmetry = 4.0 / (sizeParameter * sizeParameter * scattering) * asymmetrySum;
        return new Efficiencies(extinction, scattering, asymmetry);
    }
}
